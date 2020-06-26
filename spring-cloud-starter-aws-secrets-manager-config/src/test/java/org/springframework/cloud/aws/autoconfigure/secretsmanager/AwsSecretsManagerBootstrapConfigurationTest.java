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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClient;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerProperties;

public class AwsSecretsManagerBootstrapConfigurationTest {

	AwsSecretsManagerBootstrapConfiguration bootstrapConfig = new AwsSecretsManagerBootstrapConfiguration();

	@Test
	public void setRegion() throws NoSuchMethodException, InvocationTargetException,
			IllegalAccessException {
		String region = "us-east-2";
		AwsSecretsManagerProperties awsParamStoreProperties = new AwsSecretsManagerPropertiesBuilder()
				.withRegion(region).build();

		Method SMClientMethod = AwsSecretsManagerBootstrapConfiguration.class
				.getDeclaredMethod("smClient", AwsSecretsManagerProperties.class);
		SMClientMethod.setAccessible(true);
		AWSSecretsManagerClient awsSimpleClient = (AWSSecretsManagerClient) SMClientMethod
				.invoke(bootstrapConfig, awsParamStoreProperties);

		Method signingRegionMethod = AmazonWebServiceClient.class.getDeclaredMethod("getSigningRegion");
		signingRegionMethod.setAccessible(true);
		String signedRegion = (String) signingRegionMethod.invoke(awsSimpleClient);

		Assert.assertEquals(signedRegion, region);
	}

	private final static class AwsSecretsManagerPropertiesBuilder {

		private final AwsSecretsManagerProperties properties = new AwsSecretsManagerProperties();

		private AwsSecretsManagerPropertiesBuilder() {
		}

		public AwsSecretsManagerPropertiesBuilder withRegion(String region) {
			this.properties.setRegion(region);
			return this;
		}

		public AwsSecretsManagerProperties build() {
			return this.properties;
		}

	}
}
