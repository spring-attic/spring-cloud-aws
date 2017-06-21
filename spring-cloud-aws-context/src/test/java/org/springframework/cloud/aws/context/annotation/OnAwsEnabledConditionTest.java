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

package org.springframework.cloud.aws.context.annotation;

import org.junit.Test;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Anwar Chirakkattil
 */
public class OnAwsEnabledConditionTest {

    @Test
    public void condition_default_includes_shouldCreateBeanFoo() throws Exception {
        // Arrange & Act
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(OnAwsEnabledConditionTest.ConfigWithAwsEnabledCondition.class);

        // Assert
        assertTrue(applicationContext.containsBean("foo"));
    }

    @Test
    public void condition_withAwsDisabled_shouldNotCreateBeanFoo() throws Exception {
        // Arrange & Act
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(ConfigWithAwsEnabledCondition.class);
        EnvironmentTestUtils.addEnvironment(applicationContext, "spring.cloud.aws.enabled:false");
        applicationContext.refresh();

        // Assert
        assertFalse(applicationContext.containsBean("foo"));
    }

    @Configuration
    @ConditionalOnAwsEnabled
    protected static class ConfigWithAwsEnabledCondition {

        @Bean
        public String foo() {
            return "foo";
        }
    }
}
