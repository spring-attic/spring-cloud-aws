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

package org.springframework.cloud.aws.parameterstore;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AwsParamStorePropertySourceLocator}.
 *
 * @author Matej Nedic
 * @author Eddú Meléndez
 */
public class AwsParamStorePropertySourceLocatorTest {

	private AWSSimpleSystemsManagement ssmClient = mock(AWSSimpleSystemsManagement.class);

	private MockEnvironment env = new MockEnvironment();

	@Test
	void contextExpectedToHave2Elements() {
		GetParametersByPathResult firstResult = getFirstResult();
		GetParametersByPathResult nextResult = getNextResult();
		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(firstResult, nextResult);

		AwsParamStorePropertySourceLocator locator = new AwsParamStorePropertySourceLocator(ssmClient);
		locator.setPrefix("application");
		locator.setName("application");
		env.setActiveProfiles("test");
		locator.locate(env);

		assertThat(locator.getContexts()).hasSize(2);
	}

	@Test
	void contextExpectedToHave4Elements() {
		GetParametersByPathResult firstResult = getFirstResult();
		GetParametersByPathResult nextResult = getNextResult();
		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(firstResult, nextResult);

		AwsParamStorePropertySourceLocator locator = new AwsParamStorePropertySourceLocator(ssmClient);
		locator.setPrefix("application");
		locator.setName("messaging-service");
		env.setActiveProfiles("test");
		locator.locate(env);

		assertThat(locator.getContexts()).hasSize(4);
	}

	@Test
	void contextSpecificOrderExpected() {
		GetParametersByPathResult firstResult = getFirstResult();
		GetParametersByPathResult nextResult = getNextResult();
		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(firstResult, nextResult);

		AwsParamStorePropertySourceLocator locator = new AwsParamStorePropertySourceLocator(ssmClient);
		locator.setPrefix("application");
		locator.setName("messaging-service");
		env.setActiveProfiles("test");
		locator.locate(env);

		List<String> contextToBeTested = new ArrayList<>(locator.getContexts());

		assertThat(contextToBeTested.get(0)).isEqualTo("application/messaging-service_test/");
		assertThat(contextToBeTested.get(1)).isEqualTo("application/messaging-service/");
		assertThat(contextToBeTested.get(2)).isEqualTo("application/application_test/");
		assertThat(contextToBeTested.get(3)).isEqualTo("application/application/");
	}

	private static GetParametersByPathResult getNextResult() {
		return new GetParametersByPathResult().withParameters(
				new Parameter().withName("/config/myservice/key3").withValue("value3"),
				new Parameter().withName("/config/myservice/key4").withValue("value4"));
	}

	private static GetParametersByPathResult getFirstResult() {
		return new GetParametersByPathResult().withParameters(
				new Parameter().withName("/config/myservice/key3").withValue("value3"),
				new Parameter().withName("/config/myservice/key4").withValue("value4"));
	}

}
