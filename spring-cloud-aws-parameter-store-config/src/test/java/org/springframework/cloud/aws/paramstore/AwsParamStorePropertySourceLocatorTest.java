/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.paramstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsParamStorePropertySourceLocatorTest {

    AwsParamStorePropertySourceLocator locator;

    AWSSimpleSystemsManagement ssmClient = mock(AWSSimpleSystemsManagement.class);

    MockEnvironment env;

    @Before
    public void setup() {
        AwsParamStoreProperties properties = new AwsParamStoreProperties();
        locator = new AwsParamStorePropertySourceLocator(ssmClient, properties);

        env = new MockEnvironment()
                .withProperty("aws.paramstore.prefix", "/customPrefix")
                .withProperty("aws.paramstore.name", "customName")
                .withProperty("aws.paramstore.defaultContext", "customContext")
                .withProperty("aws.paramstore.profileSeparator", "-");
        env.setActiveProfiles("customProfile");

        GetParametersByPathResult firstResult = new GetParametersByPathResult();

        when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class)))
                .thenReturn(firstResult);
    }


    @Test
    public void overridesDefaultPropertyWithEnvironment() {
        locator.locate(env);
        assertThat(locator.getContexts()).contains("/customPrefix/customName/");
        assertThat(locator.getContexts()).contains("/customPrefix/customContext/");
        assertThat(locator.getContexts()).contains("/customPrefix/customName-customProfile/");
        assertThat(locator.getContexts()).contains("/customPrefix/customContext-customProfile/");
    }
}
