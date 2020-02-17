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

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate;
import org.springframework.cloud.aws.messaging.support.destination.DynamicEventBusDestinationResolver;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.DestinationResolver;

/**
 * @author Jakub Narloch
 * @since 2.3.0
 */
public class EventsMessagingTemplate
		extends AbstractMessageChannelMessagingSendingTemplate<EventBusMessageChannel> {

	private final AmazonCloudWatchEvents amazonEvents;

	public EventsMessagingTemplate(AmazonCloudWatchEvents amazonEvents) {
		this(amazonEvents, (ResourceIdResolver) null, null);
	}

	public EventsMessagingTemplate(AmazonCloudWatchEvents amazonEvents,
			ResourceIdResolver resourceIdResolver, MessageConverter messageConverter) {
		super(new DynamicEventBusDestinationResolver(amazonEvents, resourceIdResolver));
		this.amazonEvents = amazonEvents;
		initMessageConverter(messageConverter);
	}

	public EventsMessagingTemplate(AmazonCloudWatchEvents amazonEvents,
			DestinationResolver<String> destinationResolver,
			MessageConverter messageConverter) {
		super(destinationResolver);
		this.amazonEvents = amazonEvents;
		initMessageConverter(messageConverter);
	}

	@Override
	protected EventBusMessageChannel resolveMessageChannel(
			String physicalResourceIdentifier) {
		return new EventBusMessageChannel(this.amazonEvents, physicalResourceIdentifier);
	}

	/**
	 * Convenience method that sends an event identified by {@literal source} and
	 * {@literal detailType} with the given {@literal message} to the
	 * {@literal destination}.
	 * @param source The event source
	 * @param detailType The event detail-type
	 * @param message The event body to send
	 */
	public void sendEvent(String source, String detailType, Object message) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(EventBusMessageChannel.EVENT_SOURCE_HEADER, source);
		headers.put(EventBusMessageChannel.EVENT_DETAIL_TYPE_HEADER, detailType);
		this.convertAndSend(getRequiredDefaultDestination(), message, headers);
	}

	/**
	 * Convenience method that sends an event identified by {@literal source} and
	 * {@literal detailType} with the given {@literal message} to the specific
	 * {@literal eventBus}.
	 * @param eventBus The event bus name
	 * @param source The event source
	 * @param detailType The event detail-type
	 * @param message The event body to send
	 */
	public void sendEvent(String eventBus, String source, String detailType,
			Object message) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(EventBusMessageChannel.EVENT_SOURCE_HEADER, source);
		headers.put(EventBusMessageChannel.EVENT_DETAIL_TYPE_HEADER, detailType);
		this.convertAndSend(eventBus, message, headers);
	}

}
