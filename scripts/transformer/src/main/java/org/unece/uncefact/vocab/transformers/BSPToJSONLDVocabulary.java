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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class BSPToJSONLDVocabulary extends Transformer {

    protected static String BBIE = "BBIE";
    protected static String ABIE = "ABIE";
    protected static String ASBIE = "ASBIE";
    protected static String UNECE_ABIE = UNECE_NS+":AggregateBIE";
    protected static String UNECE_BBIE = UNECE_NS+":BasicBIE";
    protected static String UNECE_ASBIE = UNECE_NS+":AssociationBIE";
    protected static String UNECE_TDED = UNECE_NS+":tded";
    protected static String UNECE_STATUS = UNECE_NS+":status";
    protected static String UNECE_CEFACT_UN_ID = UNECE_NS+":cefactUNId";
    protected static String UNECE_CEFACT_BUSINESS_PROCESS = UNECE_NS+":cefactBusinessProcess";
    protected static String UNECE_CEFACT_ELEMENT_METADATA = UNECE_NS+":cefactElementMetadata";
    protected static String UNECE_CEFACT_BIE_DOMAIN_CLASS = UNECE_NS+":cefactBieDomainClass";

    Map<String, JsonObject> propertiesGraph = new TreeMap<>();
    Map<String, JsonObject> classesGraph = new TreeMap<>();

    public BSPToJSONLDVocabulary(String inputFile, String outputFile, boolean prettyPrint) {
        super(inputFile, outputFile,prettyPrint);
    }

    protected void setContext (){
        super.setContext();
        for (String ns : Arrays.asList(CEFACT_NS)){
            contextObjectBuilder.add(ns, NS_MAP.get(ns));
        }
    }

    public void transform() throws IOException, InvalidFormatException {
        try {
            Files.createDirectory(Paths.get(UNECE_NS));
        } catch (FileAlreadyExistsException e) {
            System.err.println(String.format("Output directory %s already exists, please remove it and repeat.", UNLOCODE_NS));
            throw e;
        }
        Workbook workbook = WorkbookFactory.create(new File(inputFile));
        readInputFileToGraphArray(workbook);
    }

    public void readInputFileToGraphArray(final Object object) {
        Workbook workbook = (Workbook) object;

        Map<String, String> accsMap = new TreeMap<String, String>();
        InputStream in = getClass().getResourceAsStream("/accs.csv");
        Reader reader = new BufferedReader(new InputStreamReader(in));
        CSVParser csvParser = null;
        try {
            csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (CSVRecord csvRecord : csvParser) {
            String accs = csvRecord.get(0).replaceAll(" ", "");
            String desc = csvRecord.get(1);
            accsMap.put(accs, desc);
        }

        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        // skip the table header - the first five rows
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
            entity.setObjectClassTermQualifier(getStringCellValue(row, 7, true));
            entity.setObjectClassTerm(getStringCellValue(row, 8));
            entity.setPropertyTermQualifier(getStringCellValue(row, 9));
            entity.setPropertyTerm(getStringCellValue(row, 10));
            entity.setDataTypeQualifier(getStringCellValue(row, 11));
            entity.setRepresentationTerm(getStringCellValue(row, 12));
            entity.setQualifiedDataTypeId(getStringCellValue(row, 13));
            entity.setAssociatedObjectClassTermQualifier(getStringCellValue(row, 14));
            entity.setAssociatedObjectClass(getStringCellValue(row, 15));
            entity.setBusinessTerm(getStringCellValue(row, 16));
            entity.setContext(getStringCellValue(row, 21, false));
            if (entity.getType() != null) {
                vocabulary.put(entity.getName(), entity);
            }
            entity.setTDED(getStringCellValue(row, 70));
        }
        Map<String, Set<Entity>> classesMap = new TreeMap<>();
        Map<String, Set<Entity>> propertiesMap = new TreeMap<>();
        Set<String> classKeys = new TreeSet<>();
        Map<String, Integer> repeatedClassKeys = new TreeMap<>();
        for (Entity entity : vocabulary.values()) {
            if (entity.getType().equalsIgnoreCase(BBIE) || entity.getType().equalsIgnoreCase(ASBIE)) {
                Set<Entity> entities = new HashSet<>();
                String key = entity.getPropertyKey();
                key = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key);
                if (propertiesMap.containsKey(key)) {
                    entities = propertiesMap.get(key);
                }
                entities.add(entity);
                propertiesMap.put(key, entities);
            }

            if(entity.getType().equalsIgnoreCase(ABIE)){
                String classKey = entity.getObjectClassTerm();
                Integer count = 2;
                if(!entity.getObjectClassTermQualifier().startsWith("Referenced")) {
                    if (repeatedClassKeys.containsKey(classKey)) {
                        count = repeatedClassKeys.get(classKey) + 1;
                        repeatedClassKeys.put(classKey, count);
                    } else if (classKeys.contains(classKey)) {
                        repeatedClassKeys.put(classKey, count);
                    } else {
                        classKeys.add(classKey);
                    }
                }
            }
        }
        for (Entity entity : vocabulary.values()) {
            if (entity.getType().equalsIgnoreCase(ABIE)) {
                Set<Entity> entities = new HashSet<>();
                String key = entity.getObjectClassTerm();
                if(repeatedClassKeys.containsKey(key)) {
                    if(repeatedClassKeys.get(entity.getObjectClassTerm()) == 2) {
                        key = entity.getObjectClassTerm();
                    } else {
                        key = entity.getObjectClassTermQualifier().concat(entity.getObjectClassTerm());
                        if (key.startsWith("Referenced")) {

                            key = StringUtils.substringAfter(key, "Referenced");
                        }
                        if (key.startsWith("_")) {
                            key = StringUtils.substringAfter(key, "_");
                        }
                    }
                }
                if (classesMap.containsKey(key)) {
                    entities = classesMap.get(key);
                }
                entities.add(entity);
                classesMap.put(key, entities);
            }
        }

        for (String key : classesMap.keySet()) {

            if (propertiesMap.containsKey(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key))){
                System.out.println(String.format("Name \"%s\"is used for both a property and a class", key));
            }
            JsonArrayBuilder metadataJsonArrayBuilder = Json.createArrayBuilder();
            String id = key;
            Set<Entity> entities = classesMap.get(key);
            HashSet<String> comment = new HashSet<>();
            for (Entity entity : entities) {
                JsonObjectBuilder metadata = Json.createObjectBuilder();
                metadata.add(ID, StringUtils.join(CEFACT_NS,":",entity.getName()));
                metadata.add(TYPE, UNECE_ABIE);
                metadata.add(UNECE_CEFACT_UN_ID, entity.getId());
                metadata.add(RDFS_COMMENT, entity.getDescription());
                metadata.add(UNECE_CEFACT_BUSINESS_PROCESS, entity.getContext());
                comment.add(entity.getDescription());
                metadataJsonArrayBuilder.add(metadata);
            }

            JsonObjectBuilder rdfClass = Json.createObjectBuilder();
            rdfClass.add(ID, StringUtils.join(UNECE_NS,":",id));
            rdfClass.add(TYPE, RDFS_CLASS);
            if (accsMap.containsKey(id)) {
                rdfClass.add(RDFS_COMMENT, accsMap.get(id));

            } else if (comment.size() == 1) {
                rdfClass.add(RDFS_COMMENT, comment.iterator().next());
            } else {
                JsonArrayBuilder commentJsonArrayBuilder = Json.createArrayBuilder();
                for (String commentValue : comment) {
                    commentJsonArrayBuilder.add(commentValue);
                }
                rdfClass.add(RDFS_COMMENT, commentJsonArrayBuilder.build());

            }
            rdfClass.add(RDFS_LABEL, id);
            rdfClass.add(UNECE_CEFACT_ELEMENT_METADATA, metadataJsonArrayBuilder.build());
            classesGraph.put(id, rdfClass.build());
        }

        for (String key : propertiesMap.keySet()) {
            String id = key;
            JsonObjectBuilder rdfProperty = Json.createObjectBuilder();
            rdfProperty.add(ID, StringUtils.join(UNECE_NS,":",id));

            JsonArrayBuilder metadataJsonArrayBuilder = Json.createArrayBuilder();
            Set<Entity> entities = propertiesMap.get(key);
            String rangeBBIE = null;
            String rangeASBIE = null;
            TreeSet<String> domain = new TreeSet<>();
            TreeSet<String> comment = new TreeSet<>();
            TreeSet<String> tded = new TreeSet<>();
            JsonArrayBuilder typeArray = Json.createArrayBuilder();
            typeArray.add(RDF_PROPERTY);
            for (Entity entity : entities) {
                JsonObjectBuilder metadata = Json.createObjectBuilder();
                metadata.add(ID, StringUtils.join(CEFACT_NS,":",entity.getName()));
                if (entity.getType().equalsIgnoreCase(BBIE)) {
                    metadata.add(TYPE, UNECE_BBIE);
                    rangeBBIE = entity.getRepresentationTerm();
                    if (StringUtils.isNotBlank(entity.getTDED()) && !".".equals(entity.getTDED())) {
                        metadata.add(UNECE_TDED, entity.getTDED());
                    }
                } else if (entity.getType().equalsIgnoreCase(ASBIE)) {
                    metadata.add(TYPE, UNECE_ASBIE);
                    rangeASBIE = entity.getAssociatedObjectClass();
                    if (repeatedClassKeys.containsKey(entity.getAssociatedObjectClass())) {
                        if(repeatedClassKeys.get(entity.getAssociatedObjectClass()) == 2) {
                            rangeASBIE = entity.getAssociatedObjectClass();
                        } else {
                            rangeASBIE = entity.getAssociatedClassTermWithQualifier();
                            if (rangeASBIE.startsWith("Referenced")) {
                                rangeASBIE = StringUtils.substringAfter(rangeASBIE, "Referenced");
                            }
                            if (rangeASBIE.startsWith("_")) {
                                rangeASBIE = StringUtils.substringAfter(rangeASBIE, "_");
                            }
                        }
                    }
                }
                metadata.add(UNECE_CEFACT_UN_ID, entity.getId());
                metadata.add(UNECE_CEFACT_BIE_DOMAIN_CLASS, StringUtils.join(CEFACT_NS,":",entity.getCefactBieDomainClass()));
                metadata.add(UNECE_CEFACT_BUSINESS_PROCESS, entity.getContext());
                String description = entity.getDescription();
                String publicationComment = entity.getPublicationComment();
                if (StringUtils.isNotBlank(publicationComment)) {
                    description = description.concat(" ").concat(publicationComment);
                }
                metadata.add(RDFS_COMMENT, description);
                if (publicationComment.startsWith("Deprecated")) {
                    metadata.add(UNECE_STATUS, "deprecated");
                }
                // TODO: properly implement
                String domainKey = entity.getObjectClassTerm();
                if (repeatedClassKeys.containsKey(domainKey)) {
                    if(repeatedClassKeys.get(entity.getObjectClassTerm()) == 2) {
                        domainKey = entity.getObjectClassTerm();
                    } else {
                        domainKey = entity.getObjectClassTermQualifier().concat(entity.getObjectClassTerm());
                        if (domainKey.startsWith("Referenced")) {
                            domainKey = StringUtils.substringAfter(domainKey, "Referenced");
                        }
                        if (domainKey.startsWith("_")) {
                            domainKey = StringUtils.substringAfter(domainKey, "_");
                        }
                    }
                }
                domain.add(domainKey);
                comment.add(entity.getDescription());
                metadataJsonArrayBuilder.add(metadata);
                if (StringUtils.isNotEmpty(entity.getTDED()) && entity.getTDED().length() > 1) {
                    tded.add(entity.getTDED());
                }
            }
            if (rangeBBIE != null) {
                typeArray.add(StringUtils.join(OWL_NS,":","DatatypeProperty"));
                if (StringUtils.isBlank(rangeBBIE)) {
                    System.err.println(String.format("rangeBBIE is blank for %s", key));
                }
                if (StringUtils.isNotBlank(rangeASBIE)) {
                    System.err.println(String.format("Property is both BBIE and ASBIE for %s", key));
                }
                Object rangeIncludes = getData(rangeBBIE, tded);
                if (rangeIncludes instanceof JsonObjectBuilder) {
                    rdfProperty.add(SCHEMA_RANGE_INCLUDES, (JsonObjectBuilder) rangeIncludes);
                } else {
                    rdfProperty.add(SCHEMA_RANGE_INCLUDES, (JsonArrayBuilder) rangeIncludes);
                }
            } else {
                typeArray.add(StringUtils.join(OWL_NS,":","ObjectProperty"));
                rdfProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder().add(ID, StringUtils.join(UNECE_NS,":",rangeASBIE)));
            }
            rdfProperty.add(TYPE, typeArray);
            if (domain.size() == 1) {
                rdfProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder().add(ID, StringUtils.join(UNECE_NS,":",domain.iterator().next())));
            } else {
                JsonArrayBuilder domainJsonArrayBuilder = Json.createArrayBuilder();
                for (String domainName : domain) {
                    domainJsonArrayBuilder.add(Json.createObjectBuilder().add(ID, StringUtils.join(UNECE_NS,":",domainName)));
                }
                rdfProperty.add(SCHEMA_DOMAIN_INCLUDES, domainJsonArrayBuilder.build());

            }
            if (comment.size() == 1) {
                rdfProperty.add(RDFS_COMMENT, comment.iterator().next());
            } else {
                JsonArrayBuilder commentJsonArrayBuilder = Json.createArrayBuilder();
                for (String commentValue : comment) {
                    commentJsonArrayBuilder.add(commentValue);
                }
                rdfProperty.add(RDFS_COMMENT, commentJsonArrayBuilder.build());
            }
            rdfProperty.add(RDFS_LABEL, id);
            rdfProperty.add(UNECE_CEFACT_ELEMENT_METADATA, metadataJsonArrayBuilder.build());
            propertiesGraph.put(id, rdfProperty.build());
        }

        try {
            for (String key : propertiesGraph.keySet()) {
                graphJsonArrayBuilder = Json.createArrayBuilder();
                JsonObject jsonObject = propertiesGraph.get(key);
                setContext();
                if (jsonObject.get(SCHEMA_RANGE_INCLUDES).asJsonObject().getString(ID).startsWith(XSD_NS)){
                    contextObjectBuilder.add(XSD_NS, NS_MAP.get(XSD_NS));
                }
                contextObjectBuilder.add(OWL_NS, NS_MAP.get(OWL_NS));
                contextObjectBuilder.add(SCHEMA_NS, NS_MAP.get(SCHEMA_NS));
                JsonObjectBuilder objectBuilder = Json.createObjectBuilder(Map.of(TYPE, ID));
                contextObjectBuilder.add(UNECE_CEFACT_BIE_DOMAIN_CLASS, objectBuilder.build());
                outputFile = StringUtils.join(UNECE_NS, "/",key,".jsonld");
                graphJsonArrayBuilder.add(jsonObject);
                super.transform();
            }
            for (String key : classesGraph.keySet()) {
                graphJsonArrayBuilder = Json.createArrayBuilder();
                setContext();
                outputFile = StringUtils.join(UNECE_NS, "/",key,".jsonld");
                graphJsonArrayBuilder.add(classesGraph.get(key));
                super.transform();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }

    Object getData(String dataType, Set<String> TDED) {
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
                String item = StringUtils.join(UNECE_NS, ":", String.format("UNCL%sCode", filteredTDED.iterator().next()));
                return result.add(ID, item);
            } else {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (String item : filteredTDED) {
                    arrayBuilder.add(Json.createObjectBuilder().add(ID, StringUtils.join(UNECE_NS, ":", String.format("UNCL%sCode", item))));
                }
                return arrayBuilder;
            }
        } else {
            try {
                UNType unType = UNType.valueOf(dataType.toUpperCase());
                switch (unType) {
                    case INDICATOR:
                        result = result.add(ID, StringUtils.join(XSD_NS,":","boolean"));
                        break;
                    case IDENTIFIER:
                    case ID:
                    case CODE:
                        result = result.add(ID, StringUtils.join(XSD_NS,":","token"));
                        break;
                    case TEXT:
                    case VALUE:
                    case TYPE:
                    default:
                        result = result.add(ID, StringUtils.join(XSD_NS,":","string"));
                        break;
                    case DATETIME:
                        result = result.add(ID, StringUtils.join(XSD_NS,":","dateTime"));
                        break;
                    case AMOUNT:
                    case PERCENT:
                    case RATE:
                    case QUANTITY:
                    case NUMERIC:
                    case MEASURE:
                        result = result.add(ID, StringUtils.join(XSD_NS,":","decimal"));
                        break;
                    case DATE:
                        result = result.add(ID, StringUtils.join(XSD_NS,":","date"));
                    case BINARYOBJECT:
                    case GRAPHIC:
                    case PICTURE:
                    case VIDEO:
                    case SOUND:
                        result = result.add(ID, StringUtils.join(XSD_NS,":","base64Binary"));
                        break;
                    case TIME:
                        result = result.add(ID, StringUtils.join(XSD_NS,":","time"));
                        break;
                }

            } catch (IllegalArgumentException e) {
                System.out.println(String.format("Check data type %s", dataType));
                if (dataType.equalsIgnoreCase("Id"))
                    result = result.add(ID, StringUtils.join(XSD_NS,":","token"));
                else
                    result = result.add(ID, StringUtils.join(XSD_NS,":","string"));
            }
        }
        return result;
    }

}
