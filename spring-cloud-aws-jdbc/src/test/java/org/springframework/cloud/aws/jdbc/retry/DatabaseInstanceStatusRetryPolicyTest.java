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

package org.springframework.cloud.aws.jdbc.retry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.context.RetryContextSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test class for {@link DatabaseInstanceStatusRetryPolicy}.
 *
 * @author Agim Emruli
 */
@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
public class DatabaseInstanceStatusRetryPolicyTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void canRetry_retryPossibleDueToAvailableDatabase_returnsTrue()
			throws Exception {
		// Arrange
		RdsClient rdsClient = mock(RdsClient.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(
				rdsClient, "test");
		when(rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder()
				.dbInstanceIdentifier("test").build()))
						.thenReturn(
								DescribeDbInstancesResponse.builder()
										.dbInstances(DBInstance.builder()
												.dbInstanceStatus("available").build())
										.build());

		RetryContext retryContext = policy.open(new RetryContextSupport(null));

		// Act
		policy.registerThrowable(retryContext,
				new TransientDataAccessResourceException("not available"));

		// Assert
		assertThat(policy.canRetry(retryContext)).isTrue();
		policy.close(retryContext);
	}

	@Test
	public void canRetry_withResourceIdResolver_returnsTrue() throws Exception {
		// Arrange
		RdsClient rdsClient = mock(RdsClient.class);
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(
				rdsClient, "foo");
		when(rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder()
				.dbInstanceIdentifier("test").build()))
						.thenReturn(
								DescribeDbInstancesResponse.builder()
										.dbInstances(DBInstance.builder()
												.dbInstanceStatus("available").build())
										.build());

		when(resourceIdResolver.resolveToPhysicalResourceId("foo")).thenReturn("test");

		policy.setResourceIdResolver(resourceIdResolver);

		RetryContext retryContext = policy.open(new RetryContextSupport(null));

		// Act
		policy.registerThrowable(retryContext,
				new TransientDataAccessResourceException("not available"));

		// Assert
		assertThat(policy.canRetry(retryContext)).isTrue();
		policy.close(retryContext);
	}

	@Test
	public void canRetry_retryNotPossibleDueToNoDatabase_returnsFalse() throws Exception {
		// Arrange
		RdsClient rdsClient = mock(RdsClient.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(
				rdsClient, "test");

		when(rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder()
				.dbInstanceIdentifier("test").build())).thenThrow(
						DbInstanceNotFoundException.builder().message("test").build());

		RetryContext retryContext = policy.open(new RetryContextSupport(null));

		// Act
		policy.registerThrowable(retryContext,
				new TransientDataAccessResourceException("not available"));

		// Assert
		assertThat(policy.canRetry(retryContext)).isFalse();
		policy.close(retryContext);
	}

	@Test
	public void canRetry_multipleDatabasesFoundForInstanceIdentifier_reportsException()
			throws Exception {
		// Arrange
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException
				.expectMessage("Multiple databases found for same identifier");

		RdsClient rdsClient = mock(RdsClient.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(
				rdsClient, "test");

		DescribeDbInstancesResponse describeDbInstancesResponse = DescribeDbInstancesResponse
				.builder()
				.dbInstances(DBInstance.builder().build(), DBInstance.builder().build())
				.build();
		when(rdsClient.describeDBInstances(DescribeDbInstancesRequest.builder()
				.dbInstanceIdentifier("test").build()))
						.thenReturn(describeDbInstancesResponse);

		RetryContext retryContext = policy.open(new RetryContextSupport(null));

		// Act
		policy.registerThrowable(retryContext,
				new TransientDataAccessResourceException("not available"));

		// Assert
		policy.canRetry(retryContext);
	}

	@Test
	public void canRetry_noExceptionRegistered_returnsTrue() throws Exception {
		// Arrange
		RdsClient rdsClient = mock(RdsClient.class);

		DatabaseInstanceStatusRetryPolicy policy = new DatabaseInstanceStatusRetryPolicy(
				rdsClient, "test");

		RetryContext retryContext = new RetryContextSupport(null);

		// Act
		policy.open(retryContext);

		// Assert
		assertThat(policy.canRetry(retryContext)).isTrue();
	}

}
