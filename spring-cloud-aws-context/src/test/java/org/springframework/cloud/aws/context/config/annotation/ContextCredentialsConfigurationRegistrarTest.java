/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.aws.context.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import org.apache.http.client.CredentialsProvider;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.BeansException;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.aws.context.annotation.ConditionalOnAwsEnabled;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Anwar Chirakkattil
 */
public class ContextCredentialsConfigurationRegistrarTest {

    private AnnotationConfigApplicationContext context;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @After
    public void tearDown() throws Exception {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void credentialsProvider_defaultCredentialsProviderWithoutFurtherConfig_awsCredentialsProviderConfigured() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithDefaultCredentialsProvider.class);

        //Act
        AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

        //Assert
        assertNotNull(awsCredentialsProvider);
        assertTrue(DefaultAWSCredentialsProviderChain.class.isInstance(awsCredentialsProvider));
    }

    @Test
    public void credentialsProvider_configWithAccessAndSecretKey_staticAwsCredentialsProviderConfigured() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithAccessKeyAndSecretKey.class);

        //Act
        AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

        //Assert
        assertNotNull(awsCredentialsProvider);

        @SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
                (List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
        assertEquals(1, credentialsProviders.size());
        assertTrue(AWSStaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)));

        assertEquals("accessTest", awsCredentialsProvider.getCredentials().getAWSAccessKeyId());
        assertEquals("testSecret", awsCredentialsProvider.getCredentials().getAWSSecretKey());
    }

    @Test
    public void credentialsProvider_configWithAccessAndSecretKeyAsExpressions_staticAwsCredentialsProviderConfiguredWithResolvedExpressions() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext();

        Map<String, Object> secretAndAccessKeyMap = new HashMap<>();
        secretAndAccessKeyMap.put("accessKey", "accessTest");
        secretAndAccessKeyMap.put("secretKey", "testSecret");

        this.context.getEnvironment().getPropertySources().addLast(new MapPropertySource("test", secretAndAccessKeyMap));

        this.context.register(ApplicationConfigurationWithAccessKeyAndSecretKeyAsExpressions.class);
        this.context.refresh();
        //Act
        AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

        //Assert
        assertNotNull(awsCredentialsProvider);

        @SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
                (List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
        assertEquals(1, credentialsProviders.size());
        assertTrue(AWSStaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)));

        assertEquals("accessTest", awsCredentialsProvider.getCredentials().getAWSAccessKeyId());
        assertEquals("testSecret", awsCredentialsProvider.getCredentials().getAWSSecretKey());
    }

    @Test
    public void credentialsProvider_configWithAccessAndSecretKeyAsPlaceHolders_staticAwsCredentialsProviderConfiguredWithResolvedPlaceHolders() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext();

        Map<String, Object> secretAndAccessKeyMap = new HashMap<>();
        secretAndAccessKeyMap.put("accessKey", "accessTest");
        secretAndAccessKeyMap.put("secretKey", "testSecret");

        this.context.getEnvironment().getPropertySources().addLast(new MapPropertySource("test", secretAndAccessKeyMap));
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setPropertySources(this.context.getEnvironment().getPropertySources());

        this.context.getBeanFactory().registerSingleton("configurer", configurer);
        this.context.register(ApplicationConfigurationWithAccessKeyAndSecretKeyAsPlaceHolder.class);
        this.context.refresh();
        //Act
        AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

        //Assert
        assertNotNull(awsCredentialsProvider);

        @SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
                (List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
        assertEquals(1, credentialsProviders.size());
        assertTrue(AWSStaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)));

        assertEquals("accessTest", awsCredentialsProvider.getCredentials().getAWSAccessKeyId());
        assertEquals("testSecret", awsCredentialsProvider.getCredentials().getAWSSecretKey());
    }

    @Test
    public void credentialsProvider_configWithAccessAndSecretKeyAndInstanceProfile_staticAwsCredentialsProviderConfiguredWithInstanceProfile() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithAccessKeyAndSecretKeyAndInstanceProfile.class);

        //Act
        AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

        //Assert
        assertNotNull(awsCredentialsProvider);

        @SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
                (List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
        assertEquals(2, credentialsProviders.size());
        assertTrue(AWSStaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)));
        assertTrue(EC2ContainerCredentialsProviderWrapper.class.isInstance(credentialsProviders.get(1)));
    }

    @Test
    public void credentialsProvider_configWithInstanceProfile_instanceProfileCredentialsProviderConfigured() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithInstanceProfileOnly.class);

        //Act
        AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

        //Assert
        assertNotNull(awsCredentialsProvider);

        @SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
                (List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
        assertEquals(1, credentialsProviders.size());
        assertTrue(EC2ContainerCredentialsProviderWrapper.class.isInstance(credentialsProviders.get(0)));
    }

    @Test
    public void credentialsProvider_configWithProfileNameAndNoProfilePath_profileCredentialsProviderConfigured() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithProfileAndDefaultProfilePath.class);

        //Act
        AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

        //Assert
        assertNotNull(awsCredentialsProvider);

        @SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
                (List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
        assertEquals(1, credentialsProviders.size());
        assertTrue(ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(0)));

        ProfileCredentialsProvider provider = (ProfileCredentialsProvider) credentialsProviders.get(0);
        assertEquals("test", ReflectionTestUtils.getField(provider, "profileName"));
    }


    @Test
    public void credentialsProvider_configWithProfileNameAndCustomProfilePath_profileCredentialsProviderConfigured() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext();

        Map<String, Object> secretAndAccessKeyMap = new HashMap<>();
        secretAndAccessKeyMap.put("profilePath", new ClassPathResource(getClass().getSimpleName() + "-profile", getClass()).getFile().getAbsolutePath());

        this.context.getEnvironment().getPropertySources().addLast(new MapPropertySource("test", secretAndAccessKeyMap));
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setPropertySources(this.context.getEnvironment().getPropertySources());

        this.context.getBeanFactory().registerSingleton("configurer", configurer);
        this.context.register(ApplicationConfigurationWithProfileAndCustomProfilePath.class);
        this.context.refresh();

        //Act
        AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

        //Assert
        assertNotNull(awsCredentialsProvider);

        @SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
                (List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
        assertEquals(1, credentialsProviders.size());
        assertTrue(ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(0)));

        ProfileCredentialsProvider provider = (ProfileCredentialsProvider) credentialsProviders.get(0);
        assertEquals("testAccessKey", provider.getCredentials().getAWSAccessKeyId());
        assertEquals("testSecretKey", provider.getCredentials().getAWSSecretKey());
    }

    @Test
    public void credentialsProvider_configWithAllProviders_allCredentialsProvidersConfigured() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithAllProviders.class);

        //Act
        AWSCredentialsProvider awsCredentialsProvider = this.context.getBean(AWSCredentialsProvider.class);

        //Assert
        assertNotNull(awsCredentialsProvider);

        @SuppressWarnings("unchecked") List<CredentialsProvider> credentialsProviders =
                (List<CredentialsProvider>) ReflectionTestUtils.getField(awsCredentialsProvider, "credentialsProviders");
        assertEquals(3, credentialsProviders.size());
        assertTrue(AWSStaticCredentialsProvider.class.isInstance(credentialsProviders.get(0)));
        assertTrue(EC2ContainerCredentialsProviderWrapper.class.isInstance(credentialsProviders.get(1)));
        assertTrue(ProfileCredentialsProvider.class.isInstance(credentialsProviders.get(2)));
    }

    @Test
    public void credentialsProvider_defaultCredentialsProvider_AwsConfigurationDisabled() throws Exception {
        //Arrange
        this.context = new AnnotationConfigApplicationContext();
        EnvironmentTestUtils.addEnvironment(this.context, "spring.cloud.aws.enabled:false");
        this.context.register(ApplicationConfigurationWithDefaultCredentialsProvider_AwsConfigDisabled.class);

        this.expectedException.expect(BeansException.class);
        this.expectedException.expectMessage("No qualifying bean of type 'com.amazonaws.auth.AWSCredentialsProvider' available");

        //Act
        this.context.refresh();

        //Assert
        this.context.getBean(AWSCredentialsProvider.class);
    }

    @EnableContextCredentials
    public static class ApplicationConfigurationWithDefaultCredentialsProvider {

    }

    @EnableContextCredentials(accessKey = "accessTest", secretKey = "testSecret")
    public static class ApplicationConfigurationWithAccessKeyAndSecretKey {

    }

    @EnableContextCredentials(accessKey = "#{environment.accessKey}", secretKey = "#{environment.secretKey}")
    public static class ApplicationConfigurationWithAccessKeyAndSecretKeyAsExpressions {

    }

    @EnableContextCredentials(accessKey = "${accessKey}", secretKey = "${secretKey}")
    public static class ApplicationConfigurationWithAccessKeyAndSecretKeyAsPlaceHolder {

    }

    @EnableContextCredentials(accessKey = "accessTest", secretKey = "testSecret", instanceProfile = true)
    public static class ApplicationConfigurationWithAccessKeyAndSecretKeyAndInstanceProfile {

    }

    @EnableContextCredentials(instanceProfile = true)
    public static class ApplicationConfigurationWithInstanceProfileOnly {

    }

    @EnableContextCredentials(profileName = "test")
    public static class ApplicationConfigurationWithProfileAndDefaultProfilePath {

    }

    @EnableContextCredentials(profileName = "customProfile", profilePath = "${profilePath}")
    public static class ApplicationConfigurationWithProfileAndCustomProfilePath {

    }

    @EnableContextCredentials(accessKey = "accessTest", secretKey = "testSecret", instanceProfile = true,
            profileName = "customProfile")
    public static class ApplicationConfigurationWithAllProviders {

    }

    @EnableContextCredentials
    @ConditionalOnAwsEnabled
    public static class ApplicationConfigurationWithDefaultCredentialsProvider_AwsConfigDisabled {

    }
}
