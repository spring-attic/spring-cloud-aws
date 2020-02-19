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

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.sns.SnsClient;

import org.springframework.cloud.aws.context.config.annotation.EnableContextRegion;
import org.springframework.cloud.aws.messaging.endpoint.NotificationStatusHandlerMethodArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Alain Sahli
 */
public class SnsConfigurationTest {

	private AnnotationConfigWebApplicationContext webApplicationContext;

	@Before
	public void setUp() throws Exception {
		this.webApplicationContext = new AnnotationConfigWebApplicationContext();
		this.webApplicationContext.setServletContext(new MockServletContext());
	}

	@Test
	public void enableSns_withMinimalConfig_shouldConfigureACompositeArgumentResolver()
			throws Exception {
		// Arrange & Act
		this.webApplicationContext.register(MinimalSnsConfiguration.class);
		this.webApplicationContext.refresh();
		RequestMappingHandlerAdapter requestMappingHandlerAdapter = this.webApplicationContext
				.getBean(RequestMappingHandlerAdapter.class);

		// Assert
		assertThat(requestMappingHandlerAdapter.getCustomArgumentResolvers().size())
				.isEqualTo(1);
		HandlerMethodArgumentResolver argumentResolver = requestMappingHandlerAdapter
				.getCustomArgumentResolvers().get(0);
		assertThat(
				HandlerMethodArgumentResolverComposite.class.isInstance(argumentResolver))
						.isTrue();

		HandlerMethodArgumentResolverComposite compositeArgumentResolver;
		compositeArgumentResolver = (HandlerMethodArgumentResolverComposite) argumentResolver;
		assertThat(compositeArgumentResolver.getResolvers().size()).isEqualTo(3);
		assertThat(
				ReflectionTestUtils
						.getField(
								getNotificationStatusHandlerMethodArgumentResolver(
										compositeArgumentResolver.getResolvers()),
								"amazonSns")).isNotNull();
	}

	@Test
	public void enableSns_withProvidedCredentials_shouldBeUsedToCreateClient()
			throws Exception {
		// Arrange & Act
		this.webApplicationContext.register(SnsConfigurationWithCredentials.class);
		this.webApplicationContext.refresh();
		SnsClient amazonSns = this.webApplicationContext.getBean(SnsClient.class);

		// Assert
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
				.getField(amazonSns, "clientConfiguration");
		assertThat(clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER))
				.isEqualTo(SnsConfigurationWithCredentials.AWS_CREDENTIALS_PROVIDER);
	}

	@Test
	public void enableSns_withCustomAmazonSnsClient_shouldBeUsedByTheArgumentResolver()
			throws Exception {
		// Arrange & Act
		this.webApplicationContext.register(SnsConfigurationWithCustomAmazonClient.class);
		this.webApplicationContext.refresh();
		RequestMappingHandlerAdapter requestMappingHandlerAdapter = this.webApplicationContext
				.getBean(RequestMappingHandlerAdapter.class);

		// Assert
		HandlerMethodArgumentResolverComposite handlerMethodArgumentResolver;
		handlerMethodArgumentResolver = (HandlerMethodArgumentResolverComposite) requestMappingHandlerAdapter
				.getCustomArgumentResolvers().get(0);
		NotificationStatusHandlerMethodArgumentResolver notificationStatusHandlerMethodArgumentResolver;
		notificationStatusHandlerMethodArgumentResolver = getNotificationStatusHandlerMethodArgumentResolver(
				handlerMethodArgumentResolver.getResolvers());
		assertThat(ReflectionTestUtils
				.getField(notificationStatusHandlerMethodArgumentResolver, "amazonSns"))
						.isEqualTo(SnsConfigurationWithCustomAmazonClient.AMAZON_SNS);
	}

	@Test
	public void enableSns_withRegionProvided_shouldBeUsedToCreateClient()
			throws Exception {
		// Arrange & Act
		this.webApplicationContext.register(SnsConfigurationWithRegionProvider.class);
		this.webApplicationContext.refresh();
		SnsClient amazonSns = this.webApplicationContext.getBean(SnsClient.class);

		// Assert
		SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils
				.getField(amazonSns, "clientConfiguration");
		assertThat(clientConfiguration.option(SdkClientOption.ENDPOINT).toString())
				.isEqualTo("https://"
						+ ServiceMetadata.of("sns").endpointFor(Region.EU_WEST_1));
	}

	private NotificationStatusHandlerMethodArgumentResolver getNotificationStatusHandlerMethodArgumentResolver(
			List<HandlerMethodArgumentResolver> resolvers) {
		for (HandlerMethodArgumentResolver resolver : resolvers) {
			if (resolver instanceof NotificationStatusHandlerMethodArgumentResolver) {
				return (NotificationStatusHandlerMethodArgumentResolver) resolver;

			}
		}

		return null;
	}

	@EnableWebMvc
	@EnableSns
	protected static class MinimalSnsConfiguration {

	}

	@EnableWebMvc
	@EnableSns
	protected static class SnsConfigurationWithCredentials {

		public static final AwsCredentialsProvider AWS_CREDENTIALS_PROVIDER = mock(
				AwsCredentialsProvider.class);

		@Bean
		public AwsCredentialsProvider awsCredentialsProvider() {
			return AWS_CREDENTIALS_PROVIDER;
		}

	}

	@EnableWebMvc
	@EnableSns
	protected static class SnsConfigurationWithCustomAmazonClient {

		public static final SnsClient AMAZON_SNS = mock(SnsClient.class);

		@Bean
		public SnsClient amazonSNS() {
			return AMAZON_SNS;
		}

	}

	@EnableWebMvc
	@EnableContextRegion(region = "eu-west-1")
	@EnableSns
	protected static class SnsConfigurationWithRegionProvider {

	}

}
