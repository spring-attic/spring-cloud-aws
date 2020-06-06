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

package org.springframework.cloud.aws.autoconfigure.micrometer;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Renan Reis Martins de Paula
 */
public class AwsEc2MicrometerMetricsPropertiesTest {

	private AwsEc2MicrometerMetricsProperties properties;

	@Before
	public void setup() {
		this.properties = new AwsEc2MicrometerMetricsProperties();
	}

	@Test
	public void tags_property_should_return_emptyList_as_default() {
		assertThat(this.properties.getTags())
				.as("Tags default value expected to be an empty list")
				.isEqualTo(EMPTY_LIST);
	}

	@Test
	public void properties_set_should_override_default_values() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withPropertyValues("cloud.aws.ec2.micrometer.metrics.tags=ami-id")
				.withConfiguration(AutoConfigurations
						.of(AwsEc2MicrometerMetricsPropertiesConfiguration.class));

		contextRunner.run((context) -> {
			assertThat(context.getBean(AwsEc2MicrometerMetricsProperties.class).getTags())
					.isEqualTo(singletonList("ami-id"));
		});
	}

	@Configuration
	@EnableConfigurationProperties(AwsEc2MicrometerMetricsProperties.class)
	protected static class AwsEc2MicrometerMetricsPropertiesConfiguration {

	}

}
