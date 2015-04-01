package edu.mayo.cts2.transform;

import edu.mayo.cts2.framework.core.json.JsonConverter;
import edu.mayo.cts2.framework.core.xml.Cts2Marshaller;
import edu.mayo.cts2.framework.core.xml.DelegatingMarshaller;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class Cts2Transformer {

    private Cts2Marshaller cts2Marshaller;
    private JsonConverter jsonConverter;

    public final static Cts2Transformer instance = new Cts2Transformer();

    private Cts2Transformer() {
        super();
        try {
            this.cts2Marshaller = new DelegatingMarshaller(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.jsonConverter = new JsonConverter();
    }

    public String toJson(String xml) {
        try {
            return this.jsonConverter.toJson(
                this.cts2Marshaller.unmarshal(new StreamSource(
                        new StringReader(xml))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toXml(String json) {
        Object object = this.jsonConverter.fromJson(json);
        StringWriter sw = new StringWriter();

        try {
            this.cts2Marshaller
                    .marshal(object, new StreamResult(sw));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }
}
