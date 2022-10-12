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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Transformer {
    public static String ID = "@id";
    public static String TYPE = "@type";
    public static String VALUE = "@value";
    public static String LANGUAGE = "@language";
    public static String OWL_NS = "owl";
    public static String RDFS_NS = "rdfs";
    public static String RDF_NS = "rdf";
    public static String UNECE_NS = "unece";
    public static String CEFACT_NS = "cefact";
    public static String XSD_NS = "xsd";
    public static String RDFS_CLASS = RDFS_NS+":Class";
    public static String RDF_PROPERTY = RDF_NS+":Property";
    public static String RDF_VALUE = RDF_NS+":value";
    public static String RDFS_COMMENT = RDFS_NS+":comment";
    public static String RDFS_LABEL = RDFS_NS+":label";
    public static String SCHEMA_NS = "schema";
    public static String SCHEMA_DOMAIN_INCLUDES = SCHEMA_NS+":domainIncludes";
    public static String SCHEMA_RANGE_INCLUDES = SCHEMA_NS+":rangeIncludes";
    protected String inputFile;
    protected Set<String> inputFiles;
    protected String outputFile;

    protected boolean prettyPrint;

    protected JsonObject context;

    protected JsonObjectBuilder jsonObjectBuilder;
    protected JsonArrayBuilder graphJsonArrayBuilder;

    protected JsonObjectBuilder contextObjectBuilder;

    protected static String UNLOCODE_NS = "unlcd";
    protected static String UNLOCODE_SUBDIVISIONS_NS = "unlcds";
    protected static String UNLOCODE_COUNTRIES_NS = "unlcdc";
    protected static String UNLOCODE_VOCAB_NS = "unlcdv";
    protected static String UNLOCODE_FUNC_NS = "unlcdf";
    public static String GEO_NS = "geo";
    protected static String REC20_NS = "rec20";
    protected static String REC21_NS = "rec21";
    protected static String REC24_NS = "rec24";
    protected static String REC28_NS = "rec28";

    /*protected static String DOMAIN = "service.unece.org";*/
    protected static String DOMAIN = "dmvc7xzscpizo.cloudfront.net";


    protected static Map<String, String> NS_MAP = new HashMap<>();

    {
        NS_MAP.put(GEO_NS, "http://www.w3.org/2003/01/geo/wgs84_pos#");
        NS_MAP.put(XSD_NS, "http://www.w3.org/2001/XMLSchema#");
        NS_MAP.put(SCHEMA_NS, "http://schema.org/");
        NS_MAP.put(UNLOCODE_NS, String.format("https://%s/unlocode#", DOMAIN));
        NS_MAP.put(UNLOCODE_COUNTRIES_NS, String.format("https://%s/unlocode-countries#", DOMAIN));
        NS_MAP.put(UNLOCODE_SUBDIVISIONS_NS, String.format("https://%s/unlocode-subdivisions#", DOMAIN));
        NS_MAP.put(UNLOCODE_FUNC_NS, String.format("https://%s/unlocode-functions#", DOMAIN));
        NS_MAP.put(UNLOCODE_VOCAB_NS, String.format("https://%s/unlocode-vocab#", DOMAIN));
        NS_MAP.put(UNECE_NS, String.format("https://%s/", DOMAIN));
        NS_MAP.put(REC20_NS, String.format("https://%s/rec20#", DOMAIN));
        NS_MAP.put(REC21_NS, String.format("https://%s/rec21#", DOMAIN));
        NS_MAP.put(REC24_NS, String.format("https://%s/rec24#", DOMAIN));
        NS_MAP.put(REC28_NS, String.format("https://%s/rec28#", DOMAIN));
        NS_MAP.put(RDF_NS, "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        NS_MAP.put(RDFS_NS, "http://www.w3.org/2000/01/rdf-schema#");
        NS_MAP.put(OWL_NS, "http://www.w3.org/2002/07/owl#");
        NS_MAP.put(CEFACT_NS, String.format("https://%s/cefact#", DOMAIN));
    }


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
        for (String ns : Arrays.asList(UNECE_NS, RDF_NS, RDFS_NS)){
            contextObjectBuilder.add(ns, NS_MAP.get(ns));
        }
    }

    protected void setMinimalContext (){
        contextObjectBuilder = Json.createObjectBuilder();
        //common context for all vocabularies
        contextObjectBuilder.add(RDFS_NS, NS_MAP.get(RDFS_NS));
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
