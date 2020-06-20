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

import com.amazonaws.services.sns.AmazonSNS;

import org.springframework.cloud.aws.context.annotation.ConditionalOnClass;
import org.springframework.cloud.aws.messaging.endpoint.NotificationMessageReactiveHandlerMethodArgumentResolver;
import org.springframework.cloud.aws.messaging.endpoint.NotificationStatusReactiveHandlerMethodArgumentResolver;
import org.springframework.cloud.aws.messaging.endpoint.NotificationSubjectReactiveHandlerMethodArgumentResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

/**
 * @author Isabek Tashiev
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass("org.springframework.web.reactive.config.WebFluxConfigurer")
public class SnsWebFluxConfiguration {

	@Bean
	public WebFluxConfigurer webFluxConfigurer(AmazonSNS amazonSns) {
		return new WebFluxConfigurer() {
			@Override
			public void configureArgumentResolvers(
					ArgumentResolverConfigurer configurer) {
				configurer.addCustomResolver(
						new NotificationStatusReactiveHandlerMethodArgumentResolver(
								amazonSns),
						new NotificationSubjectReactiveHandlerMethodArgumentResolver(),
						new NotificationMessageReactiveHandlerMethodArgumentResolver());
			}
		};
	}

}
