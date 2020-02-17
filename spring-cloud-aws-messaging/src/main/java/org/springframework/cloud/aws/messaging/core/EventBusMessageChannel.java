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

import java.util.Optional;

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.model.PutEventsRequest;
import com.amazonaws.services.cloudwatchevents.model.PutEventsRequestEntry;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.AbstractMessageChannel;

/**
 * @author Jakub Narloch
 * @since 2.3.0
 */
public class EventBusMessageChannel extends AbstractMessageChannel {

	/**
	 * The 'source' message header.
	 */
	public static final String EVENT_SOURCE_HEADER = "EVENT_SOURCE_HEADER";

	/**
	 * The 'detail-type' message header.
	 */
	public static final String EVENT_DETAIL_TYPE_HEADER = "EVENT_DETAIL_TYPE_HEADER";

	private final AmazonCloudWatchEvents amazonEvents;

	private final String eventBus;

	public EventBusMessageChannel(AmazonCloudWatchEvents amazonEvents, String eventBus) {
		this.amazonEvents = amazonEvents;
		this.eventBus = eventBus;
	}

	@Override
	protected boolean sendInternal(Message<?> message, long timeout) {
		PutEventsRequestEntry entry = new PutEventsRequestEntry()
				.withEventBusName(eventBus).withSource(findEventSource(message))
				.withDetailType(findEventDetailType(message))
				.withDetail(message.getPayload().toString());
		amazonEvents.putEvents(new PutEventsRequest().withEntries(entry));
		return true;
	}

	private static String findEventSource(Message<?> message) {
		return findHeaderValue(message, EVENT_SOURCE_HEADER);
	}

	private static String findEventDetailType(Message<?> message) {
		return findHeaderValue(message, EVENT_DETAIL_TYPE_HEADER);
	}

	private static String findHeaderValue(Message<?> message, String header) {
		return Optional.ofNullable(message.getHeaders().get(header)).map(Object::toString)
				.orElse(null);
	}

}
