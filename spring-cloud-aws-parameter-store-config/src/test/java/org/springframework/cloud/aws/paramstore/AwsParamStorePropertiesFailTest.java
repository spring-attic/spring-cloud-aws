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

package org.springframework.cloud.aws.paramstore;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AwsParamStorePropertiesFailTest {

	SpringApplication application;

	Properties properties;

	private static String prefix = AwsParamStoreProperties.CONFIG_PREFIX
			.concat(".prefix");

	private static String defaultContext = AwsParamStoreProperties.CONFIG_PREFIX
			.concat(".default-context");

	private static String profileSeparator = AwsParamStoreProperties.CONFIG_PREFIX
			.concat(".profile-separator");

	@BeforeEach
	public void setup() {
		// create Spring Application dynamically
		application = new SpringApplication(AwsParamStorePropertiesValidationTest.class);

		// setting test properties for our Spring Application
		properties = new Properties();

		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.addFirst(new PropertiesPropertySource(
				"application-validation.properties", properties));
		application.setEnvironment(environment);
	}

	@Test
	public void whenGivenPrefixNotInPattern_thenFail() {

		properties.put(prefix, "!/secret");
		properties.put(defaultContext, "app");
		properties.put(profileSeparator, "_");

		assertThatThrownBy(application::run)
				.isInstanceOf(ConfigurationPropertiesBindException.class)
				.hasRootCauseInstanceOf(BindValidationException.class)
				.hasStackTraceContaining(prefix.concat(
						"\" from property source \"application-validation.properties"))
				.hasStackTraceContaining(
						"The prefix must have pattern of:  (/[a-zA-Z0-9.\\-_]+)*");

	}

	@Test
	public void whenGivenEmptyPrefix_thenFail() {

		properties.put(prefix, "");
		properties.put(defaultContext, "app");
		properties.put(profileSeparator, "_");

		assertThatThrownBy(application::run)
				.isInstanceOf(ConfigurationPropertiesBindException.class)
				.hasRootCauseInstanceOf(BindValidationException.class)
				.hasStackTraceContaining((prefix.concat(
						"\" from property source \"application-validation.properties")))
				.hasStackTraceContaining("prefix should not be empty or null.");

	}

	@Test
	public void whenGivenEmptyProfileSeparator_thenFail() {

		properties.put(prefix, "/secert");
		properties.put(defaultContext, "app");
		properties.put(profileSeparator, "");

		assertThatThrownBy(application::run)
				.isInstanceOf(ConfigurationPropertiesBindException.class)
				.hasRootCauseInstanceOf(BindValidationException.class)
				.hasStackTraceContaining((profileSeparator.concat(
						"\" from property source \"application-validation.properties")))
				.hasStackTraceContaining("profileSeparator should not be empty or null.");
	}

	@Test
	public void whenGivenEmptyDefaultContext_thenFail() {

		properties.put(prefix, "/secert");
		properties.put(defaultContext, "");
		properties.put(profileSeparator, "_");

		assertThatThrownBy(application::run)
				.isInstanceOf(ConfigurationPropertiesBindException.class)
				.hasRootCauseInstanceOf(BindValidationException.class)
				.hasStackTraceContaining((defaultContext.concat(
						"\" from property source \"application-validation.properties")))
				.hasStackTraceContaining("defaultContext should not be empty or null.");
	}

	@Test
	public void whenGivenProfileSeparatorNotInPattern_thenFail() {

		properties.put(prefix, "/secret");
		properties.put(defaultContext, "app");
		properties.put(profileSeparator, "!");

		assertThatThrownBy(application::run)
				.isInstanceOf(ConfigurationPropertiesBindException.class)
				.hasRootCauseInstanceOf(BindValidationException.class)
				.hasStackTraceContaining(profileSeparator.concat(
						"\" from property source \"application-validation.properties"))
				.hasStackTraceContaining(
						"The profileSeparator must have pattern of:  [a-zA-Z0-9.\\-_/]+");
	}

}
