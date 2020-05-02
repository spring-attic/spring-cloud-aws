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

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.model.GetInstanceRequest;
import com.amazonaws.services.servicediscovery.model.Instance;
import com.amazonaws.services.servicediscovery.model.ListInstancesRequest;
import com.amazonaws.services.servicediscovery.model.ListServicesRequest;
import com.amazonaws.services.servicediscovery.model.ServiceSummary;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

public class AwsCloudMapDiscoveryClient implements DiscoveryClient {

	private final AWSServiceDiscovery aws;

	public AwsCloudMapDiscoveryClient(AWSServiceDiscovery aws) {
		this.aws = aws;
	}

	@Override
	public String description() {
		return "AWS Cloud Map Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		ListInstancesRequest listInstancesRequest = new ListInstancesRequest()
				.withServiceId(serviceId);
		// TODO pagination
		// TODO parallel requests?
		// TODO filter on health?
		return aws.listInstances(listInstancesRequest).getInstances().stream()
				.map(summary -> getInstance(serviceId, summary.getId()))
				.collect(Collectors.toList());

	}

	private AwsCloudMapServiceInstance getInstance(String serviceId, String instanceId) {
		Instance instance = aws.getInstance(new GetInstanceRequest().withServiceId(serviceId).withInstanceId(instanceId))
				.getInstance();
		return new AwsCloudMapServiceInstance(serviceId, instance);
	}

	@Override
	public List<String> getServices() {
		// TODO pagination
		return aws.listServices(new ListServicesRequest()).getServices().stream()
				.map(ServiceSummary::getId).collect(Collectors.toList());
	}

}
