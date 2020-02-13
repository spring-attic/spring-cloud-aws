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

package org.springframework.cloud.aws.core.config;

import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.regions.Region;

public class AmazonTestWebserviceClientBuilder extends
		AwsDefaultClientBuilder<AmazonTestWebserviceClientBuilder, TestWebserviceClient> {

	@Override
	protected String serviceEndpointPrefix() {
		return null;
	}

	@Override
	protected String signingName() {
		return null;
	}

	@Override
	protected String serviceName() {
		return null;
	}

	@Override
	protected TestWebserviceClient buildClient() {
		final Region region = super.clientConfiguration.build()
				.option(AwsClientOption.AWS_REGION);
		return new DefaultTestWebserviceClient(region);
	}

}
