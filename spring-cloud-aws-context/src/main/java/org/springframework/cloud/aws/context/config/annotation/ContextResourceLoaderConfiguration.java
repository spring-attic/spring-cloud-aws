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

package org.springframework.cloud.aws.context.config.annotation;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.context.support.io.SimpleStorageProtocolResolverConfigurer;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Agim Emruli
 */
@Configuration(proxyBeanMethods = false)
public class ContextResourceLoaderConfiguration {

	@ConditionalOnMissingAmazonClient(AmazonS3.class)
	@Bean
	public AmazonWebserviceClientFactoryBean<AmazonS3Client> amazonS3() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonS3Client.class, null, null);
	}

	@Bean
	public SimpleStorageProtocolResolver simpleStorageProtocolResolver(
			AmazonS3Client amazonS3Client) {
		SimpleStorageProtocolResolver protocolResolver = new SimpleStorageProtocolResolver(
				amazonS3Client);
		applyProperties(protocolResolver);
		return protocolResolver;
	}

	protected void applyProperties(SimpleStorageProtocolResolver protocolResolver) {

	}

	@Bean
	public SimpleStorageProtocolResolverConfigurer simpleStorageProtocolResolverConfigurer(
			SimpleStorageProtocolResolver protocolResolver) {
		return new SimpleStorageProtocolResolverConfigurer(protocolResolver);
	}

}
