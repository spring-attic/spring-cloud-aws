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

import org.junit.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleStorageProtocolResolverTest {

	@Test
	public void testGetResourceWithExistingResource() {

		S3Client amazonS3 = mock(S3Client.class);

		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		resourceLoader.addProtocolResolver(new SimpleStorageProtocolResolver(amazonS3));

		HeadObjectResponse headObjectResponse = HeadObjectResponse.builder().build();
		when(amazonS3.headObject(any(HeadObjectRequest.class)))
				.thenReturn(headObjectResponse);

		String resourceName = "s3://bucket/object/";
		Resource resource = resourceLoader.getResource(resourceName);
		assertThat(resource).isNotNull();
	}

	@Test
	public void testGetResourceWithNonExistingResource() {

		S3Client amazonS3 = mock(S3Client.class);

		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		resourceLoader.addProtocolResolver(new SimpleStorageProtocolResolver(amazonS3));

		String resourceName = "s3://bucket/object/";
		Resource resource = resourceLoader.getResource(resourceName);
		assertThat(resource).isNotNull();
	}

	@Test
	public void testGetResourceWithVersionId() {
		S3Client amazonS3 = mock(S3Client.class);

		SimpleStorageProtocolResolver resourceLoader = new SimpleStorageProtocolResolver(
				amazonS3);

		HeadObjectResponse headObjectResponse = HeadObjectResponse.builder().build();
		when(amazonS3.headObject(any(HeadObjectRequest.class)))
				.thenReturn(headObjectResponse);

		String resourceName = "s3://bucket/object^versionIdValue";
		Resource resource = resourceLoader.resolve(resourceName,
				new DefaultResourceLoader());
		assertThat(resource).isNotNull();
	}

	@Test
	public void testGetResourceWithDifferentPatterns() {

		S3Client amazonS3 = mock(S3Client.class);

		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		resourceLoader.addProtocolResolver(new SimpleStorageProtocolResolver(amazonS3));

		assertThat(resourceLoader.getResource("s3://bucket/object/")).isNotNull();

		assertThat(resourceLoader.getResource("s3://bucket/object")).isNotNull();

		assertThat(resourceLoader.getResource("s3://prefix.bucket/object.suffix"))
				.isNotNull();

		// TODO SDK2 migration: update and uncomment
		// verify(amazonS3, times(0)).getObjectMetadata("bucket", "object");
	}

	@Test
	public void testGetResourceWithMalFormedUrl() {

		S3Client amazonS3 = mock(S3Client.class);

		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		resourceLoader.addProtocolResolver(new SimpleStorageProtocolResolver(amazonS3));

		try {
			assertThat(resourceLoader.getResource("s3://bucketsAndObject")).isNotNull();
			fail("expected exception due to missing object");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage().contains("valid bucket name")).isTrue();
		}

		// TODO SDK2 migration: update and uncomment
		// verify(amazonS3, times(0)).getObjectMetadata("bucket", "object");
	}

	@Test
	public void testValidS3Pattern() {
		S3Client amazonS3 = mock(S3Client.class);

		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		resourceLoader.addProtocolResolver(new SimpleStorageProtocolResolver(amazonS3));

		// None of the patterns below should throw an exception
		resourceLoader.getResource("s3://bucket/key");
		resourceLoader.getResource("S3://BuCket/key");
		resourceLoader.getResource("s3://bucket/folder1/folder2/key");
		resourceLoader.getResource("s3://bucket/folder1/folder2/key^versionIdValue");
	}

}
