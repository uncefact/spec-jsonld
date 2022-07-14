package org.unece.uncefact.vocab.transformers;

import com.google.common.base.CaseFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.unece.uncefact.UNType;
import org.unece.uncefact.vocab.Entity;

import javax.json.*;
import java.io.*;
import java.util.*;

public class BSPToJSONLDVocabulary extends WorkBookTransformer {

    protected static String BBIE = "BBIE";
    protected static String ABIE = "ABIE";
    protected static String ASBIE = "ASBIE";
    protected static String SCHEMA_NS = "schema";
    protected static String SCHEMA_DOMAIN_INCLUDES = SCHEMA_NS+":domainIncludes";
    protected static String SCHEMA_RANGE_INCLUDES = SCHEMA_NS+":rangeIncludes";
    protected static String UNECE_ABIE = UNECE_NS+":AggregateBIE";
    protected static String UNECE_BBIE = UNECE_NS+":BasicBIE";
    protected static String UNECE_ASBIE = UNECE_NS+":AssociationBIE";
    protected static String UNECE_TDED = UNECE_NS+":TDED";
    protected static String UNECE_STATUS = UNECE_NS+":status";
    protected static String UNECE_CEFACT_UN_ID = UNECE_NS+":cefactUNId";
    protected static String UNECE_CEFACT_BUSINESS_PROCESS = UNECE_NS+":cefactBusinessProcess";
    protected static String UNECE_CEFACT_ELEMENT_METADATA = UNECE_NS+":cefactElementMetadata";
    protected static String UNECE_CEFACT_BIE_DOMAIN_CLASS = UNECE_NS+":cefactBieDomainClass";

    public BSPToJSONLDVocabulary(String inputFile, String outputFile, boolean prettyPrint) {
        super(inputFile, outputFile,prettyPrint);
    }

    protected void setContext (){
        super.setContext();

        contextObjectBuilder.add(SCHEMA_NS, "http://schema.org/");
        contextObjectBuilder.add(CEFACT_NS, "https://edi3.org/cefact#");
        contextObjectBuilder.add(XSD_NS, "http://www.w3.org/2001/XMLSchema#");
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add(TYPE, ID);
        contextObjectBuilder.add(UNECE_CEFACT_BIE_DOMAIN_CLASS, objectBuilder.build());
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
        Map<String, Set<Entity>> classesMap = new TreeMap<>();
        Map<String, Set<Entity>> propertiesMap = new TreeMap<>();
        for (Entity entity : vocabulary.values()) {
            if (entity.getType() == null) {
                entity.getName();
            } else if (entity.getType().equalsIgnoreCase(ABIE)) {
                Set<Entity> entities = new HashSet<>();
                if (classesMap.containsKey(entity.getObjectClassTerm())) {
                    entities = classesMap.get(entity.getObjectClassTerm());
                }
                entities.add(entity);
                classesMap.put(entity.getObjectClassTerm(), entities);
            } else if (entity.getType().equalsIgnoreCase(BBIE) || entity.getType().equalsIgnoreCase(ASBIE)) {
                Set<Entity> entities = new HashSet<>();
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
            graphJsonArrayBuilder.add(rdfClass);
        }

        for (String key : propertiesMap.keySet()) {
            String id = key;
            JsonObjectBuilder rdfProperty = Json.createObjectBuilder();
            rdfProperty.add(ID, StringUtils.join(UNECE_NS,":",id));
            rdfProperty.add(TYPE, RDF_PROPERTY);

            JsonArrayBuilder metadataJsonArrayBuilder = Json.createArrayBuilder();
            Set<Entity> entities = propertiesMap.get(key);
            String rangeBBIE = null;
            String rangeASBIE = null;
            TreeSet<String> domain = new TreeSet<>();
            TreeSet<String> comment = new TreeSet<>();
            TreeSet<String> tded = new TreeSet<>();
            for (Entity entity : entities) {
                JsonObjectBuilder metadata = Json.createObjectBuilder();
                metadata.add(ID, StringUtils.join(CEFACT_NS,":",entity.getName()));
                if (entity.getType().equalsIgnoreCase(BBIE)) {
                    metadata.add(TYPE, UNECE_BBIE);
                    rangeBBIE = entity.getRepresentationTerm();
                    if (StringUtils.isNotBlank(entity.getTDED())) {
                        metadata.add(UNECE_TDED, entity.getTDED());
                    }
                } else if (entity.getType().equalsIgnoreCase(ASBIE)) {
                    metadata.add(TYPE, UNECE_ASBIE);
                    rangeASBIE = entity.getAssociatedObjectClassTerm();
                }
                metadata.add(UNECE_CEFACT_UN_ID, entity.getId());
                metadata.add(UNECE_CEFACT_BIE_DOMAIN_CLASS, StringUtils.join(CEFACT_NS,":",entity.getClassKey()));
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
                    rdfProperty.add(SCHEMA_RANGE_INCLUDES, (JsonObjectBuilder) rangeIncludes);
                } else {
                    rdfProperty.add(SCHEMA_RANGE_INCLUDES, (JsonArrayBuilder) rangeIncludes);
                }
            } else {
                rdfProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder().add(ID, StringUtils.join(UNECE_NS,":",rangeASBIE)));
            }
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
            graphJsonArrayBuilder.add(rdfProperty);
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
                String item = String.format("unece:UNECECL%sCode", filteredTDED.iterator().next());
                return result.add(ID, item);
            } else {
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (String item : filteredTDED) {
                    arrayBuilder.add(Json.createObjectBuilder().add(ID, String.format("unece:UNECECL%sCode", item)));
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
