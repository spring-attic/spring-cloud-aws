/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging.listener;

import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.cloud.aws.messaging.core.QueueMessageUtils.createMessage;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer {

    private static final String DEFAULT_THREAD_NAME_PREFIX =
            ClassUtils.getShortName(SimpleMessageListenerContainer.class) + "-";

    private boolean defaultTaskExecutor;
    private long backOffTime = 10000;
    private long queueStopTimeout = 10000;

    private AsyncTaskExecutor taskExecutor;
    private ConcurrentHashMap<String, Future<?>> scheduledFutureByQueue;
    private ConcurrentHashMap<String, Boolean> runningStateByQueue;

    protected AsyncTaskExecutor getTaskExecutor() {
        return this.taskExecutor;
    }

    public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * @return The number of milliseconds the polling thread must wait before trying to recover when an error occurs
     * (e.g. connection timeout)
     */
    public long getBackOffTime() {
        return this.backOffTime;
    }

    /**
     * The number of milliseconds the polling thread must wait before trying to recover when an error occurs
     * (e.g. connection timeout). Default is 10000 milliseconds.
     *
     * @param backOffTime
     *         in milliseconds
     */
    public void setBackOffTime(long backOffTime) {
        this.backOffTime = backOffTime;
    }

    /**
     * @return The number of milliseconds the {@link SimpleMessageListenerContainer#stop(String)} method waits for a queue
     * to stop before interrupting the current thread. Default value is 10000 milliseconds (10 seconds).
     */
    public long getQueueStopTimeout() {
        return this.queueStopTimeout;
    }

    /**
     * The number of milliseconds the {@link SimpleMessageListenerContainer#stop(String)} method waits for a queue
     * to stop before interrupting the current thread. Default value is 10000 milliseconds (10 seconds).
     *
     * @param queueStopTimeout
     *         in milliseconds
     */
    public void setQueueStopTimeout(long queueStopTimeout) {
        this.queueStopTimeout = queueStopTimeout;
    }

    @Override
    protected void initialize() {
        super.initialize();

        if (this.taskExecutor == null) {
            this.defaultTaskExecutor = true;
            this.taskExecutor = createDefaultTaskExecutor();
        }

        initializeRunningStateByQueue();
        this.scheduledFutureByQueue = new ConcurrentHashMap<>(getRegisteredQueues().size());
    }

    private void initializeRunningStateByQueue() {
        this.runningStateByQueue = new ConcurrentHashMap<>(getRegisteredQueues().size());
        for (String queueName : getRegisteredQueues().keySet()) {
            this.runningStateByQueue.put(queueName, false);
        }
    }

    @Override
    protected void doStart() {
        synchronized (this.getLifecycleMonitor()) {
            scheduleMessageListeners();
        }
    }

    @Override
    protected void doStop() {
        notifyRunningQueuesToStop();
        waitForRunningQueuesToStop();
    }

    private void notifyRunningQueuesToStop() {
        for (Map.Entry<String, Boolean> runningStateByQueue : this.runningStateByQueue.entrySet()) {
            if (runningStateByQueue.getValue()) {
                stopQueue(runningStateByQueue.getKey());
            }
        }
    }

    private void waitForRunningQueuesToStop() {
        for (Map.Entry<String, Boolean> queueRunningState : this.runningStateByQueue.entrySet()) {
            String logicalQueueName = queueRunningState.getKey();
            Future<?> queueSpinningThread = this.scheduledFutureByQueue.get(logicalQueueName);

            if (queueSpinningThread != null) {
                try {
                    queueSpinningThread.get(getQueueStopTimeout(), TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException e) {
                    getLogger().warn("An exception occurred while stopping queue '" + logicalQueueName + "'", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    protected void doDestroy() {
        if (this.defaultTaskExecutor) {
            ((ThreadPoolTaskExecutor) this.taskExecutor).destroy();
        }
    }

    /**
     * Create a default TaskExecutor. Called if no explicit TaskExecutor has been specified.
     * <p>The default implementation builds a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
     * with the specified bean name (or the class name, if no bean name specified) as thread name prefix.
     *
     * @return a {@link org.springframework.core.task.SimpleAsyncTaskExecutor} configured with the thread name prefix
     * @see org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
     */
    protected AsyncTaskExecutor createDefaultTaskExecutor() {
        String beanName = getBeanName();
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix(beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
        int spinningThreads = this.getRegisteredQueues().size();

        if (spinningThreads > 0) {
            int poolSize = 0;
            int bufferSize = 0;
            for (QueueAttributes queueAttributes : this.getRegisteredQueues().values()) {
                int queueMaxNumberOfMessages = queueAttributes.getMaxNumberOfMessages();
                // Each queue needs 1 polling thread plus n handler threads
                // where n is determined by the queue concurrency or batch size
                poolSize += 1 + (queueAttributes.getMaxConcurrency() != null
                                 ? queueAttributes.getMaxConcurrency()
                                 : queueMaxNumberOfMessages);
                bufferSize += queueMaxNumberOfMessages;
            }
            threadPoolTaskExecutor.setCorePoolSize(poolSize);
            threadPoolTaskExecutor.setMaxPoolSize(poolSize);
            // Ideally we would like to set the queue capacity to 0 to avoid
            // messages waiting too long in memory, but due to a race condition
            // we may encounter a transitional state where a task finishes
            // executing but the thread is not ready to accept a new task for a
            // few milliseconds, resulting in rejected task exceptions. To
            // prevent this issue we allow a small amount of buffering.
            threadPoolTaskExecutor.setQueueCapacity(bufferSize);
            threadPoolTaskExecutor.setAllowCoreThreadTimeOut(true);
        }
        threadPoolTaskExecutor.afterPropertiesSet();

        return threadPoolTaskExecutor;

    }

    private void scheduleMessageListeners() {
        for (Map.Entry<String, QueueAttributes> registeredQueue : getRegisteredQueues().entrySet()) {
            startQueue(registeredQueue.getKey(), registeredQueue.getValue());
        }
    }

    protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
        getMessageHandler().handleMessage(stringMessage);
    }

    /**
     * Stops and waits until the specified queue has stopped. If the wait timeout specified by {@link SimpleMessageListenerContainer#getQueueStopTimeout()}
     * is reached, the current thread is interrupted.
     *
     * @param logicalQueueName
     *         the name as defined on the listener method
     */
    public void stop(String logicalQueueName) {
        stopQueue(logicalQueueName);

        try {
            if (isRunning(logicalQueueName)) {
                Future<?> future = this.scheduledFutureByQueue.remove(logicalQueueName);
                if (future != null) {
                    future.get(this.queueStopTimeout, TimeUnit.MILLISECONDS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            getLogger().warn("Error stopping queue with name: '" + logicalQueueName + "'", e);
        }
    }

    protected void stopQueue(String logicalQueueName) {
        Assert.isTrue(this.runningStateByQueue.containsKey(logicalQueueName), "Queue with name '" + logicalQueueName + "' does not exist");
        this.runningStateByQueue.put(logicalQueueName, false);
    }

    public void start(String logicalQueueName) {
        Assert.isTrue(this.runningStateByQueue.containsKey(logicalQueueName), "Queue with name '" + logicalQueueName + "' does not exist");

        QueueAttributes queueAttributes = this.getRegisteredQueues().get(logicalQueueName);
        startQueue(logicalQueueName, queueAttributes);
    }

    /**
     * Checks if the spinning thread for the specified queue {@code logicalQueueName} is still running (polling for new
     * messages) or not.
     *
     * @param logicalQueueName
     *         the name as defined on the listener method
     * @return {@code true} if the spinning thread for the specified queue is running otherwise {@code false}.
     */
    public boolean isRunning(String logicalQueueName) {
        Future<?> future = this.scheduledFutureByQueue.get(logicalQueueName);
        return future != null && !future.isCancelled() && !future.isDone();
    }

    protected void startQueue(String queueName, QueueAttributes queueAttributes) {
        if (this.runningStateByQueue.containsKey(queueName) && this.runningStateByQueue.get(queueName)) {
            return;
        }

        this.runningStateByQueue.put(queueName, true);
        Future<?> future = getTaskExecutor().submit(new AsynchronousMessageListener(queueName, queueAttributes));
        this.scheduledFutureByQueue.put(queueName, future);
    }

    private class AsynchronousMessageListener implements Runnable {

        private final QueueAttributes queueAttributes;
        private final String logicalQueueName;

        private AsynchronousMessageListener(String logicalQueueName, QueueAttributes queueAttributes) {
            this.logicalQueueName = logicalQueueName;
            this.queueAttributes = queueAttributes;
        }

        @Override
        public void run() {
            final int maxMessages = queueAttributes.getMaxNumberOfMessages();
            final int maxConcurrency = queueAttributes.getMaxConcurrency() != null ? queueAttributes.getMaxConcurrency() : maxMessages;
            // Semaphore used to limit the number of messages being handled concurrently.
            final Semaphore semaphore = new Semaphore(maxConcurrency);
            while (isQueueRunning()) {
                // How many semaphore permits this thread currently holds.
                int currentPermits = 0;
                try {
                    // Wait for sufficient threads available before requesting
                    // additional messages.
                    currentPermits += acquirePermits(semaphore, Math.min(maxConcurrency, maxMessages));
                    if (!isQueueRunning()) {
                        break;
                    }

                    ReceiveMessageResult receiveMessageResult = getAmazonSqs().receiveMessage(this.queueAttributes.getReceiveMessageRequest());
                    for (Message message : receiveMessageResult.getMessages()) {
                        // If maxConcurrency < maxMessages we might have more
                        // messages than threads available. In that case we
                        // wait to acquire a worker thread permit before
                        // submitting each message to the task executor.
                        if (currentPermits == 0) {
                            currentPermits += acquirePermits(semaphore, 1);
                        }
                        if (isQueueRunning()) {
                            MessageExecutor messageExecutor = new MessageExecutor(this.logicalQueueName, message, this.queueAttributes);
                            if (currentPermits > 0) {
                                getTaskExecutor().execute(new SignalExecutingRunnable(semaphore, messageExecutor));
                                // After the task is submitted to the executor,
                                // it's the SignalExecutingRunnable's job to
                                // release the permit, so we can decrement our
                                // permit count for this thread.
                                currentPermits -= 1;
                            } else {
                                // We failed to acquire a permit due to being
                                // interrupted. We don't want the worker to
                                // release a permit that wasn't acquired, so
                                // don't use SignalExecutingRunnable.
                                getTaskExecutor().execute(messageExecutor);
                            }
                        }
                    }
                } catch (Exception e) {
                    getLogger().warn("An Exception occurred while polling queue '{}'. The failing operation will be " +
                            "retried in {} milliseconds", this.logicalQueueName, getBackOffTime(), e);
                    try {
                        //noinspection BusyWait
                        Thread.sleep(getBackOffTime());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    semaphore.release(currentPermits);
                }
            }

            // Wait for all tasks to complete before terminating
            acquirePermits(semaphore, maxConcurrency);

            SimpleMessageListenerContainer.this.scheduledFutureByQueue.remove(this.logicalQueueName);
        }

        private boolean isQueueRunning() {
            if (SimpleMessageListenerContainer.this.runningStateByQueue.containsKey(this.logicalQueueName)) {
                return SimpleMessageListenerContainer.this.runningStateByQueue.get(this.logicalQueueName);
            } else {
                getLogger().warn("Stopped queue '" + this.logicalQueueName + "' because it was not listed as running queue.");
                return false;
            }
        }

        private int acquirePermits(Semaphore semaphore, int permits) {
            try {
                semaphore.acquire(permits);
                return permits;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0;
            }
        }
    }

    private class MessageExecutor implements Runnable {

        private final Message message;
        private final String logicalQueueName;
        private final String queueUrl;
        private final boolean hasRedrivePolicy;
        private final SqsMessageDeletionPolicy deletionPolicy;

        private MessageExecutor(String logicalQueueName, Message message, QueueAttributes queueAttributes) {
            this.logicalQueueName = logicalQueueName;
            this.message = message;
            this.queueUrl = queueAttributes.getReceiveMessageRequest().getQueueUrl();
            this.hasRedrivePolicy = queueAttributes.hasRedrivePolicy();
            this.deletionPolicy = queueAttributes.getDeletionPolicy();
        }

        @Override
        public void run() {
            String receiptHandle = this.message.getReceiptHandle();
            org.springframework.messaging.Message<String> queueMessage = getMessageForExecution();
            try {
                executeMessage(queueMessage);
                applyDeletionPolicyOnSuccess(receiptHandle);
            } catch (MessagingException messagingException) {
                applyDeletionPolicyOnError(receiptHandle, messagingException);
            }
        }

        private void applyDeletionPolicyOnSuccess(String receiptHandle) {
            if (this.deletionPolicy == SqsMessageDeletionPolicy.ON_SUCCESS ||
                    this.deletionPolicy == SqsMessageDeletionPolicy.ALWAYS ||
                    this.deletionPolicy == SqsMessageDeletionPolicy.NO_REDRIVE) {
                deleteMessage(receiptHandle);
            }
        }

        private void applyDeletionPolicyOnError(String receiptHandle, MessagingException messagingException) {
            if (this.deletionPolicy == SqsMessageDeletionPolicy.ALWAYS ||
                    (this.deletionPolicy == SqsMessageDeletionPolicy.NO_REDRIVE && !this.hasRedrivePolicy)) {
                deleteMessage(receiptHandle);
            } else if (this.deletionPolicy == SqsMessageDeletionPolicy.ON_SUCCESS) {
                getLogger().error("Exception encountered while processing message.", messagingException);
            }
        }

        private void deleteMessage(String receiptHandle) {
            getAmazonSqs().deleteMessageAsync(new DeleteMessageRequest(this.queueUrl, receiptHandle));
        }

        private org.springframework.messaging.Message<String> getMessageForExecution() {
            HashMap<String, Object> additionalHeaders = new HashMap<>();
            additionalHeaders.put(QueueMessageHandler.LOGICAL_RESOURCE_ID, this.logicalQueueName);
            if (this.deletionPolicy == SqsMessageDeletionPolicy.NEVER) {
                String receiptHandle = this.message.getReceiptHandle();
                QueueMessageAcknowledgment acknowledgment = new QueueMessageAcknowledgment(SimpleMessageListenerContainer.this.getAmazonSqs(), this.queueUrl, receiptHandle);
                additionalHeaders.put(QueueMessageHandler.ACKNOWLEDGMENT, acknowledgment);
            }
            additionalHeaders.put(QueueMessageHandler.VISIBILITY, new QueueMessageVisibility(SimpleMessageListenerContainer.this.getAmazonSqs(), this.queueUrl, this.message.getReceiptHandle()));

            return createMessage(this.message, additionalHeaders);
        }
    }

    private static class SignalExecutingRunnable implements Runnable {

        private final Semaphore semaphore;
        private final Runnable runnable;

        private SignalExecutingRunnable(Semaphore semaphore, Runnable runnable) {
            this.semaphore = semaphore;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                this.runnable.run();
            } finally {
                this.semaphore.release();
            }
        }
    }
}
