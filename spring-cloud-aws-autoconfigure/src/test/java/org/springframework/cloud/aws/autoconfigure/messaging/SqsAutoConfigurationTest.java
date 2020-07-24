/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.messaging;

import java.util.Collections;
import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.context.config.annotation.EnableContextRegion;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class SqsAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SqsAutoConfiguration.class))
			.withUserConfiguration(MinimalConfiguration.class);

	@Test
	void configuresContainerAndHandler() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean(SimpleMessageListenerContainer.class)).isNotNull();
			assertThat(context.getBean(QueueMessageHandler.class)).isNotNull();
			assertThat(context.getBean(AmazonSQS.class)).isNotNull();
		});
	}

	@Test
	void configuration_withCustomClient_shouldBeUsedByContainer() {
		this.contextRunner
				.withUserConfiguration(ConfigurationWithCustomAmazonClient.class)
				.run((context) -> {
					AmazonSQSAsync amazonSqsClient = context
							.getBean(AmazonSQSAsync.class);
					assertThat(amazonSqsClient).isEqualTo(
							ConfigurationWithCustomAmazonClient.CUSTOM_SQS_CLIENT);
				});
	}

	@Test
	void configuration_withMinimalBeans_shouldStartSqsListenerContainer()
			throws Exception {
		// Arrange & Act
		this.contextRunner.run((context) -> {
			SimpleMessageListenerContainer container = context
					.getBean(SimpleMessageListenerContainer.class);

			// Assert
			assertThat(container.isRunning()).isTrue();
			QueueMessageHandler queueMessageHandler = context
					.getBean(QueueMessageHandler.class);
			assertThat(QueueMessageHandler.class.isInstance(queueMessageHandler))
					.isTrue();

			HandlerMethodReturnValueHandler sendToReturnValueHandler = queueMessageHandler
					.getCustomReturnValueHandlers().get(0);
			QueueMessagingTemplate messagingTemplate = (QueueMessagingTemplate) ReflectionTestUtils
					.getField(sendToReturnValueHandler, "messageTemplate");
			AmazonSQSBufferedAsyncClient amazonBufferedSqsClient = (AmazonSQSBufferedAsyncClient) ReflectionTestUtils
					.getField(messagingTemplate, "amazonSqs");
			AmazonSQSAsyncClient amazonSqsClient = (AmazonSQSAsyncClient) ReflectionTestUtils
					.getField(amazonBufferedSqsClient, "realSQS");
			assertThat(ReflectionTestUtils.getField(amazonSqsClient,
					"awsCredentialsProvider")).isNotNull();
		});

	}

	@Test
	void configuration_withCustomAmazonClient_shouldBeUsedByTheContainer()
			throws Exception {
		this.contextRunner
				.withUserConfiguration(ConfigurationWithCustomAmazonClient.class)
				.run((context) -> {
					// Assert
					AmazonSQSAsync amazonSqsClient = context
							.getBean(AmazonSQSAsync.class);
					assertThat(amazonSqsClient).isEqualTo(
							ConfigurationWithCustomAmazonClient.CUSTOM_SQS_CLIENT);
				});
	}

	@Test
	void messageHandler_withFactoryConfiguration_shouldUseCustomValues()
			throws Exception {
		// Arrange & Act
		this.contextRunner
				.withUserConfiguration(ConfigurationWithCustomizedMessageHandler.class)
				.run((context) -> {
					QueueMessageHandler messageHandler = context
							.getBean(QueueMessageHandler.class);

					// Assert
					assertThat(messageHandler.getCustomArgumentResolvers().size())
							.isEqualTo(1);
					assertThat(messageHandler.getCustomArgumentResolvers().get(0))
							.isEqualTo(
									ConfigurationWithCustomizedMessageHandler.CUSTOM_ARGUMENT_RESOLVER);

					assertThat(messageHandler.getCustomReturnValueHandlers().size())
							.isEqualTo(2);
					assertThat(messageHandler.getCustomReturnValueHandlers().get(0))
							.isEqualTo(
									ConfigurationWithCustomizedMessageHandler.CUSTOM_RETURN_VALUE_HANDLER);

					Object sqsMessageDeletionPolicy = ReflectionTestUtils
							.getField(messageHandler, "sqsMessageDeletionPolicy");
					assertThat(sqsMessageDeletionPolicy)
							.isEqualTo(SqsMessageDeletionPolicy.NO_REDRIVE);

					Object sendToMessageTemplate = ReflectionTestUtils.getField(
							messageHandler.getReturnValueHandlers().get(1),
							"messageTemplate");
					assertThat(ReflectionTestUtils.getField(sendToMessageTemplate,
							"amazonSqs")).isEqualTo(
									ConfigurationWithCustomizedMessageHandler.CUSTOM_AMAZON_SQS);

					Object destinationResolver = ReflectionTestUtils
							.getField(sendToMessageTemplate, "destinationResolver");
					Object targetDestinationResolver = ReflectionTestUtils
							.getField(destinationResolver, "targetDestinationResolver");
					assertThat(ReflectionTestUtils.getField(targetDestinationResolver,
							"resourceIdResolver")).isEqualTo(
									ConfigurationWithCustomizedMessageHandler.CUSTOM_RESOURCE_ID_RESOLVER);
				});
	}

	@Test
	void messageHandler_withFactoryConfiguration_shouldUseGlobalDeletionPolicy()
			throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(
				ConfigurationWithCustomizedMessageHandlerGlobalDeletionPolicy.class)
				.run((context) -> {
					QueueMessageHandler messageHandler = context
							.getBean(QueueMessageHandler.class);

					// Assert
					assertThat(messageHandler.getCustomArgumentResolvers().size())
							.isEqualTo(1);
					assertThat(messageHandler.getCustomArgumentResolvers().get(0))
							.isEqualTo(
									ConfigurationWithCustomizedMessageHandlerGlobalDeletionPolicy.CUSTOM_ARGUMENT_RESOLVER);

					assertThat(messageHandler.getCustomReturnValueHandlers().size())
							.isEqualTo(2);
					assertThat(messageHandler.getCustomReturnValueHandlers().get(0))
							.isEqualTo(
									ConfigurationWithCustomizedMessageHandlerGlobalDeletionPolicy.CUSTOM_RETURN_VALUE_HANDLER);

					Object sqsMessageDeletionPolicy = ReflectionTestUtils
							.getField(messageHandler, "sqsMessageDeletionPolicy");
					assertThat(sqsMessageDeletionPolicy)
							.isEqualTo(SqsMessageDeletionPolicy.ON_SUCCESS);

					Object sendToMessageTemplate = ReflectionTestUtils.getField(
							messageHandler.getReturnValueHandlers().get(1),
							"messageTemplate");
					assertThat(ReflectionTestUtils.getField(sendToMessageTemplate,
							"amazonSqs")).isEqualTo(
									ConfigurationWithCustomizedMessageHandlerGlobalDeletionPolicy.CUSTOM_AMAZON_SQS);

					Object destinationResolver = ReflectionTestUtils
							.getField(sendToMessageTemplate, "destinationResolver");
					Object targetDestinationResolver = ReflectionTestUtils
							.getField(destinationResolver, "targetDestinationResolver");
					assertThat(ReflectionTestUtils.getField(targetDestinationResolver,
							"resourceIdResolver")).isEqualTo(
									ConfigurationWithCustomizedMessageHandlerGlobalDeletionPolicy.CUSTOM_RESOURCE_ID_RESOLVER);
				});
	}

	@Test
	void configuration_withCustomConfigurationFactory_shouldBeUsedToCreateTheContainer()
			throws Exception {
		// Arrange & Act
		this.contextRunner
				.withUserConfiguration(ConfigurationWithCustomContainerFactory.class)
				.run((context) -> {
					SimpleMessageListenerContainer container = context
							.getBean(SimpleMessageListenerContainer.class);

					// Assert
					assertThat(ReflectionTestUtils.getField(container, "amazonSqs"))
							.isEqualTo(
									ConfigurationWithCustomContainerFactory.AMAZON_SQS);
					assertThat(container.isAutoStartup()).isEqualTo(
							ConfigurationWithCustomContainerFactory.AUTO_STARTUP);
					assertThat(ReflectionTestUtils.getField(container,
							"maxNumberOfMessages")).isEqualTo(
									ConfigurationWithCustomContainerFactory.MAX_NUMBER_OF_MESSAGES);
					assertThat(ReflectionTestUtils.getField(container, "messageHandler"))
							.isEqualTo(
									ConfigurationWithCustomContainerFactory.MESSAGE_HANDLER);
					assertThat(ReflectionTestUtils.getField(container,
							"resourceIdResolver")).isEqualTo(
									ConfigurationWithCustomContainerFactory.RESOURCE_ID_RESOLVER);
					assertThat(ReflectionTestUtils.getField(container, "taskExecutor"))
							.isEqualTo(
									ConfigurationWithCustomContainerFactory.TASK_EXECUTOR);
					assertThat(ReflectionTestUtils.getField(container,
							"visibilityTimeout")).isEqualTo(
									ConfigurationWithCustomContainerFactory.VISIBILITY_TIMEOUT);
					assertThat(ReflectionTestUtils.getField(container, "waitTimeOut"))
							.isEqualTo(
									ConfigurationWithCustomContainerFactory.WAIT_TIME_OUT);
					assertThat(ReflectionTestUtils.getField(container,
							"queueStopTimeout")).isEqualTo(
									ConfigurationWithCustomContainerFactory.QUEUE_STOP_TIME_OUT);
					assertThat(
							ConfigurationWithCustomContainerFactory.DESTINATION_RESOLVER == ReflectionTestUtils
									.getField(container, "destinationResolver")).isTrue();
					assertThat(container.getBackOffTime()).isEqualTo(
							ConfigurationWithCustomContainerFactory.BACK_OFF_TIME);
				});
	}

	@Test
	void configuration_withCustomSendToMessageTemplate_shouldUseTheConfiguredTemplate()
			throws Exception {
		// Arrange & Act
		this.contextRunner
				.withUserConfiguration(ConfigurationWithCustomSendToMessageTemplate.class)
				.run((context) -> {
					QueueMessageHandler queueMessageHandler = context
							.getBean(QueueMessageHandler.class);

					// Assert
					assertThat(queueMessageHandler.getReturnValueHandlers().size())
							.isEqualTo(1);
					assertThat(
							ConfigurationWithCustomSendToMessageTemplate.SEND_TO_MESSAGE_TEMPLATE == ReflectionTestUtils
									.getField(queueMessageHandler.getReturnValueHandlers()
											.get(0), "messageTemplate")).isTrue();
				});
	}

	@Test
	void queueMessageHandlerBeanMustBeSetOnContainer() throws Exception {
		// Arrange & Act
		this.contextRunner.run((context) -> {
			SimpleMessageListenerContainer simpleMessageListenerContainer = context
					.getBean(SimpleMessageListenerContainer.class);
			QueueMessageHandler queueMessageHandler = context
					.getBean(QueueMessageHandler.class);

			// Assert
			assertThat(ReflectionTestUtils.getField(simpleMessageListenerContainer,
					"messageHandler")).isEqualTo(queueMessageHandler);
		});
	}

	@Test
	void configuration_withObjectMapper_shouldSetObjectMapperOnQueueMessageHandler()
			throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithObjectMapper.class)
				.run((context) -> {
					QueueMessageHandler queueMessageHandler = context
							.getBean(QueueMessageHandler.class);
					ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
					List<MessageConverter> converters = (List<MessageConverter>) ReflectionTestUtils
							.getField(queueMessageHandler, "messageConverters");
					MappingJackson2MessageConverter mappingJackson2MessageConverter = (MappingJackson2MessageConverter) converters
							.get(0);

					// Assert
					assertThat(mappingJackson2MessageConverter.getObjectMapper())
							.isEqualTo(objectMapper);
				});
	}

	@Test
	void configuration_withoutAwsCredentials_shouldCreateAClientWithDefaultCredentialsProvider()
			throws Exception {
		// Arrange & Act
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SqsAutoConfiguration.class))
				.withUserConfiguration(ConfigurationWithMissingAwsCredentials.class)
				.run((context) -> {
					// Assert
					AmazonSQSBufferedAsyncClient bufferedAmazonSqsClient = context
							.getBean(AmazonSQSBufferedAsyncClient.class);
					AmazonSQSAsyncClient amazonSqsClient = (AmazonSQSAsyncClient) ReflectionTestUtils
							.getField(bufferedAmazonSqsClient, "realSQS");
					assertThat(DefaultAWSCredentialsProviderChain.class
							.isInstance(ReflectionTestUtils.getField(amazonSqsClient,
									"awsCredentialsProvider"))).isTrue();
				});
	}

	@Test
	void configuration_withRegionProvider_shouldUseItForClient() throws Exception {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithRegionProvider.class)
				.run((context) -> {
					AmazonSQSAsync bufferedAmazonSqsClient = context
							.getBean(AmazonSQSAsync.class);
					AmazonSQSAsyncClient amazonSqs = (AmazonSQSAsyncClient) ReflectionTestUtils
							.getField(bufferedAmazonSqsClient, "realSQS");

					// Assert
					assertThat(ReflectionTestUtils.getField(amazonSqs, "endpoint")
							.toString()).isEqualTo(
									"https://" + Region.getRegion(Regions.EU_WEST_1)
											.getServiceEndpoint("sqs"));
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class MinimalConfiguration {

		@Bean
		AWSCredentialsProvider awsCredentials() {
			return mock(AWSCredentialsProvider.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithCustomizedMessageHandler {

		static final HandlerMethodReturnValueHandler CUSTOM_RETURN_VALUE_HANDLER = mock(
				HandlerMethodReturnValueHandler.class);

		static final HandlerMethodArgumentResolver CUSTOM_ARGUMENT_RESOLVER = mock(
				HandlerMethodArgumentResolver.class);

		static final AmazonSQSAsync CUSTOM_AMAZON_SQS = mock(AmazonSQSAsync.class,
				withSettings().stubOnly());

		static final ResourceIdResolver CUSTOM_RESOURCE_ID_RESOLVER = mock(
				ResourceIdResolver.class);

		@Bean
		QueueMessageHandlerFactory queueMessageHandlerFactory() {
			QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();
			factory.setArgumentResolvers(
					Collections.singletonList(CUSTOM_ARGUMENT_RESOLVER));
			factory.setReturnValueHandlers(
					Collections.singletonList(CUSTOM_RETURN_VALUE_HANDLER));
			factory.setAmazonSqs(CUSTOM_AMAZON_SQS);
			factory.setResourceIdResolver(CUSTOM_RESOURCE_ID_RESOLVER);

			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithCustomizedMessageHandlerGlobalDeletionPolicy {

		static final HandlerMethodReturnValueHandler CUSTOM_RETURN_VALUE_HANDLER = mock(
				HandlerMethodReturnValueHandler.class);

		static final HandlerMethodArgumentResolver CUSTOM_ARGUMENT_RESOLVER = mock(
				HandlerMethodArgumentResolver.class);

		static final AmazonSQSAsync CUSTOM_AMAZON_SQS = mock(AmazonSQSAsync.class,
				withSettings().stubOnly());

		static final ResourceIdResolver CUSTOM_RESOURCE_ID_RESOLVER = mock(
				ResourceIdResolver.class);

		@Bean
		QueueMessageHandlerFactory queueMessageHandlerFactory() {
			QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();
			factory.setArgumentResolvers(
					Collections.singletonList(CUSTOM_ARGUMENT_RESOLVER));
			factory.setReturnValueHandlers(
					Collections.singletonList(CUSTOM_RETURN_VALUE_HANDLER));
			factory.setSqsMessageDeletionPolicy(SqsMessageDeletionPolicy.ON_SUCCESS);
			factory.setAmazonSqs(CUSTOM_AMAZON_SQS);
			factory.setResourceIdResolver(CUSTOM_RESOURCE_ID_RESOLVER);

			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithCustomContainerFactory {

		static final AmazonSQSAsync AMAZON_SQS = mock(AmazonSQSAsync.class,
				withSettings().stubOnly());

		static final boolean AUTO_STARTUP = true;

		static final int MAX_NUMBER_OF_MESSAGES = 1456;

		static final QueueMessageHandler MESSAGE_HANDLER;

		static final ResourceIdResolver RESOURCE_ID_RESOLVER = mock(
				ResourceIdResolver.class);

		static final SimpleAsyncTaskExecutor TASK_EXECUTOR = new SimpleAsyncTaskExecutor();

		static final int VISIBILITY_TIMEOUT = 1789;

		static final int WAIT_TIME_OUT = 12;

		static final long QUEUE_STOP_TIME_OUT = 12;

		static final DestinationResolver<String> DESTINATION_RESOLVER = new DynamicQueueUrlDestinationResolver(
				mock(AmazonSQSAsync.class, withSettings().stubOnly()));

		static final long BACK_OFF_TIME = 5000;

		static {
			QueueMessageHandler queueMessageHandler = new QueueMessageHandler();
			queueMessageHandler.setApplicationContext(new StaticApplicationContext());
			MESSAGE_HANDLER = queueMessageHandler;
		}

		@Bean
		SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
			SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
			factory.setAmazonSqs(amazonSQS());
			factory.setAutoStartup(AUTO_STARTUP);
			factory.setMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES);
			factory.setQueueMessageHandler(MESSAGE_HANDLER);
			factory.setResourceIdResolver(RESOURCE_ID_RESOLVER);
			factory.setTaskExecutor(TASK_EXECUTOR);
			factory.setVisibilityTimeout(VISIBILITY_TIMEOUT);
			factory.setWaitTimeOut(WAIT_TIME_OUT);
			factory.setQueueStopTimeout(QUEUE_STOP_TIME_OUT);
			factory.setDestinationResolver(DESTINATION_RESOLVER);
			factory.setBackOffTime(BACK_OFF_TIME);

			return factory;
		}

		@Bean
		AmazonSQSAsync amazonSQS() {
			return AMAZON_SQS;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithCustomSendToMessageTemplate {

		static final DestinationResolvingMessageSendingOperations<?> SEND_TO_MESSAGE_TEMPLATE = mock(
				DestinationResolvingMessageSendingOperations.class);

		@Bean
		QueueMessageHandlerFactory queueMessageHandlerFactory() {
			QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();
			factory.setSendToMessagingTemplate(SEND_TO_MESSAGE_TEMPLATE);

			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithMissingAwsCredentials {

	}

	@EnableContextRegion(region = "eu-west-1")
	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithRegionProvider {

	}

	@EnableContextRegion(region = "eu-west-1")
	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithObjectMapper {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithCustomAmazonClient {

		static final AmazonSQSAsync CUSTOM_SQS_CLIENT = mock(AmazonSQSAsync.class);

		@Bean
		AmazonSQSAsync amazonSQS() {
			return CUSTOM_SQS_CLIENT;
		}

	}

}
