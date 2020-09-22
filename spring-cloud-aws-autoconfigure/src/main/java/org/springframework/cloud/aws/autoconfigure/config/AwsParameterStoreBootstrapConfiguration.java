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

package org.springframework.cloud.aws.autoconfigure.config;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.cloud.aws.parameterstore.AwsParamStorePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud Bootstrap Configuration for setting up an
 * {@link AwsParamStorePropertySourceLocator} and its dependencies.
 *
 * @author Joris Kuipers
 * @author Matej Nedic
 * @author Eddú Meléndez
 * @since 2.3
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ParameterStoreProperties.class)
@ConditionalOnMissingClass("org.springframework.cloud.aws.autoconfigure.paramstore.AwsParamStoreBootstrapConfiguration")
@ConditionalOnClass({ AWSSimpleSystemsManagement.class,
		AwsParamStorePropertySourceLocator.class })
@ConditionalOnProperty(prefix = "spring.cloud.aws.parameterstore", name = "enabled",
		matchIfMissing = true)
public class AwsParameterStoreBootstrapConfiguration {

	private final RegionProvider regionProvider;

	private final ParameterStoreProperties properties;

	public AwsParameterStoreBootstrapConfiguration(ParameterStoreProperties properties,
			ObjectProvider<RegionProvider> regionProvider) {
		this.regionProvider = properties.getRegion() == null
				? regionProvider.getIfAvailable()
				: new StaticRegionProvider(properties.getRegion());
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AmazonWebserviceClientFactoryBean<AWSSimpleSystemsManagementClient> ssmClient() {
		return new AmazonWebserviceClientFactoryBean<>(
				AWSSimpleSystemsManagementClient.class, null, this.regionProvider);
	}

	@Bean
	public AwsParamStorePropertySourceLocator awsParamStorePropertySourceLocator(
			AWSSimpleSystemsManagement ssmClient) {
		AwsParamStorePropertySourceLocator propertySourceLocator = new AwsParamStorePropertySourceLocator(
				ssmClient);
		propertySourceLocator.setName(this.properties.getName());
		propertySourceLocator.setPrefix(this.properties.getPrefix());
		propertySourceLocator.setDefaultContext(this.properties.getDefaultContext());
		propertySourceLocator.setFailFast(this.properties.isFailFast());
		propertySourceLocator.setProfileSeparator(this.properties.getProfileSeparator());

		return propertySourceLocator;
	}

}
