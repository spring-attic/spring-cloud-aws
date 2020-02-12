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

package org.springframework.cloud.aws.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.Endpoint;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AmazonRdsDatabaseAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void configureBean_withDefaultClientSpecifiedAndNoReadReplica_configuresFactoryBeanWithoutReadReplica()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithoutReadReplica.class);
		this.context.register(AmazonRdsDatabaseAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.rds.test.password:secret").applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();
	}

	@Test
	public void configureBean_withCustomDataBaseName_configuresFactoryBeanWithCustomDatabaseName()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithoutReadReplica.class);
		this.context.register(AmazonRdsDatabaseAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.rds.test.password:secret",
				"cloud.aws.rds.test.databaseName:fooDb").applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isNotNull();
		assertThat(this.context.getBean(AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();

		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
		assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource).getUrl()
				.endsWith("fooDb")).isTrue();
	}

	@Test
	public void configureBean_withDefaultClientSpecifiedAndNoReadReplicaAndMultipleDatabases_configuresBothDatabases()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithMultipleDatabases.class);
		this.context.register(AmazonRdsDatabaseAutoConfiguration.class);
		TestPropertyValues
				.of("cloud.aws.rds.test.password:secret",
						"cloud.aws.rds.anotherOne.password:verySecret")
				.applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean("test", DataSource.class)).isNotNull();
		assertThat(this.context.getBean("&test", AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();

		assertThat(this.context.getBean("anotherOne", DataSource.class)).isNotNull();
		assertThat(
				this.context.getBean("&anotherOne", AmazonRdsDataSourceFactoryBean.class))
						.isNotNull();
	}

	@Test
	public void configureBean_withDefaultClientSpecifiedAndReadReplica_configuresFactoryBeanWithReadReplicaEnabled()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithReadReplica.class);
		this.context.register(AmazonRdsDatabaseAutoConfiguration.class);
		TestPropertyValues
				.of("cloud.aws.rds.test.password:secret",
						"cloud.aws.rds.test.readReplicaSupport:true")
				.applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context
				.getBean(AmazonRdsReadReplicaAwareDataSourceFactoryBean.class))
						.isNotNull();
	}

	public static class ApplicationConfigurationWithoutReadReplica {

		@Bean
		public RdsClient amazonRds() {
			RdsClient client = Mockito.mock(RdsClient.class);
			when(client.describeDBInstances(DescribeDbInstancesRequest.builder()
					.dbInstanceIdentifier("test").build())).thenReturn(
							DescribeDbInstancesResponse.builder().dbInstances(DBInstance
									.builder().dbInstanceStatus("available")
									.dbName("test").dbInstanceIdentifier("test")
									.engine("mysql").masterUsername("admin")
									.endpoint(Endpoint.builder().address("localhost")
											.port(3306).build())
									.readReplicaDBInstanceIdentifiers("read1").build())
									.build());
			return client;
		}

	}

	public static class ApplicationConfigurationWithMultipleDatabases {

		@Bean
		public RdsClient amazonRds() {
			RdsClient client = Mockito.mock(RdsClient.class);
			when(client.describeDBInstances(DescribeDbInstancesRequest.builder()
					.dbInstanceIdentifier("test").build()))
							.thenReturn(DescribeDbInstancesResponse.builder()
									.dbInstances(DBInstance.builder()
											.dbInstanceStatus("available").dbName("test")
											.dbInstanceIdentifier("test").engine("mysql")
											.masterUsername("admin")
											.endpoint(Endpoint.builder()
													.address("localhost").port(3306)
													.build())
											.build())
									.build());
			when(client.describeDBInstances(DescribeDbInstancesRequest.builder()
					.dbInstanceIdentifier("anotherOne").build()))
							.thenReturn(DescribeDbInstancesResponse.builder()
									.dbInstances(DBInstance.builder()
											.dbInstanceStatus("available").dbName("test")
											.dbInstanceIdentifier("anotherOne")
											.engine("mysql").masterUsername("admin")
											.endpoint(Endpoint.builder()
													.address("localhost").port(3306)
													.build())
											.build())
									.build());
			return client;
		}

	}

	public static class ApplicationConfigurationWithReadReplica {

		@Bean
		public RdsClient amazonRds() {
			RdsClient client = Mockito.mock(RdsClient.class);
			when(client.describeDBInstances(DescribeDbInstancesRequest.builder()
					.dbInstanceIdentifier("test").build())).thenReturn(
							DescribeDbInstancesResponse.builder().dbInstances(DBInstance
									.builder().dbInstanceStatus("available")
									.dbName("test").dbInstanceIdentifier("test")
									.engine("mysql").masterUsername("admin")
									.endpoint(Endpoint.builder().address("localhost")
											.port(3306).build())
									.readReplicaDBInstanceIdentifiers("read1").build())
									.build());
			when(client.describeDBInstances(DescribeDbInstancesRequest.builder()
					.dbInstanceIdentifier("read1").build()))
							.thenReturn(DescribeDbInstancesResponse.builder()
									.dbInstances(DBInstance.builder()
											.dbInstanceStatus("available").dbName("read1")
											.dbInstanceIdentifier("read1").engine("mysql")
											.masterUsername("admin")
											.endpoint(Endpoint.builder()
													.address("localhost").port(3306)
													.build())
											.build())
									.build());
			return client;
		}

	}

}
