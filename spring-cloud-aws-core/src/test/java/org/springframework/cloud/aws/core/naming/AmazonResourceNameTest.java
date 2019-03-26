/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.core.naming;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.cloud.aws.core.naming.AmazonResourceName.Builder;
import static org.springframework.cloud.aws.core.naming.AmazonResourceName.fromString;

/**
 * Test for {@link AmazonResourceName} class. The examples are taken from the aws documentation at
 * https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonResourceNameTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testNameIsNull() {
        this.expectedException.expect(IllegalArgumentException.class);
        this.expectedException.expectMessage("name must not be null");
        fromString(null);
    }

    @Test
    public void testWithoutArnQualifier() {
        this.expectedException.expect(IllegalArgumentException.class);
        this.expectedException.expectMessage("must have an arn qualifier at the beginning");
        fromString("foo:aws:iam::123456789012:David");
    }

    @Test
    public void testWithoutAwsQualifier() {
        this.expectedException.expect(IllegalArgumentException.class);
        this.expectedException.expectMessage("must have a valid partition name");
        fromString("arn:axs:iam::123456789012:David");
    }

    @Test
    public void testWithCustomPartitionName() {
        AmazonResourceName resourceName = fromString("arn:aws-cn:iam::123456789012:David");
        assertEquals("aws-cn", resourceName.getPartition());
    }

    @Test
    public void testDynamoDb() {
        String arn = "arn:aws:dynamodb:us-east-1:123456789012:table/books_table";
        AmazonResourceName resourceName = fromString(arn);
        assertEquals("dynamodb", resourceName.getService());
        assertEquals("us-east-1", resourceName.getRegion());
        assertEquals("123456789012", resourceName.getAccount());
        assertEquals("table", resourceName.getResourceType());
        assertEquals("books_table", resourceName.getResourceName());
        assertEquals(arn, resourceName.toString());
    }

    @Test
    public void testDynamoDbBuilder() {
        Builder builder = new Builder();
        builder.withService("dynamodb");
        builder.withRegion(Region.getRegion(Regions.US_EAST_1));
        builder.withAccount("123456789012");
        builder.withResourceType("table");
        builder.withResourceName("books_table");
        builder.withResourceTypeDelimiter("/");
        assertEquals("arn:aws:dynamodb:us-east-1:123456789012:table/books_table", builder.build().toString());
    }

    @Test
    public void testElasticBeansTalkBuilder() {
        Builder builder = new Builder();
        builder.withService("elasticbeanstalk");
        builder.withRegion(Region.getRegion(Regions.US_EAST_1));
        builder.withResourceType("solutionstack");
        builder.withResourceName("32bit Amazon Linux running Tomcat 7");
        builder.withResourceTypeDelimiter("/");
        assertEquals("arn:aws:elasticbeanstalk:us-east-1::solutionstack/32bit Amazon Linux running Tomcat 7", builder.build().toString());
    }

    @Test
    public void testElasticBeansTalk() {
        String arn = "arn:aws:elasticbeanstalk:us-east-1::solutionstack/32bit Amazon Linux running Tomcat 7";
        AmazonResourceName resourceName = fromString(arn);
        assertEquals("elasticbeanstalk", resourceName.getService());
        assertEquals("us-east-1", resourceName.getRegion());
        assertNull(resourceName.getAccount());
        assertEquals("solutionstack", resourceName.getResourceType());
        assertEquals("32bit Amazon Linux running Tomcat 7", resourceName.getResourceName());
        assertEquals(arn, resourceName.toString());
    }


    @Test
    public void testIamService() {
        String arn = "arn:aws:iam::123456789012:server-certificate/ProdServerCert";
        AmazonResourceName resourceName = fromString(arn);
        assertEquals("iam", resourceName.getService());
        assertNull(resourceName.getRegion());
        assertEquals("123456789012", resourceName.getAccount());
        assertEquals("server-certificate", resourceName.getResourceType());
        assertEquals("ProdServerCert", resourceName.getResourceName());
        assertEquals(arn, resourceName.toString());
    }

    @Test
    public void testRdsService() {
        String arn = "arn:aws:rds:us-west-2:123456789012:db:mysql-db";
        AmazonResourceName resourceName = fromString(arn);
        assertEquals("rds", resourceName.getService());
        assertEquals("us-west-2", resourceName.getRegion());
        assertEquals("123456789012", resourceName.getAccount());
        assertEquals("db", resourceName.getResourceType());
        assertEquals("mysql-db", resourceName.getResourceName());
        assertEquals(arn, resourceName.toString());
    }

    @Test
    public void testRoute53Service() {
        String arn = "arn:aws:route53:::hostedzone/Z148QEXAMPLE8V";
        AmazonResourceName resourceName = fromString(arn);
        assertEquals("route53", resourceName.getService());
        assertNull(resourceName.getRegion());
        assertNull(resourceName.getAccount());
        assertEquals("hostedzone", resourceName.getResourceType());
        assertEquals("Z148QEXAMPLE8V", resourceName.getResourceName());
        assertEquals(arn, resourceName.toString());
    }

    @Test
    public void testS3Service() {
        String arn = "arn:aws:s3:::my_corporate_bucket/Development/*";
        AmazonResourceName resourceName = fromString(arn);
        assertEquals("s3", resourceName.getService());
        assertNull(resourceName.getRegion());
        assertNull(resourceName.getAccount());
        assertEquals("my_corporate_bucket", resourceName.getResourceType());
        assertEquals("Development/*", resourceName.getResourceName());
        assertEquals(arn, resourceName.toString());
    }

    @Test
    public void testSnsService() {
        String arn = "arn:aws:sns:us-east-1:123456789012:my_corporate_topic:02034b43-fefa-4e07-a5eb-3be56f8c54ce";
        AmazonResourceName resourceName = fromString(arn);
        assertEquals("sns", resourceName.getService());
        assertEquals("us-east-1", resourceName.getRegion());
        assertEquals("123456789012", resourceName.getAccount());
        assertEquals("my_corporate_topic", resourceName.getResourceType());
        assertEquals("02034b43-fefa-4e07-a5eb-3be56f8c54ce", resourceName.getResourceName());
        assertEquals(arn, resourceName.toString());
    }

    @Test
    public void testSqsService() {
        String arn = "arn:aws:sqs:us-east-1:123456789012:queue1";
        AmazonResourceName resourceName = fromString(arn);
        assertEquals("sqs", resourceName.getService());
        assertEquals("us-east-1", resourceName.getRegion());
        assertEquals("123456789012", resourceName.getAccount());
        assertEquals("queue1", resourceName.getResourceType());
        assertNull(resourceName.getResourceName());
        assertEquals(arn, resourceName.toString());
    }

    @Test
    public void testGovCloudAwsQualifier() {
        String arn = "arn:aws-us-gov:sns:us-gov-east-1:123456789012:my_corporate_topic:02034b43-fefa-4e07-a5eb-3be56f8c54ce";
        AmazonResourceName resourceName = fromString(arn);
        assertEquals("sns", resourceName.getService());
        assertEquals("us-gov-east-1", resourceName.getRegion());
        assertEquals("123456789012", resourceName.getAccount());
        assertEquals("my_corporate_topic", resourceName.getResourceType());
        assertEquals("02034b43-fefa-4e07-a5eb-3be56f8c54ce", resourceName.getResourceName());
        assertEquals(arn, resourceName.toString());
    }
}
