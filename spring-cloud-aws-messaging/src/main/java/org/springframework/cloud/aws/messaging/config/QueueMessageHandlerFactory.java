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

package org.springframework.cloud.aws.messaging.config;

import java.util.Arrays;
import java.util.List;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.CollectionUtils;

/**
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @since 1.0
 */
public class QueueMessageHandlerFactory {

	private List<HandlerMethodArgumentResolver> argumentResolvers;

	private List<HandlerMethodReturnValueHandler> returnValueHandlers;

	private DestinationResolvingMessageSendingOperations<?> sendToMessagingTemplate;

	private SqsClient amazonSqs;

	private SqsAsyncClient amazonSqsAsync;

	private ResourceIdResolver resourceIdResolver;

	private BeanFactory beanFactory;

	private List<MessageConverter> messageConverters;

	public void setArgumentResolvers(
			List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.argumentResolvers = argumentResolvers;
	}

	public void setReturnValueHandlers(
			List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.returnValueHandlers = returnValueHandlers;
	}

	/**
	 * Configures the {@link DestinationResolvingMessageSendingOperations} template used
	 * by the {@link SendToHandlerMethodReturnValueHandler} to send return values of
	 * handler methods.
	 * @param sendToMessagingTemplate A
	 * {@link DestinationResolvingMessageSendingOperations} template for sending return
	 * values of handler methods.
	 */
	public void setSendToMessagingTemplate(
			DestinationResolvingMessageSendingOperations<?> sendToMessagingTemplate) {
		this.sendToMessagingTemplate = sendToMessagingTemplate;
	}

	public SqsClient getAmazonSqs() {
		return this.amazonSqs;
	}

	public SqsAsyncClient getAmazonSqsAsync() {
		return amazonSqsAsync;
	}

	/**
	 * <p>
	 * Sets the {@link SqsClient} client that is going to be used to create a new
	 * {@link QueueMessagingTemplate} if {@code sendToMessagingTemplate} is {@code null}.
	 * This template is used by the {@link SendToHandlerMethodReturnValueHandler} to send
	 * the return values of handler methods annotated with
	 * {@link org.springframework.messaging.handler.annotation.SendTo}.
	 * </p>
	 * <p>
	 * An {@link SqsClient} client is only needed if {@code sendToMessagingTemplate} is
	 * {@code null}.
	 * </p>
	 * @param amazonSqs The {@link SqsClient} client that is going to be used by the
	 * {@link SendToHandlerMethodReturnValueHandler} to send messages.
	 */
	public void setAmazonSqs(SqsClient amazonSqs) {
		this.amazonSqs = amazonSqs;
	}

	/**
	 * <p>
	 * Sets the {@link SqsAsyncClient} client that is going to be used to create a new
	 * {@link QueueMessagingTemplate} if {@code sendToMessagingTemplate} is {@code null}.
	 * This template is used by the {@link SendToHandlerMethodReturnValueHandler} to send
	 * the return values of handler methods annotated with
	 * {@link org.springframework.messaging.handler.annotation.SendTo}.
	 * </p>
	 * <p>
	 * An {@link SqsAsyncClient} client is only needed if {@code sendToMessagingTemplate}
	 * is {@code null}.
	 * </p>
	 * @param amazonSqsAsync The {@link SqsAsyncClient} client that is going to be used by
	 * the {@link SendToHandlerMethodReturnValueHandler} to send messages.
	 */
	public void setAmazonSqsAsync(SqsAsyncClient amazonSqsAsync) {
		this.amazonSqsAsync = amazonSqsAsync;
	}

	/**
	 * This value is only used if no {@code sendToMessagingTemplate} has been set.
	 * @param resourceIdResolver the resourceIdResolver to use for resolving logical to
	 * physical ids in a CloudFormation environment. This resolver will be used by the
	 * {@link QueueMessagingTemplate} created for the
	 * {@link SendToHandlerMethodReturnValueHandler}.
	 */
	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	/**
	 * Configures a {@link BeanFactory} that should be used to resolve expressions and
	 * placeholder for {@link org.springframework.messaging.handler.annotation.SendTo}
	 * annotations. If not set, then no expressions or place holders will be resolved.
	 * @param beanFactory - the bean factory used to resolve expressions and / or place
	 * holders
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public QueueMessageHandler createQueueMessageHandler() {
		QueueMessageHandler queueMessageHandler = new QueueMessageHandler(
				CollectionUtils.isEmpty(this.messageConverters)
						? Arrays.asList(getDefaultMappingJackson2MessageConverter())
						: this.messageConverters);

		if (!CollectionUtils.isEmpty(this.argumentResolvers)) {
			queueMessageHandler.getCustomArgumentResolvers()
					.addAll(this.argumentResolvers);
		}
		if (!CollectionUtils.isEmpty(this.returnValueHandlers)) {
			queueMessageHandler.getCustomReturnValueHandlers()
					.addAll(this.returnValueHandlers);
		}

		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		if (this.sendToMessagingTemplate != null) {
			sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
					this.sendToMessagingTemplate);
		}
		else {
			sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
					getDefaultSendToQueueMessagingTemplate(this.amazonSqs,
							this.amazonSqsAsync, this.resourceIdResolver));

		}
		sendToHandlerMethodReturnValueHandler.setBeanFactory(this.beanFactory);
		queueMessageHandler.getCustomReturnValueHandlers()
				.add(sendToHandlerMethodReturnValueHandler);

		return queueMessageHandler;
	}

	private QueueMessagingTemplate getDefaultSendToQueueMessagingTemplate(
			SqsClient amazonSqs, SqsAsyncClient amazonSqsAsync,
			ResourceIdResolver resourceIdResolver) {
		return new QueueMessagingTemplate(amazonSqs, amazonSqsAsync, resourceIdResolver,
				getDefaultMappingJackson2MessageConverter());
	}

	public List<MessageConverter> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Configures a {@link MessageConverter}s that should be used to deserialize incoming
	 * message payloads and serialize messages in {@link QueueMessagingTemplate}. If not
	 * set, default {@link MappingJackson2MessageConverter} is used.
	 * @param messageConverters - the converters used for message conversion
	 */
	public void setMessageConverters(List<MessageConverter> messageConverters) {
		this.messageConverters = messageConverters;
	}

	private MappingJackson2MessageConverter getDefaultMappingJackson2MessageConverter() {
		MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
		jacksonMessageConverter.setSerializedPayloadClass(String.class);
		jacksonMessageConverter.setStrictContentTypeMatch(true);
		return jacksonMessageConverter;
	}

}
