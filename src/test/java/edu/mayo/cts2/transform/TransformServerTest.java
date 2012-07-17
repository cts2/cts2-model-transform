package edu.mayo.cts2.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import edu.mayo.cts2.framework.core.xml.Cts2Marshaller;
import edu.mayo.cts2.framework.core.xml.DelegatingMarshaller;
import edu.mayo.cts2.framework.model.codesystem.CodeSystemCatalogEntry;
import edu.mayo.cts2.framework.model.codesystem.CodeSystemCatalogEntryDirectory;

public class TransformServerTest {

	@Test
	public void TestTime() throws Exception {
		final Cts2Marshaller marshaller = new DelegatingMarshaller();

		TransformServer server = new TransformServer(7777);
		server.start();

		final CodeSystemCatalogEntryDirectory cs = (CodeSystemCatalogEntryDirectory) marshaller
				.unmarshal(new StreamSource(new ClassPathResource(
						"codeSystemDirectory.xml").getInputStream()));

		int runs = 250;

		long totalTime = 0;

		for (int i = 0; i < runs; i++) {

			final RestTemplate template = new RestTemplate();

			long start = System.currentTimeMillis();

			StringWriter sw = new StringWriter();

			try {
				marshaller.marshal(cs, new StreamResult(sw));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			String json = template
					.postForObject("http://localhost:7777/tojson",
							sw.toString(), String.class);

			String xml = template.postForObject("http://localhost:7777/toxml",
					json, String.class);

			assertNotNull(xml);

			totalTime += (System.currentTimeMillis() - start);

		}
		
		server.stop();

		System.out.println("Runs: " + runs + ", Average Time: " + totalTime
				/ runs);

		assertTrue("Actual Time: " + totalTime/runs, totalTime/runs < 100);
	}

	@Test
	public void TestRoundTrip() throws Exception {
		Cts2Marshaller marshaller = new DelegatingMarshaller();

		TransformServer server = new TransformServer(5555);
		server.start();

		RestTemplate template = new RestTemplate();

		final CodeSystemCatalogEntryDirectory cs = (CodeSystemCatalogEntryDirectory) marshaller
				.unmarshal(new StreamSource(new ClassPathResource(
						"codeSystemDirectory100.xml").getInputStream()));

		StringWriter sw = new StringWriter();

		marshaller.marshal(cs, new StreamResult(sw));

		String json = template.postForObject("http://localhost:5555/tojson",
				sw.toString(), String.class);

		String xml = template.postForObject("http://localhost:5555/toxml",
				json, String.class);

		CodeSystemCatalogEntryDirectory result = (CodeSystemCatalogEntryDirectory) marshaller
				.unmarshal(new StreamSource(new StringReader(xml)));

		server.stop();
		
		assertNotNull(result);
	}
	
	@Test
	public void TestRoundTripDirectory() throws Exception {
		Cts2Marshaller marshaller = new DelegatingMarshaller();

		TransformServer server = new TransformServer(9999);
		server.start();

		RestTemplate template = new RestTemplate();

		CodeSystemCatalogEntry cs = new CodeSystemCatalogEntry();
		cs.setAbout("http://about");
		cs.setCodeSystemName("cs_name");

		StringWriter sw = new StringWriter();

		marshaller.marshal(cs, new StreamResult(sw));

		String json = template.postForObject("http://localhost:9999/tojson",
				sw.toString(), String.class);

		String xml = template.postForObject("http://localhost:9999/toxml",
				json, String.class);

		CodeSystemCatalogEntry result = (CodeSystemCatalogEntry) marshaller
				.unmarshal(new StreamSource(new StringReader(xml)));

		server.stop();
		
		assertEquals(cs.toString(), result.toString());
	}
}
