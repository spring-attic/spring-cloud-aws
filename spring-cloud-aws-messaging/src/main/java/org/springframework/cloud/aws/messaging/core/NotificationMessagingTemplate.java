/*
 * Copyright 2013-2014 the original author or authors.
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

import com.amazonaws.services.sns.AmazonSNS;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate;
import org.springframework.cloud.aws.messaging.support.destination.DynamicTopicDestinationResolver;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Map;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class NotificationMessagingTemplate extends AbstractMessageChannelMessagingSendingTemplate<TopicMessageChannel> {

	private final AmazonSNS amazonSns;

	public NotificationMessagingTemplate(AmazonSNS amazonSns, ResourceIdResolver resourceIdResolver) {
		super(new DynamicTopicDestinationResolver(amazonSns, resourceIdResolver));
		this.amazonSns = amazonSns;
		initMessageConverter();
	}

	private void initMessageConverter() {
		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);
		setMessageConverter(stringMessageConverter);
	}

	public NotificationMessagingTemplate(AmazonSNS amazonSns) {
		this(amazonSns, null);
	}

	/**
	 * <b>IMPORTANT</b>: the underlying message channel {@link org.springframework.cloud.aws.messaging.core.TopicMessageChannel} only
	 * supports {@code String} as payload. Therefore only {@code String} payloads are accepted.
	 *
	 * @see org.springframework.messaging.core.MessageSendingOperations#convertAndSend(Object, Object, java.util.Map)
	 */
	@Override
	public <T> void convertAndSend(String destinationName, T payload) throws MessagingException {
		Assert.isInstanceOf(String.class, payload, "Payload must be of type string");
		super.convertAndSend(destinationName, payload);
	}

	/**
	 * <b>IMPORTANT</b>: the underlying message channel {@link org.springframework.cloud.aws.messaging.core.TopicMessageChannel} only
	 * supports {@code String} as payload. Therefore only {@code String} payloads are accepted.
	 *
	 * @see org.springframework.messaging.core.MessageSendingOperations#convertAndSend(Object, Object, java.util.Map)
	 */
	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers) throws MessagingException {
		Assert.isInstanceOf(String.class, payload, "Payload must be of type string");
		super.convertAndSend(destinationName, payload, headers);
	}

	/**
	 * <b>IMPORTANT</b>: the underlying message channel {@link org.springframework.cloud.aws.messaging.core.TopicMessageChannel} only
	 * supports {@code String} as payload. Therefore only {@code String} payloads are accepted.
	 *
	 * @see org.springframework.messaging.core.MessageSendingOperations#convertAndSend(Object, Object, java.util.Map)
	 */
	@Override
	public <T> void convertAndSend(String destinationName, T payload, MessagePostProcessor postProcessor) throws MessagingException {
		Assert.isInstanceOf(String.class, payload, "Payload must be of type string");
		super.convertAndSend(destinationName, payload, postProcessor);
	}

	/**
	 * <b>IMPORTANT</b>: the underlying message channel {@link org.springframework.cloud.aws.messaging.core.TopicMessageChannel} only
	 * supports {@code String} as payload. Therefore only {@code String} payloads are accepted.
	 *
	 * @see org.springframework.messaging.core.MessageSendingOperations#convertAndSend(Object, Object, java.util.Map)
	 */
	@Override
	public <T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers, MessagePostProcessor postProcessor) throws MessagingException {
		Assert.isInstanceOf(String.class, payload, "Payload must be of type string");
		super.convertAndSend(destinationName, payload, headers, postProcessor);
	}

	@Override
	protected TopicMessageChannel resolveMessageChannel(String physicalResourceIdentifier) {
		return new TopicMessageChannel(this.amazonSns, physicalResourceIdentifier);
	}

	/**
	 * Convenience method that sends a notification with the given {@literal message} and {@literal subject} to the {@literal destination}.
	 * The {@literal subject} is sent as header as defined in the <a href="https://docs.aws.amazon.com/sns/latest/dg/json-formats.html">SNS message JSON formats</a>.
	 *
	 * @param destinationName
	 * 		The logical name of the destination
	 * @param message
	 * 		The message to send
	 * @param subject
	 * 		The subject to send
	 */
	public void sendNotification(String destinationName, String message, String subject) {
		this.convertAndSend(destinationName, message, Collections.<String, Object>singletonMap(TopicMessageChannel.NOTIFICATION_SUBJECT_HEADER, subject));
	}

	/**
	 * Convenience method that sends a notification with the given {@literal message} and {@literal subject} to the {@literal destination}.
	 * The {@literal subject} is sent as header as defined in the <a href="https://docs.aws.amazon.com/sns/latest/dg/json-formats.html">SNS message JSON formats</a>.
	 * The configured default destination will be used.
	 *
	 * @param message
	 * 		The message to send
	 * @param subject
	 * 		The subject to send
	 */
	public void sendNotification(String message, String subject) {
		this.convertAndSend(getRequiredDefaultDestination(), message, Collections.<String, Object>singletonMap(TopicMessageChannel.NOTIFICATION_SUBJECT_HEADER, subject));
	}
}
