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

package org.springframework.cloud.aws.autoconfigure.paramstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.paramstore.AwsParamStorePropertySourceLocator;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AwsParamStoreBootstrapConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(AwsParamStoreBootstrapConfiguration.class))
			.withUserConfiguration(TestConfig.class);

	@Test
	void paramStoreIsEnabled() {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(AwsParamStorePropertySourceLocator.class));
	}

	@Test
	void paramStoreIsDisabled() {
		this.contextRunner.withPropertyValues("aws.paramstore.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(AwsParamStorePropertySourceLocator.class));
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		AWSSimpleSystemsManagement ssmClient() {
			return mock(AWSSimpleSystemsManagement.class);
		}

	}

}
