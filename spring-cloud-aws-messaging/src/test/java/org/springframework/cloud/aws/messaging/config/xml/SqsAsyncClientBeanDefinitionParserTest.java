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

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Test;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.core.task.ShutdownSuppressingExecutorServiceAdapter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SqsAsyncClientBeanDefinitionParserTest {

	@Test
	public void parseInternal_minimalConfiguration_createsBufferedClientWithoutExplicitTaskExecutor()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-minimal.xml", getClass()));

		// Assert
		SqsAsyncClient asyncClient = beanFactory.getBean("customClient",
				SqsAsyncClient.class);
		assertThat(asyncClient).isNotNull();
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
				.getField(asyncClient, "clientConfiguration");
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) clientConfiguration
				.option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR);
		assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(8);
	}

	// TODO SDK2 migration: re-visit after solving issue with clients
	@Test
	public void parseInternal_notBuffered_createsAsyncClientWithoutBufferedDecorator()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-not-buffered.xml", getClass()));

		// Assert
		SqsAsyncClient asyncClient = beanFactory.getBean("customClient",
				SqsAsyncClient.class);
		assertThat(asyncClient).isNotNull();
		assertThat(SqsAsyncClient.class.isInstance(asyncClient)).isTrue();
	}

	@Test
	public void parseInternal_withCustomTasExecutor_createsBufferedClientWithCustomTaskExecutor()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-task-executor.xml", getClass()));

		// Assert
		SqsAsyncClient asyncClient = beanFactory.getBean("customClient",
				SqsAsyncClient.class);
		assertThat(asyncClient).isNotNull();
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
				.getField(asyncClient, "clientConfiguration");
		Executor executor = clientConfiguration
				.option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR);
		ShutdownSuppressingExecutorServiceAdapter customExecutor = (ShutdownSuppressingExecutorServiceAdapter) ReflectionTestUtils
				.getField(executor, "executor");
		assertThat(ReflectionTestUtils.getField(customExecutor, "taskExecutor"))
				.isSameAs(beanFactory.getBean("myThreadPoolTaskExecutor"));
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
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
				.getField(amazonSqs, "clientConfiguration");
		assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT).toString())
				.isEqualTo("https://"
						+ ServiceMetadata.of("sqs").endpointFor(Region.EU_WEST_1));
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
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
				.getField(amazonSqs, "clientConfiguration");
		assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT).toString())
				.isEqualTo("https://"
						+ ServiceMetadata.of("sqs").endpointFor(Region.AP_SOUTHEAST_2));
	}

}
