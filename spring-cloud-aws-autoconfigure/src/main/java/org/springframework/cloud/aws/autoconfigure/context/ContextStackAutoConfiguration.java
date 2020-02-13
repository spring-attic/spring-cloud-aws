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

package org.springframework.cloud.aws.autoconfigure.context;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.cloud.aws.core.env.stack.config.AutoDetectingStackNameProvider;
import org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean;
import org.springframework.cloud.aws.core.env.stack.config.StaticStackNameProvider;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 */
@Configuration(proxyBeanMethods = false)
@Import({ ContextCredentialsAutoConfiguration.class,
		ContextDefaultConfigurationRegistrar.class })
@ConditionalOnClass(name = "com.amazonaws.services.cloudformation.AmazonCloudFormation")
public class ContextStackAutoConfiguration {

	@Autowired
	private Environment environment;

	@Autowired(required = false)
	private Ec2Client amazonEC2;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired(required = false)
	private AwsCredentialsProvider credentialsProvider;

	@Bean
	@ConditionalOnMissingBean(StackResourceRegistry.class)
	public StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean(
			CloudFormationClient amazonCloudFormation) {

		if (StringUtils.hasText(environment.getProperty("cloud.aws.stack.name"))) {
			return new StackResourceRegistryFactoryBean(amazonCloudFormation,
					new StaticStackNameProvider(
							this.environment.getProperty("cloud.aws.stack.name")));
		}

		if (environment.getProperty("cloud.aws.stack.auto") == null || "true"
				.equalsIgnoreCase(environment.getProperty("cloud.aws.stack.auto"))) {
			return new StackResourceRegistryFactoryBean(amazonCloudFormation,
					new AutoDetectingStackNameProvider(amazonCloudFormation,
							this.amazonEC2));
		}

		return null;
	}

	@Bean
	@ConditionalOnMissingAmazonClient(CloudFormationClient.class)
	public AmazonWebserviceClientFactoryBean<CloudFormationClient> amazonCloudFormation() {
		return new AmazonWebserviceClientFactoryBean<>(CloudFormationClient.class,
				this.credentialsProvider, this.regionProvider);
	}

}
