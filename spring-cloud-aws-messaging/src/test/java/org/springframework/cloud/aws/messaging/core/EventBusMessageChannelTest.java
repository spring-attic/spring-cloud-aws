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

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.PutEventsRequest;
import com.amazonaws.services.cloudwatchevents.model.PutEventsRequestEntry;
import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * @author Jakub Narloch
 */
public class EventBusMessageChannelTest {

	private Message<String> message;

	@Before
	public void setUp() throws Exception {
		message = MessageBuilder.withPayload("Message content")
				.setHeader(EventBusMessageChannel.EVENT_SOURCE_HEADER, "custom")
				.setHeader(EventBusMessageChannel.EVENT_DETAIL_TYPE_HEADER, "My Event")
				.build();
	}

	@Test
	public void sendMessage_validTextMessageAndSubject_returnsTrue() throws Exception {
		// Arrange
		AmazonCloudWatchEvents amazonEvents = mock(AmazonCloudWatchEvents.class);

		MessageChannel messageChannel = new EventBusMessageChannel(amazonEvents,
				"default");

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		verify(amazonEvents, only())
				.putEvents(new PutEventsRequest().withEntries(new PutEventsRequestEntry()
						.withEventBusName("default").withSource("custom")
						.withDetailType("My Event").withDetail(message.getPayload())));
		assertThat(sent).isTrue();
	}

	@Test
	public void sendMessage_validTextMessageAndTimeout_timeoutIsIgnored()
			throws Exception {
		// Arrange
		AmazonCloudWatchEvents amazonEvents = mock(AmazonCloudWatchEvents.class);
		MessageChannel messageChannel = new EventBusMessageChannel(amazonEvents,
				"default");

		// Act
		boolean sent = messageChannel.send(message, 10);

		// Assert
		verify(amazonEvents, only())
				.putEvents(new PutEventsRequest().withEntries(new PutEventsRequestEntry()
						.withEventBusName("default").withSource("custom")
						.withDetailType("My Event").withDetail(message.getPayload())));
		assertThat(sent).isTrue();
	}

}
