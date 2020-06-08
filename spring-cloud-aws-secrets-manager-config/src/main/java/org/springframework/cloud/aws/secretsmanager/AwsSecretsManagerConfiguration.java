package org.springframework.cloud.aws.secretsmanager;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * Configuration class to register AwsSecretsManagerProperties property.
 * @author Matejn
 * @since 2.3.x
 */
@Configuration
@EnableConfigurationProperties(AwsSecretsManagerProperties.class)
public class AwsSecretsManagerConfiguration {

}
