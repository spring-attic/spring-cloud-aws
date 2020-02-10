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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.Instant;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import org.springframework.core.task.SyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class SimpleStorageResourceTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void exists_withExistingObjectMetadata_returnsTrue() throws Exception {
		// Arrange
		S3Client amazonS3 = mock(S3Client.class);
		when(amazonS3.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder().build());

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.exists()).isTrue();
	}

	@Test
	public void exists_withoutExistingObjectMetadata_returnsFalse() throws Exception {
		// Arrange
		S3Client amazonS3 = mock(S3Client.class);
		when(amazonS3.headObject(any(HeadObjectRequest.class))).thenReturn(null);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Act
		assertThat(simpleStorageResource.exists()).isFalse();
	}

	@Test
	public void contentLength_withExistingResource_returnsContentLengthOfObjectMetaData()
			throws Exception {
		// Arrange
		S3Client amazonS3 = mock(S3Client.class);
		when(amazonS3.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder().contentLength(1234L).build());

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.contentLength()).isEqualTo(1234L);
	}

	@Test
	public void lastModified_withExistingResource_returnsLastModifiedDateOfResource()
			throws Exception {
		// Arrange
		S3Client amazonS3 = mock(S3Client.class);
		Instant lastModified = Instant.now();
		when(amazonS3.headObject(any(HeadObjectRequest.class))).thenReturn(
				HeadObjectResponse.builder().lastModified(lastModified).build());

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.lastModified())
				.isEqualTo(lastModified.getEpochSecond());
	}

	@Test
	public void contentLength_fileDoesNotExists_reportsError() throws Exception {
		// Arrange
		this.expectedException.expect(FileNotFoundException.class);
		this.expectedException.expectMessage("not found");

		S3Client amazonS3 = mock(S3Client.class);
		when(amazonS3.headObject(any(HeadObjectRequest.class))).thenReturn(null);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Act
		simpleStorageResource.contentLength();

		// Assert
	}

	@Test
	public void lastModified_fileDoestNotExist_reportsError() throws Exception {
		// Arrange
		this.expectedException.expect(FileNotFoundException.class);
		this.expectedException.expectMessage("not found");

		S3Client amazonS3 = mock(S3Client.class);
		when(amazonS3.headObject(any(HeadObjectRequest.class))).thenReturn(null);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Act
		simpleStorageResource.lastModified();

		// Assert
	}

	@Test
	public void getFileName_existingObject_returnsFileNameWithoutBucketNameFromParameterWithoutActuallyFetchingTheFile()
			throws Exception {
		// Arrange
		S3Client amazonS3 = mock(S3Client.class);
		when(amazonS3.headObject(any(HeadObjectRequest.class))).thenReturn(null);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.getFilename()).isEqualTo("object");
	}

	@Test
	public void getInputStream_existingObject_returnsInputStreamWithContent()
			throws Exception {
		// Arrange
		S3Client amazonS3 = mock(S3Client.class);
		HeadObjectResponse headObjectResponse = mock(HeadObjectResponse.class);
		when(amazonS3.headObject(any(HeadObjectRequest.class)))
				.thenReturn(headObjectResponse);

		when(amazonS3.getObject(any(GetObjectRequest.class)))
				.thenReturn(new ResponseInputStream(GetObjectResponse.builder().build(),
						AbortableInputStream
								.create(new ByteArrayInputStream(new byte[] { 42 }))));

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.exists()).isTrue();
		assertThat(simpleStorageResource.getInputStream().read()).isEqualTo(42);
	}

	@Test
	public void getDescription_withoutObjectMetaData_returnsDescriptiveDescription()
			throws Exception {
		// Arrange
		S3Client amazonS3 = mock(S3Client.class);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"1", "2", new SyncTaskExecutor());
		String description = simpleStorageResource.getDescription();

		// Assert
		assertThat(description.contains("bucket")).isTrue();
		assertThat(description.contains("object")).isTrue();
		assertThat(description.contains("1")).isTrue();
		assertThat(description.contains("2")).isTrue();
	}

	@Test
	public void getUrl_existingObject_returnsUrlWithS3Prefix() throws Exception {

		S3Client amazonS3 = mock(S3Client.class);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Assert
		assertThat(simpleStorageResource.getURL())
				.isEqualTo(new URL("https://s3.eu-west-1.amazonaws.com/bucket/object"));

	}

	@Test
	public void getFile_existingObject_throwsMeaningFullException() throws Exception {

		this.expectedException.expect(UnsupportedOperationException.class);
		this.expectedException.expectMessage("getInputStream()");

		S3Client amazonS3 = mock(S3Client.class);

		// Act
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Assert
		simpleStorageResource.getFile();

	}

	@Test
	public void createRelative_existingObject_returnsRelativeCreatedFile()
			throws IOException {

		// Arrange
		S3Client amazonS3 = mock(S3Client.class);
		HeadObjectResponse headObjectResponse = mock(HeadObjectResponse.class);
		when(amazonS3.headObject(any(HeadObjectRequest.class)))
				.thenReturn(HeadObjectResponse.builder().build());
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucket", "object", new SyncTaskExecutor());

		// Act
		SimpleStorageResource subObject = simpleStorageResource
				.createRelative("subObject");

		// Assert
		assertThat(subObject.getFilename()).isEqualTo("object/subObject");
	}

	@Test
	public void writeFile_forNewFile_writesFileContent() throws Exception {
		// Arrange
		S3Client amazonS3 = mock(S3Client.class);
		SimpleStorageResource simpleStorageResource = new SimpleStorageResource(amazonS3,
				"bucketName", "objectName", new SyncTaskExecutor());
		String messageContext = "myFileContent";
		// TODO SDK2 migration: assert passed in bucket and key
		when(amazonS3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.thenAnswer((Answer<PutObjectResponse>) invocation -> {
					assertThat(invocation.getArguments()[0]).isEqualTo("bucketName");
					assertThat(invocation.getArguments()[1]).isEqualTo("objectName");
					byte[] content = new byte[messageContext.length()];
					assertThat(((InputStream) invocation.getArguments()[2]).read(content))
							.isEqualTo(content.length);
					assertThat(new String(content)).isEqualTo(messageContext);
					return PutObjectResponse.builder().build();
				});
		OutputStream outputStream = simpleStorageResource.getOutputStream();

		// Act
		outputStream.write(messageContext.getBytes());
		outputStream.flush();
		outputStream.close();

		// Assert
	}

}
