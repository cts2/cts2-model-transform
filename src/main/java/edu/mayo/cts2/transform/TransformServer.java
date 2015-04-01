package edu.mayo.cts2.transform;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.webbitserver.*;
import org.webbitserver.netty.NettyWebServer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransformServer {

	private static final int DEFAULT_PORT = 9999;

	private WebServer webServer;

	private Resource toJsonHtml = new ClassPathResource("html/toJson.html");
	private Resource toXmlHtml = new ClassPathResource("html/toXml.html");

	private ExecutorService workerPool = Executors.newCachedThreadPool();

    private Cts2Transformer transformer = Cts2Transformer.instance;

	public TransformServer(int port) {
        super();
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

						String xml = transformer.toXml(json);

						response.header("Content-type", "application/xml")
								.content(xml).end();
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

						String json = transformer.toJson(body);
						
						response.header("Content-type", "application/json")
								.content(json).end();
					}
				}
			});
		}
	}

}