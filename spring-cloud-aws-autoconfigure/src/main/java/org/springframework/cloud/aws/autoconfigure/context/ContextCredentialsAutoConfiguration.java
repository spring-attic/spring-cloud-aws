/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.cloud.aws.context.annotation.ConditionalOnAwsEnabled;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import static com.amazonaws.auth.profile.internal.AwsProfileNameLoader.DEFAULT_PROFILE_NAME;
import static org.springframework.cloud.aws.context.config.support.ContextConfigurationUtils.registerCredentialsProvider;

/**
 * @author Agim Emruli
 * @author Anwar Chirakkattil
 */
@Configuration
@Import({ContextDefaultConfigurationRegistrar.class, ContextCredentialsAutoConfiguration.Registrar.class})
@ConditionalOnClass(name = "com.amazonaws.auth.AWSCredentialsProvider")
@ConditionalOnAwsEnabled
public class ContextCredentialsAutoConfiguration {

    public static class Registrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

        private Environment environment;

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            registerCredentialsProvider(registry, this.environment.getProperty("cloud.aws.credentials.accessKey"),
                    this.environment.getProperty("cloud.aws.credentials.secretKey"),
                    this.environment.getProperty("cloud.aws.credentials.instanceProfile", Boolean.class, true) &&
                            !this.environment.containsProperty("cloud.aws.credentials.accessKey"),
                    this.environment.getProperty("cloud.aws.credentials.profileName", DEFAULT_PROFILE_NAME),
                    this.environment.getProperty("cloud.aws.credentials.profilePath"));
        }
    }
}
