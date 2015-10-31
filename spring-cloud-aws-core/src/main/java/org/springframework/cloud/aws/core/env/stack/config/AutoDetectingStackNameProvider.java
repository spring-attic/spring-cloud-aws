/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.core.env.stack.config;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.TagDescription;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.aws.core.env.ec2.AmazonEc2InstanceIdProvider;
import org.springframework.cloud.aws.core.env.ec2.InstanceIdProvider;
import org.springframework.util.Assert;

import java.util.Collections;

/**
 * Represents a stack name provider that automatically detects the current stack name based on the amazon elastic cloud
 * environment.
 *
 * @author Christian Stettler
 * @author Agim Emruli
 */
class AutoDetectingStackNameProvider implements StackNameProvider, InitializingBean {

	private static final String AWS_CLOUDFORMATION_STACK_NAME_TAG_KEY = "aws:cloudformation:stack-name";
	private final InstanceIdProvider instanceIdProvider;
	private final AmazonEC2 amazonEC2;

	private String stackName;

	AutoDetectingStackNameProvider(AmazonEC2 amazonEC2, InstanceIdProvider instanceIdProvider) {
		this.amazonEC2 = amazonEC2;
		this.instanceIdProvider = instanceIdProvider;
		afterPropertiesSet();
	}

	AutoDetectingStackNameProvider(AmazonEC2 amazonEC2) {
		this(amazonEC2, new AmazonEc2InstanceIdProvider());
	}

	@Override
	public void afterPropertiesSet() {
		if (this.stackName == null) {
			this.stackName = autoDetectStackName(this.amazonEC2, this.instanceIdProvider.getCurrentInstanceId());
		}
	}

	@Override
	public String getStackName() {
		return this.stackName;
	}

	private static String autoDetectStackName(AmazonEC2 amazonEC2, String instanceId) {
		Assert.notNull(amazonEC2, "No valid amazon EC2 client defined");
		Assert.notNull(instanceId, "No valid instance id defined");

		DescribeTagsResult describeTagsResult = amazonEC2.describeTags(new DescribeTagsRequest().withFilters(
				new Filter("resource-id", Collections.singletonList(instanceId)),
				new Filter("resource-type", Collections.singletonList("instance"))));

		if (describeTagsResult != null && describeTagsResult.getTags() != null) {
			for (TagDescription tag : describeTagsResult.getTags()) {
				if (AWS_CLOUDFORMATION_STACK_NAME_TAG_KEY.equals(tag.getKey())) {
					return tag.getValue();
				}
			}
		}

		throw new IllegalStateException("No tag with stack name found in EC2 instance '" + instanceId + "'");
	}
}
