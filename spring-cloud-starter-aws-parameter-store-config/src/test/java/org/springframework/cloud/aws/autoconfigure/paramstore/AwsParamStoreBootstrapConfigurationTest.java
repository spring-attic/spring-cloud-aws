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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.cloud.aws.paramstore.AwsParamStoreProperties;

/**
 * Unit test for {@link AwsParamStoreBootstrapConfiguration}.
 * @author Matej Nedic
 */
public class AwsParamStoreBootstrapConfigurationTest {

	AwsParamStoreBootstrapConfiguration bootstrapConfig = new AwsParamStoreBootstrapConfiguration();

	@Test
	public void setRegion() throws NoSuchMethodException, InvocationTargetException,
			IllegalAccessException {
		String region = "us-east-2";
		AwsParamStoreProperties awsParamStoreProperties = new AwsParamStorePropertiesBuilder()
				.withRegion(region).build();

		Method SSMClientMethod = AwsParamStoreBootstrapConfiguration.class
				.getDeclaredMethod("ssmClient", AwsParamStoreProperties.class);
		SSMClientMethod.setAccessible(true);
		AWSSimpleSystemsManagementClient awsSimpleClient = (AWSSimpleSystemsManagementClient) SSMClientMethod
				.invoke(bootstrapConfig, awsParamStoreProperties);

		Method signingRegionMethod = AmazonWebServiceClient.class.getDeclaredMethod("getSigningRegion");
		signingRegionMethod.setAccessible(true);
		String signedRegion = (String) signingRegionMethod.invoke(awsSimpleClient);

		Assert.assertEquals(signedRegion, region);
	}

	private static final class AwsParamStorePropertiesBuilder {

		private final AwsParamStoreProperties properties = new AwsParamStoreProperties();

		private AwsParamStorePropertiesBuilder() {
		}

		public AwsParamStorePropertiesBuilder withRegion(String region) {
			this.properties.setRegion(region);
			return this;
		}

		public AwsParamStoreProperties build() {
			return this.properties;
		}

	}
}
