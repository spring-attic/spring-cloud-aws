package org.springframework.cloud.aws.paramstore;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to register AwsParamStoreProperties property.
 * @author Matejn
 **/
@Configuration
@EnableConfigurationProperties(AwsParamStoreProperties.class)
public class AwsParamStoreConfiguration {
}
