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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentMatcher;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 * @author Greg Turnquist
 * @since 1.0
 */
public class PathMatchingSimpleStorageResourcePatternResolverTest {

	@Test
	public void testWildcardInBucketName() throws Exception {
		S3Client amazonS3 = prepareMockForTestWildcardInBucketName();

		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		assertThat(resourceLoader.getResources("s3://myBucket*/test.txt").length)
				.as("test the single '*' wildcard").isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://myBucket?wo/test.txt").length)
				.as("test the '?' wildcard").isEqualTo(1);
		assertThat(resourceLoader.getResources("s3://**/test.txt").length)
				.as("test the double '**' wildcard").isEqualTo(2);
	}

	@Test
	public void testWildcardInKey() throws IOException {
		S3Client amazonS3 = prepareMockForTestWildcardInKey();

		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		assertThat(resourceLoader.getResources("s3://myBucket/foo*/bar*/test.txt").length)
				.as("test the single '*' wildcard").isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://myBucket/").length)
				.as("test the bucket name only").isEqualTo(1);
		assertThat(resourceLoader
				.getResources("s3://myBucke?/fooOne/ba?One/test.txt").length)
						.as("test the '?' wildcard").isEqualTo(2);
		assertThat(resourceLoader.getResources("s3://myBucket/**/test.txt").length)
				.as("test the double '**' wildcard").isEqualTo(5);
		assertThat(resourceLoader.getResources("s3://myBucke?/**/*.txt").length)
				.as("test all together").isEqualTo(5);
	}

	@Test
	public void testLoadingClasspathFile() throws Exception {
		S3Client amazonS3 = mock(S3Client.class);
		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		Resource[] resources = resourceLoader.getResources(
				"classpath*:org/springframework/cloud/aws/core/io/s3/PathMatchingSimpleStorageResourcePatternResolverTest.class");
		assertThat(resources.length).isEqualTo(1);
		assertThat(resources[0].exists()).as("load without wildcards").isTrue();

		Resource[] resourcesWithFileNameWildcard = resourceLoader.getResources(
				"classpath*:org/**/PathMatchingSimpleStorageResourcePatternResolverTes?.class");
		assertThat(resourcesWithFileNameWildcard.length).isEqualTo(1);
		assertThat(resourcesWithFileNameWildcard[0].exists()).as("load with wildcards")
				.isTrue();
	}

	@Test
	public void testTruncatedListings() throws Exception {
		S3Client amazonS3 = prepareMockForTestTruncatedListings();
		ResourcePatternResolver resourceLoader = getResourceLoader(amazonS3);

		assertThat(resourceLoader.getResources("s3://myBucket/**/test.txt").length).as(
				"Test that all parts are returned when object summaries are truncated")
				.isEqualTo(5);
		assertThat(resourceLoader
				.getResources("s3://myBucket/fooOne/ba*/test.txt").length).as(
						"Test that all parts are return when common prefixes are truncated")
						.isEqualTo(1);
		assertThat(resourceLoader.getResources("s3://myBucket/").length)
				.as("Test that all parts are returned when only bucket name is used")
				.isEqualTo(1);
	}

	private S3Client prepareMockForTestTruncatedListings() {
		S3Client amazonS3 = mock(S3Client.class);

		// Without prefix calls
		ListObjectsV2Response objectListingWithoutPrefixPart1 = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/test.txt"),
						createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt"),
						createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt")),
				Collections.emptyList(), "token1");
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", null, null, null))))
						.thenReturn(objectListingWithoutPrefixPart1);

		ListObjectsV2Response objectListingWithoutPrefixPart2 = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooThree/baz/test.txt"),
						createS3ObjectSummaryWithKey("foFour/barFour/test.txt")),
				Collections.emptyList(), null);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", null, null, "token1"))))
						.thenReturn(objectListingWithoutPrefixPart2);

		// With prefix calls
		ListObjectsV2Response objectListingWithPrefixPart1 = createObjectListingMock(
				Collections.emptyList(),
				Arrays.asList(CommonPrefix.builder().prefix("dooOne/").build(),
						CommonPrefix.builder().prefix("dooTwo/").build()),
				"token2");
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", null, "/", null))))
						.thenReturn(objectListingWithPrefixPart1);

		ListObjectsV2Response objectListingWithPrefixPart2 = createObjectListingMock(
				Collections.emptyList(), Collections.singletonList(
						CommonPrefix.builder().prefix("fooOne/").build()),
				null);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", null, "/", "token2"))))
						.thenReturn(objectListingWithPrefixPart2);

		ListObjectsV2Response objectListingWithPrefixFooOne = createObjectListingMock(
				Collections.emptyList(), Collections.singletonList(
						CommonPrefix.builder().prefix("fooOne/barOne/").build()),
				null);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", "fooOne/", "/", null))))
						.thenReturn(objectListingWithPrefixFooOne);

		ListObjectsV2Response objectListingWithPrefixFooOneBarOne = createObjectListingMock(
				Collections.singletonList(
						createS3ObjectSummaryWithKey("fooOne/barOne/test.txt")),
				Collections.emptyList(), null);
		when(amazonS3.listObjectsV2(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooOne/barOne/", "/", null))))
						.thenReturn(objectListingWithPrefixFooOneBarOne);

		when(amazonS3.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder().build());

		return amazonS3;
	}

	private S3Client prepareMockForTestWildcardInBucketName() {
		S3Client amazonS3 = mock(S3Client.class);
		when(amazonS3.listBuckets()).thenReturn(ListBucketsResponse.builder()
				.buckets(Bucket.builder().name("myBucketOne").build(),
						Bucket.builder().name("myBucketTwo").build(),
						Bucket.builder().name("anotherBucket").build(),
						Bucket.builder().name("myBuckez").build())
				.build());

		// Mocks for the '**' case
		ListObjectsV2Response objectListingWithOneFile = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("test.txt")),
				Collections.emptyList(), null);
		ListObjectsV2Response emptyObjectListing = createObjectListingMock(
				Collections.emptyList(), Collections.emptyList(), null);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucketOne", null, null, null))))
						.thenReturn(objectListingWithOneFile);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucketTwo", null, null, null))))
						.thenReturn(emptyObjectListing);
		when(amazonS3.listObjectsV2(argThat(
				new ListObjectsRequestMatcher("anotherBucket", null, null, null))))
						.thenReturn(objectListingWithOneFile);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBuckez", null, null, null))))
						.thenReturn(emptyObjectListing);

		when(amazonS3.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder().build());
		return amazonS3;
	}

	/**
	 * Virtual test folder structure: fooOne/barOne/test.txt fooOne/bazOne/test.txt
	 * fooTwo/barTwo/test.txt fooThree/baz/test.txt foFour/barFour/test.txt .
	 */
	private S3Client prepareMockForTestWildcardInKey() {
		S3Client amazonS3 = mock(S3Client.class);

		// List buckets mock
		when(amazonS3.listBuckets())
				.thenReturn(
						ListBucketsResponse.builder()
								.buckets(Bucket.builder().name("myBucket").build(),
										Bucket.builder().name("myBuckets").build())
								.build());

		// Root requests
		ListObjectsV2Response objectListingMockAtRoot = createObjectListingMock(
				Collections.emptyList(),
				Arrays.asList(CommonPrefix.builder().prefix("foFour/").build(),
						CommonPrefix.builder().prefix("fooOne/").build(),
						CommonPrefix.builder().prefix("fooThree/").build(),
						CommonPrefix.builder().prefix("fooTwo/").build()),
				null);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", null, "/", null))))
						.thenReturn(objectListingMockAtRoot);

		// Requests on fooOne
		ListObjectsV2Response objectListingFooOne = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("fooOne/")),
				Arrays.asList(CommonPrefix.builder().prefix("fooOne/barOne/").build(),
						CommonPrefix.builder().prefix("fooOne/bazOne/").build()),
				null);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", "fooOne/", "/", null))))
						.thenReturn(objectListingFooOne);

		ListObjectsV2Response objectListingFooOneBarOne = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/"),
						createS3ObjectSummaryWithKey("fooOne/barOne/test.txt")),
				Collections.emptyList(), null);
		when(amazonS3.listObjectsV2(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooOne/barOne/", "/", null))))
						.thenReturn(objectListingFooOneBarOne);

		ListObjectsV2Response objectListingFooOneBazOne = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooOne/bazOne/"),
						createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt")),
				Collections.emptyList(), null);
		when(amazonS3.listObjectsV2(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooOne/bazOne/", "/", null))))
						.thenReturn(objectListingFooOneBazOne);

		// Requests on fooTwo
		ListObjectsV2Response objectListingFooTwo = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("fooTwo/")),
				Collections.singletonList(
						CommonPrefix.builder().prefix("fooTwo/barTwo/").build()),
				null);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", "fooTwo/", "/", null))))
						.thenReturn(objectListingFooTwo);

		ListObjectsV2Response objectListingFooTwoBarTwo = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooTwo/barTwo/"),
						createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt")),
				Collections.emptyList(), null);
		when(amazonS3.listObjectsV2(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooTwo/barTwo/", "/", null))))
						.thenReturn(objectListingFooTwoBarTwo);

		// Requests on fooThree
		ListObjectsV2Response objectListingFooThree = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("fooThree/")),
				Collections.singletonList(
						CommonPrefix.builder().prefix("fooTwo/baz/").build()),
				null);
		when(amazonS3.listObjectsV2(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooThree/", "/", null))))
						.thenReturn(objectListingFooThree);

		ListObjectsV2Response objectListingFooThreeBaz = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooThree/baz/"),
						createS3ObjectSummaryWithKey("fooThree/baz/test.txt")),
				Collections.emptyList(), null);
		when(amazonS3.listObjectsV2(argThat(
				new ListObjectsRequestMatcher("myBucket", "fooThree/baz/", "/", null))))
						.thenReturn(objectListingFooThreeBaz);

		// Requests for foFour
		ListObjectsV2Response objectListingFoFour = createObjectListingMock(
				Collections.singletonList(createS3ObjectSummaryWithKey("foFour/")),
				Collections.singletonList(
						CommonPrefix.builder().prefix("foFour/barFour/").build()),
				null);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", "foFour/", "/", null))))
						.thenReturn(objectListingFoFour);

		ListObjectsV2Response objectListingFoFourBarFour = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("foFour/barFour/"),
						createS3ObjectSummaryWithKey("foFour/barFour/test.txt")),
				Collections.emptyList(), null);
		when(amazonS3.listObjectsV2(argThat(
				new ListObjectsRequestMatcher("myBucket", "foFour/barFour/", "/", null))))
						.thenReturn(objectListingFoFourBarFour);

		// Requests for all
		ListObjectsV2Response fullObjectListing = createObjectListingMock(
				Arrays.asList(createS3ObjectSummaryWithKey("fooOne/barOne/test.txt"),
						createS3ObjectSummaryWithKey("fooOne/bazOne/test.txt"),
						createS3ObjectSummaryWithKey("fooTwo/barTwo/test.txt"),
						createS3ObjectSummaryWithKey("fooThree/baz/test.txt"),
						createS3ObjectSummaryWithKey("foFour/barFour/test.txt")),
				Collections.emptyList(), null);
		when(amazonS3.listObjectsV2(
				argThat(new ListObjectsRequestMatcher("myBucket", null, null, null))))
						.thenReturn(fullObjectListing);

		when(amazonS3.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder().build());

		return amazonS3;
	}

	private ListObjectsV2Response createObjectListingMock(List<S3Object> contents,
			List<CommonPrefix> commonPrefixes, String nextContinuationToken) {
		ListObjectsV2Response objectListing = ListObjectsV2Response.builder()
				.contents(contents).commonPrefixes(commonPrefixes)
				.isTruncated(nextContinuationToken != null)
				.nextContinuationToken(nextContinuationToken).build();
		return objectListing;
	}

	private S3Object createS3ObjectSummaryWithKey(String key) {
		return S3Object.builder().key(key).build();
	}

	private ResourcePatternResolver getResourceLoader(S3Client amazonS3) {
		DefaultResourceLoader loader = new DefaultResourceLoader();
		loader.addProtocolResolver(new SimpleStorageProtocolResolver(amazonS3));
		return new PathMatchingSimpleStorageResourcePatternResolver(amazonS3,
				new PathMatchingResourcePatternResolver(loader));
	}

	private static final class ListObjectsRequestMatcher
			implements ArgumentMatcher<ListObjectsV2Request> {

		private final String bucketName;

		private final String prefix;

		private final String delimiter;

		private final String continuationToken;

		private ListObjectsRequestMatcher(String bucketName, String prefix,
				String delimiter, String continuationToken) {
			this.bucketName = bucketName;
			this.prefix = prefix;
			this.delimiter = delimiter;
			this.continuationToken = continuationToken;
		}

		@Override
		public boolean matches(ListObjectsV2Request listObjectsRequest) {
			if (listObjectsRequest == null) {
				return false;
			}
			boolean bucketNameIsEqual;
			if (listObjectsRequest.bucket() != null) {
				bucketNameIsEqual = listObjectsRequest.bucket().equals(this.bucketName);
			}
			else {
				bucketNameIsEqual = this.bucketName == null;
			}

			boolean prefixIsEqual;
			if (listObjectsRequest.prefix() != null) {
				prefixIsEqual = listObjectsRequest.prefix().equals(this.prefix);
			}
			else {
				prefixIsEqual = this.prefix == null;
			}

			boolean delimiterIsEqual;
			if (listObjectsRequest.delimiter() != null) {
				delimiterIsEqual = listObjectsRequest.delimiter().equals(this.delimiter);
			}
			else {
				delimiterIsEqual = this.delimiter == null;
			}

			boolean continuationTokenIsEqual;
			if (listObjectsRequest.continuationToken() != null) {
				continuationTokenIsEqual = listObjectsRequest.continuationToken()
						.equals(this.continuationToken);
			}
			else {
				continuationTokenIsEqual = this.continuationToken == null;
			}

			return delimiterIsEqual && prefixIsEqual
					&& bucketNameIsEqual & continuationTokenIsEqual;
		}

	}

}
