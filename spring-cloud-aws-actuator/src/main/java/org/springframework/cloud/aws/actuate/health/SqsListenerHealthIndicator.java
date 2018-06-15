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

package org.springframework.cloud.aws.actuate.health;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for the
 * SQS messaging system.
 *
 * Checks if all {@link SimpleMessageListenerContainer} for all configured queues are in running state
 * and if all configured queues exist and are reachable over the network.
 *
 * @author Maciej Walkowiak
 * @since 2.0
 */
public class SqsListenerHealthIndicator extends AbstractHealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsListenerHealthIndicator.class);

    private final SimpleMessageListenerContainer simpleMessageListenerContainer;
    private final AmazonSQS amazonSQS;

    public SqsListenerHealthIndicator(SimpleMessageListenerContainer simpleMessageListenerContainer, AmazonSQS amazonSQS) {
        Assert.notNull(simpleMessageListenerContainer, "SimpleMessageListenerContainer must not be null");
        Assert.notNull(amazonSQS, "AmazonSQS must not be null");
        this.simpleMessageListenerContainer = simpleMessageListenerContainer;
        this.amazonSQS = amazonSQS;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean allListenersRunning = true;
        for (String queueName : this.simpleMessageListenerContainer.getConfiguredQueueNames()) {
            if (!this.simpleMessageListenerContainer.isRunning(queueName)) {
                builder.down().withDetail(queueName, "listener is not running");
                allListenersRunning = false;
            }

            if (!isQueueReachable(queueName)) {
                builder.down().withDetail(queueName, "queue is not reachable");
                allListenersRunning = false;
            }
        }
        if (allListenersRunning) {
            builder.up();
        }
    }

    /**
     * Checks if queue exists and is reachable over the network.
     *
     * @param queueName -  SQS queue name
     * @return <code>true</code> if queue exists and is reachable
     *         <code>false</code> otherwise
     */
    private boolean isQueueReachable(String queueName) {
        try {
            amazonSQS.getQueueUrl(queueName);
            return true;
        } catch (QueueDoesNotExistException e) {
            LOGGER.warn("Queue '{}' does not exist", queueName);
            return false;
        } catch (SdkClientException e) {
            LOGGER.error("Queue '{}' is not reachable", queueName, e);
            return false;
        }

    }
}
