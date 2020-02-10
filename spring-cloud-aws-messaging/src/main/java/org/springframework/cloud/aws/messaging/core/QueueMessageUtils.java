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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public final class QueueMessageUtils {

	private static final String RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME = "ReceiptHandle";

	private static final String MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME = "MessageId";

	private QueueMessageUtils() {
		// Avoid instantiation
	}

	public static Message<String> createMessage(
			software.amazon.awssdk.services.sqs.model.Message message) {
		return createMessage(message, Collections.emptyMap());
	}

	public static Message<String> createMessage(
			software.amazon.awssdk.services.sqs.model.Message message,
			Map<String, Object> additionalHeaders) {
		HashMap<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME, message.messageId());
		messageHeaders.put(RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME,
				message.receiptHandle());

		messageHeaders.putAll(additionalHeaders);
		messageHeaders.putAll(getAttributesAsMessageHeaders(message));
		messageHeaders.putAll(getMessageAttributesAsMessageHeaders(message));

		return new GenericMessage<>(message.body(),
				new SqsMessageHeaders(messageHeaders));
	}

	private static Map<String, Object> getAttributesAsMessageHeaders(
			software.amazon.awssdk.services.sqs.model.Message message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		for (Map.Entry<MessageSystemAttributeName, String> attributeKeyValuePair : message
				.attributes().entrySet()) {
			messageHeaders.put(attributeKeyValuePair.getKey().name(),
					attributeKeyValuePair.getValue());
		}

		return messageHeaders;
	}

	private static Map<String, Object> getMessageAttributesAsMessageHeaders(
			software.amazon.awssdk.services.sqs.model.Message message) {
		Map<String, Object> messageHeaders = new HashMap<>();
		for (Map.Entry<String, MessageAttributeValue> messageAttribute : message
				.messageAttributes().entrySet()) {
			if (MessageHeaders.CONTENT_TYPE.equals(messageAttribute.getKey())) {
				messageHeaders.put(MessageHeaders.CONTENT_TYPE,
						MimeType.valueOf(messageAttribute.getValue().stringValue()));
			}
			else if (MessageHeaders.ID.equals(messageAttribute.getKey())) {
				messageHeaders.put(MessageHeaders.ID,
						UUID.fromString(messageAttribute.getValue().stringValue()));
			}
			else if (MessageAttributeDataTypes.STRING
					.equals(messageAttribute.getValue().dataType())) {
				messageHeaders.put(messageAttribute.getKey(),
						messageAttribute.getValue().stringValue());
			}
			else if (messageAttribute.getValue().dataType()
					.startsWith(MessageAttributeDataTypes.NUMBER)) {
				Object numberValue = getNumberValue(messageAttribute.getValue());
				if (numberValue != null) {
					messageHeaders.put(messageAttribute.getKey(), numberValue);
				}
			}
			else if (MessageAttributeDataTypes.BINARY
					.equals(messageAttribute.getValue().dataType())) {
				messageHeaders.put(messageAttribute.getKey(),
						messageAttribute.getValue().binaryValue());
			}
		}

		return messageHeaders;
	}

	private static Object getNumberValue(MessageAttributeValue value) {
		String numberType = value.dataType()
				.substring(MessageAttributeDataTypes.NUMBER.length() + 1);
		try {
			Class<? extends Number> numberTypeClass = Class.forName(numberType)
					.asSubclass(Number.class);
			return NumberUtils.parseNumber(value.stringValue(), numberTypeClass);
		}
		catch (ClassNotFoundException e) {
			throw new MessagingException(String.format(
					"Message attribute with value '%s' and data type '%s' could not be converted "
							+ "into a Number because target class was not found.",
					value.stringValue(), value.dataType()), e);
		}
	}

}
