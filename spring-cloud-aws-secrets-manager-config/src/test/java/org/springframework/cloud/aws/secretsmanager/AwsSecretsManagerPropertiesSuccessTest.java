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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { AwsSecretsManagerProperties.CONFIG_PREFIX + ".prefix=/sec",
		AwsSecretsManagerProperties.CONFIG_PREFIX + ".default-context=app",
		AwsSecretsManagerProperties.CONFIG_PREFIX + ".profile-separator=." }, classes = {
				AwsSecretsManagerConfiguration.class })
public class AwsSecretsManagerPropertiesSuccessTest {

	@Autowired
	AwsSecretsManagerProperties awsSecretsManagerProperties;

	@Test
	void awsSecretsManagerPropertiesAreLoaded() {
		assertThat(awsSecretsManagerProperties).isNotNull();
		assertThat(awsSecretsManagerProperties.getPrefix()).isEqualTo("/sec");
		assertThat(awsSecretsManagerProperties.getPrefix()).isNotNull();
		assertThat(awsSecretsManagerProperties.getDefaultContext()).isEqualTo("app");
		assertThat(awsSecretsManagerProperties.getDefaultContext()).isNotNull();
		assertThat(awsSecretsManagerProperties.getProfileSeparator()).isEqualTo(".");
		assertThat(awsSecretsManagerProperties.getProfileSeparator()).isNotNull();
	}
}
