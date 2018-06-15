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
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import org.assertj.core.util.Sets;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SqsListenerHealthIndicator}.
 *
 * @author Maciej Walkowiak
 * @since 2.0
 */
public class SqsListenerHealthIndicatorTest {

    private final SimpleMessageListenerContainer simpleMessageListenerContainer = mock(SimpleMessageListenerContainer.class);
    private final AmazonSQS amazonSQS = mock(AmazonSQS.class);

    private final SqsListenerHealthIndicator healthIndicator = new SqsListenerHealthIndicator(simpleMessageListenerContainer, amazonSQS);

    @Test
    public void reportsTrueWhenAllConfiguredQueuesAreRunning() {
        when(simpleMessageListenerContainer.getConfiguredQueueNames()).thenReturn(Sets.newTreeSet("queue1", "queue2"));
        when(simpleMessageListenerContainer.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl("http://queue.url"));
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    public void reportsTrueNoQueuesAreConfigured() {
        when(simpleMessageListenerContainer.getConfiguredQueueNames()).thenReturn(Sets.newTreeSet());
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    public void reportsFalseIfAtLeastOneConfiguredQueueIsNotRunning() {
        when(simpleMessageListenerContainer.getConfiguredQueueNames()).thenReturn(Sets.newTreeSet("queue1", "queue2"));
        when(amazonSQS.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl("http://queue.url"));
        when(simpleMessageListenerContainer.isRunning("queue1")).thenReturn(true);
        when(simpleMessageListenerContainer.isRunning("queue2")).thenReturn(false);
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("queue2"));
    }

    @Test
    public void reportsFalseIfAtLeastOneConfiguredQueueDoesNotExist() {
        when(simpleMessageListenerContainer.getConfiguredQueueNames()).thenReturn(Sets.newTreeSet("queue1", "queue2"));
        when(simpleMessageListenerContainer.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenThrow(QueueDoesNotExistException.class);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("queue1"));
    }

    @Test
    public void reportsFalseIfAtLeastOneConfiguredQueueIsNotReachable() {
        when(simpleMessageListenerContainer.getConfiguredQueueNames()).thenReturn(Sets.newTreeSet("queue1", "queue2"));
        when(simpleMessageListenerContainer.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenThrow(SdkClientException.class);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("queue1"));
    }

}
