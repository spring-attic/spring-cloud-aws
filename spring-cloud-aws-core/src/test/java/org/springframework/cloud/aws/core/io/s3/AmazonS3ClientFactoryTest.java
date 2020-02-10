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

package org.springframework.cloud.aws.core.io.s3;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 */
// TODO SDK2 migration: test for missing header
public class AmazonS3ClientFactoryTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void createClientForEndpointUrl_withNullEndpoint_throwsIllegalArgumentException() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		S3Client amazonS3 = S3Client.builder().region(Region.US_WEST_2).build();

		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("Endpoint Url must not be null");

		// Act
		amazonS3ClientFactory.createClientForRegion(amazonS3, null);

		// Prepare

	}

	@Test
	public void createClientForEndpointUrl_withNullAmazonS3Client_throwsIllegalArgumentException() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();

		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("AmazonS3 must not be null");

		// Act
		amazonS3ClientFactory.createClientForRegion(null, "eu-central-1");

		// Prepare

	}

	@Test
	public void createClientForEndpointUrl_withRegion_createClientForRegion() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		S3Client amazonS3 = S3Client.builder().region(Region.EU_CENTRAL_1).build();

		// Act
		S3Client newClient = amazonS3ClientFactory.createClientForRegion(amazonS3,
				"us-west-1");

		// Prepare
		// TODO SDK2 migration: update and uncomment
		// assertThat(newClient.getRegionName()).isEqualTo(Region.US_WEST_1);
	}

	@Test
	public void createClientForEndpointUrl_withProxiedClient_createClientForCustomRegion() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		S3Client amazonS3 = S3Client.builder().region(Region.EU_WEST_1).build();

		// Act
		S3Client newClient = amazonS3ClientFactory.createClientForRegion(amazonS3,
				"eu-central-1");

		// Prepare
		// TODO SDK2 migration: update and uncomment
		// assertThat(newClient.getRegionName()).isEqualTo(Regions.EU_CENTRAL_1.getName());
	}

	@Test
	public void createClientForEndpointUrl_withCustomRegionUrlAndCachedClient_returnsCachedClient() {
		// Arrange
		AmazonS3ClientFactory amazonS3ClientFactory = new AmazonS3ClientFactory();
		S3Client amazonS3 = S3Client.builder().region(Region.EU_WEST_1).build();

		S3Client existingClient = amazonS3ClientFactory.createClientForRegion(amazonS3,
				"eu-central-1");

		// Act
		S3Client cachedClient = amazonS3ClientFactory.createClientForRegion(amazonS3,
				"eu-central-1");

		// Prepare
		assertThat(existingClient).isSameAs(cachedClient);
	}

}
