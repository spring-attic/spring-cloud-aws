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

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Collections.EMPTY_LIST;

/**
 * @author Renan Reis Martins de Paula
 */
@ConfigurationProperties(prefix = "cloud.aws.ec2.micrometer.metrics")
public class AwsEc2MicrometerMetricsProperties {

	private Collection<String> tags = EMPTY_LIST;

	public Collection<String> getTags() {
		return tags;
	}

	public void setTags(Collection<String> tags) {
		this.tags = tags;
	}

}
