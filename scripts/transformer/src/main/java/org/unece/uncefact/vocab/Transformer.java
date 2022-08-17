package org.unece.uncefact.vocab;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.unece.uncefact.vocab.transformers.UNCLToJSONLDVocabulary;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Transformer {
    protected static String ID = "@id";
    protected static String TYPE = "@type";
    protected static String VALUE = "@value";
    protected static String LANGUAGE = "@language";
    protected static String RDFS_NS = "rdfs";
    protected static String RDF_NS = "rdf";
    protected static String UNECE_NS = "unece";
    protected static String CEFACT_NS = "cefact";
    protected static String XSD_NS = "xsd";
    protected static String RDFS_CLASS = RDFS_NS+":Class";
    protected static String RDF_PROPERTY = RDF_NS+":Property";
    protected static String RDF_VALUE = RDF_NS+":value";
    protected static String RDFS_COMMENT = RDFS_NS+":comment";
    protected static String RDFS_LABEL = RDFS_NS+":label";
    protected static String SCHEMA_NS = "schema";
    protected static String SCHEMA_DOMAIN_INCLUDES = SCHEMA_NS+":domainIncludes";
    protected static String SCHEMA_RANGE_INCLUDES = SCHEMA_NS+":rangeIncludes";
    protected String inputFile;
    protected Set<String> inputFiles;
    protected String outputFile;

    private boolean prettyPrint;

    protected JsonObject context;

    protected JsonObjectBuilder jsonObjectBuilder;
    protected JsonArrayBuilder graphJsonArrayBuilder;

    protected JsonObjectBuilder contextObjectBuilder;

    protected Transformer(String inputFile, String outputFile, boolean prettyPrint) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.prettyPrint = prettyPrint;

        jsonObjectBuilder = Json.createObjectBuilder();
        graphJsonArrayBuilder = Json.createArrayBuilder();

        setContext();

    }

    protected void setContext (){
        contextObjectBuilder = Json.createObjectBuilder();
        //common context for all vocabularies
        contextObjectBuilder.add(UNECE_NS, "https://service.unece.org/trade/uncefact/vocabulary/unece#");
        contextObjectBuilder.add(RDF_NS, "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        contextObjectBuilder.add(RDFS_NS, "http://www.w3.org/2000/01/rdf-schema#");
    }

    protected void setMinimalContext (){
        contextObjectBuilder = Json.createObjectBuilder();
        //common context for all vocabularies
        contextObjectBuilder.add(RDFS_NS, "http://www.w3.org/2000/01/rdf-schema#");
    }

    public void transform() throws IOException, InvalidFormatException {
        FileGenerator fileGenerator = new FileGenerator();
        fileGenerator.generateFile(contextObjectBuilder, graphJsonArrayBuilder, this.prettyPrint, outputFile);
    }

    protected abstract void readInputFileToGraphArray(Object object);

    protected String getCellValue(Row row, int cellNumber) {
       return getCellValue(row, cellNumber, false);
    }

    protected String getCellValue(Row row, int cellNumber, boolean convertToInteger) {
        Object result = null;
        if (row.getCell(cellNumber) != null) {
            switch (row.getCell(cellNumber).getCellTypeEnum()) {
                case NUMERIC:
                    double value = row.getCell(cellNumber).getNumericCellValue();
                    if(convertToInteger)
                        result = Integer.valueOf((int) value);
                    else
                        result = value;
                    break;
                case STRING:
                    result = row.getCell(cellNumber).getStringCellValue();
                    break;
            }
        }
        return result != null ? result.toString() : "";
    }

    protected Double getNumericValue(Row row, int cellNumber) {
        if (row.getCell(cellNumber) != null) {
            return row.getCell(cellNumber).getNumericCellValue();
        }
        return null;
    }

    protected String getStringCellValue(Row row, int cellNumber) {
        return getStringCellValue(row, cellNumber, true);
    }

    protected String getStringCellValue(Row row, int cellNumber, boolean cleanup) {
        if (row.getCell(cellNumber) != null) {
            String result = row.getCell(cellNumber).getStringCellValue();
            if (cleanup) {
                return cleanUp(result);
            }
            return StringUtils.defaultIfEmpty(result, "");
        }
        return "";
    }

    protected String cleanUp(String attribute) {
        return attribute.replaceAll(" ", "").replaceAll("_", "").replaceAll("-", "").replaceAll("/", "")/*.replaceAll(".","")*/;
    }

    protected void setInputFiles(Set<String> inputFiles){
        this.inputFiles = inputFiles;
    }
}
