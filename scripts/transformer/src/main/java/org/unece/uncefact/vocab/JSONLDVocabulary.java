package org.unece.uncefact.vocab;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public class JSONLDVocabulary implements FileEntity{
    protected JsonArrayBuilder graphJsonArrayBuilder;

    protected JsonObjectBuilder contextObjectBuilder;

    protected String outputFile;

    protected boolean prettyPrint;

    public JSONLDVocabulary() {
        graphJsonArrayBuilder = Json.createArrayBuilder();
        contextObjectBuilder = Json.createObjectBuilder();
    }

    public JSONLDVocabulary(String outputFile, boolean prettyPrint) {
        this();
        this.outputFile = outputFile;
        this.prettyPrint = prettyPrint;
    }

    public JsonArrayBuilder getGraphJsonArrayBuilder() {
        return graphJsonArrayBuilder;
    }

    public void setGraphJsonArrayBuilder(JsonArrayBuilder graphJsonArrayBuilder) {
        this.graphJsonArrayBuilder = graphJsonArrayBuilder;
    }

    public JsonObjectBuilder getContextObjectBuilder() {
        return contextObjectBuilder;
    }

    public void setContextObjectBuilder(JsonObjectBuilder contextObjectBuilder) {
        this.contextObjectBuilder = contextObjectBuilder;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
}
