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

package org.springframework.cloud.aws.autoconfigure.context;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.util.EC2MetadataUtils;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.cloud.aws.context.support.env.AwsCloudEnvironmentCheckUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

/**
 * @author Agim Emruli
 * @author Matt Benson
 */
public final class MetaDataServer implements TestRule {

	private static final int HTTP_SERVER_TEST_PORT = SocketUtils.findAvailableTcpPort();

	private static final Field CACHED_CLOUD_ENVIRONMENT_FLAG;
	static {
		CACHED_CLOUD_ENVIRONMENT_FLAG = ReflectionUtils
				.findField(AwsCloudEnvironmentCheckUtils.class, "isCloudEnvironment");
		Assert.notNull(CACHED_CLOUD_ENVIRONMENT_FLAG,
				"Cannot find " + AwsCloudEnvironmentCheckUtils.class.getSimpleName()
						+ "#isCloudEnvironment field");
		ReflectionUtils.makeAccessible(CACHED_CLOUD_ENVIRONMENT_FLAG);
	}

	@SuppressWarnings("StaticNonFinalField")
	private static HttpServer httpServer;

	@SuppressWarnings("NonThreadSafeLazyInitialization")
	public static HttpServer setupHttpServer() throws Exception {

		if (httpServer == null) {
			InetSocketAddress address = new InetSocketAddress(HTTP_SERVER_TEST_PORT);
			httpServer = HttpServer.create(address, -1);
			httpServer.start();
			overwriteMetadataEndpointUrl("http://" + address.getHostName() + ":" + address.getPort());
			resetMetaDataCache();
		}

		return httpServer;
	}

	public static void shutdownHttpServer() {
		if (httpServer != null) {
			httpServer.stop(10);
			httpServer = null;
		}
		resetMetaDataCache();
		resetMetadataEndpointUrlOverwrite();
	}

	private static void resetMetaDataCache() {
		try {
			Field metadataCacheField = EC2MetadataUtils.class.getDeclaredField("cache");
			metadataCacheField.setAccessible(true);
			metadataCacheField.set(null, new HashMap<String, String>());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to clear metadata cache in '" + EC2MetadataUtils.class.getName() + "'", e);
		}
	}

	private static void overwriteMetadataEndpointUrl(String localMetadataServiceEndpointUrl) {
		System.setProperty(SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY, localMetadataServiceEndpointUrl);
	}

	private static void resetMetadataEndpointUrlOverwrite() {
		System.clearProperty(SDKGlobalConfiguration.EC2_METADATA_SERVICE_OVERRIDE_SYSTEM_PROPERTY);
	}

	@Override
	public Statement apply(Statement base, Description description) {
		MetaData config = description.getAnnotation(MetaData.class);
		if (config == null) {
			return base;
		}
		return new Stmt(base, config);
	}

	public static class HttpResponseWriterHandler implements HttpHandler {

		private final String content;

		public HttpResponseWriterHandler(String content) {
			this.content = content;
		}

		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			httpExchange.sendResponseHeaders(200, this.content.getBytes().length);

			OutputStream responseBody = httpExchange.getResponseBody();
			responseBody.write(this.content.getBytes());
			responseBody.flush();
			responseBody.close();
		}
	}

	private static class Stmt extends Statement {
		private final Statement base;
		private final MetaData config;

		Stmt(Statement base, MetaData config) {
			super();
			this.base = base;
			this.config = config;
		}

		@Override
		public void evaluate() throws Throwable {
			final boolean stopServer = httpServer == null;
			HttpServer server = MetaDataServer.setupHttpServer();
			Deque<HttpContext> contexts = new ArrayDeque<>();
			for (MetaData.Context context : config.value()) {
				contexts.push(server.createContext(context.path(),
						new HttpResponseWriterHandler(
								context.nullValue() ? null : context.value())));
			}
			try {
				base.evaluate();
			}
			finally {
				for (HttpContext context : contexts) {
					server.removeContext(context);
					if ("/latest/meta-data/instance-id".equals(context.getPath())) {
						ReflectionUtils.setField(CACHED_CLOUD_ENVIRONMENT_FLAG, null,
								null);
					}
				}
				if (stopServer) {
					shutdownHttpServer();
				}
				else {
					resetMetaDataCache();
				}
			}
		}
	}

}
