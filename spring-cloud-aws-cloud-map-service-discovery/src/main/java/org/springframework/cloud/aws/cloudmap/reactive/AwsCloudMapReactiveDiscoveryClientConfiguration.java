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

package org.springframework.cloud.aws.cloudmap.reactive;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.AWSServiceDiscoveryClientBuilder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.cloudmap.AwsCloudMapDiscoveryProperties;
import org.springframework.cloud.aws.cloudmap.ConditionalOnAwsCloudMapDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnDiscoveryEnabled
@ConditionalOnReactiveDiscoveryEnabled
@ConditionalOnAwsCloudMapDiscoveryEnabled
@EnableConfigurationProperties
public class AwsCloudMapReactiveDiscoveryClientConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AWSServiceDiscovery awsServiceDiscovery() {
		return AWSServiceDiscoveryClientBuilder.defaultClient();
	}

	@Bean
	@ConditionalOnMissingBean
	public AwsCloudMapDiscoveryProperties awsCloudMapDiscoveryProperties() {
		return new AwsCloudMapDiscoveryProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public AwsCloudMapReactiveDiscoveryClient awsCloudMapReactiveDiscoveryClient(
			AWSServiceDiscovery awsServiceDiscovery) {
		return new AwsCloudMapReactiveDiscoveryClient(awsServiceDiscovery);
	}

}
