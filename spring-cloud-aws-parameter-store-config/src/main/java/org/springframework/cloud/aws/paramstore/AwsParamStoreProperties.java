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

package org.springframework.cloud.aws.paramstore;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.regex.Pattern;

/**
 * Configuration properties for the AWS Parameter Store integration. Mostly based on the
 * Spring Cloud Consul Configuration equivalent.
 *
 * @author Joris Kuipers
 * @author Matejn
 * @since 2.0.0
 */
@ConfigurationProperties(AwsParamStoreProperties.CONFIG_PREFIX)
public class AwsParamStoreProperties implements Validator {

	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "aws.paramstore";

	/**
	 * Patterns used for validating prefix and profileSeparator values.
	 */
	private static final Pattern prefixPattern = Pattern.compile("(/[a-zA-Z0-9.\\-_]+)*");

	private static final Pattern profileSeparatorPatten = Pattern.compile("[a-zA-Z0-9.\\-_/]+");

	/**
	 * Prefix indicating first level for every property. Value must start with a forward
	 * slash followed by a valid path segment or be empty. Defaults to "/config".
	 */
	private String prefix = "/config";

	private String defaultContext = "application";

	private String profileSeparator = "_";

	/** Throw exceptions during config lookup if true, otherwise, log warnings. */
	private boolean failFast = true;

	/**
	 * Alternative to spring.application.name to use in looking up values in AWS Parameter
	 * Store.
	 */
	private String name;

	/** Is AWS Parameter Store support enabled. */
	private boolean enabled = true;

	@Override
	public boolean supports(Class clazz) {
		return AwsParamStoreProperties.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "prefix", "field.required",
				new Object[]{prefix},"prefix should not be empty or null.");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "defaultContext",
				"field.required",new Object[]{defaultContext}, "defaultContext should not be empty or null.");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "profileSeparator",
			"field.required",new Object[]{profileSeparator},"profileSeparator should not be empty or null.");

		AwsParamStoreProperties awsParamStoreProperties = (AwsParamStoreProperties) target;

		if (!prefixPattern.matcher(awsParamStoreProperties.getPrefix()).matches()) {
			errors.rejectValue("prefix", "prefix.pattern.wrong",new Object[]{prefix},
					"The prefix must have pattern of:  " + prefixPattern.toString());
		}
		if (!profileSeparatorPatten.matcher(awsParamStoreProperties.getProfileSeparator())
				.matches()) {
			errors.rejectValue("profileSeparator", "separator.pattern.wrong",new Object[]{profileSeparator},
					"The profileSeparator must have pattern of:  "
							+ profileSeparatorPatten.toString());
		}
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getDefaultContext() {
		return defaultContext;
	}

	public void setDefaultContext(String defaultContext) {
		this.defaultContext = defaultContext;
	}

	public String getProfileSeparator() {
		return profileSeparator;
	}

	public void setProfileSeparator(String profileSeparator) {
		this.profileSeparator = profileSeparator;
	}

	public boolean isFailFast() {
		return failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
