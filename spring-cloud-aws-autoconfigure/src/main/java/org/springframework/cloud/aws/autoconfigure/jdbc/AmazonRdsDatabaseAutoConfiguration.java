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

package org.springframework.cloud.aws.autoconfigure.jdbc;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.jdbc.config.annotation.AmazonRdsInstanceConfiguration;
import org.springframework.cloud.aws.jdbc.config.annotation.AmazonRdsInstanceConfiguration.RdsInstanceConfigurerBeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @author Eddú Meléndez
 * @author Mete Alpaslan Katırcıoğlu
 */
// @checkstyle:off
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@Import(AmazonRdsDatabaseAutoConfiguration.Registrar.class)
@ConditionalOnClass(name = { "com.amazonaws.services.rds.AmazonRDSClient",
		"org.springframework.cloud.aws.jdbc.config.annotation.AmazonRdsInstanceConfiguration" })
@ConditionalOnMissingBean(AmazonRdsInstanceConfiguration.class)
@ConditionalOnProperty(name = "cloud.aws.rds.enabled", havingValue = "true",
		matchIfMissing = true)
public class AmazonRdsDatabaseAutoConfiguration {

	// @checkstyle:on

	@Bean
	public static RdsInstanceConfigurerBeanPostProcessor rdsInstanceConfigurerBeanPostProcessor() {
		return new RdsInstanceConfigurerBeanPostProcessor();
	}

	/**
	 * Registrar for Amazon RDS.
	 */
	public static class Registrar extends AmazonRdsInstanceConfiguration.AbstractRegistrar
			implements EnvironmentAware {

		private ConfigurableEnvironment environment;

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			String amazonRdsClientBeanName = AmazonWebserviceClientConfigurationUtils
					.registerAmazonWebserviceClient(this, registry,
							"com.amazonaws.services.rds.AmazonRDSClient", null, null)
					.getBeanName();

			AmazonRdsDatabaseProperties properties = getDbInstanceConfigurations();
			properties.getInstances().stream()
					.filter(rdsInstance -> !StringUtils
							.isEmpty(rdsInstance.getDbInstanceIdentifier())
							&& !StringUtils.isEmpty(rdsInstance.getPassword()))
					.forEach(rdsInstance -> registerDataSource(registry,
							amazonRdsClientBeanName,
							rdsInstance.getDbInstanceIdentifier(),
							rdsInstance.getPassword(), rdsInstance.isReadReplicaSupport(),
							rdsInstance.getUsername(), rdsInstance.getDatabaseName()));
		}

		@Override
		public void setEnvironment(Environment environment) {
			Assert.isInstanceOf(ConfigurableEnvironment.class, environment,
					"Amazon RDS auto configuration requires a configurable environment");
			this.environment = (ConfigurableEnvironment) environment;
		}

		private AmazonRdsDatabaseProperties getDbInstanceConfigurations() {
			ConfigurationProperties annotation = AnnotationUtils.findAnnotation(
					AmazonRdsDatabaseProperties.class, ConfigurationProperties.class);
			String prefix = StringUtils.hasText(annotation.value()) ? annotation.value()
					: annotation.prefix();
			Assert.hasText(prefix,
					"The AmazonRdsDatabaseProperties prefix can't be empty.");
			return Binder.get(this.environment)
					.bind(prefix, AmazonRdsDatabaseProperties.class)
					.orElseGet(AmazonRdsDatabaseProperties::new);
		}

	}

}
