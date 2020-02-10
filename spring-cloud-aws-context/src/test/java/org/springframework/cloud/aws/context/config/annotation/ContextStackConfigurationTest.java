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

package org.springframework.cloud.aws.context.config.annotation;

import java.util.Collections;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

import org.springframework.cloud.aws.context.MetaDataServer;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextStackConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}

		MetaDataServer.shutdownHttpServer();
	}

	@Test
	public void stackRegistry_noStackNameConfigured_returnsAutoConfiguredStackRegistry()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithEmptyStackName.class);
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext httpContext = httpServer.createContext(
				"/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("test"));

		// Act
		this.context.refresh();

		// Assert
		StackResourceRegistry stackResourceRegistry = this.context
				.getBean(StackResourceRegistry.class);
		assertThat(stackResourceRegistry).isNotNull();
		assertThat(stackResourceRegistry.getStackName()).isEqualTo("testStack");

		httpServer.removeContext(httpContext);
	}

	@Test
	public void stackRegistry_stackNameConfigured_returnsConfiguredStackRegistryForName()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ManualConfigurationStackRegistryTestConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		StackResourceRegistry stackResourceRegistry = this.context
				.getBean(StackResourceRegistry.class);
		assertThat(stackResourceRegistry).isNotNull();
		assertThat(stackResourceRegistry.getStackName()).isEqualTo("manualStackName");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableStackConfiguration
	static class ApplicationConfigurationWithEmptyStackName {

		@Bean
		public CloudFormationClient amazonCloudFormation() {
			CloudFormationClient amazonCloudFormation = Mockito
					.mock(CloudFormationClient.class);
			Mockito.when(amazonCloudFormation
					.describeStackResources(DescribeStackResourcesRequest.builder()
							.physicalResourceId("test").build()))
					.thenReturn(DescribeStackResourcesResponse.builder().stackResources(
							StackResource.builder().stackName("testStack").build())
							.build());
			Mockito.when(amazonCloudFormation.listStackResources(
					ListStackResourcesRequest.builder().stackName("testStack").build()))
					.thenReturn(ListStackResourcesResponse.builder()
							.stackResourceSummaries(Collections.emptyList()).build());
			return amazonCloudFormation;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableStackConfiguration(stackName = "manualStackName")
	static class ManualConfigurationStackRegistryTestConfiguration {

		@Bean
		public CloudFormationClient amazonCloudFormation() {
			CloudFormationClient amazonCloudFormation = Mockito
					.mock(CloudFormationClient.class);
			Mockito.when(amazonCloudFormation.listStackResources(ListStackResourcesRequest
					.builder().stackName("manualStackName").build()))
					.thenReturn(ListStackResourcesResponse.builder()
							.stackResourceSummaries(Collections.emptyList()).build());
			return amazonCloudFormation;
		}

	}

}
