package org.unece.uncefact.vocab.transformers;

import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;
import org.unece.uncefact.UNType;
import org.unece.uncefact.vocab.*;

import javax.json.*;
import java.io.*;
import java.util.*;

public class BSPJSONSchemaToJSONLDVocabulary extends Transformer {

    protected static String UNECE_ABIE_PROPERTY_NAME = "AggregateBIE";
    protected static String UNECE_ABIE_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_ABIE_PROPERTY_NAME);
    protected static String UNECE_BBIE_PROPERTY_NAME = "BasicBIE";
    protected static String UNECE_BBIE_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_BBIE_PROPERTY_NAME);
    protected static String UNECE_ASBIE_PROPERTY_NAME = "AssociationBIE";
    protected static String UNECE_ASBIE_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_ASBIE_PROPERTY_NAME);
    protected static String UNECE_TDED_PROPERTY_NAME = "tded";
    protected static String UNECE_TDED_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_TDED_PROPERTY_NAME);
    protected static String UNECE_SCHEMA_NAME_PROPERTY_NAME = "propertySchemaName";
    protected static String UNECE_SCHEMA_NAME_PROPERTY = StringUtils.join(UNECE_NS, ":", UNECE_SCHEMA_NAME_PROPERTY_NAME);
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
    Map<String, JsonObject> codeValuesGraph = new TreeMap<>();

    Map<String, String> rangeDataTypeMap = new HashMap<>();
    Map<String, String> rangeObjectTypeMap = new HashMap<>();
    Set<String> unitCodeTypes = new HashSet<>();
    Set<String> amountTypes = new HashSet<>();
    Map<String, String> qdtMap = new HashMap<>();
    Map<String, String> codeListContext = new HashMap<>();
    Map<String, String> codeListMapping = new HashMap<>();
    JSONLDContext jsonldContext = new JSONLDContext();
    JSONLDVocabulary jsonldVocabulary = new JSONLDVocabulary();

    public JSONLDContext getJsonldContext() {
        return jsonldContext;
    }

    public JSONLDVocabulary getJsonldVocabulary() {
        return jsonldVocabulary;
    }
    public BSPJSONSchemaToJSONLDVocabulary(String inputFile, String defaultFile) {
        super(inputFile, defaultFile);
    }

    protected JsonObjectBuilder getContext (){
        JsonObjectBuilder result = super.getMinimalContext();
        for (String ns : Arrays.asList(UNECE_NS)){
            result.add(ns, NS_MAP.get(ns));
        }
        return result;
    }

    public void transform() {
        InputStream fis = null;
        try {
            if (inputFile == null){
                fis = getClass().getResourceAsStream(defaultFile);
            } else {
                fis = new FileInputStream(inputFile);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        JsonReader reader = Json.createReader(fis);
        JsonObject vocabulary = reader.readObject();
        reader.close();
        readInputFileToGraphArray(vocabulary);
    }

    public void readInputFileToGraphArray(final Object object) {
        JsonObject vocabularyObject = (JsonObject) object;
        JsonObject defs = vocabularyObject.getJsonObject("$defs");
        Map<String, Entity> vocabulary = new HashMap<>();
        Map<String, JsonObject> qdtMapJson = new HashMap<>();
        Map<String, JsonObject> udtMap = new HashMap<>();
        Map<String, JsonObject> pdtMap = new HashMap<>();
        Set<String> refTypes = new HashSet<>();
        int downgradeCount = 0;
        for (String key : defs.keySet()) {
            if (key.equalsIgnoreCase("qdt")) {
                {
                    //read PDTs and UDTs
                    String refPath = "UNECE-BasicComponents.json";
                    String pdtPath = "$defs/pdt/$defs";
                    String udtPath = "$defs/udt/$defs";
                    InputStream fis = null;
                    try {
                        if (inputFile == null) {
                            fis = getClass().getResourceAsStream("/D22A/".concat(refPath));
                        } else {
                            //TODO: parse bath path from input file
                            fis = new FileInputStream(inputFile);
                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    JsonReader reader = Json.createReader(fis);
                    JsonObject basicComponents = reader.readObject();
                    JsonObject pdtList = basicComponents;
                    JsonObject udtList = basicComponents;
                    reader.close();
                    for (String path : StringUtils.split(pdtPath, "/")) {
                        pdtList = pdtList.getJsonObject(path);
                    }
                    for (String pdtKey : pdtList.keySet()) {
                        pdtMap.put(StringUtils.join(refPath, "#/", pdtPath, "/", pdtKey), pdtList.get(pdtKey).asJsonObject());
                    }
                    for (String path : StringUtils.split(udtPath, "/")) {
                        udtList = udtList.getJsonObject(path);
                    }
                    for (String udtKey : udtList.keySet()) {
                        udtMap.put(StringUtils.join(refPath, "#/", udtPath, "/", udtKey), udtList.get(udtKey).asJsonObject());
                    }
                    System.out.println(pdtMap);
                    System.out.println(udtMap);
                }

                //Do the magic for QDT
                JsonObject qdtDefs = defs.getJsonObject(key).getJsonObject("$defs");
                for (String qdtKey :qdtDefs.keySet()){
                    qdtMapJson.put(StringUtils.join("#/$defs/qdt/$defs/", qdtKey), qdtDefs.get(qdtKey).asJsonObject());
                }
                System.out.println(qdtMapJson);
                readDTs(pdtMap, udtMap, qdtMapJson);
            } else {
                JsonObject jsonObject = defs.getJsonObject(key);
                Entity entity = new Entity();
                entity.setId(jsonObject.getString("uncefact:cefactUNId"));
                String type = jsonObject.getString("uncefact:type");
                entity.setType(type);
                String title = jsonObject.getString("title");
                entity.setName(title);
                entity.setDescription(jsonObject.getString("description"));
                String objectClassTermQualifier = null;
                String objectClassTerm = null;
                if (type.equalsIgnoreCase(UNECE_ABIE_PROPERTY_NAME)) {
                    if (title.contains("_ ")) {
                        objectClassTermQualifier = cleanUp(StringUtils.substringBeforeLast(title, "_ "));
                    } else {
                        objectClassTermQualifier = "";
                    }
                    entity.setObjectClassTermQualifier(objectClassTermQualifier);
                    objectClassTerm = StringUtils.substringBefore(title, ". Details");
                    if (objectClassTerm.contains("_ ")) {
                        objectClassTerm = StringUtils.substringAfterLast(objectClassTerm, "_ ");
                    }
                    entity.setObjectClassTerm(cleanUp(objectClassTerm));
                    entity.setContext(jsonObject.getString("uncefact:cefactBusinessProcess"));
                    vocabulary.put(entity.getName(), entity);

                }

                JsonObject properties = jsonObject.getJsonObject("properties");

                for (String propertyKey : properties.keySet()) {
                    JsonObject propertyJsonObject = properties.getJsonObject(propertyKey);

                    Entity propertyEntity = new Entity();
                    propertyEntity.setSchemaName(propertyKey);
                    propertyEntity.setId(propertyJsonObject.getString("uncefact:cefactUNId"));
                    String propertyType = propertyJsonObject.getString("uncefact:type");
                    propertyEntity.setType(propertyType);
                    String propertyTitle = propertyJsonObject.getString("title");
                    propertyEntity.setName(propertyTitle);
                    propertyEntity.setDescription(propertyJsonObject.getString("description"));

                    propertyEntity.setObjectClassTermQualifier(cleanUp(objectClassTermQualifier));
                    propertyEntity.setObjectClassTerm(cleanUp(objectClassTerm));

                    String propertyTermWithQualifier = StringUtils.substringBetween(propertyTitle, ". ");
                    if (propertyTermWithQualifier == null) {
                        System.err.println(propertyTitle);
                    }
                    if (propertyTermWithQualifier.contains("_ ")) {
                        propertyEntity.setPropertyTermQualifier(cleanUp(StringUtils.substringBefore(propertyTermWithQualifier, "_ ")));
                        propertyEntity.setPropertyTerm(cleanUp(StringUtils.substringAfter(propertyTermWithQualifier, "_ ")));
                    } else {
                        propertyEntity.setPropertyTerm(cleanUp(propertyTermWithQualifier));
                    }

                    if (propertyType.equalsIgnoreCase(UNECE_BBIE_PROPERTY_NAME)) {
                        propertyEntity.setRepresentationTerm(StringUtils.substringAfterLast(propertyTitle, ". "));
                        if (propertyJsonObject.keySet().contains("uncefact:TDED")) {
                            propertyEntity.setTDED(propertyJsonObject.getString("uncefact:TDED"));
                        } else {
                            propertyEntity.setTDED("");
                        }
                        String ref = propertyJsonObject.getString("$ref");
                        refTypes.add(ref);
                        if (rangeObjectTypeMap.containsKey(ref)) {
                            propertyEntity.setRangeIncludes(rangeObjectTypeMap.get(ref));
                        } else if (rangeDataTypeMap.containsKey(ref)) {
                            propertyEntity.setRangeIncludes(rangeDataTypeMap.get(ref));
                        }

                        if (propertyJsonObject.containsKey("properties")) {
                            JsonObject propertyProperties = propertyJsonObject.getJsonObject("properties");
                            if (propertyProperties.containsKey("unitCode") ||
                                    propertyProperties.containsKey("currencyId")/* ||
                                    propertyProperties.containsKey("languageId") ||
                                    propertyProperties.containsKey("format")*/) {
                                //TODO: downgrade to pdt
                                System.out.println("Downgrade needed");
                                downgradeCount +=1;
                            }
                        }

                        if (propertyEntity.getId().equalsIgnoreCase("UN01012488")){
                            System.out.println();
                        }


                        if (ref.startsWith("UNECE-BasicComponents.json#/$defs/udt/$defs")) {
                            if (udtMap.isEmpty()) {

                            }
                        } else if (ref.startsWith("#/$defs/qdt/$defs/")) {
                            String dataTypeQualifier = StringUtils.substringAfter(ref, "#/$defs/qdt/$defs/");

                            JsonObject jsonObjectQDT = defs.getJsonObject("qdt").getJsonObject("$defs").getJsonObject(dataTypeQualifier);
                            String qdtTitle = cleanUp(StringUtils.substringBefore(jsonObjectQDT.getString("title"), ". Type"));
                            String unitCodeQDTTitle = null;
                            String formatQDTTitle = null;
                            String qdtDescription = jsonObjectQDT.getString("description");
                            if (jsonObjectQDT.containsKey("properties")) {

                                JsonObject jsonObjectProperties = jsonObjectQDT.getJsonObject("properties");
                                JsonObject jsonObjectPropertiesUnitCode = null;
                                JsonObject jsonObjectPropertiesFormat = null;

                                if (jsonObjectProperties.containsKey("unitCode")){
                                    jsonObjectPropertiesUnitCode = jsonObjectProperties.getJsonObject("unitCode");
                                }
                                if (jsonObjectProperties.containsKey("format")){
                                    jsonObjectPropertiesFormat = jsonObjectProperties.getJsonObject("format");
                                }

                                    JsonObject jsonObjectPropertiesContent = jsonObjectProperties.getJsonObject("content");
                                    String refString = null;
                                    JsonArray allOfArray = null;
                                    if (jsonObjectPropertiesUnitCode!=null) {
                                        if (jsonObjectPropertiesUnitCode.containsKey("$ref")){
                                            refString = jsonObjectPropertiesUnitCode.getString("$ref");
                                            unitCodeQDTTitle = qdtTitle.replace("UnitMeasure","UnitCode");
                                        } else {
                                            System.out.println("unitCode is missing $ref property");
                                        }
                                    }
                                    if (jsonObjectPropertiesFormat!=null) {
                                        if (jsonObjectPropertiesFormat.containsKey("$ref")){
                                            refString = jsonObjectPropertiesFormat.getString("$ref");
                                            formatQDTTitle = qdtTitle.concat("Format");
                                        } else {
                                            System.out.println("format is missing $ref property");
                                        }
                                    }
                                    else if (jsonObjectPropertiesContent.containsKey("$ref")) {
                                        refString = jsonObjectPropertiesContent.getString("$ref");
                                    } else if (jsonObjectPropertiesContent.containsKey("allOf")) {
                                        Iterator<JsonValue> iterator = jsonObjectPropertiesContent.getJsonArray("allOf").iterator();
                                        while (iterator.hasNext()) {
                                            JsonObject allOffObject = iterator.next().asJsonObject();
                                            if (allOffObject.containsKey("$ref")) {
                                                refString = allOffObject.getString("$ref");
                                            } else if (allOffObject.containsKey("enum")) {
                                                allOfArray = allOffObject.getJsonArray("enum");
                                            }
                                        }
                                    } else {
                                        System.out.println(jsonObjectPropertiesContent);
                                    }


                                    String refPath = StringUtils.substringBefore(refString, ".json#/");
                                    String jsonPath = StringUtils.substringAfter(refString, ".json#/");

                                    if (dataTypeQualifier.endsWith("CodeType"))
                                        dataTypeQualifier = StringUtils.substringBeforeLast(dataTypeQualifier, "CodeType");
                                    else if (dataTypeQualifier.endsWith("IdType"))
                                        dataTypeQualifier = StringUtils.substringBeforeLast(dataTypeQualifier, "IdType");
                                    else if (dataTypeQualifier.endsWith("MeasureType"))
                                        dataTypeQualifier = StringUtils.substringBeforeLast(dataTypeQualifier, "MeasureType");
                                    else if (dataTypeQualifier.endsWith("FormattedDateTimeType"))
                                        dataTypeQualifier = "";
                                    propertyEntity.setDataTypeQualifier(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, dataTypeQualifier));


                                    //TODO: add to context
                                    //codeListContext.add(codeListId, StringUtils.join(NS_MAP.get(UNECE_NS), "codelists/", codeListId));



                            } else {
                                System.out.println(String.format("%s has no properties", jsonObjectQDT));

                            }

                        }
                        else {
                            System.out.println(String.format("Unexpected ref value - %s", ref));
                        }
                    } else if (propertyType.equalsIgnoreCase(UNECE_ASBIE_PROPERTY_NAME)) {
                        String associatedObjectClassTermWithQualifier = StringUtils.substringAfterLast(propertyTitle, ". ");
                        if (associatedObjectClassTermWithQualifier.contains("_ ")) {
                            propertyEntity.setAssociatedObjectClassTermQualifier(cleanUp(StringUtils.substringBeforeLast(associatedObjectClassTermWithQualifier, "_ ")));
                            propertyEntity.setAssociatedObjectClass(cleanUp(StringUtils.substringAfterLast(associatedObjectClassTermWithQualifier, "_ ")));
                        } else {
                            propertyEntity.setAssociatedObjectClass(cleanUp(associatedObjectClassTermWithQualifier));
                        }
                    }
                    propertyEntity.setContext(propertyJsonObject.getString("uncefact:cefactBusinessProcess"));

                    vocabulary.put(propertyEntity.getName(), propertyEntity);
                /*
                if (entity.getType() != null) {
                    vocabulary.put(entity.getName(), entity);
                }*/
                }

            }
        }
        Set<String> unusedQdts = new HashSet<>();
        unusedQdts.addAll(qdtMapJson.keySet());
        unusedQdts.removeAll(refTypes);
        if(unusedQdts.size()!=0){
            System.err.println(String.format("The number of defined but not used QDTs %s out of %s total QDTs", unusedQdts.size(), qdtMapJson.size()));
            ArrayList<String> list = new ArrayList(unusedQdts);
            Collections.sort(list);
            for (String unusedQdt:list){
                System.out.println(StringUtils.substringAfterLast(unusedQdt, "/"));
            }
        }

        Map<String, Set<Entity>> classesMap = new TreeMap<>();
        Map<String, Set<Entity>> propertiesMap = new TreeMap<>();
        Map<String, Set<Entity>> newPropertiesMap = new TreeMap<>();
        Map<String, Set<String>> relatedClasses = new TreeMap<>();
        for (Entity entity : vocabulary.values()) {
            if (entity.getType().equalsIgnoreCase(UNECE_ABIE_PROPERTY_NAME)) {
                String classKey = entity.getObjectClassTerm();
                String classTermWithQualifier = stripReferencedPrefix(entity.getObjectClassTermQualifier().concat(classKey));
                if (relatedClasses.containsKey(classKey)) {
                    Set<String> values = relatedClasses.get(classKey);
                    values.add(classTermWithQualifier);
                    relatedClasses.put(classKey, values);
                } else {
                    Set<String> values = new HashSet<>();
                    values.add(classTermWithQualifier);
                    relatedClasses.put(classKey, values);
                }
            }
        }
        for (Entity entity : vocabulary.values()) {
            if (entity.getType().equalsIgnoreCase(UNECE_ASBIE_PROPERTY_NAME)) {
                Set<Entity> entities = new HashSet<>();
                String key = stripReferencedPrefix(entity.getPropertyKey());
                key = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key);
                if (propertiesMap.containsKey(key)) {
                    entities = propertiesMap.get(key);
                }
                entities.add(entity);
                propertiesMap.put(key, entities);
            }
        }
        Set<String> keysToRemove = new HashSet<>();
        for (String key : propertiesMap.keySet()) {
            // fix for https://github.com/uncefact/spec-jsonld/issues/139
            // fixes the cases when the properties have the same key, but different type
            // in such cases the key is changed by using the proper associated class key
            Set<Entity> entities = propertiesMap.get(key);
            Iterator<Entity> iterator = entities.iterator();
            Entity entity = iterator.next();
            while (iterator.hasNext()) {
                Entity nextEntity = iterator.next();
                String classKey = stripReferencedPrefix(entity.getAssociatedClassTermWithQualifier());
                String classKeyToCompare = stripReferencedPrefix(nextEntity.getAssociatedClassTermWithQualifier());
                if (!classKeyToCompare.equalsIgnoreCase(classKey)) {
                    keysToRemove.add(key);
                    break;
                }
            }
        }
        for (String key : propertiesMap.keySet()) {
            if (keysToRemove.contains(key)) {
                Set<Entity> entities = propertiesMap.get(key);
                for (Entity e : entities) {
                    Set<Entity> newEntities = new HashSet<>();
                    String classKey = stripReferencedPrefix(e.getAssociatedClassTermWithQualifier());
                    String newKey = e.getPropertyTermWithQualifierForNDRRules().concat(classKey);
                    newKey = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, newKey);
                    if (newPropertiesMap.containsKey(newKey)) {
                        newEntities = newPropertiesMap.get(newKey);
                    }
                    newEntities.add(e);
                    newPropertiesMap.put(newKey, newEntities);
                }
            }
        }

        for (String key : keysToRemove) {
            propertiesMap.remove(key);
        }

        for (String key : newPropertiesMap.keySet()) {
            propertiesMap.put(key, newPropertiesMap.get(key));
        }

        for (Entity entity : vocabulary.values()) {
            if (entity.getType().equalsIgnoreCase(UNECE_BBIE_PROPERTY_NAME)) {
                Set<Entity> entities = new HashSet<>();
                if (entity.getId().equalsIgnoreCase("UN01013236")) {
                    System.out.println(entity);
                }
                String key = stripReferencedPrefix(entity.getPropertyKey());
                key = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key);
                if (propertiesMap.containsKey(key)) {
                    entities = propertiesMap.get(key);
                }
                entities.add(entity);
                propertiesMap.put(key, entities);
            }
        }

        Entity.printRuleCounter();

        for (Entity entity : vocabulary.values()) {
            if (entity.getType().equalsIgnoreCase(UNECE_ABIE_PROPERTY_NAME)) {
                Set<Entity> entities = new HashSet<>();
                String key = entity.getObjectClassTerm();
                if (relatedClasses.containsKey(key)) {
                    if (relatedClasses.get(key).size() > 1) {
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

            if (propertiesMap.containsKey(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key))) {
                System.out.println(String.format("Name \"%s\"is used for both a property and a class", key));
            }
            JsonArrayBuilder metadataJsonArrayBuilder = Json.createArrayBuilder();
            String id = key;
            Set<Entity> entities = classesMap.get(key);
            HashSet<String> comment = new HashSet<>();
            for (Entity entity : entities) {
                JsonObjectBuilder metadata = Json.createObjectBuilder();
                metadata.add(ID, StringUtils.join(CEFACT_NS, ":", entity.getName()));
                metadata.add(TYPE, UNECE_ABIE_PROPERTY);
                metadata.add(UNECE_CEFACT_UN_ID_PROPERTY, entity.getId());
                metadata.add(RDFS_COMMENT, entity.getDescription());
                metadata.add(UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY, entity.getContext());
                comment.add(entity.getDescription());
                metadataJsonArrayBuilder.add(metadata);
            }

            JsonObjectBuilder rdfClass = Json.createObjectBuilder();
            rdfClass.add(ID, StringUtils.join(UNECE_NS, ":", id));
            rdfClass.add(TYPE, RDFS_CLASS);
            /*if (accsMap.containsKey(id)) {
                rdfClass.add(RDFS_COMMENT, accsMap.get(id));

            } else*/
            if (comment.size() == 1) {
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
            JsonArrayBuilder relatedClass = Json.createArrayBuilder();
            for (Set<String> relatedClassesValues : relatedClasses.values()) {
                if (relatedClassesValues.contains(id)) {
                    for (String relatedClassesValue : relatedClassesValues) {
                        if (!id.equalsIgnoreCase(relatedClassesValue))
                            relatedClass.add(StringUtils.join(UNECE_NS, ":", relatedClassesValue));
                    }
                    rdfClass.add(StringUtils.join(UNECE_NS, ":", "relatedClass"), relatedClass.build());
                }
            }
            classesGraph.put(id, rdfClass.build());
        }

        for (String key : propertiesMap.keySet()) {
            String id = key;
            JsonObjectBuilder rdfProperty = Json.createObjectBuilder();
            rdfProperty.add(ID, StringUtils.join(UNECE_NS, ":", id));

            JsonArrayBuilder metadataJsonArrayBuilder = Json.createArrayBuilder();
            Set<Entity> entities = propertiesMap.get(key);
            String rangeBBIE = null;
            String rangeASBIE = null;
            String rangeIncludes = null;
            String schemaName = null;
            String TDED = null;
            TreeSet<String> domain = new TreeSet<>();
            TreeSet<String> comment = new TreeSet<>();
            TreeSet<String> tded = new TreeSet<>();
            JsonArrayBuilder typeArray = Json.createArrayBuilder();
            typeArray.add(RDF_PROPERTY);
            for (Entity entity : entities) {
                JsonObjectBuilder metadata = Json.createObjectBuilder();
                metadata.add(ID, StringUtils.join(CEFACT_NS, ":", entity.getName()));
                if (entity.getType().equalsIgnoreCase(UNECE_BBIE_PROPERTY_NAME)) {
                    metadata.add(TYPE, UNECE_BBIE_PROPERTY);
                    rangeBBIE = entity.getRepresentationTerm();
                    if (StringUtils.isNotBlank(entity.getTDED()) && !".".equals(entity.getTDED())) {
                        metadata.add(UNECE_TDED_PROPERTY, entity.getTDED());
                        if (TDED != null && !TDED.equalsIgnoreCase(entity.getTDED())) {
                            // TODO: check if TDED mismatch can be a problem
                            // System.err.println(String.format("TDED for %s is different different for entities of the same property, %s and %s", id, TDED, entity.getTDED()));
                        }
                        TDED = entity.getTDED();
                    }
                    if (entity.getRangeIncludes() != null) {
                        if (rangeIncludes != null && !rangeIncludes.equals(entity.getRangeIncludes())) {
                            //System.err.println(String.format("for %s range includes are different for entities of the same property, %s and %s", id, rangeIncludes, entity.getRangeIncludes()));
                        }
                        rangeIncludes = entity.getRangeIncludes();
                    }
                    if (entity.getSchemaName() != null) {
                        metadata.add(UNECE_SCHEMA_NAME_PROPERTY, entity.getSchemaName());

                        if (schemaName != null && !schemaName.equals(entity.getSchemaName())) {
                            //System.out.println(String.format("for %s schema name are different for entities of the same property, %s and %s", id, schemaName, entity.getSchemaName()));
                        }
                        if (schemaName != null && !schemaName.equals(id)) {
                            //System.out.println(String.format("for %s schema name is different, %s", id, schemaName));
                        }
                        schemaName = entity.getSchemaName();
                    }
                } else if (entity.getType().equalsIgnoreCase(UNECE_ASBIE_PROPERTY_NAME)) {
                    metadata.add(TYPE, UNECE_ASBIE_PROPERTY);
                    rangeASBIE = entity.getAssociatedObjectClass();
                    if (relatedClasses.containsKey(entity.getAssociatedObjectClass())) {
                        if (relatedClasses.get(entity.getAssociatedObjectClass()).size() > 1) {
                            rangeASBIE = stripReferencedPrefix(entity.getAssociatedClassTermWithQualifier());
                        }
                    }
                }
                metadata.add(UNECE_CEFACT_UN_ID_PROPERTY, entity.getId());
                metadata.add(UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY, StringUtils.join(CEFACT_NS, ":", entity.getCefactBieDomainClass()));
                metadata.add(UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY, entity.getContext());
                String description = entity.getDescription();
                String publicationComment = StringUtils.defaultIfEmpty(entity.getPublicationComment(), "");
                if (StringUtils.isNotBlank(publicationComment)) {
                    description = description.concat(" ").concat(publicationComment);
                }
                metadata.add(RDFS_COMMENT, description);
                if (publicationComment.startsWith("Deprecated")) {
                    metadata.add(UNECE_STATUS_PROPERTY, "deprecated");
                }
                // TODO: properly implement
                String domainKey = entity.getObjectClassTerm();
                if (relatedClasses.containsKey(domainKey)) {
                    if (relatedClasses.get(domainKey).size() > 1) {
                        domainKey = stripReferencedPrefix(entity.getObjectClassTermQualifier().concat(entity.getObjectClassTerm()));
                    }
                }
                domain.add(domainKey);
                metadata.add(StringUtils.join(UNECE_NS, ":", "domainName"), domainKey);

                comment.add(entity.getDescription());
                metadataJsonArrayBuilder.add(metadata);
                if (StringUtils.isNotEmpty(entity.getTDED()) && entity.getTDED().length() > 1) {
                    tded.add(entity.getTDED());
                }
            }
            if (rangeBBIE != null) {
                if (rangeDataTypeMap.containsKey(rangeIncludes)) {
                    typeArray.add(StringUtils.join(OWL_NS, ":", "DatatypeProperty"));
                } else {
                    typeArray.add(StringUtils.join(OWL_NS, ":", "ObjectProperty"));
                }
                if (StringUtils.isBlank(rangeBBIE)) {
                    System.err.println(String.format("rangeBBIE is blank for %s", key));
                }
                if (StringUtils.isNotBlank(rangeASBIE)) {
                    System.err.println(String.format("Property is both BBIE and ASBIE for %s", key));
                }
                if (rangeIncludes != null) {
                    rdfProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder().add(ID, rangeIncludes));
                } else {
                    Object rangeIncludesObj = getData(rangeBBIE, tded);
                    if (rangeIncludesObj instanceof JsonObjectBuilder) {
                        rdfProperty.add(SCHEMA_RANGE_INCLUDES, (JsonObjectBuilder) rangeIncludesObj);
                    } else {
                        rdfProperty.add(SCHEMA_RANGE_INCLUDES, (JsonArrayBuilder) rangeIncludesObj);
                    }
                }
            } else {
                typeArray.add(StringUtils.join(OWL_NS, ":", "ObjectProperty"));
                rdfProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder().add(ID, StringUtils.join(UNECE_NS, ":", rangeASBIE)));
            }
            rdfProperty.add(TYPE, typeArray);
            if (domain.size() == 1) {
                rdfProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder().add(ID, StringUtils.join(UNECE_NS, ":", domain.iterator().next())));
            } else {
                JsonArrayBuilder domainJsonArrayBuilder = Json.createArrayBuilder();
                for (String domainName : domain) {
                    domainJsonArrayBuilder.add(Json.createObjectBuilder().add(ID, StringUtils.join(UNECE_NS, ":", domainName)));
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

        for (String key : codeValuesGraph.keySet()) {
            jsonldVocabulary.getGraphJsonArrayBuilder().add(codeValuesGraph.get(key));
        }

        JsonObjectBuilder aggregateBIE = Json.createObjectBuilder();
        aggregateBIE.add(ID, UNECE_ABIE_PROPERTY);
        aggregateBIE.add(TYPE, RDF_PROPERTY);
        aggregateBIE.add(RDFS_LABEL, UNECE_ABIE_PROPERTY_NAME);
        aggregateBIE.add(RDFS_COMMENT, "Aggregate Business Information Entity");
        jsonldVocabulary.getGraphJsonArrayBuilder().add(aggregateBIE.build());

        JsonObjectBuilder basicBIE = Json.createObjectBuilder();
        basicBIE.add(ID, UNECE_BBIE_PROPERTY);
        basicBIE.add(TYPE, RDF_PROPERTY);
        basicBIE.add(RDFS_LABEL, UNECE_BBIE_PROPERTY_NAME);
        basicBIE.add(RDFS_COMMENT, "Basic Business Information Entity contained within the ABIE");

        jsonldVocabulary.getGraphJsonArrayBuilder().add(basicBIE.build());

        JsonObjectBuilder associationBIE = Json.createObjectBuilder();
        associationBIE.add(ID, UNECE_ASBIE_PROPERTY);
        associationBIE.add(TYPE, RDF_PROPERTY);
        associationBIE.add(RDFS_LABEL, UNECE_ASBIE_PROPERTY_NAME);
        associationBIE.add(RDFS_COMMENT, "Basic Business Information Entity contained within the ABIE");

        jsonldVocabulary.getGraphJsonArrayBuilder().add(associationBIE.build());

        JsonObjectBuilder tded = Json.createObjectBuilder();
        tded.add(ID, UNECE_TDED_PROPERTY);
        tded.add(TYPE, RDF_PROPERTY);
        tded.add(RDFS_LABEL, UNECE_TDED_PROPERTY_NAME);
        tded.add(RDFS_COMMENT, "TDED reference number");

        jsonldVocabulary.getGraphJsonArrayBuilder().add(tded.build());

        JsonObjectBuilder cefactUNId = Json.createObjectBuilder();
        cefactUNId.add(ID, UNECE_CEFACT_UN_ID_PROPERTY);
        cefactUNId.add(TYPE, RDF_PROPERTY);
        cefactUNId.add(RDFS_LABEL, UNECE_CEFACT_UN_ID_PROPERTY_NAME);
        cefactUNId.add(RDFS_COMMENT, "TBG assigned Unique ID for approved Library Objects");
        jsonldVocabulary.getGraphJsonArrayBuilder().add(cefactUNId.build());

        JsonObjectBuilder cefactBusinessProcess = Json.createObjectBuilder();
        cefactBusinessProcess.add(ID, UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY);
        cefactBusinessProcess.add(TYPE, RDF_PROPERTY);
        cefactBusinessProcess.add(RDFS_LABEL, UNECE_CEFACT_BUSINESS_PROCESS_PROPERTY_NAME);
        cefactBusinessProcess.add(RDFS_COMMENT, "CEFACT Business Process");
        jsonldVocabulary.getGraphJsonArrayBuilder().add(cefactBusinessProcess.build());

        JsonObjectBuilder cefactElementMetadata = Json.createObjectBuilder();
        cefactElementMetadata.add(ID, UNECE_CEFACT_ELEMENT_METADATA_PROPERTY);
        cefactElementMetadata.add(TYPE, RDF_SEQ);
        cefactElementMetadata.add(RDFS_LABEL, UNECE_CEFACT_ELEMENT_METADATA_PROPERTY_NAME);
        cefactElementMetadata.add(RDFS_COMMENT, "CEFACT Element Metadata");
        jsonldVocabulary.getGraphJsonArrayBuilder().add(cefactElementMetadata.build());

        JsonObjectBuilder cefactBieDomainClass = Json.createObjectBuilder();
        cefactBieDomainClass.add(ID, UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY);
        cefactBieDomainClass.add(TYPE, RDF_PROPERTY);
        cefactBieDomainClass.add(RDFS_LABEL, UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY_NAME);
        cefactBieDomainClass.add(RDFS_COMMENT, "CEFACT Business Process");
        jsonldVocabulary.getGraphJsonArrayBuilder().add(cefactBieDomainClass.build());

        for (String qdt : qdtMap.keySet()) {
            JsonObjectBuilder qdtClass = Json.createObjectBuilder();
            qdtClass.add(ID, StringUtils.join(UNECE_NS, ":", qdt));
            qdtClass.add(TYPE, RDFS_CLASS);
            qdtClass.add(RDFS_LABEL, qdt);
            qdtClass.add(RDFS_COMMENT, qdtMap.get(qdt));
            jsonldVocabulary.getGraphJsonArrayBuilder().add(qdtClass.build());

            if (unitCodeTypes.contains(qdt)){
                String valueId = qdt.concat("Value");
                JsonObjectBuilder unitCodeValueProperty = Json.createObjectBuilder();
                unitCodeValueProperty.add(ID, StringUtils.join(UNECE_NS, ":", valueId));
                unitCodeValueProperty.add(TYPE, RDF_PROPERTY);
                unitCodeValueProperty.add(RDFS_LABEL, valueId);
                unitCodeValueProperty.add(RDFS_COMMENT, "The numeric value.");
                unitCodeValueProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(XSD_NS, ":", "decimal")
                )));
                unitCodeValueProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(UNECE_NS, ":", qdt)
                )));
                jsonldVocabulary.getGraphJsonArrayBuilder().add(unitCodeValueProperty.build());

                JsonArrayBuilder typeArray = Json.createArrayBuilder();
                typeArray.add(RDF_PROPERTY);
                typeArray.add(StringUtils.join(OWL_NS, ":", "ObjectProperty"));

                String codeId = qdt.concat("Code");
                String unitCodeCodePropertyRange = qdt;
                if (unitCodeCodePropertyRange.contains("Measure")){
                    unitCodeCodePropertyRange = qdt.replace("MeasureType", "MeasureCode");
                } else if (unitCodeCodePropertyRange.contains("Quantity")) {
                    unitCodeCodePropertyRange = qdt.replace("QuantityType", "QuantityCode");
                } else {
                    System.out.println(unitCodeCodePropertyRange);
                }
                JsonObjectBuilder unitCodeCodeProperty = Json.createObjectBuilder();
                unitCodeCodeProperty.add(ID, StringUtils.join(UNECE_NS, ":", codeId));
                unitCodeCodeProperty.add(TYPE, typeArray);
                unitCodeCodeProperty.add(RDFS_LABEL, codeId);
                unitCodeCodeProperty.add(RDFS_COMMENT, "The unit code.");
                unitCodeCodeProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(UNECE_NS, ":", unitCodeCodePropertyRange)
                )));
                unitCodeCodeProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(UNECE_NS, ":", qdt)
                )));
                jsonldVocabulary.getGraphJsonArrayBuilder().add(unitCodeCodeProperty.build());

                JsonObjectBuilder typeClass = Json.createObjectBuilder();
                typeClass.add(ID, StringUtils.join(UNECE_NS, ":", unitCodeCodePropertyRange));
                typeClass.add(TYPE, RDFS_CLASS);
                typeClass.add(RDFS_LABEL, unitCodeCodePropertyRange);
                typeClass.add(RDFS_COMMENT, String.format("RDF Class for %s unit code type to define unit code values.", qdt));
                jsonldVocabulary.getGraphJsonArrayBuilder().add(typeClass.build());
            }
            else if (amountTypes.contains(qdt)) {
                String valueId = qdt.concat("Value");
                JsonObjectBuilder unitCodeValueProperty = Json.createObjectBuilder();
                unitCodeValueProperty.add(ID, StringUtils.join(UNECE_NS, ":", valueId));
                unitCodeValueProperty.add(TYPE, RDF_PROPERTY);
                unitCodeValueProperty.add(RDFS_LABEL, valueId);
                unitCodeValueProperty.add(RDFS_COMMENT, "A number of monetary units.");
                unitCodeValueProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(XSD_NS, ":", "decimal")
                )));
                unitCodeValueProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(UNECE_NS, ":", qdt)
                )));

                jsonldVocabulary.getGraphJsonArrayBuilder().add(unitCodeValueProperty.build());

                JsonArrayBuilder typeArray = Json.createArrayBuilder();
                typeArray.add(RDF_PROPERTY);
                typeArray.add(StringUtils.join(OWL_NS, ":", "ObjectProperty"));

                String codeId = qdt.concat("Currency");
                String unitCodeCodePropertyRange = qdt;
                if (unitCodeCodePropertyRange.contains("AmountType")){
                    unitCodeCodePropertyRange = qdt.replace("AmountType", "AmountCurrency");
                } else {
                    System.out.println(unitCodeCodePropertyRange);
                }
                JsonObjectBuilder unitCodeCodeProperty = Json.createObjectBuilder();
                unitCodeCodeProperty.add(ID, StringUtils.join(UNECE_NS, ":", codeId));
                unitCodeCodeProperty.add(RDFS_LABEL, codeId);
                unitCodeCodeProperty.add(RDFS_COMMENT, "An amount currency code.");
                unitCodeCodeProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(UNECE_NS, ":", unitCodeCodePropertyRange)
                )));
                unitCodeCodeProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(UNECE_NS, ":", qdt)
                )));
                jsonldVocabulary.getGraphJsonArrayBuilder().add(unitCodeCodeProperty.build());

                JsonObjectBuilder typeClass = Json.createObjectBuilder();
                typeClass.add(ID, StringUtils.join(UNECE_NS, ":", unitCodeCodePropertyRange));
                typeClass.add(TYPE, RDFS_CLASS);
                typeClass.add(RDFS_LABEL, unitCodeCodePropertyRange);
                typeClass.add(RDFS_COMMENT, String.format("RDF Class for %s amount type to define currency codes.", qdt));
                jsonldVocabulary.getGraphJsonArrayBuilder().add(typeClass.build());
            }

        }


        jsonldContext.getContextObjectBuilder().add(UNECE_NS, NS_MAP.get(UNECE_NS));
        jsonldContext.getContextObjectBuilder().add(RDF_NS, NS_MAP.get(RDF_NS));
        jsonldContext.getContextObjectBuilder().add(RDFS_NS, NS_MAP.get(RDFS_NS));
        jsonldContext.getContextObjectBuilder().add(XSD_NS, NS_MAP.get(XSD_NS));
        jsonldContext.getContextObjectBuilder().add(OWL_NS, NS_MAP.get(OWL_NS));
        jsonldContext.getContextObjectBuilder().add(SCHEMA_NS, NS_MAP.get(SCHEMA_NS));
        jsonldContext.getContextObjectBuilder().add("id", ID);
        jsonldContext.getContextObjectBuilder().add("type", TYPE);
        objectBuilder = Json.createObjectBuilder(Map.of(TYPE, ID));
        jsonldContext.getContextObjectBuilder().add(UNECE_CEFACT_BIE_DOMAIN_CLASS_PROPERTY_NAME, objectBuilder.build());
        for (String key : propertiesGraph.keySet()) {
            JsonObject property = propertiesGraph.get(key);
            String propertyType = property.getJsonObject(SCHEMA_RANGE_INCLUDES).getString(ID);
            Map<String, String> map;
            JsonObjectBuilder propertyObjectBuilder = Json.createObjectBuilder(Map.of(ID, StringUtils.join(UNECE_NS, ":", key)));
            //TODO: fix type check
            if (propertyType.startsWith(StringUtils.join(UNECE_NS, ":", "UNCL")) && propertyType.endsWith("Code")) {
                propertyObjectBuilder.add(TYPE, "@vocab");
            } else if (propertyType.startsWith(StringUtils.join(UNECE_NS, ":"))) {
                propertyObjectBuilder.add(TYPE, ID);
            } else {
                propertyObjectBuilder.add(TYPE, propertyType);
            }
            jsonldContext.getContextObjectBuilder().add(key, propertyObjectBuilder.build());
        }
        for (String key : classesGraph.keySet()) {
            jsonldContext.getContextObjectBuilder().add(key, Json.createObjectBuilder(Map.of(ID, StringUtils.join(UNECE_NS, ":", key))).build());
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


        System.out.println("Code lists mapping:");
        ArrayList<String> codeClassList = new ArrayList<>(codeListMapping.keySet());
        Collections.sort(codeClassList);
        for (String codeClass:codeClassList){
            if (StringUtils.join("codelists/", codeClass).equalsIgnoreCase(codeListMapping.get(codeClass))) {
                System.out.println(String.format("%s - %s", codeClass, codeListMapping.get(codeClass)));
            }
        }
    }

    Object getData(String dataType, Set<String> TDED) {
        //TODO: add a sanity check for TDED and dataType
        JsonObjectBuilder result = Json.createObjectBuilder();
        TreeSet<String> filteredTDED = new TreeSet<>();
        /*for (String item : TDED) {
            if (item.length() > 4) {
                item = item.substring(1, 5);
            }
            if (Entity.codes.contains(item)) {
                filteredTDED.add(item);
            } *//*else {
                filteredTDED.add(item.concat("-missing"));
            }*//*
        }*/

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
                        result = result.add(ID, StringUtils.join(XSD_NS, ":", "boolean"));
                        break;
                    case IDENTIFIER:
                    case ID:
                    case CODE:
                        result = result.add(ID, StringUtils.join(XSD_NS, ":", "token"));
                        break;
                    case TEXT:
                    case VALUE:
                    case TYPE:
                    default:
                        result = result.add(ID, StringUtils.join(XSD_NS, ":", "string"));
                        break;
                    case DATETIME:
                        result = result.add(ID, StringUtils.join(XSD_NS, ":", "dateTime"));
                        break;
                    case AMOUNT:
                    case PERCENT:
                    case RATE:
                    case QUANTITY:
                    case NUMERIC:
                    case MEASURE:
                        result = result.add(ID, StringUtils.join(XSD_NS, ":", "decimal"));
                        break;
                    case DATE:
                        result = result.add(ID, StringUtils.join(XSD_NS, ":", "date"));
                    case BINARYOBJECT:
                    case GRAPHIC:
                    case PICTURE:
                    case VIDEO:
                    case SOUND:
                        result = result.add(ID, StringUtils.join(XSD_NS, ":", "base64Binary"));
                        break;
                    case TIME:
                        result = result.add(ID, StringUtils.join(XSD_NS, ":", "time"));
                        break;
                }

            } catch (IllegalArgumentException e) {
                System.out.println(String.format("Check data type %s", dataType));
                if (dataType.equalsIgnoreCase("Id"))
                    result = result.add(ID, StringUtils.join(XSD_NS, ":", "token"));
                else
                    result = result.add(ID, StringUtils.join(XSD_NS, ":", "string"));
            }
        }
        return result;
    }

    public static String stripReferencedPrefix(String input) {
        if (input.startsWith("Referenced")) {
            input = StringUtils.substringAfter(input, "Referenced");
        }
        if (input.startsWith("_")) {
            input = StringUtils.substringAfter(input, "_");
        }
        return input;
    }

    public void readDTs(Map<String, JsonObject> pdts, Map<String, JsonObject> udts, Map<String, JsonObject> qdts) {
        for (String key: pdts.keySet()) {
            rangeDataTypeMap.put(key, getJsonLDPDT(pdts.get(key)));
        }

        for (String key: udts.keySet()) {
            JsonObject udt = udts.get(key);
            String title = udt.getString("title");
            String description = null;
            JsonObject properties = null;
            if (udt.containsKey("description")){
                description = udt.getString("description");
            }
            if (udt.containsKey("type")){
                String type = udt.getString("type");
                if (!type.equalsIgnoreCase("object")){
                    rangeDataTypeMap.put(key, getJsonLDPDT(udt));

                }
            }
            if (udt.containsKey("properties")){
                properties = udt.getJsonObject("properties");
                if (properties.containsKey("unitCode") ||
                        properties.containsKey("currencyId")/* ||
                        properties.containsKey("languageId") ||
                        properties.containsKey("format")*/){
                    String udtTitle = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, StringUtils.substringAfterLast(key, "/"));
                    rangeObjectTypeMap.put(key,
                            StringUtils.join(UNECE_NS, ":", udtTitle));
                    description = description!=null?description:"Missing description";
                    if (qdtMap.containsKey(udtTitle) && !qdtMap.get(udtTitle).equalsIgnoreCase(description)) {
                        System.err.println(
                                String.format("repeated qdt %s with different descriptions - '%s' and '%s'",
                                        udtTitle, qdtMap.get(udtTitle), description));
                    } else {
                        qdtMap.put(udtTitle, description);
                    }

                    if (properties.containsKey("unitCode")){
                        unitCodeTypes.add(udtTitle);
                    }
                    else if (properties.containsKey("currencyId")){
                        amountTypes.add(udtTitle);
                        String ref = properties.getJsonObject("currencyId").getString("$ref");
                        String refPath = StringUtils.substringBefore(ref, ".json#/");
                        String jsonPath = StringUtils.substringAfter(ref, ".json#/");

                        readCodelistToJSONLD(refPath, jsonPath, null,
                                description, udtTitle.replace("AmountType", "AmountCurrency"));
                    }
                    //TODO: add class and properties generation
                }
                else if (properties.containsKey("content")){
                    String content = properties.getJsonObject("content").getString("$ref");
                    if (content.startsWith("#/$defs/pdt/$defs/")){
                        content = content.replace("#/", "UNECE-BasicComponents.json#/");
                        rangeDataTypeMap.put(key, rangeDataTypeMap.get(content));
                    }
                }
            }
        }
        for (String key: udts.keySet()) {
            JsonObject udt = udts.get(key);
            if (udt.containsKey("$ref")) {
                String ref = udt.getString("$ref");
                ref = ref.replace("#/","UNECE-BasicComponents.json#/");
                if (rangeDataTypeMap.containsKey(ref)){
                    rangeDataTypeMap.put(key, rangeDataTypeMap.get(ref));
                } else if (rangeObjectTypeMap.containsKey(ref)){
                    rangeObjectTypeMap.put(key, rangeObjectTypeMap.get(ref));
                }
            }
        }
        Set<String> qdtCOntentRef = new HashSet<>();
        for (String key: qdts.keySet()) {
            JsonObject qdt = qdts.get(key);
            String title = qdt.getString("title");
            String description = null;
            JsonObject properties = null;
            if (qdt.containsKey("description")){
                description = qdt.getString("description");
            }
            if (qdt.containsKey("type")){
                String type = qdt.getString("type");
                if (!type.equalsIgnoreCase("object")){
                    rangeDataTypeMap.put(key, getJsonLDPDT(qdt));
                }
            }
            if (qdt.containsKey("properties")){
                properties = qdt.getJsonObject("properties");
                if (properties.containsKey("unitCode") ||
                        properties.containsKey("currencyId")/* ||
                        properties.containsKey("languageId") ||
                        properties.containsKey("format")*/){
                    String qdtTitle = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, StringUtils.substringAfterLast(key, "/"));

                    rangeObjectTypeMap.put(key,
                            StringUtils.join(UNECE_NS, ":", qdtTitle
                            ));
                    if (properties.get("unitCode")!= null){
                        unitCodeTypes.add(qdtTitle);
                        String ref = properties.getJsonObject("unitCode").getString("$ref");
                        String refPath = StringUtils.substringBefore(ref, ".json#/");
                        String jsonPath = StringUtils.substringAfter(ref, ".json#/");

                        readCodelistToJSONLD(refPath, jsonPath, null,
                                description, qdtTitle.replace("MeasureType", "MeasureCode"));
                    } else if (properties.get("currencyId")!=null){
                        amountTypes.add(qdtTitle);
                        String ref = properties.getJsonObject("currencyId").getString("$ref");
                        String refPath = StringUtils.substringBefore(ref, ".json#/");
                        String jsonPath = StringUtils.substringAfter(ref, ".json#/");

                        readCodelistToJSONLD(refPath, jsonPath, null,
                                description, qdtTitle.replace("AmountType", "AmountCurrency"));

                    }

                    if (qdtMap.containsKey(qdtTitle) && !qdtMap.get(qdtTitle).equalsIgnoreCase(description)) {
                        System.err.println(
                                String.format("repeated qdt %s with different descriptions - '%s' and '%s'",
                                        qdtTitle, qdtMap.get(qdtTitle), description));
                    } else {
                        qdtMap.put(qdtTitle, description);
                    }

                    //TODO: add class and properties generation
                }
                else if (properties.containsKey("content")){
                    JsonObject content = properties.getJsonObject("content");
                    JsonArray allOfArray = null;
                    String ref = null;
                    if (content.containsKey("allOf")) {
                        Iterator<JsonValue> iterator = content.getJsonArray("allOf").iterator();
                        while (iterator.hasNext()) {
                            JsonObject allOffObject = iterator.next().asJsonObject();
                            if (allOffObject.containsKey("$ref")) {
                                ref = allOffObject.getString("$ref");
                            } else if (allOffObject.containsKey("enum")) {
                                allOfArray = allOffObject.getJsonArray("enum");
                            }
                        }
                    }
                    if (content.containsKey("$ref")) {
                        ref = content.getString("$ref");
                    }
                    if (ref!=null){
                        if (ref.startsWith("codelist")){
                            String qdtTitle = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, StringUtils.substringAfterLast(key, "/"));
                            if (qdtTitle.endsWith("Type")){
                                qdtTitle = StringUtils.substringBeforeLast(qdtTitle, "Type");
                            }
                            if (qdtTitle.endsWith("Code")){
                                qdtTitle = qdtTitle.concat("List");
                            }
                            rangeObjectTypeMap.put(key,
                                    StringUtils.join(UNECE_NS, ":", qdtTitle
                                    ));

                            String refPath = StringUtils.substringBefore(ref, ".json#/");
                            String jsonPath = StringUtils.substringAfter(ref, ".json#/");


                            if (qdtMap.containsKey(qdtTitle) && !qdtMap.get(qdtTitle).equalsIgnoreCase(description)) {
                                System.err.println(
                                        String.format("repeated qdt %s with different descriptions - '%s' and '%s'",
                                                qdtTitle, qdtMap.get(qdtTitle), description));
                            } else {
                                qdtMap.put(qdtTitle, description);
                            }
                            readCodelistToJSONLD(refPath, jsonPath, allOfArray,
                                    description, qdtTitle);


                        } else if (ref.startsWith("UNECE-BasicComponents.json#")){
                            if (rangeDataTypeMap.containsKey(ref)){
                                rangeDataTypeMap.put(key, rangeDataTypeMap.get(ref));
                            } else if (rangeObjectTypeMap.containsKey(ref)){
                                rangeObjectTypeMap.put(key, rangeObjectTypeMap.get(key));
                            } else {
                                System.err.println(String.format("Unexpected ref %s", ref));
                            }
                        }
                    }
                }
            }
        }
    }

    public static String getJsonLDPDT(JsonObject pdt) {
        String result = null;
        switch (pdt.getString("type")){
            case "string":
                if (pdt.containsKey("contentEncoding")){
                    switch (pdt.getString("contentEncoding")) {
                        case "base64":
                            result = StringUtils.join(XSD_NS, ":", "base64Binary");
                            break;
                        case "hex":
                            result = StringUtils.join(XSD_NS, ":", "hexBinary");
                            break;
                    }
                } else {
                    result = StringUtils.join(XSD_NS, ":", "string");
                }
                break;
            case "boolean":
                result = StringUtils.join(XSD_NS, ":", "boolean");
                break;
            case "integer":
                result = StringUtils.join(XSD_NS, ":", "integer");
                break;
        }
        return result;
    }

    protected void readCodelistToJSONLD(String refPath, String jsonPath, JsonArray allOfArray,
                                        String description, String qdtTitle   ) {
        InputStream fis = null;
        try {
            if (inputFile == null) {
                fis = getClass().getResourceAsStream("/D22A/".concat(refPath).concat(".json"));
            } else {
                //TODO: parse bath path from input file
                fis = new FileInputStream(inputFile);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        JsonReader reader = Json.createReader(fis);
        JsonObject codeList = reader.readObject();
        reader.close();
        for (String path : StringUtils.split(jsonPath, "/")) {
            codeList = codeList.getJsonObject(path);
        }
        String codeListId = StringUtils.substringAfterLast(jsonPath, "/");
        codeListId = cleanUp(codeListId);
        if (allOfArray != null) {
            description = description.concat(String.format(" Subset of %s.", codeListId));
            if (qdtTitle.endsWith("Code")){
                codeListId = StringUtils.substringBeforeLast(qdtTitle, "Code");
            } else {
                codeListId = qdtTitle;
            }

        }
        if (codeListId.endsWith("Type")) {
            codeListId = StringUtils.substringBeforeLast(codeListId, "Type");
        }
        else if (codeListId.endsWith("CodeList")) {
            codeListId = StringUtils.substringBeforeLast(codeListId, "CodeList");
        }

        if (codeListId.equalsIgnoreCase(qdtTitle)){
            System.out.println(String.format("Code list class and value ids are equals for %s", codeListId));
            if (codeListId.endsWith("Code")) {
                codeListId = StringUtils.substringBeforeLast(codeListId, "Code");
            }
        }
        JSONLDVocabulary codeListVocabulary = new JSONLDVocabulary();
        if (codeList.containsKey("oneOf")) {
            JsonArray oneOfArray = codeList.getJsonArray("oneOf");
            Iterator<JsonValue> iterator = oneOfArray.iterator();
            Map<String, String> consts = new HashMap();
            while (iterator.hasNext()) {
                JsonObject oneOfItem = iterator.next().asJsonObject();
                String oneOfId = oneOfItem.getString("const");
                String oneOfTitle = oneOfItem.getString("title");
                boolean skip = false;
                if (consts.containsKey(oneOfId)) {
                    skip = true;
/*                                                    System.err.println(String.format("Code list %s already has the constant %s with the title '%s'. " +
                                                            "Constant with the tile '%s' will be ignored.", codeListId, oneOfId, consts.get(oneOfId), oneOfTitle));*/
                } else {
                    consts.put(oneOfId, oneOfTitle);
                }
                if (skip)
                    continue;
                if (allOfArray != null) {
                    skip = true;
                    Iterator<JsonValue> jsonValueIterator = allOfArray.iterator();
                    while (jsonValueIterator.hasNext()) {
                        JsonValue value = jsonValueIterator.next();
                        if (value.getValueType() == JsonValue.ValueType.STRING) {
                            String stringValue = ((JsonString) value).getString();
                            if (stringValue.equalsIgnoreCase(oneOfId)) {
                                skip = false;
                            }
                        }
                    }

                    if (skip)
                        continue;
                }
                JsonObjectBuilder oneOfObject = Json.createObjectBuilder();
                String id = StringUtils.join(UNECE_NS, ":", qdtTitle, "#", oneOfId);
                oneOfObject.add(ID, id);
                oneOfObject.add(TYPE, StringUtils.join(UNECE_NS, ":", qdtTitle));
                oneOfObject.add(RDF_VALUE, oneOfId);
                oneOfObject.add(RDFS_COMMENT, oneOfTitle);
                codeValuesGraph.put(id, oneOfObject.build());
            }
        }
        if (codeListId.equalsIgnoreCase("string")) {
            System.out.println(qdtTitle);
        }


    }
}
