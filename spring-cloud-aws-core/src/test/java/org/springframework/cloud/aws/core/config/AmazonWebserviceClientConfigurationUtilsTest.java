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

package org.springframework.cloud.aws.core.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 */
public class AmazonWebserviceClientConfigurationUtilsTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void registerAmazonWebserviceClient_withMinimalConfiguration_returnsDefaultBeanDefinition()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				new StaticAwsCredentialsProvider());

		BeanDefinitionHolder beanDefinitionHolder = AmazonWebserviceClientConfigurationUtils
				.registerAmazonWebserviceClient(new Object(), beanFactory,
						AmazonTestWebserviceClient.class.getName(), null, null);

		// Act
		beanFactory.preInstantiateSingletons();
		AmazonTestWebserviceClient client = beanFactory.getBean(
				beanDefinitionHolder.getBeanName(), AmazonTestWebserviceClient.class);

		// Assert
		assertThat(client).isNotNull();
		assertThat(beanDefinitionHolder.getBeanName()).isEqualTo("amazonTestWebservice");
	}

	// @checkstyle:off
	@Test
	public void registerAmazonWebserviceClient_withCustomRegionProviderConfiguration_returnsBeanDefinitionWithRegionConfiguredThatIsReturnedByTheRegionProvider()
			throws Exception {
		// @checkstyle:on
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(
				CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME,
				new StaticAwsCredentialsProvider());
		beanFactory.registerSingleton("myRegionProvider",
				new StaticRegionProvider(Region.AP_SOUTHEAST_2.id()));

		BeanDefinitionHolder beanDefinitionHolder = AmazonWebserviceClientConfigurationUtils
				.registerAmazonWebserviceClient(new Object(), beanFactory,
						AmazonTestWebserviceClient.class.getName(), "myRegionProvider",
						null);

		// Act
		beanFactory.preInstantiateSingletons();
		AmazonTestWebserviceClient client = beanFactory.getBean(
				beanDefinitionHolder.getBeanName(), AmazonTestWebserviceClient.class);

		// Assert
		assertThat(client).isNotNull();
		assertThat(beanDefinitionHolder.getBeanName()).isEqualTo("amazonTestWebservice");
	}

	@Test
	public void registerAmazonWebserviceClient_withCustomRegionConfiguration_returnsBeanDefinitionWithRegionConfigured()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				new StaticAwsCredentialsProvider());

		BeanDefinitionHolder beanDefinitionHolder = AmazonWebserviceClientConfigurationUtils
				.registerAmazonWebserviceClient(new Object(), beanFactory,
						AmazonTestWebserviceClient.class.getName(), null,
						Region.EU_WEST_1.id());

		// Act
		beanFactory.preInstantiateSingletons();
		AmazonTestWebserviceClient client = beanFactory.getBean(
				beanDefinitionHolder.getBeanName(), AmazonTestWebserviceClient.class);

		// Assert
		assertThat(client).isNotNull();
		assertThat(beanDefinitionHolder.getBeanName()).isEqualTo("amazonTestWebservice");
	}

	@Test
	public void registerAmazonWebserviceClient_withCustomRegionAndRegionProviderConfigured_reportsError()
			throws Exception {
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage(
				"Only region or regionProvider can be configured, but not both");

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(
				AmazonWebserviceClientConfigurationUtils.CREDENTIALS_PROVIDER_BEAN_NAME,
				new StaticAwsCredentialsProvider());

		BeanDefinitionHolder beanDefinitionHolder = AmazonWebserviceClientConfigurationUtils
				.registerAmazonWebserviceClient(new Object(), beanFactory,
						AmazonTestWebserviceClient.class.getName(), "someProvider",
						Region.EU_WEST_1.id());

		// Act
		beanFactory.getBean(beanDefinitionHolder.getBeanName(),
				AmazonTestWebserviceClient.class);

		// Assert
	}

	@Test
	public void generateBeanName_withInterfaceAndCapitalLetterInSequence_producesDeCapitalizedBeanName()
			throws Exception {
		// Arrange

		// Act
		String beanName = AmazonWebserviceClientConfigurationUtils
				.getBeanName("com.amazonaws.services.rds.AmazonRDS");

		// Assert
		assertThat(beanName).isEqualTo("amazonRDS");
	}

	private static class StaticAwsCredentialsProvider implements AwsCredentialsProvider {

		@Override
		public AwsCredentials resolveCredentials() {
			return AwsBasicCredentials.create("test", "secret");
		}

	}

}
