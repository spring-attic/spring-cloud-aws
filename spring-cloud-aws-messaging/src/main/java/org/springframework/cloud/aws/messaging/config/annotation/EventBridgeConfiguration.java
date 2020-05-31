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

package org.springframework.cloud.aws.messaging.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jakub Narloch
 * @since 2.3.0
 */
@Configuration(proxyBeanMethods = false)
public class EventBridgeConfiguration {

	@Autowired(required = false)
	private AWSCredentialsProvider awsCredentialsProvider;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@ConditionalOnMissingAmazonClient(AmazonCloudWatchEvents.class)
	@Bean
	public AmazonWebserviceClientFactoryBean<AmazonCloudWatchEventsClient> amazonEvents() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonCloudWatchEventsClient.class,
				this.awsCredentialsProvider, this.regionProvider);
	}

}
