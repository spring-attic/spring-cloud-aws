/*
 * Copyright 2017-2020 the original author or authors.
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

package com.example.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring Cloud AWS Secret Manager Demo.
 *
 * @author Harsha Jayamanna
 */
@RestController
public class SecreteManagerController {

	@Value("${projectId}")
	private String id;

	@Value("${projectName}")
	private String name;

	@RequestMapping(value = "get/my-secrete", method = RequestMethod.GET)
	public Map<String, Object> getSecret() {

		final Map<String, Object> map = new HashMap<>();
		map.put("projectId", id);
		map.put("projectName", name);
		return map;
	}

}
