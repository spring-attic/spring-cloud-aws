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

package org.springframework.cloud.aws.secretsmanager;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AwsSecretsManagerProperties
 * @author Matej Nedic
 **/
public class AwsSecretsManagerPropertiesFailTest {

	private static final HashMap<ErrorCode, String> errorCodes = new HashMap() {
		{
			put(ErrorCode.PREF_NULL, "prefix should not be empty or null.");
			put(ErrorCode.PREF_PATTERN_WRONG,
					"\"The prefix must have pattern of:  (/[a-zA-Z0-9.\\\\-_]+)*\"");
			put(ErrorCode.DC_NULL, "defaultContext should not be empty or null.");
			put(ErrorCode.PS_NULL, "profileSeparator should not be empty or null.");
			put(ErrorCode.PS_PATTERN_WRONG,
					"\"The profileSeparator must have pattern of:  [a-zA-Z0-9.\\\\-_/]+");
		}
	};

	@ParameterizedTest
	@MethodSource("provideCase")
	public void AwsSecretsManagerProperties_Fail(String prefix, String defaultContext, String profileSeparator,
			String message) {
		AwsSecretsManagerProperties properties = buildAwsParamStoreProperties(prefix,
				defaultContext, profileSeparator);
		Errors errors = new BeanPropertyBindingResult(properties, "properties");
		properties.validate(properties, errors);
		assertThat(errors.getAllErrors().stream().filter(error -> Objects
			.equals(error.getDefaultMessage(), message)).findAny());
	}

	private static Stream<Arguments> provideCase() {
		return Stream.of(
				Arguments.of("", "application", "_", errorCodes.get(ErrorCode.PREF_NULL)),
				Arguments.of("!.", "application", "_",
						errorCodes.get(ErrorCode.PREF_PATTERN_WRONG)),
				Arguments.of("/secret", "", "_", errorCodes.get(ErrorCode.DC_NULL)),
				Arguments.of("/secret", "application", "", errorCodes.get(ErrorCode.PS_NULL)),
				Arguments.of("/secret", "application", "!_",
						errorCodes.get(ErrorCode.PS_PATTERN_WRONG)));
	}

	private static AwsSecretsManagerProperties buildAwsParamStoreProperties(String prefix,
			String defaultContext, String profileSeparator) {
		AwsSecretsManagerProperties awsSecretsManagerProperties = new AwsSecretsManagerProperties();
		awsSecretsManagerProperties.setPrefix(prefix);
		awsSecretsManagerProperties.setDefaultContext(defaultContext);
		awsSecretsManagerProperties.setProfileSeparator(profileSeparator);
		return awsSecretsManagerProperties;
	}

	private enum ErrorCode {
		PREF_NULL, PREF_PATTERN_WRONG, DC_NULL, PS_NULL, PS_PATTERN_WRONG
	}
}
