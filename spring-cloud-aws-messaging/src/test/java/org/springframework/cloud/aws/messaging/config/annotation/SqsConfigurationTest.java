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

package org.springframework.cloud.aws.messaging.config.annotation;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import org.springframework.cloud.aws.context.config.annotation.EnableContextRegion;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.cloud.aws.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * @author Alain Sahli
 */
public class SqsConfigurationTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void configuration_withMinimalBeans_shouldStartSqsListenerContainer()
			throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				MinimalConfiguration.class);
		SimpleMessageListenerContainer container = applicationContext
				.getBean(SimpleMessageListenerContainer.class);

		// Assert
		assertThat(container.isRunning()).isTrue();
		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);
		assertThat(QueueMessageHandler.class.isInstance(queueMessageHandler)).isTrue();

		HandlerMethodReturnValueHandler sendToReturnValueHandler = queueMessageHandler
				.getCustomReturnValueHandlers().get(0);
		QueueMessagingTemplate messagingTemplate = (QueueMessagingTemplate) ReflectionTestUtils
				.getField(sendToReturnValueHandler, "messageTemplate");

		SqsAsyncClient amazonSqsAsync = (SqsAsyncClient) ReflectionTestUtils.getField(messagingTemplate, "amazonSqsAsync");
		SdkClientConfiguration clientConfigurationAmazonSqsAsync = (SdkClientConfiguration) ReflectionTestUtils.getField(amazonSqsAsync, "clientConfiguration");
		assertThat(clientConfigurationAmazonSqsAsync.option(AwsClientOption.CREDENTIALS_PROVIDER)).isNotNull();

		SqsClient amazonSqs = (SqsClient) ReflectionTestUtils.getField(messagingTemplate, "amazonSqs");
		SdkClientConfiguration clientConfigurationAmazonSqs = (SdkClientConfiguration) ReflectionTestUtils.getField(amazonSqs, "clientConfiguration");
		assertThat(clientConfigurationAmazonSqs.option(AwsClientOption.CREDENTIALS_PROVIDER)).isNotNull();
	}

	@Test
	public void configuration_withCustomAmazonClient_shouldBeUsedByTheContainer()
			throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigurationWithCustomAmazonClient.class);

		// Assert
		SqsClient amazonSqsClient = applicationContext.getBean(SqsClient.class);
		assertThat(amazonSqsClient)
				.isEqualTo(ConfigurationWithCustomAmazonClient.CUSTOM_SQS_CLIENT);
	}

	@Test
	public void messageHandler_withFactoryConfiguration_shouldUseCustomValues()
			throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigurationWithCustomizedMessageHandler.class);
		QueueMessageHandler messageHandler = applicationContext
				.getBean(QueueMessageHandler.class);

		// Assert
		assertThat(messageHandler.getCustomArgumentResolvers().size()).isEqualTo(1);
		assertThat(messageHandler.getCustomArgumentResolvers().get(0)).isEqualTo(
				ConfigurationWithCustomizedMessageHandler.CUSTOM_ARGUMENT_RESOLVER);

		assertThat(messageHandler.getCustomReturnValueHandlers().size()).isEqualTo(2);
		assertThat(messageHandler.getCustomReturnValueHandlers().get(0)).isEqualTo(
				ConfigurationWithCustomizedMessageHandler.CUSTOM_RETURN_VALUE_HANDLER);

		Object sendToMessageTemplate = ReflectionTestUtils.getField(
				messageHandler.getReturnValueHandlers().get(1), "messageTemplate");
		assertThat(ReflectionTestUtils.getField(sendToMessageTemplate, "amazonSqs"))
				.isEqualTo(ConfigurationWithCustomizedMessageHandler.CUSTOM_AMAZON_SQS);

		Object destinationResolver = ReflectionTestUtils.getField(sendToMessageTemplate,
				"destinationResolver");
		Object targetDestinationResolver = ReflectionTestUtils
				.getField(destinationResolver, "targetDestinationResolver");
		assertThat(ReflectionTestUtils.getField(targetDestinationResolver,
				"resourceIdResolver")).isEqualTo(
						ConfigurationWithCustomizedMessageHandler.CUSTOM_RESOURCE_ID_RESOLVER);
	}

	@Test
	public void configuration_withCustomConfigurationFactory_shouldBeUsedToCreateTheContainer()
			throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigurationWithCustomContainerFactory.class);
		SimpleMessageListenerContainer container = applicationContext
				.getBean(SimpleMessageListenerContainer.class);

		// Assert
		assertThat(ReflectionTestUtils.getField(container, "amazonSqs"))
				.isEqualTo(ConfigurationWithCustomContainerFactory.AMAZON_SQS);
		assertThat(container.isAutoStartup())
				.isEqualTo(ConfigurationWithCustomContainerFactory.AUTO_STARTUP);
		assertThat(ReflectionTestUtils.getField(container, "maxNumberOfMessages"))
				.isEqualTo(
						ConfigurationWithCustomContainerFactory.MAX_NUMBER_OF_MESSAGES);
		assertThat(ReflectionTestUtils.getField(container, "messageHandler"))
				.isEqualTo(ConfigurationWithCustomContainerFactory.MESSAGE_HANDLER);
		assertThat(ReflectionTestUtils.getField(container, "resourceIdResolver"))
				.isEqualTo(ConfigurationWithCustomContainerFactory.RESOURCE_ID_RESOLVER);
		assertThat(ReflectionTestUtils.getField(container, "taskExecutor"))
				.isEqualTo(ConfigurationWithCustomContainerFactory.TASK_EXECUTOR);
		assertThat(ReflectionTestUtils.getField(container, "visibilityTimeout"))
				.isEqualTo(ConfigurationWithCustomContainerFactory.VISIBILITY_TIMEOUT);
		assertThat(ReflectionTestUtils.getField(container, "waitTimeOut"))
				.isEqualTo(ConfigurationWithCustomContainerFactory.WAIT_TIME_OUT);
		assertThat(
				ConfigurationWithCustomContainerFactory.DESTINATION_RESOLVER == ReflectionTestUtils
						.getField(container, "destinationResolver")).isTrue();
		assertThat(container.getBackOffTime())
				.isEqualTo(ConfigurationWithCustomContainerFactory.BACK_OFF_TIME);
	}

	@Test
	public void configuration_withCustomSendToMessageTemplate_shouldUseTheConfiguredTemplate()
			throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigurationWithCustomSendToMessageTemplate.class);
		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);

		// Assert
		assertThat(queueMessageHandler.getReturnValueHandlers().size()).isEqualTo(1);
		assertThat(
				ConfigurationWithCustomSendToMessageTemplate.SEND_TO_MESSAGE_TEMPLATE == ReflectionTestUtils
						.getField(queueMessageHandler.getReturnValueHandlers().get(0),
								"messageTemplate")).isTrue();
	}

	@Test
	public void queueMessageHandlerBeanMustBeSetOnContainer() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				MinimalConfiguration.class);
		SimpleMessageListenerContainer simpleMessageListenerContainer = applicationContext
				.getBean(SimpleMessageListenerContainer.class);
		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);

		// Assert
		assertThat(ReflectionTestUtils.getField(simpleMessageListenerContainer,
				"messageHandler")).isEqualTo(queueMessageHandler);
	}

	@Test
	public void configuration_withoutAwsCredentials_shouldCreateAClientWithDefaultCredentialsProvider()
			throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigurationWithMissingAwsCredentials.class);

		// Assert
		SqsAsyncClient amazonSqs = applicationContext.getBean(SqsAsyncClient.class);
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(amazonSqs, "clientConfiguration");
		assertThat(
			DefaultCredentialsProvider.class.isInstance(clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER)))
			.isTrue();
	}

	@Test
	public void configuration_withRegionProvider_shouldUseItForClient() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				ConfigurationWithRegionProvider.class);
		SqsAsyncClient amazonSqs = applicationContext.getBean(SqsAsyncClient.class);

		// Assert
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(amazonSqs, "clientConfiguration");
		assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT).toString())
			.isEqualTo("https://" + ServiceMetadata.of("sqs").endpointFor(Region.EU_WEST_1));
	}

	@EnableSqs
	@Configuration(proxyBeanMethods = false)
	public static class MinimalConfiguration {

		@Bean
		public AwsCredentialsProvider awsCredentials() {
			return mock(AwsCredentialsProvider.class);
		}

	}

	@EnableSqs
	@Configuration(proxyBeanMethods = false)
	public static class ConfigurationWithCustomAmazonClient {

		public static final SqsClient CUSTOM_SQS_CLIENT = mock(SqsClient.class,
				withSettings().stubOnly());

		@Bean
		public AwsCredentialsProvider awsCredentials() {
			return mock(AwsCredentialsProvider.class);
		}

		@Bean
		public SqsClient amazonSqs() {
			return CUSTOM_SQS_CLIENT;
		}

	}

	@EnableSqs
	@Configuration(proxyBeanMethods = false)
	public static class ConfigurationWithCustomizedMessageHandler
			extends MinimalConfiguration {

		public static final HandlerMethodReturnValueHandler CUSTOM_RETURN_VALUE_HANDLER = mock(
				HandlerMethodReturnValueHandler.class);

		public static final HandlerMethodArgumentResolver CUSTOM_ARGUMENT_RESOLVER = mock(
				HandlerMethodArgumentResolver.class);

		public static final SqsClient CUSTOM_AMAZON_SQS = mock(SqsClient.class,
				withSettings().stubOnly());

		public static final ResourceIdResolver CUSTOM_RESOURCE_ID_RESOLVER = mock(
				ResourceIdResolver.class);

		@Bean
		public QueueMessageHandlerFactory queueMessageHandlerFactory() {
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

	@EnableSqs
	@Configuration(proxyBeanMethods = false)
	public static class ConfigurationWithCustomContainerFactory {

		public static final SqsClient AMAZON_SQS = mock(SqsClient.class,
				withSettings().stubOnly());

		public static final boolean AUTO_STARTUP = true;

		public static final int MAX_NUMBER_OF_MESSAGES = 1456;

		public static final QueueMessageHandler MESSAGE_HANDLER;

		public static final ResourceIdResolver RESOURCE_ID_RESOLVER = mock(
				ResourceIdResolver.class);

		public static final SimpleAsyncTaskExecutor TASK_EXECUTOR = new SimpleAsyncTaskExecutor();

		public static final int VISIBILITY_TIMEOUT = 1789;

		public static final int WAIT_TIME_OUT = 12;

		public static final DestinationResolver<String> DESTINATION_RESOLVER = new DynamicQueueUrlDestinationResolver(
				mock(SqsClient.class, withSettings().stubOnly()));

		public static final long BACK_OFF_TIME = 5000;

		static {
			QueueMessageHandler queueMessageHandler = new QueueMessageHandler();
			queueMessageHandler.setApplicationContext(new StaticApplicationContext());
			MESSAGE_HANDLER = queueMessageHandler;
		}

		@Bean
		public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
			SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
			factory.setAmazonSqs(amazonSQS());
			factory.setAutoStartup(AUTO_STARTUP);
			factory.setMaxNumberOfMessages(MAX_NUMBER_OF_MESSAGES);
			factory.setQueueMessageHandler(MESSAGE_HANDLER);
			factory.setResourceIdResolver(RESOURCE_ID_RESOLVER);
			factory.setTaskExecutor(TASK_EXECUTOR);
			factory.setVisibilityTimeout(VISIBILITY_TIMEOUT);
			factory.setWaitTimeOut(WAIT_TIME_OUT);
			factory.setDestinationResolver(DESTINATION_RESOLVER);
			factory.setBackOffTime(BACK_OFF_TIME);

			return factory;
		}

		@Bean
		public SqsClient amazonSQS() {
			return AMAZON_SQS;
		}

	}

	@EnableSqs
	@Configuration(proxyBeanMethods = false)
	public static class ConfigurationWithCustomSendToMessageTemplate {

		public static final DestinationResolvingMessageSendingOperations<?> SEND_TO_MESSAGE_TEMPLATE = mock(
				DestinationResolvingMessageSendingOperations.class);

		@Bean
		public QueueMessageHandlerFactory queueMessageHandlerFactory() {
			QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory();
			factory.setSendToMessagingTemplate(SEND_TO_MESSAGE_TEMPLATE);

			return factory;
		}

	}

	@EnableSqs
	@Configuration(proxyBeanMethods = false)
	public static class ConfigurationWithMissingAwsCredentials {

	}

	@EnableSqs
	@EnableContextRegion(region = "eu-west-1")
	@Configuration(proxyBeanMethods = false)
	public static class ConfigurationWithRegionProvider {

	}

}
