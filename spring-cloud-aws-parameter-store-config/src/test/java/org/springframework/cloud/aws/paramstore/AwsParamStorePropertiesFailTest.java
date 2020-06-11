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
 * Tests for AwsParamStoreProperties validity.
 *
 * @author Matej Nedic
 */
public class AwsParamStorePropertiesFailTest {

	private static final HashMap errorCodes = new HashMap() {
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
	public void AwsParamStoreProperties_Fail(String prefix, String defaultContext,
			String profileSeparator, String message) {
		AwsParamStoreProperties properties = buildAwsParamStoreProperties(prefix,
				defaultContext, profileSeparator);
		Errors errors = new BeanPropertyBindingResult(properties, "properties");
		assertThat(errors.getAllErrors().stream()
				.filter(error -> Objects.equals(error.getDefaultMessage(), message))
				.findAny());
	}

	private static Stream<Arguments> provideCase() {
		return Stream.of(
				Arguments.of("", "application", "_", errorCodes.get(ErrorCode.PREF_NULL)),
				Arguments.of("!.", "application", "_",
						errorCodes.get(ErrorCode.PREF_PATTERN_WRONG)),
				Arguments.of("/config", "", "_", errorCodes.get(ErrorCode.DC_NULL)),
				Arguments.of("/config", "application", "",
						errorCodes.get(ErrorCode.PS_NULL)),
				Arguments.of("/config", "application", "!_",
						errorCodes.get(ErrorCode.PS_PATTERN_WRONG)));
	}

	private static AwsParamStoreProperties buildAwsParamStoreProperties(String prefix,
			String defaultContext, String profileSeparator) {
		AwsParamStoreProperties awsParamStoreProperties = new AwsParamStoreProperties();
		awsParamStoreProperties.setPrefix(prefix);
		awsParamStoreProperties.setDefaultContext(defaultContext);
		awsParamStoreProperties.setProfileSeparator(profileSeparator);
		return awsParamStoreProperties;
	}

	private enum ErrorCode {

		PREF_NULL, PREF_PATTERN_WRONG, DC_NULL, PS_NULL, PS_PATTERN_WRONG

	}

}
