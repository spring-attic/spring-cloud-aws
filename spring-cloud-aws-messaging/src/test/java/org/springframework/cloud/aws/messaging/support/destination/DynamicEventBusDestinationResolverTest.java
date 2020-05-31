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

package org.springframework.cloud.aws.messaging.support.destination;

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.CreateEventBusRequest;
import com.amazonaws.services.cloudwatchevents.model.CreateEventBusResult;
import com.amazonaws.services.cloudwatchevents.model.EventBus;
import com.amazonaws.services.cloudwatchevents.model.ListEventBusesRequest;
import com.amazonaws.services.cloudwatchevents.model.ListEventBusesResult;
import org.junit.Test;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicEventBusDestinationResolverTest {

	@Test
	public void resolveDestination_withAlreadyExistingArn_returnsArnWithoutValidatingIt()
			throws Exception {
		// Arrange
		String eventBusArn = "arn:aws:events:us-east-1:123456789012:event-bus/test";
		String eventBusName = "test";

		AmazonCloudWatchEvents amazonEvents = mock(AmazonCloudWatchEvents.class);
		DynamicEventBusDestinationResolver resolver = new DynamicEventBusDestinationResolver(
				amazonEvents);

		// Act
		String resolvedDestinationName = resolver.resolveDestination(eventBusArn);

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(eventBusName);
	}

	@Test
	public void resolveDestination_withAutoCreateEnabled_shouldCreateEventBusDirectly()
			throws Exception {
		// Arrange
		String eventBusArn = "arn:aws:events:us-east-1:123456789012:event-bus/test";
		String eventBusName = "test";

		AmazonCloudWatchEvents amazonEvents = mock(AmazonCloudWatchEvents.class);
		when(amazonEvents.createEventBus(new CreateEventBusRequest().withName("test")))
				.thenReturn(new CreateEventBusResult().withEventBusArn(eventBusArn));

		DynamicEventBusDestinationResolver resolver = new DynamicEventBusDestinationResolver(
				amazonEvents);
		resolver.setAutoCreate(true);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(eventBusName);
	}

	@Test
	public void resolveDestination_withResourceIdResolver_shouldCallIt()
			throws Exception {
		// Arrange
		String eventBusArn = "arn:aws:events:us-east-1:123456789012:event-bus/test";
		String eventBusName = "test";

		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(eventBusName))
				.thenReturn(eventBusArn);

		AmazonCloudWatchEvents amazonEvents = mock(AmazonCloudWatchEvents.class);
		when(amazonEvents.listEventBuses(new ListEventBusesRequest()))
				.thenReturn(new ListEventBusesResult().withEventBuses(
						new EventBus().withName(eventBusName).withArn(eventBusArn)));

		DynamicEventBusDestinationResolver resolver = new DynamicEventBusDestinationResolver(
				amazonEvents, resourceIdResolver);

		// Assert
		String resolvedDestinationName = resolver.resolveDestination(eventBusName);

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(eventBusName);
	}

}
