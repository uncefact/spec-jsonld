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
import org.unece.uncefact.vocab.JSONLDContext;
import org.unece.uncefact.vocab.JSONLDVocabulary;
import org.unece.uncefact.vocab.Transformer;

import javax.json.*;
import java.io.*;
import java.net.URL;
import java.util.*;

public class BSPToJSONLDVocabulary extends Transformer {

    protected static String BBIE = "BBIE";
    protected static String ABIE = "ABIE";
    protected static String ASBIE = "ASBIE";
    protected static String UNECE_ABIE_PROPERTY_NAME = "AggregateBIE";
    protected static String UNECE_ABIE_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_ABIE_PROPERTY_NAME);
    protected static String UNECE_BBIE_PROPERTY_NAME = "BasicBIE";
    protected static String UNECE_BBIE_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_BBIE_PROPERTY_NAME);
    protected static String UNECE_ASBIE_PROPERTY_NAME = "AssociationBIE";
    protected static String UNECE_ASBIE_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_ASBIE_PROPERTY_NAME);
    protected static String UNECE_TDED_PROPERTY_NAME = "tded";
    protected static String UNECE_TDED_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_TDED_PROPERTY_NAME);
    protected static String UNECE_STATUS_PROPERTY_NAME = "status";
    protected static String UNECE_STATUS_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_STATUS_PROPERTY_NAME);
    protected static String UNECE_CEFACT_UN_ID_PROPERTY_NAME = "cefactUNId";
    protected static String UNECE_CEFACT_UN_ID_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_CEFACT_UN_ID_PROPERTY_NAME);
    protected static String UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY_NAME = "cefactBusinessProcess";
    protected static String UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY_NAME);
    protected static String UNECE_CEFACT_ELEMENT_METADATA_PROPERTY_NAME = "cefactElementMetadata";
    protected static String UNECE_CEFACT_ELEMENT_METADATA_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_CEFACT_ELEMENT_METADATA_PROPERTY_NAME);
    protected static String UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY_NAME = "cefactBieDomainClass";
    protected static String UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY_NAME);

    Map<String, JsonObject> propertiesGraph = new TreeMap<>();
    Map<String, JsonObject> classesGraph = new TreeMap<>();

    JSONLDContext jsonldContext = new JSONLDContext();
    JSONLDVocabulary jsonldVocabulary = new JSONLDVocabulary();

    public JSONLDContext getJsonldContext() {
        return jsonldContext;
    }

    public JSONLDVocabulary getJsonldVocabulary() {
        return jsonldVocabulary;
    }
    public BSPToJSONLDVocabulary(String inputFile, String defaultFile) {
        super(inputFile, defaultFile);
    }

    protected JsonObjectBuilder getContext (){
        JsonObjectBuilder result = super.getMinimalContext();
        for (String ns : Arrays.asList(UNECE_NS)){
            result.add(ns, NS_MAP.get(ns));
        }
        return result;
    }

    public void transform() throws IOException, InvalidFormatException {
        Workbook workbook;
        if (inputFile == null){
            workbook = WorkbookFactory.create(getClass().getResourceAsStream(defaultFile));
        } else {
            workbook = WorkbookFactory.create(new File(inputFile));
        }
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
            if (entity.getType().equalsIgnoreCase(BBIE) || entity.getType().equalsIgnoreCase(ASBIE)) {
                Set<Entity> entities = new HashSet<>();
                String key = entity.getPropertyKey();
                key = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key);
                if (propertiesMap.containsKey(key)) {
                    entities = propertiesMap.get(key);
                }
                if (repeatedClassKeys.containsKey(entity.getAssociatedObjectClass())) {
                    // fix for https://github.com/uncefact/spec-jsonld/issues/139
                    // fixes the cases when the properties have the same key, but different type
                    // in such cases the key is changed by using the proper associated class key
                    if(repeatedClassKeys.get(entity.getAssociatedObjectClass()) != 2) {
                        for (Entity e:entities){
                            String classKeyToCompare = stripReferencedPrefix(e.getAssociatedClassTermWithQualifier());
                            String classKey = stripReferencedPrefix(entity.getAssociatedClassTermWithQualifier());
                            if (!classKeyToCompare.equalsIgnoreCase(classKey)) {
                                key = entity.getPropertyTermWithQualifierForNDRRules().concat(classKey);
                                key = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key);
                                if (propertiesMap.containsKey(key)) {
                                    entities = propertiesMap.get(key);
                                } else {
                                    entities = new HashSet<>();
                                }
                                break;
                            }
                        }
                    }
                }
                entities.add(entity);
                propertiesMap.put(key, entities);
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
                        key = stripReferencedPrefix(entity.getObjectClassTermQualifier().concat(entity.getObjectClassTerm()));
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
                metadata.add(TYPE, UNECE_ABIE_PROPERTY);
                metadata.add(UNECE_CEFACT_UN_ID_PROPERTY, entity.getId());
                metadata.add(RDFS_COMMENT, entity.getDescription());
                metadata.add(UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY, entity.getContext());
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
            rdfClass.add(UNECE_CEFACT_ELEMENT_METADATA_PROPERTY, metadataJsonArrayBuilder.build());
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
                    metadata.add(TYPE, UNECE_BBIE_PROPERTY);
                    rangeBBIE = entity.getRepresentationTerm();
                    if (StringUtils.isNotBlank(entity.getTDED()) && !".".equals(entity.getTDED())) {
                        metadata.add(UNECE_TDED_PROPERTY, entity.getTDED());
                    }
                } else if (entity.getType().equalsIgnoreCase(ASBIE)) {
                    metadata.add(TYPE, UNECE_ASBIE_PROPERTY);
                    rangeASBIE = entity.getAssociatedObjectClass();
                    if (repeatedClassKeys.containsKey(entity.getAssociatedObjectClass())) {
                        if(repeatedClassKeys.get(entity.getAssociatedObjectClass()) == 2) {
                            rangeASBIE = entity.getAssociatedObjectClass();
                        } else {
                            rangeASBIE = stripReferencedPrefix(entity.getAssociatedClassTermWithQualifier());
                        }
                    }
                }
                metadata.add(UNECE_CEFACT_UN_ID_PROPERTY, entity.getId());
                metadata.add(UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY, StringUtils.join(CEFACT_NS,":",entity.getCefactBieDomainClass()));
                metadata.add(UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY, entity.getContext());
                String description = entity.getDescription();
                String publicationComment = entity.getPublicationComment();
                if (StringUtils.isNotBlank(publicationComment)) {
                    description = description.concat(" ").concat(publicationComment);
                }
                metadata.add(RDFS_COMMENT, description);
                if (publicationComment.startsWith("Deprecated")) {
                    metadata.add(UNECE_STATUS_PROPERTY, "deprecated");
                }
                // TODO: properly implement
                String domainKey = entity.getObjectClassTerm();
                if (repeatedClassKeys.containsKey(domainKey)) {
                    if(repeatedClassKeys.get(entity.getObjectClassTerm()) == 2) {
                        domainKey = entity.getObjectClassTerm();
                    } else {
                        domainKey = stripReferencedPrefix(entity.getObjectClassTermQualifier().concat(entity.getObjectClassTerm()));
                    }
                }
                domain.add(domainKey);
                metadata.add(StringUtils.join(UNECE_NS,":","domainName"), domainKey);

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
            rdfProperty.add(UNECE_CEFACT_ELEMENT_METADATA_PROPERTY, metadataJsonArrayBuilder.build());
            propertiesGraph.put(id, rdfProperty.build());
        }

        /*for (String key : propertiesGraph.keySet()) {
            graphJsonArrayBuilder = Json.createArrayBuilder();
            JsonObject jsonObject = propertiesGraph.get(key);
            setContext();
            if (jsonObject.get(SCHEMA_RANGE_INCLUDES).asJsonObject().getString(ID).startsWith(XSD_NS)){
                contextObjectBuilder.add(XSD_NS, NS_MAP.get(XSD_NS));
            }
            contextObjectBuilder.add(OWL_NS, NS_MAP.get(OWL_NS));
            contextObjectBuilder.add(SCHEMA_NS, NS_MAP.get(SCHEMA_NS));
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder(Map.of(TYPE, ID));
            contextObjectBuilder.add(UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY, objectBuilder.build());
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
        }*/
        jsonldVocabulary.setContextObjectBuilder(getContext());
        jsonldVocabulary.getContextObjectBuilder().add(XSD_NS, NS_MAP.get(XSD_NS));
        jsonldVocabulary.getContextObjectBuilder().add(OWL_NS, NS_MAP.get(OWL_NS));
        jsonldVocabulary.getContextObjectBuilder().add(SCHEMA_NS, NS_MAP.get(SCHEMA_NS));
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder(Map.of(TYPE, ID));
        jsonldVocabulary.getContextObjectBuilder().add(UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY, objectBuilder.build());
        for (String key : propertiesGraph.keySet()) {
            jsonldVocabulary.getGraphJsonArrayBuilder().add(propertiesGraph.get(key));
        }
        for (String key : classesGraph.keySet()) {
            jsonldVocabulary.getGraphJsonArrayBuilder().add(classesGraph.get(key));
        }

        JsonObjectBuilder aggregateBIE = Json.createObjectBuilder(Map.of(
                ID, UNECE_ABIE_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, UNECE_ABIE_PROPERTY_NAME,
                RDFS_COMMENT, "Aggregate Business Information Entity"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(aggregateBIE.build());

        JsonObjectBuilder basicBIE = Json.createObjectBuilder(Map.of(
                ID, UNECE_BBIE_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, UNECE_BBIE_PROPERTY_NAME,
                RDFS_COMMENT, "Basic Business Information Entity contained within the ABIE"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(basicBIE.build());

        JsonObjectBuilder associationBIE = Json.createObjectBuilder(Map.of(
                ID, UNECE_ASBIE_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, UNECE_ASBIE_PROPERTY_NAME,
                RDFS_COMMENT, "Associated (Aggregate) Business Information Entity, associated with the ABIE"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(associationBIE.build());

        JsonObjectBuilder tded = Json.createObjectBuilder(Map.of(
                ID, UNECE_ASBIE_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, UNECE_ASBIE_PROPERTY_NAME,
                RDFS_COMMENT, "TDED reference number"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(tded.build());

        JsonObjectBuilder status = Json.createObjectBuilder(Map.of(
                ID, UNECE_STATUS_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, UNECE_STATUS_PROPERTY_NAME,
                RDFS_COMMENT, "Status"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(status.build());

        JsonObjectBuilder cefactUNId = Json.createObjectBuilder(Map.of(
                ID, UNECE_CEFACT_UN_ID_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, UNECE_CEFACT_UN_ID_PROPERTY_NAME,
                RDFS_COMMENT, "TBG assigned Unique ID for approved Library Objects"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(cefactUNId.build());

        JsonObjectBuilder cefactBusinessProcess = Json.createObjectBuilder(Map.of(
                ID, UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY_NAME,
                RDFS_COMMENT, "CEFACT Business Process"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(cefactBusinessProcess.build());

        JsonObjectBuilder cefactElementMetadata = Json.createObjectBuilder(Map.of(
                ID, UNECE_CEFACT_ELEMENT_METADATA_PROPERTY,
                TYPE, RDF_SEQ,
                RDFS_LABEL, UNECE_CEFACT_ELEMENT_METADATA_PROPERTY_NAME,
                RDFS_COMMENT, "CEFACT Element Metadata"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(cefactElementMetadata.build());

        JsonObjectBuilder cefactBieDomainClass = Json.createObjectBuilder(Map.of(
                ID, UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY_NAME,
                RDFS_COMMENT, "CEFACT Business Process"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(cefactBieDomainClass.build());

        JsonObjectBuilder rec20Class = Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","UNECERec20Code"),
                TYPE, RDFS_CLASS,
                RDFS_LABEL, "UNECERec20Code",
                RDFS_COMMENT, "Recommendations 20"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(rec20Class.build());

        JsonObjectBuilder rec21Class = Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","UNECERec21Code"),
                TYPE, RDFS_CLASS,
                RDFS_LABEL, "UNECERec21Code",
                RDFS_COMMENT, "Recommendations 21"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(rec21Class.build());

        JsonObjectBuilder rec24Class = Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","UNECERec24Code"),
                TYPE, RDFS_CLASS,
                RDFS_LABEL, "UNECERec24Code",
                RDFS_COMMENT, "Recommendations 24"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(rec24Class.build());

        JsonObjectBuilder rec28Class = Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","UNECERec28Code"),
                TYPE, RDFS_CLASS,
                RDFS_LABEL, "UNECERec28Code",
                RDFS_COMMENT, "Recommendations 28"
        ));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(rec28Class.build());

        JsonObjectBuilder levelCategoryProperty = Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","levelCategory"),
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, "levelCategory",
                RDFS_COMMENT, "Level Category."
        ));
        levelCategoryProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(XSD_NS,":","string")
        )));
        levelCategoryProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","UNECERec20Code")
        )));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(levelCategoryProperty.build());

        JsonObjectBuilder symbolProperty = Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","symbol"),
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, "symbol",
                RDFS_COMMENT, "Symbol."
        ));
        symbolProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(XSD_NS,":","string")
        )));
        symbolProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","UNECERec20Code")
        )));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(symbolProperty.build());

        JsonObjectBuilder conversionFactorProperty = Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","conversionFactor"),
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, "conversionFactor",
                RDFS_COMMENT, "Conversion Factor."
        ));
        conversionFactorProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(XSD_NS,":","string")
        )));
        conversionFactorProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","UNECERec20Code")
        )));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(conversionFactorProperty.build());

        JsonObjectBuilder statusProperty = Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","status"),
                TYPE, RDF_PROPERTY,
                RDFS_LABEL, "status",
                RDFS_COMMENT, "Status."
        ));
        statusProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(XSD_NS,":","string")
        )));
        statusProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, StringUtils.join(UNECE_NS,":","UNECERec20Code")
        )));
        jsonldVocabulary.getGraphJsonArrayBuilder().add(statusProperty.build());


        jsonldContext.getContextObjectBuilder().add("@vocab", NS_MAP.get(UNECE_NS));
        jsonldContext.getContextObjectBuilder().add(UNECE_NS, NS_MAP.get(UNECE_NS));
        jsonldContext.getContextObjectBuilder().add(RDF_NS, NS_MAP.get(RDF_NS));
        jsonldContext.getContextObjectBuilder().add(RDFS_NS, NS_MAP.get(RDFS_NS));
        jsonldContext.getContextObjectBuilder().add(XSD_NS, NS_MAP.get(XSD_NS));
        jsonldContext.getContextObjectBuilder().add(OWL_NS, NS_MAP.get(OWL_NS));
        jsonldContext.getContextObjectBuilder().add(SCHEMA_NS, NS_MAP.get(SCHEMA_NS));
        objectBuilder = Json.createObjectBuilder(Map.of(TYPE, ID));
        jsonldContext.getContextObjectBuilder().add(UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY_NAME, objectBuilder.build());
        for (String key : propertiesGraph.keySet()) {
            jsonldContext.getContextObjectBuilder().add(key, Json.createObjectBuilder(Map.of(ID, StringUtils.join(UNECE_NS,":", key))).build());
        }
        for (String key : classesGraph.keySet()) {
            jsonldContext.getContextObjectBuilder().add(key, Json.createObjectBuilder(Map.of(ID, StringUtils.join(UNECE_NS,":", key))).build());
        }
        jsonldContext.getContextObjectBuilder().add(UNECE_ABIE_PROPERTY_NAME, Json.createObjectBuilder(Map.of(ID, UNECE_ABIE_PROPERTY)).build());
        jsonldContext.getContextObjectBuilder().add(UNECE_BBIE_PROPERTY_NAME, Json.createObjectBuilder(Map.of(ID, UNECE_BBIE_PROPERTY)).build());
        jsonldContext.getContextObjectBuilder().add(UNECE_ASBIE_PROPERTY_NAME, Json.createObjectBuilder(Map.of(ID, UNECE_ASBIE_PROPERTY)).build());
        jsonldContext.getContextObjectBuilder().add(UNECE_TDED_PROPERTY_NAME, Json.createObjectBuilder(Map.of(ID, UNECE_TDED_PROPERTY)).build());
        jsonldContext.getContextObjectBuilder().add(UNECE_STATUS_PROPERTY_NAME, Json.createObjectBuilder(Map.of(ID, UNECE_STATUS_PROPERTY)).build());
        jsonldContext.getContextObjectBuilder().add(UNECE_CEFACT_UN_ID_PROPERTY_NAME, Json.createObjectBuilder(Map.of(ID, UNECE_CEFACT_UN_ID_PROPERTY)).build());
        jsonldContext.getContextObjectBuilder().add(UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY_NAME, Json.createObjectBuilder(Map.of(ID, UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY)).build());
        jsonldContext.getContextObjectBuilder().add(UNECE_CEFACT_ELEMENT_METADATA_PROPERTY_NAME, Json.createObjectBuilder(Map.of(ID, UNECE_CEFACT_ELEMENT_METADATA_PROPERTY)).build());
        jsonldContext.getContextObjectBuilder().add(UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY_NAME, Json.createObjectBuilder(Map.of(ID, UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY)).build());

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

    public static String stripReferencedPrefix(String input){
        if (input.startsWith("Referenced")) {
            input = StringUtils.substringAfter(input, "Referenced");
        }
        if (input.startsWith("_")) {
            input = StringUtils.substringAfter(input, "_");
        }
        return input;
    }

}
