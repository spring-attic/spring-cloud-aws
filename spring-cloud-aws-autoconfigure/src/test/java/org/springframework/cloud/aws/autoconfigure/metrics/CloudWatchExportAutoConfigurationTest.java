/*
 * Copyright 2013-2018 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.metrics;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.micrometer.cloudwatch.CloudWatchConfig;
import io.micrometer.cloudwatch.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;

/**
 * Test for the {@link CloudWatchExportAutoConfiguration}.
 *
 * @author Dawid Kublik
 */
public class CloudWatchExportAutoConfigurationTest {

    private MockEnvironment env;

    private AnnotationConfigApplicationContext context;

    @Before
    public void before() {
        this.env = new MockEnvironment();
        this.context = new AnnotationConfigApplicationContext();
        this.context.setEnvironment(this.env);
    }

    @Test
    public void testWithoutSettingAnyConfigProperties() {
        this.context.register(CloudWatchExportAutoConfiguration.class);
        this.context.refresh();
        assertTrue(this.context.getBeansOfType(CloudWatchMeterRegistry.class).isEmpty());
    }

    @Test
    public void testConfiguration() throws Exception {
        this.env.setProperty("management.metrics.export.cloudwatch.namespace", "test");

        this.context.register(CloudWatchExportAutoConfiguration.class);
        this.context.refresh();

        CloudWatchMeterRegistry metricsExporter = this.context.getBean(CloudWatchMeterRegistry.class);
        assertNotNull(metricsExporter);

        CloudWatchConfig cloudWatchConfig = this.context.getBean(CloudWatchConfig.class);
        assertNotNull(cloudWatchConfig);

        Clock clock = this.context.getBean(Clock.class);
        assertNotNull(clock);

        CloudWatchProperties cloudWatchProperties = this.context.getBean(CloudWatchProperties.class);
        assertNotNull(cloudWatchProperties);

        assertEquals(cloudWatchConfig.namespace(), cloudWatchProperties.getNamespace());
    }

}