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

package org.springframework.cloud.aws.paramstore;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AwsParamStorePropertySourceLocator}.
 *
 * @author Matej Nedic
 * @author Pete Guyatt
 */
public class AwsParamStorePropertySourceLocatorTest {

	private AWSSimpleSystemsManagement ssmClient = mock(AWSSimpleSystemsManagement.class);

	private MockEnvironment env = new MockEnvironment();

	@Test
	public void contextExpectedToHave2Elements() {
		AwsParamStoreProperties properties = new AwsParamStorePropertiesBuilder()
				.withDefaultContext("application").withName("application").build();

		GetParametersByPathResult firstResult = getFirstResult();
		GetParametersByPathResult nextResult = getNextResult();
		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class)))
				.thenReturn(firstResult, nextResult);

		AwsParamStorePropertySourceLocator locator = new AwsParamStorePropertySourceLocator(
				ssmClient, properties);
		env.setActiveProfiles("test");
		locator.locate(env);

		assertThat(locator.getContexts()).hasSize(2);
	}

	@Test
	public void contextExpectedToHave4Elements() {
		AwsParamStoreProperties properties = new AwsParamStorePropertiesBuilder()
				.withDefaultContext("application").withName("messaging-service").build();

		GetParametersByPathResult firstResult = getFirstResult();
		GetParametersByPathResult nextResult = getNextResult();
		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class)))
				.thenReturn(firstResult, nextResult);

		AwsParamStorePropertySourceLocator locator = new AwsParamStorePropertySourceLocator(
				ssmClient, properties);
		env.setActiveProfiles("test");
		locator.locate(env);

		assertThat(locator.getContexts()).hasSize(4);
	}

	@Test
	public void contextSpecificOrderExpected() {
		AwsParamStoreProperties properties = new AwsParamStorePropertiesBuilder()
				.withDefaultContext("application").withName("messaging-service").build();

		GetParametersByPathResult firstResult = getFirstResult();
		GetParametersByPathResult nextResult = getNextResult();
		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class)))
				.thenReturn(firstResult, nextResult);

		AwsParamStorePropertySourceLocator locator = new AwsParamStorePropertySourceLocator(
				ssmClient, properties);
		env.setActiveProfiles("test", "more-specific", "most-specific");
		locator.locate(env);

		List<String> contextToBeTested = new ArrayList<>(locator.getContexts());

		assertThat(contextToBeTested.get(0))
				.isEqualTo("application/messaging-service_most-specific/");
		assertThat(contextToBeTested.get(1))
				.isEqualTo("application/messaging-service_more-specific/");
		assertThat(contextToBeTested.get(2))
				.isEqualTo("application/messaging-service_test/");
		assertThat(contextToBeTested.get(3)).isEqualTo("application/messaging-service/");
		assertThat(contextToBeTested.get(4))
				.isEqualTo("application/application_most-specific/");
		assertThat(contextToBeTested.get(5))
				.isEqualTo("application/application_more-specific/");
		assertThat(contextToBeTested.get(6)).isEqualTo("application/application_test/");
		assertThat(contextToBeTested.get(7)).isEqualTo("application/application/");
	}

	@Test
	public void contextWithMultipleProfilesValidatePropertyPrecedence() {
		AwsParamStoreProperties properties = new AwsParamStorePropertiesBuilder()
				.withDefaultContext("context").withName("application").build();

		String[] activeProfiles = { "test", "more-specific", "most-specific" };
		String propertyKey = "my-test-value";

		when(ssmClient.getParametersByPath(
				eq(newGetParametersByPathRequest("context/application/")))).thenReturn(
						newGetParametersByPathResult("context/application/" + propertyKey,
								""));

		for (String profile : activeProfiles) {
			String path = String.format("context/application_%s/", profile);
			when(ssmClient.getParametersByPath(eq(newGetParametersByPathRequest(path))))
					.thenReturn(
							newGetParametersByPathResult(path + propertyKey, profile));
		}

		AwsParamStorePropertySourceLocator locator = new AwsParamStorePropertySourceLocator(
				ssmClient, properties);
		env.setActiveProfiles(activeProfiles);
		PropertySource<?> propertySource = locator.locate(env);

		assertThat(propertySource.getProperty(propertyKey))
				.isEqualTo(activeProfiles[activeProfiles.length - 1]);

		List<String> contexts = locator.getContexts();
		assertThat(contexts).hasSize(4);

		assertThat(contexts.get(0)).isEqualTo("context/application_most-specific/");
		assertThat(contexts.get(1)).isEqualTo("context/application_more-specific/");
		assertThat(contexts.get(2)).isEqualTo("context/application_test/");
		assertThat(contexts.get(3)).isEqualTo("context/application/");
	}

	private static GetParametersByPathResult newGetParametersByPathResult(String name,
			String value) {
		return newGetParametersByPathResult(newParameter(name, value));
	}

	private static GetParametersByPathResult newGetParametersByPathResult(
			Parameter... parameters) {
		return new GetParametersByPathResult().withParameters(parameters);
	}

	private static GetParametersByPathRequest newGetParametersByPathRequest(String path) {
		return new GetParametersByPathRequest().withPath(path).withRecursive(true)
				.withWithDecryption(true);
	}

	public static Parameter newParameter(String name, String value) {
		return new Parameter().withName(name).withValue(value);
	}

	private static GetParametersByPathResult getNextResult() {
		return newGetParametersByPathResult(
				newParameter("/config/myservice/key3", "value3"),
				newParameter("/config/myservice/key4", "value3"));
	}

	private static GetParametersByPathResult getFirstResult() {
		return newGetParametersByPathResult(
				newParameter("/config/myservice/key3", "value3"),
				newParameter("/config/myservice/key4", "value3"));
	}

	private static final class AwsParamStorePropertiesBuilder {

		private final AwsParamStoreProperties properties = new AwsParamStoreProperties();

		private AwsParamStorePropertiesBuilder() {
		}

		public AwsParamStorePropertiesBuilder withDefaultContext(String defaultContext) {
			this.properties.setPrefix(defaultContext);
			return this;
		}

		public AwsParamStorePropertiesBuilder withName(String name) {
			this.properties.setName(name);
			return this;
		}

		public AwsParamStoreProperties build() {
			return this.properties;
		}

	}

}
