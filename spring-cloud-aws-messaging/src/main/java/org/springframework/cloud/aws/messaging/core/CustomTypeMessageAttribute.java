/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.messaging.core;

import java.util.Objects;

/**
 * @author Wojciech Mąka
 */
public final class CustomTypeMessageAttribute {

	private final String type;

	private final String customType;

	private final Object value;

	public CustomTypeMessageAttribute(String type, String customType, Object value) {
		this.type = type;
		this.customType = customType;
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public String getCustomType() {
		return customType;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CustomTypeMessageAttribute that = (CustomTypeMessageAttribute) o;
		return type.equals(that.type) && customType.equals(that.customType)
				&& value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, customType, value);
	}

	@Override
	public String toString() {
		return "CustomTypeMessageAttribute{" + "type='" + type + '\'' + ", customType='"
				+ customType + '\'' + ", value=" + value + '}';
	}

}
