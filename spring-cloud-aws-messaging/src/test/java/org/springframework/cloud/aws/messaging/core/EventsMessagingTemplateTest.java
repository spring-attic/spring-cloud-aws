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

package org.springframework.cloud.aws.messaging.core;

import java.util.Locale;

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.EventBus;
import com.amazonaws.services.cloudwatchevents.model.ListEventBusesRequest;
import com.amazonaws.services.cloudwatchevents.model.ListEventBusesResult;
import com.amazonaws.services.cloudwatchevents.model.PutEventsRequest;
import com.amazonaws.services.cloudwatchevents.model.PutEventsRequestEntry;
import org.junit.Test;

import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.MessageBuilder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventsMessagingTemplateTest {

	@Test
	public void send_validTextMessage_usesEventBusChannel() throws Exception {
		// Arrange
		AmazonCloudWatchEvents amazonEvents = mock(AmazonCloudWatchEvents.class);
		EventsMessagingTemplate eventsMessagingTemplate = new EventsMessagingTemplate(
				amazonEvents);
		String physicalEventBusName = "arn:aws:events:us-east-1:123456789012:event-bus/default";
		when(amazonEvents.listEventBuses(new ListEventBusesRequest()))
				.thenReturn(new ListEventBusesResult().withEventBuses(new EventBus()
						.withName("default").withArn(physicalEventBusName)));
		eventsMessagingTemplate.setDefaultDestinationName("default");

		// Act
		eventsMessagingTemplate.send(MessageBuilder.withPayload("Message content")
				.setHeader(EventBusMessageChannel.EVENT_SOURCE_HEADER, "custom")
				.setHeader(EventBusMessageChannel.EVENT_DETAIL_TYPE_HEADER, "My Event")
				.build());

		// Assert
		verify(amazonEvents)
				.putEvents(new PutEventsRequest().withEntries(new PutEventsRequestEntry()
						.withEventBusName("default").withSource("custom")
						.withDetailType("My Event").withDetail("Message content")));
	}

	@Test
	public void send_validTextMessageWithCustomDestinationResolver_usesEventBusChannel()
			throws Exception {
		// Arrange
		AmazonCloudWatchEvents amazonEvents = mock(AmazonCloudWatchEvents.class);
		EventsMessagingTemplate eventsMessagingTemplate = new EventsMessagingTemplate(
				amazonEvents,
				(DestinationResolver<String>) name -> name.toUpperCase(Locale.ENGLISH),
				null);

		// Act
		eventsMessagingTemplate.send("test", MessageBuilder.withPayload("Message content")
				.setHeader(EventBusMessageChannel.EVENT_SOURCE_HEADER, "custom")
				.setHeader(EventBusMessageChannel.EVENT_DETAIL_TYPE_HEADER, "My Event")
				.build());

		// Assert
		verify(amazonEvents).putEvents(new PutEventsRequest().withEntries(
				new PutEventsRequestEntry().withEventBusName("TEST").withSource("custom")
						.withDetailType("My Event").withDetail("Message content")));
	}

	@Test
	public void convertAndSend_withDestinationPayloadAndSubject_shouldSetSourceAndDetailType()
			throws Exception {
		// Arrange
		AmazonCloudWatchEvents amazonEvents = mock(AmazonCloudWatchEvents.class);
		EventsMessagingTemplate eventsMessagingTemplate = new EventsMessagingTemplate(
				amazonEvents);
		String physicalEventBusName = "arn:aws:events:us-east-1:123456789012:event-bus/default";
		when(amazonEvents.listEventBuses(new ListEventBusesRequest()))
				.thenReturn(new ListEventBusesResult().withEventBuses(new EventBus()
						.withName("default").withArn(physicalEventBusName)));

		// Act
		eventsMessagingTemplate.sendEvent(physicalEventBusName, "custom", "My Event",
				"Message content");

		// Assert
		verify(amazonEvents)
				.putEvents(new PutEventsRequest().withEntries(new PutEventsRequestEntry()
						.withEventBusName("default").withSource("custom")
						.withDetailType("My Event").withDetail("Message content")));
	}

	@Test
	public void convertAndSend_withPayloadAndSubject_shouldSetSourceAndDetailType()
			throws Exception {
		// Arrange
		AmazonCloudWatchEvents amazonEvents = mock(AmazonCloudWatchEvents.class);
		EventsMessagingTemplate eventsMessagingTemplate = new EventsMessagingTemplate(
				amazonEvents);
		String physicalEventBusName = "arn:aws:events:us-east-1:123456789012:event-bus/default";
		eventsMessagingTemplate.setDefaultDestinationName(physicalEventBusName);

		// Act
		eventsMessagingTemplate.sendEvent("custom", "My Event", "Message content");

		// Assert
		verify(amazonEvents)
				.putEvents(new PutEventsRequest().withEntries(new PutEventsRequestEntry()
						.withEventBusName("default").withSource("custom")
						.withDetailType("My Event").withDetail("Message content")));
	}

}
