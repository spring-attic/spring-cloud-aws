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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public final class QueueMessageUtils {

    private static final String RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME = "ReceiptHandle";
    private static final String MESSAGE_ID_MESSAGE_ATTRIBUTE_NAME = "MessageId";

    private static final Map<Class<? extends Number>, Class<? extends Number>> PRIMITIVE_NUMBER_TYPE_TO_WRAPPED_TYPE_MAP = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>(byte.class, Byte.class),
            new AbstractMap.SimpleImmutableEntry<>(short.class, Short.class),
            new AbstractMap.SimpleImmutableEntry<>(int.class, Integer.class),
            new AbstractMap.SimpleImmutableEntry<>(long.class, Long.class),
            new AbstractMap.SimpleImmutableEntry<>(float.class, Float.class),
            new AbstractMap.SimpleImmutableEntry<>(double.class, Double.class))
            .collect(collectingAndThen(toMap(Map.Entry::getKey, Map.Entry::getValue), Collections::unmodifiableMap));

    private static final Map<String, Class<? extends Number>> NUMBER_TYPE_NAME_TO_CLASS_MAP = Stream.of(
            PRIMITIVE_NUMBER_TYPE_TO_WRAPPED_TYPE_MAP,
            Stream.concat(PRIMITIVE_NUMBER_TYPE_TO_WRAPPED_TYPE_MAP.values().parallelStream(), Stream.of(BigInteger.class, BigDecimal.class, Number.class))
                    .collect(collectingAndThen(toMap(Function.identity(), Function.identity()), Collections::unmodifiableMap)))
            .map(Map::entrySet)
            .flatMap(Collection::parallelStream)
            .collect(collectingAndThen(toMap(e -> e.getKey().getSimpleName(), Map.Entry::getValue), Collections::unmodifiableMap));

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
            if (MessageHeaders.CONTENT_TYPE.equals(messageAttribute.getKey())) {
                messageHeaders.put(MessageHeaders.CONTENT_TYPE, MimeType.valueOf(messageAttribute.getValue().getStringValue()));
            } else if (MessageHeaders.ID.equals(messageAttribute.getKey())) {
                messageHeaders.put(MessageHeaders.ID, UUID.fromString(messageAttribute.getValue().getStringValue()));
            } else if (MessageAttributeDataTypes.STRING.equals(messageAttribute.getValue().getDataType())) {
                messageHeaders.put(messageAttribute.getKey(), messageAttribute.getValue().getStringValue());
            } else if (messageAttribute.getValue().getDataType().startsWith(MessageAttributeDataTypes.NUMBER)) {
                Number numberValue = getNumberValue(messageAttribute.getValue().getDataType(), messageAttribute.getValue().getStringValue());
                if (numberValue != null) {
                    messageHeaders.put(messageAttribute.getKey(), numberValue);
                }
            } else if (MessageAttributeDataTypes.BINARY.equals(messageAttribute.getValue().getDataType())) {
                messageHeaders.put(messageAttribute.getKey(), messageAttribute.getValue().getBinaryValue());
            }
        }

        return messageHeaders;
    }

    public static Number getNumberValue(final String attributeType, final String attributeValue) {
        final String numberType;


        if (attributeType.equals(MessageAttributeDataTypes.NUMBER)) {
            numberType = Number.class.getSimpleName();
        } else if (attributeType.startsWith(MessageAttributeDataTypes.NUMBER + ".")) {
            numberType = attributeType.substring(MessageAttributeDataTypes.NUMBER.length() + 1);
        } else {
            throw new IllegalArgumentException(String.format("data type \"%s\" must be one of: {\"Number\", \"Number.*\"}", attributeType));
        }

        try {
            final Class<? extends Number> typeClass = NUMBER_TYPE_NAME_TO_CLASS_MAP.containsKey(numberType) ? NUMBER_TYPE_NAME_TO_CLASS_MAP.get(numberType) : ClassUtils.forName(numberType, QueueMessageUtils.class.getClassLoader()).asSubclass(Number.class);
            return NumberUtils.parseNumber(attributeValue, typeClass);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new MessagingException(String.format("Message attribute with value '%s' and data type '%s' could not be converted " +
                    "into a Number because target class was not found.", attributeValue, attributeType), e);
        }
    }
}
