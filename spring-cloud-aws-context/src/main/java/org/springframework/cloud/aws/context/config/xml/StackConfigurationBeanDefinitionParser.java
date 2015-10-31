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

package org.springframework.cloud.aws.context.config.xml;

import org.springframework.cloud.aws.core.env.stack.config.StackResourceUserTagsFactoryBean;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import static org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils.registerResourceIdResolverBeanIfNeeded;
import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.getCustomClientOrDefaultClientBeanName;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

/**
 * Parser for the {@code <aws-context:stack-configuration />} element.
 *
 * @author Christian Stettler
 * @author Agim Emruli
 */
class StackConfigurationBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser {

	private static final String STACK_RESOURCE_REGISTRY_FACTORY_BEAN_CLASS_NAME = "org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean";
	private static final String STATIC_STACK_NAME_PROVIDER_CLASS_NAME = "org.springframework.cloud.aws.core.env.stack.config.StaticStackNameProvider";
	private static final String AUTO_DETECTING_STACK_NAME_PROVIDER_CLASS_NAME = "org.springframework.cloud.aws.core.env.stack.config.AutoDetectingStackNameProvider";
	private static final String INSTANCE_ID_PROVIDER_CLASS_NAME = "org.springframework.cloud.aws.core.env.ec2.AmazonEc2InstanceIdProvider";
	private static final String CLOUD_FORMATION_CLIENT_CLASS_NAME = "com.amazonaws.services.cloudformation.AmazonCloudFormationClient";
	private static final String AMAZON_EC2_CLASS_NAME = " com.amazonaws.services.ec2.AmazonEC2Client";

	private static final String STACK_NAME_ATTRIBUTE_NAME = "stack-name";

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		registerResourceIdResolverBeanIfNeeded(parserContext.getRegistry());

		String amazonCloudFormationClientBeanName = getCustomClientOrDefaultClientBeanName(element, parserContext, "amazon-cloud-formation", CLOUD_FORMATION_CLIENT_CLASS_NAME);
		String amazonEC2BeanName = getCustomClientOrDefaultClientBeanName(element, parserContext, "amazon-ec2", AMAZON_EC2_CLASS_NAME);
		String stackName = element.getAttribute(STACK_NAME_ATTRIBUTE_NAME);

		builder.addConstructorArgReference(amazonCloudFormationClientBeanName);
		AbstractBeanDefinition stackNameProviderBeanDefinition = StringUtils.isEmpty(stackName) ? buildAutoDetectingStackNameProviderBeanDefinition(amazonEC2BeanName) : buildStaticStackNameProviderBeanDefinition(stackName);
		builder.addConstructorArgValue(stackNameProviderBeanDefinition);

		buildAndRegisterStackUserTagsIfNeeded(element, parserContext, amazonCloudFormationClientBeanName, stackNameProviderBeanDefinition);
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		return element.hasAttribute(STACK_NAME_ATTRIBUTE_NAME) ? element.getAttribute(STACK_NAME_ATTRIBUTE_NAME) : parserContext.getReaderContext().generateBeanName(definition);
	}

	@Override
	protected String getBeanClassName(Element element) {
		return STACK_RESOURCE_REGISTRY_FACTORY_BEAN_CLASS_NAME;
	}

	private static AbstractBeanDefinition buildStaticStackNameProviderBeanDefinition(String stackName) {
		BeanDefinitionBuilder staticStackNameProviderBeanDefinitionBuilder = genericBeanDefinition(STATIC_STACK_NAME_PROVIDER_CLASS_NAME);
		staticStackNameProviderBeanDefinitionBuilder.addConstructorArgValue(stackName);

		return staticStackNameProviderBeanDefinitionBuilder.getBeanDefinition();
	}

	private static AbstractBeanDefinition buildAutoDetectingStackNameProviderBeanDefinition(String amazonEC2BeanName) {
		BeanDefinitionBuilder autoDetectingStackNameProviderBeanDefinitionBuilder = genericBeanDefinition(AUTO_DETECTING_STACK_NAME_PROVIDER_CLASS_NAME);
		autoDetectingStackNameProviderBeanDefinitionBuilder.addConstructorArgReference(amazonEC2BeanName);
		autoDetectingStackNameProviderBeanDefinitionBuilder.addConstructorArgValue(buildInstanceIdProviderBeanDefinition());

		return autoDetectingStackNameProviderBeanDefinitionBuilder.getBeanDefinition();
	}

	private static AbstractBeanDefinition buildInstanceIdProviderBeanDefinition() {
		BeanDefinitionBuilder instanceIdProviderBeanDefinitionBuilder = genericBeanDefinition(INSTANCE_ID_PROVIDER_CLASS_NAME);

		return instanceIdProviderBeanDefinitionBuilder.getBeanDefinition();
	}

	private static void buildAndRegisterStackUserTagsIfNeeded(Element element, ParserContext parserContext, String cloudformationBeanName, BeanDefinition stackNameProvider) {
		if (StringUtils.hasText(element.getAttribute("user-tags-map"))) {
			BeanDefinitionBuilder builder = genericBeanDefinition(StackResourceUserTagsFactoryBean.class);
			builder.addConstructorArgReference(cloudformationBeanName);
			builder.addConstructorArgValue(stackNameProvider);
			parserContext.getRegistry().registerBeanDefinition(element.getAttribute("user-tags-map"), builder.getBeanDefinition());
		}
	}
}
