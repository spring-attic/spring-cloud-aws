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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Cloud AWS Secret Manager Demo.
 *
 * @author Harsha Jayamanna
 */
@SpringBootTest
public class SecreteManagerControllerTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Test
	public void giveUserRecipe_whenCreate_thenReturnJsonArray() throws Exception {

		MockMvc mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

		mvc.perform(get("/get/my-secrete").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

	}

}
