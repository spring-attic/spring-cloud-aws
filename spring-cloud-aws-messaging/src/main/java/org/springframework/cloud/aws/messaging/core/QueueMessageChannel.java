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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;

import static org.springframework.cloud.aws.messaging.core.QueueMessageUtils.createMessage;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class QueueMessageChannel extends AbstractMessageChannel
		implements PollableChannel {

	static final QueueAttributeName ATTRIBUTE_NAMES = QueueAttributeName.ALL;

	private static final String MESSAGE_ATTRIBUTE_NAMES = "All";

	private final SqsClient amazonSqs;

	private final SqsAsyncClient amazonSqsAsync;

	private final String queueUrl;

	public QueueMessageChannel(SqsClient amazonSqs, SqsAsyncClient amazonSqsAsync,
			String queueUrl) {
		this.amazonSqs = amazonSqs;
		this.amazonSqsAsync = amazonSqsAsync;
		this.queueUrl = queueUrl;
	}

	private static boolean isSkipHeader(String headerName) {
		return SqsMessageHeaders.SQS_DELAY_HEADER.equals(headerName)
				|| SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER.equals(headerName)
				|| SqsMessageHeaders.SQS_GROUP_ID_HEADER.equals(headerName);
	}

	@Override
	protected boolean sendInternal(Message<?> message, long timeout) {
		try {
			sendMessageAndWaitForResult(prepareSendMessageRequest(message), timeout);
		}
		catch (AwsServiceException e) {
			throw new MessageDeliveryException(message, e.getMessage(), e);
		}
		catch (ExecutionException e) {
			throw new MessageDeliveryException(message, e.getMessage(), e.getCause());
		}
		catch (TimeoutException e) {
			return false;
		}

		return true;
	}

	private SendMessageRequest prepareSendMessageRequest(Message<?> message) {
		SendMessageRequest.Builder sendMessageRequest = SendMessageRequest.builder()
				.queueUrl(this.queueUrl)
				.messageBody(String.valueOf(message.getPayload()));

		if (message.getHeaders().containsKey(SqsMessageHeaders.SQS_GROUP_ID_HEADER)) {
			sendMessageRequest.messageGroupId(message.getHeaders()
					.get(SqsMessageHeaders.SQS_GROUP_ID_HEADER, String.class));
		}

		if (message.getHeaders()
				.containsKey(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER)) {
			sendMessageRequest.messageDeduplicationId(message.getHeaders()
					.get(SqsMessageHeaders.SQS_DEDUPLICATION_ID_HEADER, String.class));
		}

		if (message.getHeaders().containsKey(SqsMessageHeaders.SQS_DELAY_HEADER)) {
			sendMessageRequest.delaySeconds(message.getHeaders()
					.get(SqsMessageHeaders.SQS_DELAY_HEADER, Integer.class));
		}

		Map<String, MessageAttributeValue> messageAttributes = getMessageAttributes(
				message);
		if (!messageAttributes.isEmpty()) {
			sendMessageRequest.messageAttributes(messageAttributes);
		}

		return sendMessageRequest.build();
	}

	private void sendMessageAndWaitForResult(SendMessageRequest sendMessageRequest,
			long timeout) throws ExecutionException, TimeoutException {
		if (timeout > 0) {
			Future<SendMessageResponse> sendMessageFuture = this.amazonSqsAsync
					.sendMessage(sendMessageRequest);

			try {
				sendMessageFuture.get(timeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		else {
			this.amazonSqs.sendMessage(sendMessageRequest);
		}
	}

	private Map<String, MessageAttributeValue> getMessageAttributes(Message<?> message) {
		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		for (Map.Entry<String, Object> messageHeader : message.getHeaders().entrySet()) {
			String messageHeaderName = messageHeader.getKey();
			Object messageHeaderValue = messageHeader.getValue();

			if (isSkipHeader(messageHeaderName)) {
				continue;
			}

			if (MessageHeaders.CONTENT_TYPE.equals(messageHeaderName)
					&& messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName,
						getContentTypeMessageAttribute(messageHeaderValue));
			}
			else if (MessageHeaders.ID.equals(messageHeaderName)
					&& messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName,
						getStringMessageAttribute(messageHeaderValue.toString()));
			}
			else if (messageHeaderValue instanceof String) {
				messageAttributes.put(messageHeaderName,
						getStringMessageAttribute((String) messageHeaderValue));
			}
			else if (messageHeaderValue instanceof Number) {
				messageAttributes.put(messageHeaderName,
						getNumberMessageAttribute(messageHeaderValue));
			}
			else if (messageHeaderValue instanceof ByteBuffer) {
				messageAttributes.put(messageHeaderName,
						getBinaryMessageAttribute((ByteBuffer) messageHeaderValue));
			}
			else {
				this.logger.warn(String.format(
						"Message header with name '%s' and type '%s' cannot be sent as"
								+ " message attribute because it is not supported by SQS.",
						messageHeaderName, messageHeaderValue != null
								? messageHeaderValue.getClass().getName() : ""));
			}
		}

		return messageAttributes;
	}

	private MessageAttributeValue getBinaryMessageAttribute(
			ByteBuffer messageHeaderValue) {
		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.BINARY)
				.binaryValue(SdkBytes.fromByteBuffer(messageHeaderValue)).build();
	}

	private MessageAttributeValue getContentTypeMessageAttribute(
			Object messageHeaderValue) {
		if (messageHeaderValue instanceof MimeType) {
			return MessageAttributeValue.builder()
					.dataType(MessageAttributeDataTypes.STRING)
					.stringValue(messageHeaderValue.toString()).build();
		}
		else if (messageHeaderValue instanceof String) {
			return MessageAttributeValue.builder()
					.dataType(MessageAttributeDataTypes.STRING)
					.stringValue((String) messageHeaderValue).build();
		}
		return null;
	}

	private MessageAttributeValue getStringMessageAttribute(String messageHeaderValue) {
		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
				.stringValue(messageHeaderValue).build();
	}

	private MessageAttributeValue getNumberMessageAttribute(Object messageHeaderValue) {
		Assert.isTrue(
				NumberUtils.STANDARD_NUMBER_TYPES.contains(messageHeaderValue.getClass()),
				"Only standard number types are accepted as message header.");

		return MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.NUMBER + "."
						+ messageHeaderValue.getClass().getName())
				.stringValue(messageHeaderValue.toString()).build();
	}

	@Override
	public Message<String> receive() {
		return this.receive(0);
	}

	@Override
	public Message<String> receive(long timeout) {
		ReceiveMessageResponse receiveMessageResult = this.amazonSqs
				.receiveMessage(ReceiveMessageRequest.builder().queueUrl(this.queueUrl)
						.maxNumberOfMessages(1)
						.waitTimeSeconds(Long.valueOf(timeout).intValue())
						.attributeNames(ATTRIBUTE_NAMES)
						.messageAttributeNames(MESSAGE_ATTRIBUTE_NAMES).build());
		if (receiveMessageResult.messages().isEmpty()) {
			return null;
		}
		software.amazon.awssdk.services.sqs.model.Message amazonMessage = receiveMessageResult
				.messages().get(0);
		Message<String> message = createMessage(amazonMessage);
		this.amazonSqs
				.deleteMessage(DeleteMessageRequest.builder().queueUrl(this.queueUrl)
						.receiptHandle(amazonMessage.receiptHandle()).build());
		return message;
	}

}
