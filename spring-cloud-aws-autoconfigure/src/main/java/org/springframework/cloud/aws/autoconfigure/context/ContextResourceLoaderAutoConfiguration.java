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

package org.springframework.cloud.aws.autoconfigure.context;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.aws.autoconfigure.context.properties.AwsS3ResourceLoaderProperties;
import org.springframework.cloud.aws.context.config.annotation.ContextResourceLoaderConfiguration;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Agim Emruli
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsS3ResourceLoaderProperties.class)
@ConditionalOnClass(AmazonS3Client.class)
public class ContextResourceLoaderAutoConfiguration
		extends ContextResourceLoaderConfiguration {

	private final Environment environment;

	public ContextResourceLoaderAutoConfiguration(Environment environment) {
		this.environment = environment;
	}

	@ConditionalOnMissingBean(AmazonS3.class)
	@Bean
	public AmazonWebserviceClientFactoryBean<AmazonS3Client> amazonS3() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonS3Client.class, null, null);
	}

	protected void applyProperties(SimpleStorageProtocolResolver protocolResolver) {
		if (this.environment
				.containsProperty(AwsS3ResourceLoaderProperties.PREFIX + ".corePoolSize")
				|| this.environment.containsProperty(
						AwsS3ResourceLoaderProperties.PREFIX + ".maxPoolSize")
				|| this.environment.containsProperty(
						AwsS3ResourceLoaderProperties.PREFIX + ".queueCapacity")) {
			AwsS3ResourceLoaderProperties properties = awsS3ResourceLoaderProperties();
			ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
			if (this.environment.containsProperty(
					AwsS3ResourceLoaderProperties.PREFIX + ".corePoolSize")) {
				taskExecutor.setCorePoolSize(properties.getCorePoolSize());
			}
			if (this.environment.containsProperty(
					AwsS3ResourceLoaderProperties.PREFIX + ".maxPoolSize")) {
				taskExecutor.setMaxPoolSize(properties.getMaxPoolSize());
			}
			if (this.environment.containsProperty(
					AwsS3ResourceLoaderProperties.PREFIX + ".queueCapacity")) {
				taskExecutor.setQueueCapacity(properties.getQueueCapacity());
			}
			protocolResolver.setTaskExecutor(taskExecutor);
		}
	}

	private AwsS3ResourceLoaderProperties awsS3ResourceLoaderProperties() {
		return Binder.get(this.environment).bindOrCreate(
				AwsS3ResourceLoaderProperties.PREFIX,
				AwsS3ResourceLoaderProperties.class);
	}

}
