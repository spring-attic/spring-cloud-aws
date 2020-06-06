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

import java.util.Collection;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Renan Reis Martins de Paula
 */
public class AwsEc2MicrometerMetricsAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setUp() {
		context = new AnnotationConfigApplicationContext();
	}

	@After
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void should_not_register_meterRegistryCustomizer_if_property_is_not_set() {
		// Arrange
		this.context.register(AwsEc2MicrometerMetricsAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		this.context.getBean(MeterRegistryCustomizer.class);
	}

	@Test
	public void defaultServiceBacksOff() {
		// Arrange
		TestPropertyValues.of("cloud.aws.ec2.micrometer.metrics.tags=aim-id")
				.applyTo(this.context);
		this.context.register(AwsEc2MicrometerMetricsAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(MeterRegistryCustomizer.class)).isNotNull();
	}

	@Test
	public void defaultServiceBacksOff2() {
		// Arrange
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withPropertyValues("cloud.aws.ec2.micrometer.metrics.tags=aim-id")
				.withConfiguration(AutoConfigurations
						.of(AwsEc2MicrometerMetricsAutoConfiguration.class));

		// Assert
		contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(MeterRegistryCustomizer.class);
		});
	}

	@Test
	public void should_create_tags_for_existing_ec2_metadata() {
		// Arrange
		AwsEc2MicrometerMetricsProperties properties = new AwsEc2MicrometerMetricsProperties();
		properties.setTags(asList("existing_metadata", "non_existing_metadata"));

		Environment environment = mock(Environment.class);
		when(environment.getProperty("existing_metadata")).thenReturn("value");

		// Act
		Collection<Tag> tags = new AwsEc2MicrometerMetricsAutoConfiguration(properties,
				environment).createTags();

		// Assert
		assertThat(tags)
				.isEqualTo(asList(new ImmutableTag("existing_metadata", "value")));
	}

}
