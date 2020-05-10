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

package org.springframework.cloud.aws.cloudmap.reactive;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.model.GetInstanceRequest;
import com.amazonaws.services.servicediscovery.model.GetInstanceResult;
import com.amazonaws.services.servicediscovery.model.ListInstancesRequest;
import com.amazonaws.services.servicediscovery.model.ListServicesRequest;
import com.amazonaws.services.servicediscovery.model.ListServicesResult;
import com.amazonaws.services.servicediscovery.model.ServiceSummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.aws.cloudmap.AwsCloudMapServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

public class AwsCloudMapReactiveDiscoveryClient implements ReactiveDiscoveryClient {

	private final AWSServiceDiscovery aws;

	public AwsCloudMapReactiveDiscoveryClient(AWSServiceDiscovery aws) {
		this.aws = aws;
	}

	@Override
	public String description() {
		return "AWS Cloud Map Reactive Discovery Client";
	}

	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {
		ListInstancesRequest request = new ListInstancesRequest().withServiceId(serviceId);

		return Mono.fromSupplier(() -> aws.listInstances(request)).flatMapMany(resp -> Flux
				.fromIterable(resp.getInstances()).flatMap(summary -> getInstance(serviceId, summary.getId())));
	}

	private Mono<AwsCloudMapServiceInstance> getInstance(String serviceId, String instanceId) {
		GetInstanceRequest request = new GetInstanceRequest().withServiceId(serviceId).withInstanceId(instanceId);

		return Mono.fromSupplier(() -> aws.getInstance(request)).map(GetInstanceResult::getInstance)
				.map(instance -> new AwsCloudMapServiceInstance(serviceId, instance));
	}

	@Override
	public Flux<String> getServices() {
		ListServicesRequest request = new ListServicesRequest();

		return Mono.fromSupplier(() -> aws.listServices(request)).flatMapIterable(ListServicesResult::getServices)
				.map(ServiceSummary::getId);
	}

}
