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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Matej Nedic
 */
public class AwsParamStorePropertiesSuccessTest {

	@Test
	void awsParamStorePropertiesAreLoaded() {
		AwsParamStoreProperties awsParamStoreProperties = buildProperties();
		assertThat(awsParamStoreProperties).isNotNull();
		assertThat(awsParamStoreProperties.getPrefix()).isEqualTo("/con");
		assertThat(awsParamStoreProperties.getPrefix()).isNotNull();
		assertThat(awsParamStoreProperties.getDefaultContext()).isEqualTo("app");
		assertThat(awsParamStoreProperties.getDefaultContext()).isNotNull();
		assertThat(awsParamStoreProperties.getProfileSeparator()).isEqualTo(".");
		assertThat(awsParamStoreProperties.getProfileSeparator()).isNotNull();
	}

	private static AwsParamStoreProperties buildProperties() {
		AwsParamStoreProperties awsParamStoreProperties = new AwsParamStoreProperties();
		awsParamStoreProperties.setPrefix("/con");
		awsParamStoreProperties.setDefaultContext("app");
		awsParamStoreProperties.setProfileSeparator(".");
		return awsParamStoreProperties;
	}

}
