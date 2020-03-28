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

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.aws.autoconfigure.context.properties.AwsCredentialsProperties;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import static com.amazonaws.auth.profile.internal.AwsProfileNameLoader.DEFAULT_PROFILE_NAME;
import static org.springframework.cloud.aws.context.config.support.ContextConfigurationUtils.registerCredentialsProvider;
import static org.springframework.cloud.aws.context.config.support.ContextConfigurationUtils.registerDefaultAWSCredentialsProvider;

/**
 * @author Agim Emruli
 */
@Configuration(proxyBeanMethods = false)
@Import({ ContextDefaultConfigurationRegistrar.class,
		ContextCredentialsAutoConfiguration.Registrar.class })
@ConditionalOnClass(name = "com.amazonaws.auth.AWSCredentialsProvider")
public class ContextCredentialsAutoConfiguration {

	/**
	 * The prefix used for AWS credentials related properties.
	 */
	public static final String AWS_CREDENTIALS_PROPERTY_PREFIX = "cloud.aws.credentials";

	/**
	 * Bind AWS credentials related properties to a property instance.
	 * @return An {@link AwsCredentialsProperties} instance
	 */
	@Bean
	@ConfigurationProperties(prefix = AWS_CREDENTIALS_PROPERTY_PREFIX)
	public AwsCredentialsProperties awsCredentialsProperties() {
		return new AwsCredentialsProperties();
	}

	/**
	 * Registrar for the credentials provider.
	 */
	public static class Registrar
			implements ImportBeanDefinitionRegistrar, EnvironmentAware {

		private Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			// Do not register a credentials provider if a bean with the same name is
			// already registered.
			if (registry.containsBeanDefinition(
					CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME)) {
				return;
			}

			Binder binder = Binder.get(environment);

			Boolean useDefaultCredentialsChain = binder.bind(
					AWS_CREDENTIALS_PROPERTY_PREFIX + ".use-default-aws-credentials-chain",
					Boolean.class).orElse(false);
			String accessKey = binder.bind(AWS_CREDENTIALS_PROPERTY_PREFIX + ".access-key", String.class).orElse(null);
			String secretKey = binder.bind(AWS_CREDENTIALS_PROPERTY_PREFIX + ".secret-key", String.class).orElse(null);
			if (useDefaultCredentialsChain && (StringUtils.isEmpty(accessKey)
					|| StringUtils.isEmpty(secretKey))) {
				registerDefaultAWSCredentialsProvider(registry);
			}
			else {
				registerCredentialsProvider(registry, accessKey, secretKey,
						binder.bind(AWS_CREDENTIALS_PROPERTY_PREFIX + ".instance-profile",
								Boolean.class).orElse(true)
								&& !binder.bind(AWS_CREDENTIALS_PROPERTY_PREFIX + ".access-key", String.class).isBound(),
						binder.bind(
								AWS_CREDENTIALS_PROPERTY_PREFIX + ".profile-name", String.class).orElse(
								DEFAULT_PROFILE_NAME),
						binder.bind(
								AWS_CREDENTIALS_PROPERTY_PREFIX + ".profile-path", String.class).orElse(
								null));
			}
		}

	}

}
