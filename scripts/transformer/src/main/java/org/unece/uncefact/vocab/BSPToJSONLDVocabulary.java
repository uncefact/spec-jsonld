package org.unece.uncefact.vocab;

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

import javax.json.*;
import java.io.*;
import java.util.*;

public class BSPToJSONLDVocabulary extends Transformer {

    static List<String> codes = Arrays.asList(new String[]{"1001", "1049", "1073", "1153", "1159", "1225", "1227", "1229", "1373", "1501", "1503", "1505", "1507", "2005", "2009", "2013", "2015", "2017", "2023", "2151", "2155", "2379", "2475", "3035", "3045", "3055", "3077", "3079", "3083", "3131", "3139", "3153", "3155", "3227", "3237", "3279", "3285", "3289", "3295", "3299", "3301", "3397", "3401", "3403", "3405", "3455", "3457", "3475", "3477", "3479", "3493", "4017", "4025", "4027", "4035", "4037", "4043", "4049", "4051", "4053", "4055", "4059", "4065", "4071", "4079", "4153", "4183", "4215", "4219", "4221", "4233", "4237", "4277", "4279", "4295", "4343", "4347", "4383", "4401", "4403", "4405", "4407", "4419", "4431", "4435", "4437", "4439", "4447", "4451", "4453", "4455", "4457", "4461", "4463", "4465", "4471", "4475", "4487", "4493", "4499", "4501", "4503", "4505", "4507", "4509", "4511", "4513", "4517", "4525", "5007", "5013", "5025", "5027", "5039", "5047", "5049", "5125", "5153", "5189", "5213", "5237", "5243", "5245", "5249", "5261", "5267", "5273", "5283", "5305", "5307", "5315", "5375", "5379", "5387", "5393", "5419", "5463", "5495", "5501", "6029", "6063", "6069", "6071", "6077", "6079", "6085", "6087", "6113", "6145", "6155", "6167", "6173", "6245", "6311", "6313", "6321", "6331", "6341", "6343", "6347", "6353", "6415", "7001", "7007", "7009", "7011", "7039", "7041", "7045", "7047", "7059", "7073", "7075", "7077", "7081", "7083", "7085", "7111", "7133", "7139", "7143", "7161", "7171", "7173", "7187", "7233", "7273", "7293", "7295", "7297", "7299", "7365", "7383", "7405", "7429", "7431", "7433", "7449", "7451", "7455", "7459", "7491", "7493", "7495", "7497", "7511", "7515", "8015", "8025", "8035", "8051", "8053", "8077", "8101", "8155", "8169", "8179", "8249", "8273", "8281", "8323", "8335", "8339", "8341", "8393", "8395", "8457", "8459", "9003", "9013", "9015", "9017", "9023", "9025", "9029", "9031", "9035", "9037", "9039", "9043", "9045", "9051", "9141", "9143", "9153", "9161", "9169", "9175", "9213", "9285", "9303", "9353", "9411", "9415", "9417", "9421", "9437", "9441", "9443", "9447", "9453", "9501", "9507", "9509", "9601", "9623", "9625", "9635", "9641", "9643", "9645", "9649"});

    BSPToJSONLDVocabulary(String inputFile, String outputFile) {
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
                metadata.add("unece:cefactUNId", "cefact:".concat(entity.getId()));
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
                metadata.add("unece:cefactUNId", "cefact:".concat(entity.getId()));
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


        try {
            writer = new PrintWriter("index.md", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
            if (codes.contains(item)) {
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
