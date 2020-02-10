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

package org.springframework.cloud.aws.jdbc.config.annotation;

import java.util.HashMap;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.Endpoint;

import org.springframework.cloud.aws.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AmazonRdsInstanceConfigurationTest {

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

		// Act
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithoutReadReplica.class);

		// Assert
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();
	}

	@Test
	public void configureBean_withCustomDatabaseNameConfigured_configuresDataSourceWithCustomDatabaseName()
			throws Exception {
		// Arrange

		// Act
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithoutReadReplicaAndCustomDbName.class);

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
	public void configureBean_withCustomDatabaseNameConfigured_configuresDataSourceWithCustomDataSourceFactory()
			throws Exception {
		// Arrange

		// Act
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithoutReadReplicaAndCustomDataSourceFactory.class);

		// Assert
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isNotNull();
		assertThat(this.context.getBean(AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();

		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
		assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource)
				.getValidationQuery()).isEqualTo("SELECT 1 FROM TEST");
		assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource).getInitialSize())
				.isEqualTo(0);
	}

	// @checkstyle:off
	@Test
	public void configureBean_withDefaultClientSpecifiedAndNoReadReplicaWithExpressions_configuresFactoryBeanWithoutReadReplicaAndResolvedExpressions()
			throws Exception {
		// @checkstyle:on
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		HashMap<String, Object> propertySourceProperties = new HashMap<>();
		propertySourceProperties.put("dbInstanceIdentifier", "test");
		propertySourceProperties.put("password", "secret");
		propertySourceProperties.put("username", "admin");

		this.context.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("test", propertySourceProperties));

		// Act
		this.context
				.register(ApplicationConfigurationWithoutReadReplicaAndExpressions.class);
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();
	}

	// @checkstyle:off
	@Test
	public void configureBean_withDefaultClientSpecifiedAndNoReadReplicaWithPlaceHolder_configuresFactoryBeanWithoutReadReplicaAndResolvedPlaceHolders()
			throws Exception {
		// @checkstyle:on
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		HashMap<String, Object> propertySourceProperties = new HashMap<>();
		propertySourceProperties.put("dbInstanceIdentifier", "test");
		propertySourceProperties.put("password", "secret");
		propertySourceProperties.put("username", "admin");

		this.context.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("test", propertySourceProperties));

		// Act
		this.context
				.register(ApplicationConfigurationWithoutReadReplicaAndPlaceHolder.class);
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();
	}

	@Test
	public void configureBean_withDefaultClientSpecifiedAndReadReplica_configuresFactoryBeanWithReadReplicaEnabled()
			throws Exception {
		// Arrange

		// Act
		this.context = new AnnotationConfigApplicationContext(
				ApplicationConfigurationWithReadReplica.class);

		// Assert
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context
				.getBean(AmazonRdsReadReplicaAwareDataSourceFactoryBean.class))
						.isNotNull();
	}

	@EnableRdsInstance(dbInstanceIdentifier = "test", password = "secret")
	public static class ApplicationConfigurationWithoutReadReplica {

		@Bean
		public RdsClient amazonRDS() {
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

	@EnableRdsInstance(dbInstanceIdentifier = "test", password = "secret",
			databaseName = "fooDb")
	public static class ApplicationConfigurationWithoutReadReplicaAndCustomDbName {

		@Bean
		public RdsClient amazonRDS() {
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

	@EnableRdsInstance(dbInstanceIdentifier = "test", password = "secret")
	public static class ApplicationConfigurationWithoutReadReplicaAndCustomDataSourceFactory {

		@Bean
		public RdsClient amazonRDS() {
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

		@Bean
		public RdsInstanceConfigurer instanceConfigurer() {
			return () -> {
				TomcatJdbcDataSourceFactory dataSourceFactory = new TomcatJdbcDataSourceFactory();
				dataSourceFactory.setInitialSize(0);
				dataSourceFactory.setValidationQuery("SELECT 1 FROM TEST");
				return dataSourceFactory;
			};
		}

	}

	// @checkstyle:off
	@EnableRdsInstance(dbInstanceIdentifier = "#{environment.dbInstanceIdentifier}",
			password = "#{environment.password}", username = "#{environment.username}")
	public static class ApplicationConfigurationWithoutReadReplicaAndExpressions {

		// @checkstyle:on

		@Bean
		public RdsClient amazonRDS() {
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

	// @checkstyle:off
	@EnableRdsInstance(dbInstanceIdentifier = "${dbInstanceIdentifier}",
			password = "${password}", username = "${username}")
	public static class ApplicationConfigurationWithoutReadReplicaAndPlaceHolder {

		// @checkstyle:on

		@Bean
		static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		public RdsClient amazonRDS() {
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

	@EnableRdsInstance(dbInstanceIdentifier = "test", password = "secret",
			readReplicaSupport = true)
	public static class ApplicationConfigurationWithReadReplica {

		@Bean
		public RdsClient amazonRDS() {
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
