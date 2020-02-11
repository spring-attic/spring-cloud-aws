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

package org.springframework.cloud.aws.jdbc.rds;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.Test;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.Endpoint;

import org.springframework.cloud.aws.jdbc.datasource.DataSourceFactory;
import org.springframework.cloud.aws.jdbc.datasource.DataSourceInformation;
import org.springframework.cloud.aws.jdbc.datasource.ReadOnlyRoutingDataSource;
import org.springframework.cloud.aws.jdbc.datasource.support.DatabaseType;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class AmazonRdsReadReplicaAwareDataSourceFactoryBeanTest {

	@Test
	public void afterPropertiesSet_instanceWithoutReadReplica_createsNoDataSourceRouter()
			throws Exception {
		// Arrange
		RdsClient rdsClient = mock(RdsClient.class);
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);

		when(rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder()
				.dbInstanceIdentifier("test").build())).thenReturn(
						DescribeDbInstancesResponse.builder().dbInstances(DBInstance
								.builder().dbInstanceStatus("available").dbName("test")
								.dbInstanceIdentifier("test").engine("mysql")
								.masterUsername("admin").endpoint(Endpoint.builder()
										.address("localhost").port(3306).build())
								.build()).build());

		AmazonRdsReadReplicaAwareDataSourceFactoryBean factoryBean = new AmazonRdsReadReplicaAwareDataSourceFactoryBean(
				rdsClient, "test", "secret");
		factoryBean.setDataSourceFactory(dataSourceFactory);
		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret")))
						.thenReturn(mock(DataSource.class));

		// Act
		factoryBean.afterPropertiesSet();

		// Assert
		DataSource datasource = factoryBean.getObject();
		assertThat(datasource).isNotNull();

		verify(dataSourceFactory, times(1)).createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret"));
	}

	@Test
	public void afterPropertiesSet_instanceWithReadReplica_createsDataSourceRouter()
			throws Exception {
		// Arrange
		RdsClient rdsClient = mock(RdsClient.class);
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);

		when(rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder()
				.dbInstanceIdentifier("test").build())).thenReturn(
						DescribeDbInstancesResponse.builder().dbInstances(DBInstance
								.builder().dbInstanceStatus("available").dbName("test")
								.dbInstanceIdentifier("test").engine("mysql")
								.masterUsername("admin")
								.endpoint(Endpoint.builder().address("localhost")
										.port(3306).build())
								.readReplicaDBInstanceIdentifiers("read1", "read2")
								.build()).build());

		when(rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder()
				.dbInstanceIdentifier("read1").build())).thenReturn(
						DescribeDbInstancesResponse.builder().dbInstances(DBInstance
								.builder().dbInstanceStatus("available").dbName("read1")
								.dbInstanceIdentifier("read1").engine("mysql")
								.masterUsername("admin").endpoint(Endpoint.builder()
										.address("localhost").port(3306).build())
								.build()).build());

		when(rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder()
				.dbInstanceIdentifier("read2").build())).thenReturn(
						DescribeDbInstancesResponse.builder().dbInstances(DBInstance
								.builder().dbInstanceStatus("available").dbName("read2")
								.dbInstanceIdentifier("read2").engine("mysql")
								.masterUsername("admin").endpoint(Endpoint.builder()
										.address("localhost").port(3306).build())
								.build()).build());

		DataSource createdDataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);

		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret")))
						.thenReturn(createdDataSource);
		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "read1", "admin", "secret")))
						.thenReturn(createdDataSource);
		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "read2", "admin", "secret")))
						.thenReturn(createdDataSource);
		when(createdDataSource.getConnection()).thenReturn(connection);

		AmazonRdsReadReplicaAwareDataSourceFactoryBean factoryBean = new AmazonRdsReadReplicaAwareDataSourceFactoryBean(
				rdsClient, "test", "secret");
		factoryBean.setDataSourceFactory(dataSourceFactory);

		// Act
		factoryBean.afterPropertiesSet();

		// Assert
		DataSource datasource = factoryBean.getObject();
		assertThat(datasource).isNotNull();
		assertThat(datasource instanceof LazyConnectionDataSourceProxy).isTrue();

		ReadOnlyRoutingDataSource source = (ReadOnlyRoutingDataSource) ((LazyConnectionDataSourceProxy) datasource)
				.getTargetDataSource();
		assertThat(source.getDataSources().size()).isEqualTo(3);
	}

}
