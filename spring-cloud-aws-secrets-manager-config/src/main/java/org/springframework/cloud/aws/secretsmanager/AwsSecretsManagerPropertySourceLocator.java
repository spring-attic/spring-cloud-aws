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

package org.springframework.cloud.aws.secretsmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Builds a {@link CompositePropertySource} with various
 * {@link AwsSecretsManagerPropertySource} instances based on active profiles, application
 * name and default context permutations. Mostly copied from Spring Cloud Consul's config
 * support.
 *
 * @author Fabio Maia
 * @since 2.0.0
 */
public class AwsSecretsManagerPropertySourceLocator implements PropertySourceLocator {

	private AWSSecretsManager smClient;

	private AwsSecretsManagerProperties properties;

	private List<String> contexts = new ArrayList<>();

	private Log logger = LogFactory.getLog(getClass());

	public AwsSecretsManagerPropertySourceLocator(AWSSecretsManager smClient,
			AwsSecretsManagerProperties properties) {
		this.smClient = smClient;
		this.properties = properties;
	}

	public List<String> getContexts() {
		return contexts;
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		if (!(environment instanceof ConfigurableEnvironment)) {
			return null;
		}

		ConfigurableEnvironment env = (ConfigurableEnvironment) environment;

		String appName = properties.getName();

		if (appName == null) {
			appName = env.getProperty("spring.application.name");
		}

		List<String> profiles = Arrays.asList(env.getActiveProfiles());

		String prefix = this.properties.getPrefix();
		String prefixSeparator = this.properties.getPrefixSeparator();

		String defaultContext = StringUtils.isEmpty(prefix) ? this.properties.getDefaultContext() : prefix + prefixSeparator + this.properties.getDefaultContext();
		// Prevent to add an empty context if there is no prefix, and an empty properties.getDefaultContext()
		if (!StringUtils.isEmpty(defaultContext)) {
			this.contexts.add(defaultContext);
			addProfiles(this.contexts, defaultContext, profiles);
		}

		// appName can be null or empty
		if (!StringUtils.isEmpty(appName)) {
			String baseContext = StringUtils.isEmpty(prefix) ? appName : prefix + prefixSeparator + appName;
			// Prevent to add an empty context if there is no prefix, and an empty appName
			if (!StringUtils.isEmpty(baseContext)) {
				this.contexts.add(baseContext);
				addProfiles(this.contexts, baseContext, profiles);
			}
		}

		Collections.reverse(this.contexts);

		CompositePropertySource composite = new CompositePropertySource(
				"aws-secrets-manager");

		for (String propertySourceContext : this.contexts) {
			try {
				composite.addPropertySource(create(propertySourceContext));
			}
			catch (Exception e) {
				if (this.properties.isFailFast()) {
					logger.error(
							"Fail fast is set and there was an error reading configuration from AWS Secrets Manager:\n"
									+ e.getMessage());
					ReflectionUtils.rethrowRuntimeException(e);
				}
				else {
					logger.warn("Unable to load AWS secret from " + propertySourceContext,
							e);
				}
			}
		}

		return composite;
	}

	private AwsSecretsManagerPropertySource create(String context) {
		AwsSecretsManagerPropertySource propertySource = new AwsSecretsManagerPropertySource(
				context, this.smClient);
		propertySource.init();
		return propertySource;
	}

	private void addProfiles(List<String> contexts, String baseContext,
			List<String> profiles) {
		for (String profile : profiles) {
			contexts.add(baseContext + this.properties.getProfileSeparator() + profile);
		}
	}

}
