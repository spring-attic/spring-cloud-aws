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

package org.springframework.cloud.aws.context.config.annotation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextCredentialsConfigurationRegistrarTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		System.clearProperty(
				ProfileFileSystemSetting.AWS_SHARED_CREDENTIALS_FILE.property());
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void credentialsProvider_defaultCredentialsProviderWithoutFurtherConfig_awsCredentialsProviderConfigured()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithDefaultCredentialsProvider.class);

		// Act
		AwsCredentialsProvider awsCredentialsProvider = this.context
				.getBean(AwsCredentialsProvider.class);

		// Assert
		assertThat(awsCredentialsProvider).isNotNull();
		assertThat(DefaultCredentialsProvider.class.isInstance(awsCredentialsProvider))
				.isTrue();
	}

	@Test
	public void credentialsProvider_configWithAccessAndSecretKey_staticAwsCredentialsProviderConfigured()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithAccessKeyAndSecretKey.class);

		// Act
		AwsCredentialsProvider awsCredentialsProvider = this.context
				.getBean(AwsCredentialsProvider.class);

		// Assert
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(1);
		assertThat(
				StaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)))
						.isTrue();

		assertThat(awsCredentialsProvider.resolveCredentials().accessKeyId())
				.isEqualTo("accessTest");
		assertThat(awsCredentialsProvider.resolveCredentials().secretAccessKey())
				.isEqualTo("testSecret");
	}

	// @checkstyle:off
	@Test
	public void credentialsProvider_configWithAccessAndSecretKeyAsExpressions_staticAwsCredentialsProviderConfiguredWithResolvedExpressions()
			throws Exception {
		// @checkstyle:on
		// Arrange
		this.context = new AnnotationConfigApplicationContext();

		Map<String, Object> secretAndAccessKeyMap = new HashMap<>();
		secretAndAccessKeyMap.put("accessKey", "accessTest");
		secretAndAccessKeyMap.put("secretKey", "testSecret");

		this.context.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("test", secretAndAccessKeyMap));

		this.context.register(
				ApplicationConfigurationWithAccessKeyAndSecretKeyAsExpressions.class);
		this.context.refresh();
		// Act
		AwsCredentialsProvider awsCredentialsProvider = this.context
				.getBean(AwsCredentialsProvider.class);

		// Assert
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(1);
		assertThat(
				StaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)))
						.isTrue();

		assertThat(awsCredentialsProvider.resolveCredentials().accessKeyId())
				.isEqualTo("accessTest");
		assertThat(awsCredentialsProvider.resolveCredentials().secretAccessKey())
				.isEqualTo("testSecret");
	}

	// @checkstyle:off
	@Test
	public void credentialsProvider_configWithAccessAndSecretKeyAsPlaceHolders_staticAwsCredentialsProviderConfiguredWithResolvedPlaceHolders()
			throws Exception {
		// @checkstyle:on
		// Arrange
		this.context = new AnnotationConfigApplicationContext();

		Map<String, Object> secretAndAccessKeyMap = new HashMap<>();
		secretAndAccessKeyMap.put("accessKey", "accessTest");
		secretAndAccessKeyMap.put("secretKey", "testSecret");

		this.context.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("test", secretAndAccessKeyMap));
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setPropertySources(this.context.getEnvironment().getPropertySources());

		this.context.getBeanFactory().registerSingleton("configurer", configurer);
		this.context.register(
				ApplicationConfigurationWithAccessKeyAndSecretKeyAsPlaceHolder.class);
		this.context.refresh();
		// Act
		AwsCredentialsProvider awsCredentialsProvider = this.context
				.getBean(AwsCredentialsProvider.class);

		// Assert
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(1);
		assertThat(
				StaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)))
						.isTrue();

		assertThat(awsCredentialsProvider.resolveCredentials().accessKeyId())
				.isEqualTo("accessTest");
		assertThat(awsCredentialsProvider.resolveCredentials().secretAccessKey())
				.isEqualTo("testSecret");
	}

	// @checkstyle:off
	@Test
	public void credentialsProvider_configWithAccessAndSecretKeyAndInstanceProfile_staticAwsCredentialsProviderConfiguredWithInstanceProfile()
			throws Exception {
		// @checkstyle:on
		// Arrange
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithAccessKeyAndSecretKeyAndInstanceProfile.class);

		// Act
		AwsCredentialsProvider awsCredentialsProvider = this.context
				.getBean(AwsCredentialsProvider.class);

		// Assert
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(2);
		assertThat(
				StaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)))
						.isTrue();
		assertThat(InstanceProfileCredentialsProvider.class
				.isInstance(credentialsProviders.get(1))).isTrue();
	}

	@Test
	public void credentialsProvider_configWithInstanceProfile_instanceProfileCredentialsProviderConfigured()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithInstanceProfileOnly.class);

		// Act
		AwsCredentialsProvider awsCredentialsProvider = this.context
				.getBean(AwsCredentialsProvider.class);

		// Assert
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(1);
		assertThat(InstanceProfileCredentialsProvider.class
				.isInstance(credentialsProviders.get(0))).isTrue();
	}

	@Test
	public void credentialsProvider_configWithProfileNameAndNoProfilePath_profileCredentialsProviderConfigured()
			throws Exception {
		// Arrange
		System.setProperty(
				ProfileFileSystemSetting.AWS_SHARED_CREDENTIALS_FILE.property(),
				new ClassPathResource(getClass().getSimpleName() + "-profile", getClass())
						.getFile().getAbsolutePath());
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithProfileAndDefaultProfilePath.class);

		// Act
		AwsCredentialsProvider awsCredentialsProvider = this.context
				.getBean(AwsCredentialsProvider.class);

		// Assert
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(1);
		assertThat(
				ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(0)))
						.isTrue();

		ProfileCredentialsProvider provider = (ProfileCredentialsProvider) credentialsProviders
				.get(0);
		assertThat(ReflectionTestUtils.getField(provider, "profileName"))
				.isEqualTo("customProfile");
	}

	@Test
	public void credentialsProvider_configWithProfileNameAndCustomProfilePath_profileCredentialsProviderConfigured()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();

		Map<String, Object> secretAndAccessKeyMap = new HashMap<>();
		secretAndAccessKeyMap.put("profilePath",
				new ClassPathResource(getClass().getSimpleName() + "-profile", getClass())
						.getFile().getAbsolutePath());

		this.context.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("test", secretAndAccessKeyMap));
		PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
		configurer.setPropertySources(this.context.getEnvironment().getPropertySources());

		this.context.getBeanFactory().registerSingleton("configurer", configurer);
		this.context
				.register(ApplicationConfigurationWithProfileAndCustomProfilePath.class);
		this.context.refresh();

		// Act
		AwsCredentialsProvider awsCredentialsProvider = this.context
				.getBean(AwsCredentialsProvider.class);

		// Assert
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(1);
		assertThat(
				ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(0)))
						.isTrue();

		ProfileCredentialsProvider provider = (ProfileCredentialsProvider) credentialsProviders
				.get(0);
		assertThat(provider.resolveCredentials().accessKeyId())
				.isEqualTo("testAccessKey");
		assertThat(provider.resolveCredentials().secretAccessKey())
				.isEqualTo("testSecretKey");
	}

	@Test
	public void credentialsProvider_configWithAllProviders_allCredentialsProvidersConfigured()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithAllProviders.class);

		// Act
		AwsCredentialsProvider awsCredentialsProvider = this.context
				.getBean(AwsCredentialsProvider.class);

		// Assert
		assertThat(awsCredentialsProvider).isNotNull();

		@SuppressWarnings("unchecked")
		List<AwsCredentialsProvider> credentialsProviders = (List<AwsCredentialsProvider>) ReflectionTestUtils
				.getField(awsCredentialsProvider, "credentialsProviders");
		assertThat(credentialsProviders.size()).isEqualTo(3);
		assertThat(
				StaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)))
						.isTrue();
		assertThat(InstanceProfileCredentialsProvider.class
				.isInstance(credentialsProviders.get(1))).isTrue();
		assertThat(
				ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(2)))
						.isTrue();
	}

	@EnableContextCredentials
	public static class ApplicationConfigurationWithDefaultCredentialsProvider {

	}

	@EnableContextCredentials(accessKey = "accessTest", secretKey = "testSecret")
	public static class ApplicationConfigurationWithAccessKeyAndSecretKey {

	}

	@EnableContextCredentials(accessKey = "#{environment.accessKey}",
			secretKey = "#{environment.secretKey}")
	public static class ApplicationConfigurationWithAccessKeyAndSecretKeyAsExpressions {

	}

	@EnableContextCredentials(accessKey = "${accessKey}", secretKey = "${secretKey}")
	public static class ApplicationConfigurationWithAccessKeyAndSecretKeyAsPlaceHolder {

	}

	@EnableContextCredentials(accessKey = "accessTest", secretKey = "testSecret",
			instanceProfile = true)
	public static class ApplicationConfigurationWithAccessKeyAndSecretKeyAndInstanceProfile {

	}

	@EnableContextCredentials(instanceProfile = true)
	public static class ApplicationConfigurationWithInstanceProfileOnly {

	}

	@EnableContextCredentials(profileName = "customProfile")
	public static class ApplicationConfigurationWithProfileAndDefaultProfilePath {

	}

	@EnableContextCredentials(profileName = "customProfile",
			profilePath = "${profilePath}")
	public static class ApplicationConfigurationWithProfileAndCustomProfilePath {

	}

	// @checkstyle:off
	@EnableContextCredentials(accessKey = "accessTest", secretKey = "testSecret",
			instanceProfile = true, profileName = "customProfile")
	public static class ApplicationConfigurationWithAllProviders {

	}
	// @checkstyle:on

}
