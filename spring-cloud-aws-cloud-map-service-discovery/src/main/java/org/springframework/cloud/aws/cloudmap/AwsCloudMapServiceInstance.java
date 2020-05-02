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

package org.springframework.cloud.aws.cloudmap;

import java.net.URI;
import java.util.Map;

import com.amazonaws.services.servicediscovery.model.Instance;

import org.springframework.cloud.client.ServiceInstance;

public class AwsCloudMapServiceInstance implements ServiceInstance {

	private static final String AWS_INSTANCE_IPV_4 = "AWS_INSTANCE_IPV4";

	private static final String AWS_INSTANCE_PORT = "AWS_INSTANCE_PORT";

	private final String serviceId;

	private final Instance instance;

	public AwsCloudMapServiceInstance(String serviceId, Instance instance) {
		this.serviceId = serviceId;
		this.instance = instance;
	}

	@Override
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String getHost() {
		// TODO alternate host attributes
		return instance.getAttributes().get(AWS_INSTANCE_IPV_4);
	}

	@Override
	public int getPort() {
		// TODO are there other possible values?
		String port = instance.getAttributes().get(AWS_INSTANCE_PORT);
		// TODO error handling?
		return Integer.parseInt(port);
	}

	@Override
	public boolean isSecure() {
		return getPort() == 443;
	}

	@Override
	public URI getUri() {
		String scheme = isSecure() ? "https" : "http";
		return URI.create(String.format("%s:%s/%s", scheme, getHost(), getPort()));
	}

	@Override
	public Map<String, String> getMetadata() {
		return instance.getAttributes();
	}

}
