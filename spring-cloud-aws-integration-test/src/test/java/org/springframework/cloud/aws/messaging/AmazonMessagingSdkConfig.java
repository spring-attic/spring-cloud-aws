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

package org.springframework.cloud.aws.messaging;

import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.springframework.cloud.aws.messaging.AbstractContainerTest.localStack;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * @author Philip Riecks
 */
@TestConfiguration
public class AmazonMessagingSdkConfig {

	@Bean
	@Primary
	public AmazonSQSAsync amazonSQSAsync() {
		return AmazonSQSAsyncClientBuilder.standard()
				.withCredentials(localStack.getDefaultCredentialsProvider())
				.withEndpointConfiguration(localStack.getEndpointConfiguration(SQS))
				.build();
	}

	@Bean
	@Primary
	public AmazonSNSAsync amazonSNSAsync() {
		return AmazonSNSAsyncClientBuilder.standard()
				.withCredentials(localStack.getDefaultCredentialsProvider())
				.withEndpointConfiguration(localStack.getEndpointConfiguration(SNS))
				.build();
	}

}
