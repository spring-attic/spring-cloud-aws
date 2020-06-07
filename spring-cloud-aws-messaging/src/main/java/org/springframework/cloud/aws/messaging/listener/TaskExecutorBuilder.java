/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.messaging.listener;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * @author Kevin Gath
 * @since 1.0
 */
public class TaskExecutorBuilder {

	private static final int WORKER_THREADS = 2;

	private int queueCapacity;

	private int maxPoolSize;

	private int corePoolSize;

	private String threadNamePrefix;

	/**
	 * Initialized the member variables with default values.
	 */
	public TaskExecutorBuilder() {
		// No use of a thread pool executor queue to avoid retaining message to long in
		// memory
		this.queueCapacity = 0;

		this.maxPoolSize = 1;
		this.corePoolSize = 1;
		this.threadNamePrefix = "thread";
	}

	/**
	 * Initialized the member variables with an existing {@ThreadPoolTaskExecutor}.
	 */
	public TaskExecutorBuilder(ThreadPoolTaskExecutor taskExecutor) {

		this();
		this.maxPoolSize = taskExecutor.getMaxPoolSize();
		this.corePoolSize = taskExecutor.getCorePoolSize();
		this.threadNamePrefix = taskExecutor.getThreadNamePrefix();
	}

	/**
	 * Set the member variables with input form
	 * {@link org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer}.
	 * So the behavior before this class is not changed.
	 * @param messageListenerContainer implementation to extract information to calculate
	 * pool size information
	 * @param threadNamePrefix which is required and constructed by the
	 * _messageListenerContainer_
	 * @return The instance of this builder to manipulate the values
	 */
	public TaskExecutorBuilder fromMessageListenerContainer(
			AbstractMessageListenerContainer messageListenerContainer,
			String threadNamePrefix) {
		int spinningThreads = messageListenerContainer.getRegisteredQueues().size();
		int maxNumberOfMessagePerBatch = messageListenerContainer
				.getMaxNumberOfMessages() != null
						? messageListenerContainer.getMaxNumberOfMessages()
						: AbstractMessageListenerContainer.DEFAULT_MAX_NUMBER_OF_MESSAGES;

		if (spinningThreads > 0) {
			this.maxPoolSize = spinningThreads * (maxNumberOfMessagePerBatch + 1);
			this.corePoolSize = spinningThreads * WORKER_THREADS;
		}
		this.threadNamePrefix = threadNamePrefix;

		return this;
	}

	public TaskExecutorBuilder withQueueCapacity(int queueCapacity) {
		Assert.state(queueCapacity >= 0, "The queue capacity should not be negative");
		this.queueCapacity = queueCapacity;
		return this;
	}

	public TaskExecutorBuilder withSize(int corePoolSize, int maxPoolSize) {
		Assert.state(corePoolSize >= 1, "The max core size has to be at least one");
		Assert.state(maxPoolSize >= corePoolSize,
				"The max pool size must be greater or equals than the core pool size");
		this.maxPoolSize = maxPoolSize;
		this.corePoolSize = corePoolSize;
		return this;
	}

	public TaskExecutorBuilder withThreadNamePrefix(String threadNamePrefix) {
		Assert.notNull(threadNamePrefix, "The prefix has not to be null");
		this.threadNamePrefix = threadNamePrefix;
		return this;
	}

	public AsyncTaskExecutor build() {
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setThreadNamePrefix(threadNamePrefix);
		threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
		threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
		threadPoolTaskExecutor.setQueueCapacity(queueCapacity);
		threadPoolTaskExecutor.afterPropertiesSet();
		return threadPoolTaskExecutor;
	}

}
