package org.unece.uncefact.vocab;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileGenerator {

    public void generateFile(final JsonObjectBuilder contextObjectBuilder,
                             final JsonArrayBuilder graphJsonArrayBuilder, boolean prettyPrint, String outputFile) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("@context", contextObjectBuilder.build());
        if (graphJsonArrayBuilder!=null) {
            JsonArray graph = graphJsonArrayBuilder.build();
            verify(graph);
            jsonObjectBuilder.add("@graph", graph);
        }

        Map<String, Boolean> config = new HashMap<>();
        if (prettyPrint) {
            config.put(JsonGenerator.PRETTY_PRINTING, true);
        }
        StringWriter stringWriter = new StringWriter();
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);

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

    public void verify (JsonArray array){
        Set<String> ids = new HashSet<>();
        Set<String> idsLC = new HashSet<>();
        for (JsonObject jsonObject:array.getValuesAs(JsonObject.class)) {
            String id = jsonObject.getString(Transformer.ID);
            if (ids.contains(id)) {
                System.err.println(String.format("%s already exists in the vocabulary", id));
            } else {
                ids.add(id);
            } if (idsLC.contains(id.toLowerCase())) {
                System.out.println(String.format("%s exists in the vocabulary as another resource (case sensitive)", id));
            } else {
                idsLC.add(id.toLowerCase());
            }
        }

        Set<String> missingRanges = new HashSet<>();
        for (JsonObject jsonObject:array.getValuesAs(JsonObject.class)) {
            if (jsonObject.containsKey(Transformer.SCHEMA_RANGE_INCLUDES)) {
                String rangeIncludes = jsonObject.getJsonObject(Transformer.SCHEMA_RANGE_INCLUDES).getString(Transformer.ID);
                if (!ids.contains(rangeIncludes)) {
                    if (!missingRanges.contains(rangeIncludes)) {
                        System.err.println(String.format("%s missing from the vocabulary", rangeIncludes));
                        missingRanges.add(rangeIncludes);
                    }
                }
            }
        }
    }
}
