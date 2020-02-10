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

package org.springframework.cloud.aws.messaging.config.xml;

import java.util.List;

import org.junit.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.support.converter.ObjectMessageConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alain Sahli
 */
public class QueueMessagingTemplateBeanDefinitionParserTest {

	@Test
	public void parseInternal_withMinimalConfig_shouldProduceAQueueMessagingTemplateWithDefaults()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-minimal.xml", getClass()));

		// Assert
		QueueMessagingTemplate queueMessagingTemplate = registry
				.getBean(QueueMessagingTemplate.class);
		assertThat(ReflectionTestUtils.getField(queueMessagingTemplate, "amazonSqs"))
				.isSameAs(registry.getBean(SqsClient.class));
		Object cachingDestinationResolverProxy = ReflectionTestUtils
				.getField(queueMessagingTemplate, "destinationResolver");
		Object targetDestinationResolver = ReflectionTestUtils
				.getField(cachingDestinationResolverProxy, "targetDestinationResolver");
		assertThat(ReflectionTestUtils.getField(targetDestinationResolver,
				"resourceIdResolver")).isEqualTo(registry.getBean(
						GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME));
		assertThat(CompositeMessageConverter.class
				.isInstance(queueMessagingTemplate.getMessageConverter())).isTrue();

		assertThat(CompositeMessageConverter.class
				.isInstance(queueMessagingTemplate.getMessageConverter())).isTrue();
		@SuppressWarnings("unchecked")
		List<MessageConverter> messageConverters = (List<MessageConverter>) ReflectionTestUtils
				.getField(queueMessagingTemplate.getMessageConverter(), "converters");
		assertThat(messageConverters.size()).isEqualTo(2);
		assertThat(StringMessageConverter.class.isInstance(messageConverters.get(0)))
				.isTrue();
		assertThat(MappingJackson2MessageConverter.class
				.isInstance(messageConverters.get(1))).isTrue();

		StringMessageConverter stringMessageConverter = (StringMessageConverter) messageConverters
				.get(0);
		assertThat(stringMessageConverter.getSerializedPayloadClass())
				.isSameAs(String.class);
		assertThat(ReflectionTestUtils.getField(stringMessageConverter,
				"strictContentTypeMatch")).isEqualTo(false);

		MappingJackson2MessageConverter jackson2MessageConverter = (MappingJackson2MessageConverter) messageConverters
				.get(1);
		assertThat(jackson2MessageConverter.getSerializedPayloadClass())
				.isSameAs(String.class);
		assertThat(ReflectionTestUtils.getField(jackson2MessageConverter,
				"strictContentTypeMatch")).isEqualTo(false);
	}

	@Test
	public void parseInternal_withCustomAmazonSqsClient_shouldPassItAsConstructorArg()
			throws Exception {
		// Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-amazon-sqs.xml", getClass()));

		// Assert
		BeanDefinition queueMessagingTemplateBeanDefinition = registry
				.getBeanDefinition("queueMessagingTemplate");
		assertThat(((RuntimeBeanReference) queueMessagingTemplateBeanDefinition
				.getConstructorArgumentValues()
				.getArgumentValue(0, RuntimeBeanReference.class).getValue())
						.getBeanName()).isEqualTo("myClient");
	}

	@Test
	public void parseInternal_withCustomConverter_mustBeSetOnTemplate() throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-converter.xml", getClass()));

		// Assert
		QueueMessagingTemplate queueMessagingTemplateBeanDefinition = registry
				.getBean(QueueMessagingTemplate.class);
		MessageConverter messageConverter = queueMessagingTemplateBeanDefinition
				.getMessageConverter();
		assertThat(CompositeMessageConverter.class.isInstance(messageConverter)).isTrue();
		CompositeMessageConverter compositeMessageConverter = (CompositeMessageConverter) messageConverter;
		assertThat(compositeMessageConverter.getConverters().size()).isEqualTo(2);
		assertThat(ObjectMessageConverter.class
				.isInstance(compositeMessageConverter.getConverters().get(1))).isTrue();
	}

	@Test
	public void parseInternal_withDefaultDestination_mustBeSetOnTemplate()
			throws Exception {
		// Arrange
		SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-with-default-destination.xml",
				getClass()));

		// Assert
		BeanDefinition queueMessagingTemplateBeanDefinition = registry
				.getBeanDefinition("queueMessagingTemplate");
		assertThat(queueMessagingTemplateBeanDefinition.getPropertyValues()
				.getPropertyValue("defaultDestinationName").getValue())
						.isEqualTo("myDefaultDestination");
	}

	@Test
	public void parseInternal_withCustomRegion_shouldConfigureDefaultClientWithCustomRegion()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region.xml", getClass()));

		// Assert
		SqsAsyncClient amazonSqs = registry.getBean(SqsAsyncClient.class);
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(amazonSqs, "clientConfiguration");
		assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT).toString())
			.isEqualTo(ServiceMetadata.of("sqs").endpointFor(Region.SA_EAST_1));
	}

	@Test
	public void parseInternal_withCustomRegionProvider_shouldConfigureDefaultClientWithCustomRegionReturnedByProvider()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region-provider.xml", getClass()));

		// Assert
		SqsAsyncClient amazonSqs = registry.getBean(SqsAsyncClient.class);
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(amazonSqs, "clientConfiguration");
		assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT).toString())
			.isEqualTo(ServiceMetadata.of("sqs").endpointFor(Region.AP_SOUTHEAST_2));
	}

	@Test
	public void parseInternal_withMultipleMessagingTemplatesDefined_shouldConfigureOnlyOneSqsClientAndDecorateOnlyOnce()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-multiple-templates.xml", getClass()));

		// Assert
		// TODO SDK2 migration: re-add after solving issue with clients
		// AmazonSQSBufferedAsyncClient amazonSqs = registry
		// .getBean(AmazonSQSBufferedAsyncClient.class);
		// assertThat(ReflectionTestUtils.getField(amazonSqs,
		// "realSQS") instanceof AmazonSQSAsyncClient).isTrue();
	}

}
