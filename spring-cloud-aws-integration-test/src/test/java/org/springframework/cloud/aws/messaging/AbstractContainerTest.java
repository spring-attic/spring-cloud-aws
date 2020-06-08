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

package org.springframework.cloud.aws.messaging;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Import;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * @author Alain Sahli
 * @author Philip Riecks
 */
@Testcontainers
@Import(AmazonMessagingSdkConfig.class)
abstract class AbstractContainerTest {

	@Container
	public static LocalStackContainer localStack = new LocalStackContainer("0.11.2")
		.withReuse(true)
		.withEnv("DEFAULT_REGION", "eu-west-1")
		.withCopyFileToContainer(MountableFile
			.forClasspathResource("/messaging/redrivePolicy.json"), "/tmp/redrivePolicy.json")
		.withServices(SQS, SNS);

	@Autowired
	protected SimpleMessageListenerContainer simpleMessageListenerContainer;

	@BeforeAll
	static void beforeAll() throws IOException, InterruptedException {

		// TODO: Extract the initialization to a shell script and copy it on container startup

		List<String> queuesToCreate = Arrays
			.asList("LoadTestQueue", "DeadLetterQueue", "ManualDeletionQueue",
				"QueueListenerTest", "SendToQueue", "NotificationQueue",
				"JsonQueue", "StringQueue", "StreamQueue");

		for (String queueToCreate : queuesToCreate) {
			localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name",
				queueToCreate);
		}

		localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name",
			"QueueWithRedrivePolicy", "--attributes", "file:///tmp/redrivePolicy.json");

		List<String> topicsToCreate = Arrays.asList("SqsReceivingSnsTopic");

		for (String topicToCreate : topicsToCreate) {
			localStack.execInContainer("awslocal", "sns", "create-topic", "--name",
				topicToCreate);
		}

		localStack
			.execInContainer("awslocal", "sns", "subscribe",
				"--topic-arn", "arn:aws:sns:eu-west-1:000000000000:SqsReceivingSnsTopic",
				"--protocol", "sqs",
				"--notification-endpoint", "arn:aws:sqs:eu-west-1:000000000000:NotificationQueue");
	}

	@BeforeEach
	void setUp() throws Exception {
		if (!this.simpleMessageListenerContainer.isRunning()) {
			this.simpleMessageListenerContainer.start();
		}
	}

	@AfterEach
	void tearDown() throws Exception {
		if (this.simpleMessageListenerContainer.isRunning()) {
			CountDownLatch countDownLatch = new CountDownLatch(1);
			this.simpleMessageListenerContainer.stop(countDownLatch::countDown);

			if (!countDownLatch.await(15, TimeUnit.SECONDS)) {
				throw new Exception("Couldn't stop container within 15 seconds");
			}
		}
	}

}
