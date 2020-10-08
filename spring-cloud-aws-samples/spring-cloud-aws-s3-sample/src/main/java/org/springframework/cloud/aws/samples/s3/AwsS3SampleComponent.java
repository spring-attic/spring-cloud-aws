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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


/**
 * @author Shameer Yusuff
 */
@Component
@Slf4j
public class AwsS3SampleComponent {

	@Autowired
	AmazonS3 amazonS3;

	@Value("${aws.s3.bucket}")
	private String bucketName;

	/**
	 * Returns byte array stream of the file from the default bucket specified in properties
	 */
	public byte[] getContent(String fileName) {
		return getContent(bucketName, fileName);
	}

	/**
	 * Returns byte array stream of the file from the input bucket
	 */
	public byte[] getContent(String bucketName, String fileName) {
		log.debug("Enter getContent fileName={}", fileName);
		byte[] contentAsBytes;
		try {
			S3Object s3Object = amazonS3.getObject(new GetObjectRequest(bucketName, fileName));
			contentAsBytes = IOUtils.toByteArray(s3Object.getObjectContent());
		} catch (Exception e) {
			log.error("IOException getContent fileName={}", fileName, e);
			throw new RuntimeException("Error while getting content for filename=" + fileName, e);
		}
		log.debug("Exit getContent fileName={}", fileName);
		return contentAsBytes;
	}

	/**
	 * Uploads the content of the provided input file to the default bucket specified in properties in S3
	 */
	public void uploadContent(String fileName, MultipartFile content) {
		uploadContent(bucketName, fileName, content);
	}

	/**
	 * Uploads the content of the provided input file to the input bucket in S3
	 */
	public void uploadContent(String bucketName, String fileName, MultipartFile content) {
		ByteArrayInputStream contentAsStream = null;
		try {
			contentAsStream = new ByteArrayInputStream(content.getBytes());
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(content.getBytes().length);
			amazonS3.putObject(new PutObjectRequest(bucketName, fileName, contentAsStream, metadata));
		}
		catch (AmazonServiceException e) {
			log.error("Error while uploading content in bucket={} fileName={}, exception={}", bucketName, fileName, e.getMessage());
			throw new RuntimeException("Error while uploading content for filename=" + fileName, e);
		}
		catch (IOException e) {
			log.error("IOException uploadContent fileName={}", fileName, e);
			throw new RuntimeException("Error while uploading content for filename=" + fileName, e);
		}
		finally {
			if (contentAsStream != null) {
				try {
					contentAsStream.close();
				}
				catch (IOException e) {
					log.error("Content Stream close error in uploadContent fileName={}, exception={}", fileName, e.getMessage());
				}
			}
		}
	}

	/**
	 * Returns the objects list from the default bucket and contentPath provided
	 */
	public List<String> getObjectsList(String contentPath) {
		return getObjectsList(bucketName, contentPath);
	}

	/**
	 * Returns the objects list from the input bucket and contentPath provided
	 */
	public List<String> getObjectsList(String bucketName, String contentPath) {
		List<String> objectNames = new ArrayList<>();
		ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withPrefix(contentPath);
		ListObjectsV2Result listing = amazonS3.listObjectsV2(req);
		for (S3ObjectSummary summary : listing.getObjectSummaries()) {
			objectNames.add(summary.getKey());
		}
		return objectNames;
	}
}
