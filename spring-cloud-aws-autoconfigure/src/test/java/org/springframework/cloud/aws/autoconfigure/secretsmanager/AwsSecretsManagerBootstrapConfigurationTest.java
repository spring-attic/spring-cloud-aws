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

package org.springframework.cloud.aws.autoconfigure.secretsmanager;

import java.lang.reflect.Method;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClient;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerProperties;
import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerPropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link AwsSecretsManagerBootstrapConfiguration}.
 *
 * @author Matej Nedic
 * @author Eddú Meléndez
 */
class AwsSecretsManagerBootstrapConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(AwsSecretsManagerBootstrapConfiguration.class))
			.withUserConfiguration(TestConfig.class);

	@Test
	void secretsManagerIsEnabled() {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(AwsSecretsManagerPropertySourceLocator.class));
	}

	@Test
	void secretsManagerIsDisabled() {
		this.contextRunner.withPropertyValues("aws.secretsmanager.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(AwsSecretsManagerPropertySourceLocator.class));
	}

	@Test
	void testWithStaticRegion() {
		AwsSecretsManagerBootstrapConfiguration bootstrapConfig = new AwsSecretsManagerBootstrapConfiguration();

		String region = "us-east-2";
		AwsSecretsManagerProperties awsParamStoreProperties = new AwsSecretsManagerProperties();
		awsParamStoreProperties.setRegion(region);

		Method SMClientMethod = ReflectionUtils.findMethod(
				AwsSecretsManagerBootstrapConfiguration.class, "smClient",
				AwsSecretsManagerProperties.class);
		SMClientMethod.setAccessible(true);
		AWSSecretsManagerClient awsSimpleClient = (AWSSecretsManagerClient) ReflectionUtils
				.invokeMethod(SMClientMethod, bootstrapConfig, awsParamStoreProperties);

		Method signingRegionMethod = ReflectionUtils
				.findMethod(AmazonWebServiceClient.class, "getSigningRegion");
		signingRegionMethod.setAccessible(true);
		String signedRegion = (String) ReflectionUtils.invokeMethod(signingRegionMethod,
				awsSimpleClient);

		assertThat(signedRegion).isEqualTo(region);
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		AWSSecretsManager secretmManagerClient() {
			return mock(AWSSecretsManager.class);
		}

	}

}
