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

package org.springframework.cloud.aws.messaging.core;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public final class QueueMessageUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueMessageUtils.class);
    private static final String RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME = "ReceiptHandle";
    private static final String MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME = "MessageId";

    private QueueMessageUtils() {
        // Avoid instantiation
    }

    public static Message<String> createMessage(com.amazonaws.services.sqs.model.Message message) {
        return createMessage(message, Collections.emptyMap());
    }

    public static Message<String> createMessage(com.amazonaws.services.sqs.model.Message message, Map<String, Object> additionalHeaders) {
        HashMap<String, Object> messageHeaders = new HashMap<>();
        messageHeaders.put(MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME, message.getMessageId());
        messageHeaders.put(RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME, message.getReceiptHandle());

        messageHeaders.putAll(additionalHeaders);
        messageHeaders.putAll(getAttributesAsMessageHeaders(message));
        messageHeaders.putAll(getMessageAttributesAsMessageHeaders(message));

        return new GenericMessage<>(message.getBody(), new SqsMessageHeaders(messageHeaders));
    }

    private static Map<String, Object> getAttributesAsMessageHeaders(com.amazonaws.services.sqs.model.Message message) {
        Map<String, Object> messageHeaders = new HashMap<>();
        for (Map.Entry<String, String> attributeKeyValuePair : message.getAttributes().entrySet()) {
            messageHeaders.put(attributeKeyValuePair.getKey(), attributeKeyValuePair.getValue());
        }

        return messageHeaders;
    }

    private static Map<String, Object> getMessageAttributesAsMessageHeaders(com.amazonaws.services.sqs.model.Message message) {
        Map<String, Object> messageHeaders = new HashMap<>();
        for (Map.Entry<String, MessageAttributeValue> messageAttribute : message.getMessageAttributes().entrySet()) {
            String attributeName = messageAttribute.getKey();
            String attributeValue = messageAttribute.getValue().getStringValue();
            String attributeType = messageAttribute.getValue().getDataType();
            if (MessageHeaders.CONTENT_TYPE.equals(attributeName)) {
                messageHeaders.put(MessageHeaders.CONTENT_TYPE, MimeType.valueOf(attributeValue));
            } else if (MessageHeaders.ID.equals(attributeName)) {
                messageHeaders.put(MessageHeaders.ID, UUID.fromString(attributeValue));
            } else {
                if (MessageAttributeDataTypes.STRING.equals(attributeType)) {
                    messageHeaders.put(attributeName, attributeValue);
                } else if (attributeType.startsWith(MessageAttributeDataTypes.NUMBER)) {
                    messageHeaders.put(attributeName, getNumberValue(attributeType, attributeValue));
                } else if (MessageAttributeDataTypes.BINARY.equals(attributeType)) {
                    messageHeaders.put(attributeName, messageAttribute.getValue().getBinaryValue());
                }
            }
        }

        return messageHeaders;
    }

    private static Object getNumberValue(String attributeType, String attributeValue) {
        Class<? extends Number> numberTypeClass;
        if (MessageAttributeDataTypes.NUMBER.equals(attributeType)) {
            numberTypeClass = Number.class;
        } else {
            try {
                String numberType = attributeType.substring(MessageAttributeDataTypes.NUMBER.length() + 1);
                numberTypeClass = ClassUtils.resolvePrimitiveIfNecessary(ClassUtils.forName(numberType, null)).asSubclass(Number.class);
            } catch (ClassNotFoundException e) {
                LOGGER.warn(
                        "Message attribute with value '{}' and data type '{}' could not be converted into a Number because target class was not found.",
                        attributeValue, attributeType, e);
                numberTypeClass = Number.class;
            }
        }
        return NumberUtils.parseNumber(attributeValue, numberTypeClass);
    }

}
