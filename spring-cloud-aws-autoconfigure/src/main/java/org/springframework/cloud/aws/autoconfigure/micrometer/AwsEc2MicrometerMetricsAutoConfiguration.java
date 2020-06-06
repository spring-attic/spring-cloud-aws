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
import java.util.Optional;
import java.util.stream.Stream;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * Configuration that adds EC2 metadata as tags for the Micrometer metrics.
 *
 * @author Renan Reis Martins de Paula
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsEc2MicrometerMetricsProperties.class)
@AutoConfigureBefore({ CompositeMeterRegistryAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureAfter({ MetricsAutoConfiguration.class,
		ContextInstanceDataAutoConfiguration.class })
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "cloud.aws.ec2.micrometer.metrics", name = "tags")
public class AwsEc2MicrometerMetricsAutoConfiguration {

	private final AwsEc2MicrometerMetricsProperties metricsProperties;

	private final Environment environment;

	public AwsEc2MicrometerMetricsAutoConfiguration(
			AwsEc2MicrometerMetricsProperties metricsProperties,
			Environment environment) {
		this.metricsProperties = metricsProperties;
		this.environment = environment;
	}

	@Bean
	public MeterRegistryCustomizer meterRegistryCustomizer() {
		return registry -> registry.config().commonTags(createTags());
	}

	Collection<Tag> createTags() {
		// Java 9
		// Collection<Tag> tags = metricsProperties.getTags()
		// .stream()
		// .map(this::retrieveMetadataValue)
		// .flatMap(Optional::stream)
		// .collect(toList());

		return metricsProperties.getTags()
								.stream()
								.map(this::retrieveMetadataValue)
								.flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
								.collect(toList());
	}

	private Optional<ImmutableTag> retrieveMetadataValue(String tag) {
		return ofNullable(environment.getProperty(tag))
				.map(v -> new ImmutableTag(tag, v));
	}
}
