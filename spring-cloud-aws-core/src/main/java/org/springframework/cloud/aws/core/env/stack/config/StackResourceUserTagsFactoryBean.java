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

package org.springframework.cloud.aws.core.env.stack.config;

import java.util.LinkedHashMap;
import java.util.Map;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * @author Agim Emruli
 */
public class StackResourceUserTagsFactoryBean
		extends AbstractFactoryBean<Map<String, String>> {

	private final CloudFormationClient amazonCloudFormation;

	private final StackNameProvider stackNameProvider;

	public StackResourceUserTagsFactoryBean(CloudFormationClient amazonCloudFormation,
			StackNameProvider stackNameProvider) {
		this.amazonCloudFormation = amazonCloudFormation;
		this.stackNameProvider = stackNameProvider;
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}

	@Override
	protected Map<String, String> createInstance() throws Exception {
		LinkedHashMap<String, String> userTags = new LinkedHashMap<>();
		DescribeStacksResponse stacksResult = this.amazonCloudFormation
				.describeStacks(DescribeStacksRequest.builder()
						.stackName(this.stackNameProvider.getStackName()).build());
		for (Stack stack : stacksResult.stacks()) {
			for (Tag tag : stack.tags()) {
				userTags.put(tag.key(), tag.value());
			}
		}
		return userTags;
	}

}
