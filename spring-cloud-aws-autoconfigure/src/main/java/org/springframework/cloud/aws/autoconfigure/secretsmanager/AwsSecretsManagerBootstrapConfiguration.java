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

package org.springframework.cloud.aws.autoconfigure.secretsmanager;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerPropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud Bootstrap Configuration for setting up an
 * {@link AwsSecretsManagerPropertySourceLocator} and its dependencies.
 *
 * @author Fabio Maia
 * @author Matej Nedic
 * @author Eddú Meléndez
 * @since 2.3
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecretsManagerProperties.class)
@ConditionalOnMissingClass("org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerProperties")
@ConditionalOnClass({ AWSSecretsManager.class,
		AwsSecretsManagerPropertySourceLocator.class })
@ConditionalOnProperty(prefix = "spring.cloud.aws.secretsmanager", name = "enabled",
		matchIfMissing = true)
public class AwsSecretsManagerBootstrapConfiguration {

	private final RegionProvider regionProvider;

	private final SecretsManagerProperties properties;

	public AwsSecretsManagerBootstrapConfiguration(SecretsManagerProperties properties,
			ObjectProvider<RegionProvider> regionProvider) {
		this.regionProvider = properties.getRegion() == null
				? regionProvider.getIfAvailable()
				: new StaticRegionProvider(properties.getRegion());
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AmazonWebserviceClientFactoryBean<AWSSecretsManagerClient> ssmClient() {
		return new AmazonWebserviceClientFactoryBean<>(AWSSecretsManagerClient.class,
				null, this.regionProvider);
	}

	@Bean
	public AwsSecretsManagerPropertySourceLocator awsSecretsManagerPropertySourceLocator(
			AWSSecretsManager smClient) {
		AwsSecretsManagerPropertySourceLocator propertySourceLocator = new AwsSecretsManagerPropertySourceLocator(
				smClient);
		propertySourceLocator.setName(this.properties.getName());
		propertySourceLocator.setPrefix(this.properties.getPrefix());
		propertySourceLocator.setDefaultContext(this.properties.getDefaultContext());
		propertySourceLocator.setFailFast(this.properties.isFailFast());
		propertySourceLocator.setProfileSeparator(this.properties.getProfileSeparator());

		return propertySourceLocator;
	}

}
