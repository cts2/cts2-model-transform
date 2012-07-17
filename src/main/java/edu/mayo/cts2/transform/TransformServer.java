package edu.mayo.cts2.transform;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;
import org.webbitserver.WebServer;
import org.webbitserver.netty.NettyWebServer;

import edu.mayo.cts2.framework.core.json.JsonConverter;
import edu.mayo.cts2.framework.core.xml.Cts2Marshaller;
import edu.mayo.cts2.framework.core.xml.DelegatingMarshaller;

public class TransformServer {

	private static final int DEFAULT_PORT = 9999;

	private Cts2Marshaller cts2Marshaller;
	private JsonConverter jsonConverter;

	private WebServer webServer;

	private Resource toJsonHtml = new ClassPathResource("html/toJson.html");
	private Resource toXmlHtml = new ClassPathResource("html/toXml.html");

	private ExecutorService workerPool = Executors.newCachedThreadPool();

	public TransformServer(int port) {
		super();
		try {
			this.cts2Marshaller = new DelegatingMarshaller();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.jsonConverter = new JsonConverter();

		NettyWebServer nettyWebServer = new NettyWebServer(port)
				.maxContentLength(Integer.MAX_VALUE)
				.add("/tojson", new ToJsonHandler())
				.add("/toxml", new ToXmlHandler());

		nettyWebServer.staleConnectionTimeout(10000);
		this.webServer = nettyWebServer;
	}

	public void start() {
		this.webServer.start();
	}

	public void stop() {
		this.webServer.stop();
	}

	public static void main(String[] args) throws Exception {
		int port;

		if (args != null && args.length == 1) {
			port = Integer.parseInt(args[0]);
		} else {
			port = DEFAULT_PORT;
		}

		TransformServer server = new TransformServer(port);

		server.start();

		System.out.println("Server started on port: " + port);
	}

	private class ToXmlHandler implements HttpHandler {

		@Override
		public void handleHttpRequest(final HttpRequest request,
				final HttpResponse response, HttpControl control) {

			workerPool.execute(new Runnable() {

				@Override
				public void run() {
					if (request.method().equals("GET")) {
						try {
							response.header("Content-type", "text/html")
									.content(
											IOUtils.toString(toXmlHtml
													.getInputStream())).end();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					} else {
						String json = request.body();
						
						System.out.println(json);

						Object object = jsonConverter.fromJson(json);

						StringWriter sw = new StringWriter();

						try {
							cts2Marshaller
									.marshal(object, new StreamResult(sw));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}

						response.header("Content-type", "application/xml")
								.content(sw.toString()).end();
					}
				}

			});

		}
	}

	private class ToJsonHandler implements HttpHandler {

		@Override
		public void handleHttpRequest(final HttpRequest request,
				final HttpResponse response, HttpControl control) {

			workerPool.execute(new Runnable() {

				@Override
				public void run() {
					if (request.method().equals("GET")) {
						try {
							response.header("Content-type", "text/html")
									.content(
											IOUtils.toString(toJsonHtml
													.getInputStream())).end();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					} else {

						String body = request.body();

						Object object;
						try {
							object = cts2Marshaller.unmarshal(new StreamSource(
									new StringReader(body)));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}

						response.header("Content-type", "application/json")
								.content(jsonConverter.toJson(object)).end();
					}
				}
			});
		}
	}

}