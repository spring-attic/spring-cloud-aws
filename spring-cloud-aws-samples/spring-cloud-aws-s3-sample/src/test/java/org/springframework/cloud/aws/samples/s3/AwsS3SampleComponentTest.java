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

package org.springframework.cloud.aws.samples.s3;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Shameer Yusuff
 */
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class AwsS3SampleComponentTest {

	@Autowired
	AwsS3SampleComponent awsS3SampleComponent;

	@MockBean
	AmazonS3 amazonS3;

	@MockBean
	S3Object s3Object;

	@MockBean
	ListObjectsV2Result listObjectsV2Result;

	List<S3ObjectSummary> s3ObjectSummaries = new ArrayList<>();

	private static final String CONTENT = "Sample text content";

	private MultipartFile multipartFile;



	@BeforeEach
	public void setUp() {
		multipartFile = new MultipartFile() {
			@Override
			public String getName() {
				return "Test";
			}

			@Override
			public String getOriginalFilename() {
				return null;
			}

			@Override
			public String getContentType() {
				return null;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}

			@Override
			public long getSize() {
				return 0;
			}

			@Override
			public byte[] getBytes() throws IOException {
				return CONTENT.getBytes();
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return null;
			}

			@Override
			public void transferTo(File dest) throws IOException, IllegalStateException {

			}
		};

		setupS3ObjectSummaries();
	}

	private void setupS3ObjectSummaries() {
		S3ObjectSummary s3ObjectSummary1 = new S3ObjectSummary();
		s3ObjectSummary1.setKey("Bucket/Test/One.mov");
		s3ObjectSummaries.add(s3ObjectSummary1);

		S3ObjectSummary s3ObjectSummary2 = new S3ObjectSummary();
		s3ObjectSummary2.setKey("Bucket/Test/Two.mov");
		s3ObjectSummaries.add(s3ObjectSummary2);

		S3ObjectSummary s3ObjectSummary3 = new S3ObjectSummary();
		s3ObjectSummary3.setKey("Bucket/Test/Three.mov");
		s3ObjectSummaries.add(s3ObjectSummary3);
	}

	@Test
	public void testGetContentWithProvidedBucketName() {
		when(amazonS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);
		when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(
			new ByteArrayInputStream(CONTENT.getBytes()), null));

		byte[] resultContent = awsS3SampleComponent.getContent("Test", "Test");

		assertEquals(CONTENT, new String(resultContent));
	}

	@Test
	public void testGetContentWithDefaultBucketName() {
		when(amazonS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Object);
		when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(
			new ByteArrayInputStream(CONTENT.getBytes()), null));

		byte[] resultContent = awsS3SampleComponent.getContent("Test");

		assertEquals(CONTENT, new String(resultContent));
	}


	@Test
	void testUploadContentWithDefaultBucketName() {
		awsS3SampleComponent.uploadContent("Test", multipartFile);
		verify(amazonS3).putObject(any());
	}

	@Test
	void testUploadContentWithProvidedBucketName() {
		awsS3SampleComponent.uploadContent("Test", "Test", multipartFile);
		verify(amazonS3).putObject(any());
	}


	@Test
	void testGetObjectsListWithProvidedBucketName() {
		when(amazonS3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listObjectsV2Result);
		when(listObjectsV2Result.getObjectSummaries()).thenReturn(s3ObjectSummaries);

		List<String> resultContent = awsS3SampleComponent.getObjectsList("Test", "Test");

		assertEquals(Arrays.asList("Bucket/Test/One.mov", "Bucket/Test/Two.mov", "Bucket/Test/Three.mov"), resultContent);
	}

	@Test
	void testGetObjectsListWithDefaultBucketName() {
		when(amazonS3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listObjectsV2Result);
		when(listObjectsV2Result.getObjectSummaries()).thenReturn(s3ObjectSummaries);

		List<String> resultContent = awsS3SampleComponent.getObjectsList("Test");

		assertEquals(Arrays.asList("Bucket/Test/One.mov", "Bucket/Test/Two.mov", "Bucket/Test/Three.mov"), resultContent);
	}

	@TestConfiguration
	static class AwsS3SampleComponentTestConfig {
		@Bean
		public AwsS3SampleComponent awsS3SampleComponent() {
			return new AwsS3SampleComponent();
		}
	}
}
