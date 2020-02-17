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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.aws.context.config.annotation.EnableContextRegion;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class EventBridgeConfigurationTest {

	private AnnotationConfigWebApplicationContext webApplicationContext;

	@Before
	public void setUp() throws Exception {
		this.webApplicationContext = new AnnotationConfigWebApplicationContext();
		this.webApplicationContext.setServletContext(new MockServletContext());
	}

	@Test
	public void enableEventBridge_withMinimalConfig__shouldBeUsedToCreateClient()
			throws Exception {
		// Arrange & Act
		this.webApplicationContext.register(MinimalEventBridgeConfiguration.class);
		this.webApplicationContext.refresh();
		AmazonCloudWatchEvents amazonEvents = this.webApplicationContext
				.getBean(AmazonCloudWatchEvents.class);

		// Assert
		assertThat(amazonEvents).isNotNull();
	}

	@Test
	public void enableEventBridge_withProvidedCredentials_shouldBeUsedToCreateClient()
			throws Exception {
		// Arrange & Act
		this.webApplicationContext
				.register(EventBridgeConfigurationWithCredentials.class);
		this.webApplicationContext.refresh();
		AmazonCloudWatchEvents amazonEvents = this.webApplicationContext
				.getBean(AmazonCloudWatchEvents.class);

		// Assert
		assertThat(amazonEvents).isNotNull();
		assertThat(ReflectionTestUtils.getField(amazonEvents, "awsCredentialsProvider"))
				.isEqualTo(
						EventBridgeConfigurationWithCredentials.AWS_CREDENTIALS_PROVIDER);
	}

	@Test
	public void enableEventBridge_withCustomAmazonSnsClient_shouldBeUsedToCreateClient()
			throws Exception {
		// Arrange & Act
		this.webApplicationContext
				.register(EventBridgeConfigurationWithCustomAmazonClient.class);
		this.webApplicationContext.refresh();
		AmazonCloudWatchEvents amazonEvents = this.webApplicationContext
				.getBean(AmazonCloudWatchEvents.class);

		// Assert
		assertThat(amazonEvents).isNotNull();
		assertThat(amazonEvents)
				.isEqualTo(EventBridgeConfigurationWithCustomAmazonClient.AMAZON_EVENTS);
	}

	@Test
	public void enableSns_withRegionProvided_shouldBeUsedToCreateClient()
			throws Exception {
		// Arrange & Act
		this.webApplicationContext
				.register(EventBridgeConfigurationWithRegionProvider.class);
		this.webApplicationContext.refresh();
		AmazonCloudWatchEvents amazonEvents = this.webApplicationContext
				.getBean(AmazonCloudWatchEvents.class);

		// Assert
		assertThat(ReflectionTestUtils.getField(amazonEvents, "endpoint").toString())
				.isEqualTo("https://" + Region.getRegion(Regions.EU_WEST_1)
						.getServiceEndpoint("events"));
	}

	@EnableWebMvc
	@EnableEventBridge
	protected static class MinimalEventBridgeConfiguration {

	}

	@EnableWebMvc
	@EnableEventBridge
	protected static class EventBridgeConfigurationWithCredentials {

		public static final AWSCredentialsProvider AWS_CREDENTIALS_PROVIDER = mock(
				AWSCredentialsProvider.class);

		@Bean
		public AWSCredentialsProvider awsCredentialsProvider() {
			return AWS_CREDENTIALS_PROVIDER;
		}

	}

	@EnableWebMvc
	@EnableEventBridge
	protected static class EventBridgeConfigurationWithCustomAmazonClient {

		public static final AmazonCloudWatchEvents AMAZON_EVENTS = mock(
				AmazonCloudWatchEvents.class);

		@Bean
		public AmazonCloudWatchEvents amazonEvents() {
			return AMAZON_EVENTS;
		}

	}

	@EnableWebMvc
	@EnableContextRegion(region = "eu-west-1")
	@EnableEventBridge
	protected static class EventBridgeConfigurationWithRegionProvider {

	}

}
