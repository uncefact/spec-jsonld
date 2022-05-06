package org.unece.uncefact.vocab.transformers;

import com.google.common.base.CaseFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.unece.uncefact.UNType;
import org.unece.uncefact.vocab.Entity;
import org.unece.uncefact.vocab.Transformer;

import javax.json.*;
import java.io.*;
import java.util.*;

public class BSPToJSONLDVocabulary extends Transformer {

    public BSPToJSONLDVocabulary(String inputFile, String outputFile) {
        super(inputFile, outputFile);
    }

    public void transform() throws IOException, InvalidFormatException {

        Map<String, String> accsMap = new TreeMap<String, String>();
        InputStream in = getClass().getResourceAsStream("/accs.csv");
        Reader reader = new BufferedReader(new InputStreamReader(in));
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
        for (CSVRecord csvRecord : csvParser) {
            String accs = csvRecord.get(0).replaceAll(" ", "");
            String desc = csvRecord.get(1);
            accsMap.put(accs, desc);
        }

        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        JsonObjectBuilder contextObjectBuilder = Json.createObjectBuilder();
        JsonArrayBuilder graphJsonArrayBuilder = Json.createArrayBuilder();

        Workbook workbook = WorkbookFactory.create(new File(inputFile));
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        for (int i = 0; i < 5; i++) {
            rowIterator.next();
        }
        Map<String, Entity> vocabulary = new HashMap<String, Entity>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Entity entity = new Entity();
            entity.setId(getStringCellValue(row, 1));
            entity.setType(getStringCellValue(row, 2));
            entity.setName(getStringCellValue(row, 3, false));
            entity.setDescription(getStringCellValue(row, 4, false));
            entity.setPublicationComment(getStringCellValue(row, 6, false));
            entity.setObjectClassTermQualifier(getStringCellValue(row, 7, false));
            entity.setObjectClassTerm(getStringCellValue(row, 8));
            entity.setPropertyTermQualifier(getStringCellValue(row, 9));
            entity.setPropertyTerm(getStringCellValue(row, 10));
            entity.setDataTypeQualifier(getStringCellValue(row, 11));
            entity.setRepresentationTerm(getStringCellValue(row, 12));
            entity.setQualifiedDataTypeId(getStringCellValue(row, 13));
            entity.setAssociatedObjectClassTermQualifier(getStringCellValue(row, 14));
            entity.setAssociatedObjectClassTerm(getStringCellValue(row, 15));
            entity.setBusinessTerm(getStringCellValue(row, 16));
            entity.setContext(getStringCellValue(row, 21, false));
            if (entity.getType() != null) {
                vocabulary.put(entity.getName(), entity);
            }
            entity.setTDED(getStringCellValue(row, 70));
        }
        Map<String, Set<Entity>> classesMap = new TreeMap<String, Set<Entity>>();
        Map<String, Set<Entity>> propertiesMap = new TreeMap<String, Set<Entity>>();
        for (Entity entity : vocabulary.values()) {
            if (entity.getType() == null) {
                entity.getName();
            } else if (entity.getType().equalsIgnoreCase("ABIE")) {
                Set<Entity> entities = new HashSet<Entity>();
                if (classesMap.containsKey(entity.getObjectClassTerm())) {
                    entities = classesMap.get(entity.getObjectClassTerm());
                }
                entities.add(entity);
                classesMap.put(entity.getObjectClassTerm(), entities);
            } else if (entity.getType().equalsIgnoreCase("BBIE") || entity.getType().equalsIgnoreCase("ASBIE")) {
                Set<Entity> entities = new HashSet<Entity>();
                String key = entity.getPropertyKey();
                key = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key);
                if (propertiesMap.containsKey(key)) {
                    entities = propertiesMap.get(key);
                }
                entities.add(entity);
                propertiesMap.put(key, entities);
            }
        }

        for (String key : classesMap.keySet()) {
            JsonArrayBuilder metadataJsonArrayBuilder = Json.createArrayBuilder();
            String id = key;
            Set<Entity> entities = classesMap.get(key);
            HashSet<String> comment = new HashSet<String>();
            for (Entity entity : entities) {
                JsonObjectBuilder metadata = Json.createObjectBuilder();
                metadata.add("@id", "cefact:".concat(entity.getName()));
                metadata.add("@type", "unece:AggregateBIE");
                metadata.add("unece:cefactUNId", entity.getId());
                metadata.add("rdfs:comment", entity.getDescription());
                metadata.add("unece:cefactBusinessProcess", entity.getContext());
                comment.add(entity.getDescription());
                metadataJsonArrayBuilder.add(metadata);
            }

            JsonObjectBuilder rdfClass = Json.createObjectBuilder();
            rdfClass.add("@id", "unece:".concat(id));
            rdfClass.add("@type", "rdfs:Class");
            if (accsMap.containsKey(id)) {
                rdfClass.add("rdfs:comment", accsMap.get(id));

            } else if (comment.size() == 1) {
                rdfClass.add("rdfs:comment", comment.iterator().next());
            } else {
                JsonArrayBuilder commentJsonArrayBuilder = Json.createArrayBuilder();
                for (String commentValue : comment) {
                    commentJsonArrayBuilder.add(commentValue);
                }
                rdfClass.add("rdfs:comment", commentJsonArrayBuilder.build());

            }
            rdfClass.add("rdfs:label", id);
            rdfClass.add("unece:cefactElementMetadata", metadataJsonArrayBuilder.build());
            graphJsonArrayBuilder.add(rdfClass);
        }

        for (String key : propertiesMap.keySet()) {
            String id = key;
            JsonObjectBuilder rdfProperty = Json.createObjectBuilder();
            rdfProperty.add("@id", "unece:".concat(id));
            rdfProperty.add("@type", "rdf:Property");

            JsonArrayBuilder metadataJsonArrayBuilder = Json.createArrayBuilder();
            Set<Entity> entities = propertiesMap.get(key);
            String rangeBBIE = null;
            String rangeASBIE = null;
            TreeSet<String> domain = new TreeSet<String>();
            TreeSet<String> comment = new TreeSet<String>();
            TreeSet<String> tded = new TreeSet<String>();
            for (Entity entity : entities) {
                JsonObjectBuilder metadata = Json.createObjectBuilder();
                metadata.add("@id", "cefact:".concat(entity.getName()));
                if (entity.getType().equalsIgnoreCase("BBIE")) {
                    metadata.add("@type", "unece:BasicBIE");
                    rangeBBIE = entity.getRepresentationTerm();
                    if (StringUtils.isNotBlank(entity.getTDED())) {
                        metadata.add("unece:TDED", entity.getTDED());
                    }
                } else if (entity.getType().equalsIgnoreCase("ASBIE")) {
                    metadata.add("@type", "unece:AssociationBIE");
                    rangeASBIE = entity.getAssociatedObjectClassTerm();
                }
                metadata.add("unece:cefactUNId", entity.getId());
                metadata.add("unece:cefactBieDomainClass", "cefact:".concat(entity.getClassKey()));
                metadata.add("unece:cefactBusinessProcess", entity.getContext());
                String description = entity.getDescription();
                String publicationComment = entity.getPublicationComment();
                if (StringUtils.isNotBlank(publicationComment)) {
                    description = description.concat(" ").concat(publicationComment);
                }
                metadata.add("rdfs:comment", description);
                if (publicationComment.startsWith("Deprecated")) {
                    metadata.add("unece:status", "deprecated");
                }
                domain.add(entity.getObjectClassTerm());
                comment.add(entity.getDescription());
                metadataJsonArrayBuilder.add(metadata);
                if (StringUtils.isNotEmpty(entity.getTDED()) && entity.getTDED().length() > 1) {
                    tded.add(entity.getTDED());
                }
            }
            if (rangeBBIE != null) {
                if (StringUtils.isBlank(rangeBBIE)) {
                    System.err.println(String.format("rangeBBIE is blank for %s", key));
                }
                if (StringUtils.isNotBlank(rangeASBIE)) {
                    System.err.println(String.format("Property is both BBIE and ASBIE for %s", key));
                }
                Object rangeIncludes = getData(rangeBBIE, tded);
                if (rangeIncludes instanceof JsonObjectBuilder) {
                    rdfProperty.add("schema:rangeIncludes", (JsonObjectBuilder) rangeIncludes);
                } else {
                    rdfProperty.add("schema:rangeIncludes", (JsonArrayBuilder) rangeIncludes);
                }
            } else {
                rdfProperty.add("schema:rangeIncludes", Json.createObjectBuilder().add("@id", "unece:".concat(rangeASBIE)));
            }
            if (domain.size() == 1) {
                rdfProperty.add("schema:domainIncludes", Json.createObjectBuilder().add("@id", "unece:".concat(domain.iterator().next())));
            } else {
                JsonArrayBuilder domainJsonArrayBuilder = Json.createArrayBuilder();
                for (String domainName : domain) {
                    domainJsonArrayBuilder.add(Json.createObjectBuilder().add("@id", "unece:".concat(domainName)));
                }
                rdfProperty.add("schema:domainIncludes", domainJsonArrayBuilder.build());

            }
            if (comment.size() == 1) {
                rdfProperty.add("rdfs:comment", comment.iterator().next());
            } else {
                JsonArrayBuilder commentJsonArrayBuilder = Json.createArrayBuilder();
                for (String commentValue : comment) {
                    commentJsonArrayBuilder.add(commentValue);
                }
                rdfProperty.add("rdfs:comment", commentJsonArrayBuilder.build());

            }
            rdfProperty.add("rdfs:label", id);
            rdfProperty.add("unece:cefactElementMetadata", metadataJsonArrayBuilder.build());
            graphJsonArrayBuilder.add(rdfProperty);
        }


        contextObjectBuilder.add("schema", "http://schema.org/");
        contextObjectBuilder.add("unece", "https://service.unece.org/trade/uncefact/trade/uncefact/vocabulary/unece#");
        contextObjectBuilder.add("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        contextObjectBuilder.add("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        contextObjectBuilder.add("cefact", "https://edi3.org/cefact#");
        contextObjectBuilder.add("xsd", "http://www.w3.org/2001/XMLSchema#");
        contextObjectBuilder.add("dc", "http://purl.org/dc/elements/1.1/");

        jsonObjectBuilder.add("@context", contextObjectBuilder.build());
        jsonObjectBuilder.add("@graph", graphJsonArrayBuilder.build());
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(outputFile, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JsonObject jsonObject = jsonObjectBuilder.build();
        writer.print(jsonObject);
        writer.close();


        Map<String, List<JsonObject>> bbieMapForMd = new HashMap<String, List<JsonObject>>();
        Map<String, List<JsonObject>> asbieMapForMd = new HashMap<String, List<JsonObject>>();
        Map<String, Set<String>> rangeMapForMd = new HashMap<String, Set<String>>();
        JsonArray graph = jsonObject.getJsonArray("@graph");


        for (int i = 0; i < graph.size(); i++) {
            JsonObject graphItem = graph.getJsonObject(i);

            String type = graphItem.getString("@type");
            if (type.equalsIgnoreCase("rdf:Property")) {
                TreeSet<String> rangeIncludes = new TreeSet<>();
                JsonValue jsonValue = graphItem.get("schema:rangeIncludes");
                if (jsonValue instanceof JsonObject) {
                    rangeIncludes.add(((JsonObject) jsonValue).getString("@id"));
                } else {
                    JsonArray jsonArray = (JsonArray) jsonValue;
                    for (int j = 0; j < jsonArray.size(); j++) {
                        rangeIncludes.add(jsonArray.getJsonObject(j).getString("@id"));
                    }
                }
                for (String rdfsRange : rangeIncludes) {
                    if (rdfsRange.startsWith("unece:")) {
                        String range = StringUtils.substringAfter(rdfsRange, "unece:");
                        String id = StringUtils.substringAfter(graphItem.getString("@id"), "unece:");
                        Set<String> ranges = rangeMapForMd.get(range);
                        if (ranges == null) {
                            ranges = new TreeSet<String>();
                        }
                        ranges.add(id);
                        rangeMapForMd.put(range, ranges);
                        JsonValue domainObject = graphItem.get("schema:domainIncludes");
                        if (domainObject instanceof JsonArray) {
                            JsonArray domains = (JsonArray) domainObject;
                            for (int j = 0; j < domains.size(); j++) {
                                String domain = domains.getJsonObject(j).getString("@id");
                                List<JsonObject> items = asbieMapForMd.get(domain);
                                if (items == null) {
                                    items = new ArrayList<JsonObject>();
                                }
                                items.add(graphItem);
                                asbieMapForMd.put(domain, items);
                            }

                        } else if (domainObject instanceof JsonObject) {
                            String domain = ((JsonObject) domainObject).getString("@id");
                            List<JsonObject> items = asbieMapForMd.get(domain);
                            if (items == null) {
                                items = new ArrayList<JsonObject>();
                            }
                            items.add(graphItem);
                            asbieMapForMd.put(domain, items);
                        }
                    } else {
                        JsonValue domainObject = graphItem.get("schema:domainIncludes");
                        if (domainObject instanceof JsonArray) {
                            JsonArray domains = (JsonArray) domainObject;
                            for (int j = 0; j < domains.size(); j++) {
                                String domain = domains.getJsonObject(j).getString("@id");
                                List<JsonObject> items = bbieMapForMd.get(domain);
                                if (items == null) {
                                    items = new ArrayList<JsonObject>();
                                } else {
                                }
                                items.add(graphItem);
                                bbieMapForMd.put(domain, items);
                            }

                        } else if (domainObject instanceof JsonString) {
                            String domain = ((JsonObject) domainObject).getString("@id");
                            List<JsonObject> items = bbieMapForMd.get(domain);
                            if (items == null) {
                                items = new ArrayList<JsonObject>();
                            }
                            items.add(graphItem);
                            bbieMapForMd.put(domain, items);
                        }
                    }
                }
            }
        }
        writer.close();
    }

    static String getStringCellValue(Row row, int cellNumber) {
        return getStringCellValue(row, cellNumber, true);
    }

    static String getStringCellValue(Row row, int cellNumber, boolean cleanup) {
        if (row.getCell(cellNumber) != null) {
            String result = row.getCell(cellNumber).getStringCellValue();
            if (cleanup) {
                return cleanUp(result);
            }
            return StringUtils.defaultIfEmpty(result, "");
        }
        return "";
    }

    static String cleanUp(String attribute) {
        return attribute.replaceAll(" ", "").replaceAll("_", "").replaceAll("-", "").replaceAll("/", "")/*.replaceAll(".","")*/;
    }

    static Object getData(String dataType, Set<String> TDED) {
        JsonObjectBuilder result = Json.createObjectBuilder();
        TreeSet<String> filteredTDED = new TreeSet<>();
        for (String item : TDED) {
            if (item.length() > 4) {
                item = item.substring(1, 5);
            }
            if (Entity.codes.contains(item)) {
                filteredTDED.add(item);
            } /*else {
                filteredTDED.add(item.concat("-missing"));
            }*/
        }

        if (filteredTDED.size() != 0) {
            if (filteredTDED.size() == 1) {
                String item = String.format("unece:UNECECL%sCode", filteredTDED.iterator().next());
                return result.add("@id", item);
            } else {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (String item : filteredTDED) {
                    arrayBuilder.add(Json.createObjectBuilder().add("@id", String.format("unece:UNECECL%sCode", item)));
                }
                return arrayBuilder;
            }
        } else {
            try {
                UNType unType = UNType.valueOf(dataType.toUpperCase());
                switch (unType) {
                    case INDICATOR:
                        result = result.add("@id", "xsd:boolean");
                        break;
                    case IDENTIFIER:
                    case ID:
                    case CODE:
                        result = result.add("@id", "xsd:token");
                        break;
                    case TEXT:
                    case VALUE:
                    case TYPE:
                        result = result.add("@id", "xsd:string");
                        break;
                    case DATETIME:
                        result = result.add("@id", "xsd:dateTime");
                        break;
                    case AMOUNT:
                    case PERCENT:
                    case RATE:
                    case QUANTITY:
                    case NUMERIC:
                    case MEASURE:
                        result = result.add("@id", "xsd:decimal");
                        break;
                    case DATE:
                        result = result.add("@id", "xsd:date");
                    case BINARYOBJECT:
                    case GRAPHIC:
                    case PICTURE:
                    case VIDEO:
                    case SOUND:
                        result = result.add("@id", "xsd:base64Binary");
                        break;
                    case TIME:
                        result = result.add("@id", "xsd:time");
                        break;
                    default:
                        result = result.add("@id", "xsd:string");
                        break;
                }

            } catch (IllegalArgumentException e) {
                System.out.println(String.format("Check data type %s", dataType));
                if (dataType.equalsIgnoreCase("Id"))
                    result = result.add("@id", "xsd:token");
                else
                    result = result.add("@id", "xsd:string");
            }
        }
        return result;
    }

}
