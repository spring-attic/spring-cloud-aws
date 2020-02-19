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

import java.net.URI;

import org.junit.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.sns.SnsClient;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.getBeanName;

public class NotificationArgumentResolverBeanDefinitionParserTest {

	// @checkstyle:off
	@Test
	public void parseInternal_minimalConfiguration_configuresHandlerMethodArgumentResolverWithAmazonSnsImplicitlyConfigured()
			throws Exception {
		// @checkstyle:on
		// Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-minimal.xml", getClass());

		// Act
		HandlerMethodArgumentResolver argumentResolver = context
				.getBean(HandlerMethodArgumentResolver.class);

		// Assert
		assertThat(argumentResolver).isNotNull();
		assertThat(context.containsBean(getBeanName(SnsClient.class.getName()))).isTrue();
	}

	// @checkstyle:off
	@Test
	public void parseInternal_customRegion_configuresHandlerMethodArgumentResolverWithAmazonSnsImplicitlyConfiguredAndCustomRegionSet()
			throws Exception {
		// @checkstyle:on
		// Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-customRegion.xml", getClass());

		// Act
		SnsClient snsClient = context.getBean(SnsClient.class);

		// Assert
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
				.getField(snsClient, "clientConfiguration");
		assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT))
				.isEqualTo(new URI("https", ServiceMetadata.of("sns")
						.endpointFor(Region.EU_WEST_1).toString(), null, null));
	}

	// @checkstyle:off
	@Test
	public void parseInternal_customRegionProvider_configuresHandlerMethodArgumentResolverWithAmazonSnsImplicitlyConfiguredAndCustomRegionSet()
			throws Exception {
		// @checkstyle:on
		// Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-customRegionProvider.xml", getClass());

		// Act
		SnsClient snsClient = context.getBean(SnsClient.class);

		// Assert
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
				.getField(snsClient, "clientConfiguration");
		assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT))
				.isEqualTo(new URI("https", ServiceMetadata.of("sns")
						.endpointFor(Region.US_WEST_2).toString(), null, null));
	}

	@Test
	public void parseInternal_customSnsClient_configuresHandlerMethodArgumentResolverWithCustomSnsClient()
			throws Exception {
		// Arrange
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-customSnsClient.xml", getClass());

		// Act
		SnsClient snsClient = context.getBean("customSnsClient", SnsClient.class);

		// Assert
		assertThat(snsClient).isNotNull();
		assertThat(context.containsBean(getBeanName(SnsClient.class.getName())))
				.isFalse();
	}

}
