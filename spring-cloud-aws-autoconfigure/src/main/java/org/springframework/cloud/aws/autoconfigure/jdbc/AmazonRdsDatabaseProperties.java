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

package org.springframework.cloud.aws.autoconfigure.jdbc;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.style.ToStringCreator;

/**
 * Properties related to AWS Rds.
 *
 * @author Mete Alpaslan Katircioglu
 * @see org.springframework.cloud.aws.autoconfigure.jdbc.AmazonRdsDatabaseAutoConfiguration
 */
@ConfigurationProperties(prefix = "cloud.aws.rds")
public class AmazonRdsDatabaseProperties {

	/**
	 * List of RdsInstances.
	 */
	private List<RdsInstance> instances = Collections.emptyList();

	public List<RdsInstance> getInstances() {
		return instances;
	}

	public void setInstances(List<RdsInstance> instances) {
		this.instances = instances;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("instanceDefinitions", instances)
				.toString();
	}

	public static class RdsInstance {

		/**
		 * The unique database instance identifier in the AmazonRDS service.
		 */
		private String dbInstanceIdentifier;

		/**
		 * The username used to connect to the datasource.
		 */
		private String username;

		/**
		 * The databaseName used to connect to the datasource.
		 */
		private String databaseName;

		/**
		 * The password used to connect to the datasource.
		 */
		private String password;

		/**
		 * The readReplicaSupport used to connect to the datasource.
		 */
		private boolean readReplicaSupport = false;

		public String getDbInstanceIdentifier() {
			return dbInstanceIdentifier;
		}

		public void setDbInstanceIdentifier(String dbInstanceIdentifier) {
			this.dbInstanceIdentifier = dbInstanceIdentifier;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getDatabaseName() {
			return databaseName;
		}

		public void setDatabaseName(String databaseName) {
			this.databaseName = databaseName;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public boolean isReadReplicaSupport() {
			return readReplicaSupport;
		}

		public void setReadReplicaSupport(boolean readReplicaSupport) {
			this.readReplicaSupport = readReplicaSupport;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			RdsInstance that = (RdsInstance) o;
			return readReplicaSupport == that.readReplicaSupport
					&& Objects.equals(this.dbInstanceIdentifier,
							that.dbInstanceIdentifier)
					&& Objects.equals(this.username, that.username)
					&& Objects.equals(this.databaseName, that.databaseName)
					&& Objects.equals(this.password, that.password);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.dbInstanceIdentifier, this.username,
					this.databaseName, this.password, this.readReplicaSupport);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("dbInstanceIdentifier", dbInstanceIdentifier)
					.append("username", username).append("databaseName", databaseName)
					.append("password", password)
					.append("readReplicaSupport", readReplicaSupport).toString();
		}

	}

}
