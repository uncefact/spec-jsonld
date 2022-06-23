package org.unece.uncefact.vocab;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class FileGenerator {

    public void generateFile(final JsonObjectBuilder contextObjectBuilder,
                             final JsonArrayBuilder graphJsonArrayBuilder, boolean prettyPrint, String outputFile) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("@context", contextObjectBuilder.build());
        jsonObjectBuilder.add("@graph", graphJsonArrayBuilder.build());

        Map<String, Boolean> config = new HashMap<>();
        if (prettyPrint) {
            config.put(JsonGenerator.PRETTY_PRINTING, true);
        }
        StringWriter stringWriter = new StringWriter();
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        ;
        try (JsonWriter jsonWriter = writerFactory.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObjectBuilder.build());
        }
        try (PrintWriter writer =  new PrintWriter(outputFile, "UTF-8")){
            writer.print(stringWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
