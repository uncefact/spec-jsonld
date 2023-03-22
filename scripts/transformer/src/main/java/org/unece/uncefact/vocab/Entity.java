package org.unece.uncefact.vocab;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class Entity {
    /**
     * Codes list is being checked and if it TDED of the property exists in it, Data Type is used to generate the JSON LD property name.
     *
     * Note for the issue: https://github.com/uncefact/vocab/issues/52
     * Data type qualifier for properties with TDED 2379 is being ignored (removed from the list).
     * Because properties with Date Time representation term are expected to be always formatted
     * and no need to mention it in the property name.
     */
    public static List<String> codes = Arrays.asList(new String[]{"1001", "1049", "1073", "1153", "1159", "1225", "1227", "1229", "1373", "1501", "1503", "1505", "1507", "2005", "2009", "2013", "2015", "2017", "2023", "2151", "2155", "2475", "3035", "3045", "3055", "3077", "3079", "3083", "3131", "3139", "3153", "3155", "3207", "3227", "3237", "3279", "3285", "3289", "3295", "3299", "3301", "3397", "3401", "3403", "3405", "3455", "3457", "3475", "3477", "3479", "3493", "4017", "4025", "4027", "4035", "4037", "4043", "4049", "4051", "4053", "4055", "4059", "4065", "4071", "4079", "4153", "4183", "4215", "4219", "4221", "4233", "4237", "4277", "4279", "4295", "4343", "4347", "4383", "4401", "4403", "4405", "4407", "4419", "4431", "4435", "4437", "4439", "4447", "4451", "4453", "4455", "4457", "4461", "4463", "4465", "4471", "4475", "4487", "4493", "4499", "4501", "4503", "4505", "4507", "4509", "4511", "4513", "4517", "4525", "5007", "5013", "5025", "5027", "5039", "5047", "5049", "5125", "5153", "5189", "5213", "5237", "5243", "5245", "5249", "5261", "5267", "5273", "5283", "5305", "5307", "5315", "5375", "5379", "5387", "5393", "5419", "5463", "5495", "5501", "6029", "6063", "6069", "6071", "6077", "6079", "6085", "6087", "6113", "6145", "6155", "6167", "6173", "6245", "6311", "6313", "6321", "6331", "6341", "6343", "6347", "6353", "6415", "7001", "7007", "7009", "7011", "7039", "7041", "7045", "7047", "7059", "7073", "7075", "7077", "7081", "7083", "7085", "7111", "7133", "7139", "7143", "7161", "7171", "7173", "7187", "7233", "7273", "7293", "7295", "7297", "7299", "7365", "7383", "7405", "7429", "7431", "7433", "7449", "7451", "7455", "7459", "7491", "7493", "7495", "7497", "7511", "7515", "8015", "8025", "8035", "8051", "8053", "8077", "8101", "8155", "8169", "8179", "8249", "8273", "8281", "8323", "8335", "8339", "8341", "8393", "8395", "8457", "8459", "9003", "9013", "9015", "9017", "9023", "9025", "9029", "9031", "9035", "9037", "9039", "9043", "9045", "9051", "9141", "9143", "9153", "9161", "9169", "9175", "9213", "9285", "9303", "9353", "9411", "9415", "9417", "9421", "9437", "9441", "9443", "9447", "9453", "9501", "9507", "9509", "9601", "9623", "9625", "9635", "9641", "9643", "9645", "9649"});
    String id;
    String type;
    String name;
    String description;
    String objectClassTermQualifier;
    String objectClassTerm;
    String propertyTermQualifier;
    String propertyTerm;
    String dataTypeQualifier;
    String representationTerm;
    String qualifiedDataTypeId;
    String associatedObjectClassTermQualifier;
    String associatedObjectClass;
    String businessTerm;
    String context;
    String publicationComment;
    String TDED;
    String rangeIncludes;

    String schemaName;
    Set<String> properties;

    static int ruleCount[] = new int[10];
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.replaceAll(" ", "");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getObjectClassTermQualifier() {
        return objectClassTermQualifier;
    }

    public void setObjectClassTermQualifier(String objectClassTermQualifier) {
        this.objectClassTermQualifier = StringUtils.defaultIfEmpty(objectClassTermQualifier.replaceAll(" ", ""), "");
    }

    public String getObjectClassTerm() {
        return objectClassTerm;
    }

    public void setObjectClassTerm(String objectClassTerm) {
        this.objectClassTerm = objectClassTerm.replaceAll(" ", "");
    }

    public String getPropertyTermQualifier() {
        return propertyTermQualifier;
    }

    public void setPropertyTermQualifier(String propertyTermQualifier) {
        this.propertyTermQualifier = StringUtils.defaultIfEmpty(propertyTermQualifier.replaceAll(" ", ""), "");
        ;
    }

    public String getPropertyTerm() {
        return propertyTerm;
    }

    public void setPropertyTerm(String propertyTerm) {
        this.propertyTerm = propertyTerm.replaceAll(" ", "");
    }

    public String getDataTypeQualifier() {
        return dataTypeQualifier;
    }

    public void setDataTypeQualifier(String dataTypeQualifier) {
        this.dataTypeQualifier = dataTypeQualifier.replaceAll(" ", "");
    }

    public String getRepresentationTermForNDRRules() {
        if (representationTerm == null){
            return "";
        }
        if ( representationTerm.equalsIgnoreCase("Text")) {
            List<String> exceptions = Arrays.asList(
                    new String[]{"deliveryinstructions"});
            if (getPropertyTerm() != null && exceptions.contains(getPropertyTerm().toLowerCase()))
                return representationTerm;
            else
                return "";
        } else if (representationTerm.equalsIgnoreCase("Identifier")) {
            return "Id";
        } else {
            return representationTerm;
        }
    }

    public String getRepresentationTerm() {
        return representationTerm;
    }

    public void setRepresentationTerm(String representationTerm) {
        this.representationTerm = representationTerm.replaceAll(" ", "");
    }

    public String getQualifiedDataTypeId() {
        return qualifiedDataTypeId;
    }

    public void setQualifiedDataTypeId(String qualifiedDataTypeId) {
        this.qualifiedDataTypeId = qualifiedDataTypeId.replaceAll(" ", "");
    }

    public String getAssociatedObjectClassTermQualifier() {
        return associatedObjectClassTermQualifier;
    }

    public void setAssociatedObjectClassTermQualifier(String associatedObjectClassTermQualifier) {
        this.associatedObjectClassTermQualifier = StringUtils.defaultIfEmpty(associatedObjectClassTermQualifier.replaceAll(" ", ""), "");
        ;
    }

    public String getAssociatedObjectClass() {
        return associatedObjectClass;
    }

    public void setAssociatedObjectClass(String associatedObjectClass) {
        this.associatedObjectClass = StringUtils.defaultIfEmpty(associatedObjectClass.replaceAll(" ", ""), "");
        ;
    }

    public String getBusinessTerm() {
        return businessTerm;
    }

    public void setBusinessTerm(String businessTerm) {
        this.businessTerm = businessTerm;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = StringUtils.defaultIfEmpty(context, "");
    }

    public String getPublicationComment() {
        return publicationComment;
    }

    public void setPublicationComment(String publicationComment) {
        this.publicationComment = StringUtils.defaultIfEmpty(publicationComment, "");
    }

    public String getClassTermWithQualfier() {
        return this.objectClassTermQualifier.concat(this.objectClassTerm);
    }

    public String getAssociatedClassTermWithQualifier() {
        return StringUtils.defaultIfEmpty(this.associatedObjectClassTermQualifier,"").concat(this.associatedObjectClass);
    }

    public String getPropertyTermWithQualifier() {
        return this.propertyTermQualifier.concat(this.propertyTerm);
    }

    public String getPropertyTermWithQualifierForNDRRules() {
        /**
         * fix for the issue: https://github.com/uncefact/vocab/issues/48
         * Identification word is being amended when a representation term is Identifier (which is shortened to Id)
         */
        if (StringUtils.equalsIgnoreCase(propertyTerm,"Identification") && StringUtils.equalsIgnoreCase(representationTerm,"Identifier")) {
            return propertyTermQualifier;
        }
        /**
         * fix for the issue: https://github.com/uncefact/vocab/issues/52
         * If representation term is DateTime the value assumed to be always formatted so no need to include "formatted" word into the property name
         * We strip it off from property term qualifier
         */
        else if (StringUtils.startsWithIgnoreCase(propertyTermQualifier, "Formatted")
                && StringUtils.equalsIgnoreCase(representationTerm, "DateTime")) {
            return StringUtils.join(StringUtils.substringAfter(propertyTermQualifier,"Formatted").trim(),propertyTerm);
        }

        return StringUtils.join(propertyTermQualifier,propertyTerm);
    }

    // This gets the name of the object in the JSON LD vocab
    public String getPropertyKey() {
        int ruleIndex = 0;
        String propertyKey = getRepresentationTermForNDRRules();
        if (StringUtils.isBlank(getTDED()) || !codes.contains(getTDED())) {
            propertyKey = StringUtils.join(getPropertyTermWithQualifierForNDRRules(), propertyKey);
            if (StringUtils.isBlank(getPropertyTermQualifier())){
                if(!StringUtils.isBlank(getDataTypeQualifier())) {
                    if (!StringUtils.contains(getDataTypeQualifier(), getPropertyTerm())) {
                        //if the property term isn't a part of the DTQ, both of them are used
                        propertyKey = filterDuplicatesOut(getDataTypeQualifier(), getPropertyTermWithQualifierForNDRRules(), propertyKey);
                        ruleIndex = 1;
                    } else {
                        if (StringUtils.equalsIgnoreCase(getDataTypeQualifier(), getPropertyTerm())) {
                            //if the property term equals to the DTQ, the object class data is being used
                            //to distinct it from a property with the same key, but no DTQ defined
                            //check UN01002112 for example
                            propertyKey = filterDuplicatesOut(getObjectClassTermQualifier(), getObjectClassTerm(), getDataTypeQualifier(), propertyKey);
                            ruleIndex = 2;
                        }
                        else {
                            //otherwise use DTQ to distinct properties from ones with the same property term
                            propertyKey = filterDuplicatesOut(getDataTypeQualifier(), propertyKey);
                            ruleIndex = 3;
                        }
                    }
                }
            }
            // https://github.com/uncefact/spec-jsonld/issues/144#issuecomment-1333493717
            // as type is widely used as an alias for @type in JSON-LD
            // it was agreed to rename split `type` property
            if (propertyKey.equalsIgnoreCase("type")) {
                propertyKey = StringUtils.join(getObjectClassTerm(), propertyKey);
                ruleIndex = 4;
            } else if (!StringUtils.isBlank(getAssociatedObjectClass())) {
                propertyKey = StringUtils.join(propertyKey, getAssociatedObjectClass());
                ruleIndex = 5;
            }
        }
        else {
            // Checking for data type qualifiers (DTQs)
            if (StringUtils.isBlank(getDataTypeQualifier())) {
                // when the TDED is specified but the DTQ isn't, the object class data is being used to create a meaningful property key
                propertyKey = filterDuplicatesOut(getObjectClassTermQualifier(), getObjectClassTerm(), getPropertyTermWithQualifierForNDRRules(), propertyKey);
                ruleIndex = 6;
            } else {
                // If the DTQ is present
                if (!StringUtils.contains(getDataTypeQualifier(), getPropertyTerm())) {
                    //if the property term isn't a part of the DTQ, both of them are used
                    propertyKey = filterDuplicatesOut(getDataTypeQualifier(), getPropertyTermWithQualifierForNDRRules(), propertyKey);
                    ruleIndex = 1;
                } else {
                    if (StringUtils.equalsIgnoreCase(getDataTypeQualifier(), getPropertyTerm())) {
                        //if the property term equals to the DTQ, the object class data is being used
                        //to distinct it from a property with the same key, but no DTQ defined
                        //check UN01002112 for example
                        propertyKey = filterDuplicatesOut(getObjectClassTermQualifier(), getObjectClassTerm(), getDataTypeQualifier(), propertyKey);
                        ruleIndex = 2;
                    }
                    else {
                        //otherwise use DTQ to distinct properties from ones with the same property term
                        propertyKey = filterDuplicatesOut(getDataTypeQualifier(), propertyKey);
                        ruleIndex = 3;
                    }
                }
            }
        }
        ruleCount[ruleIndex] +=1;
        //remove duplicated words
        //TODO: replace with regex implementation
        String[] split = StringUtils.splitByCharacterTypeCamelCase(propertyKey);
        propertyKey = split[split.length-1];
        for (int i = split.length -2; i >=0; i--){
            if(!StringUtils.startsWithIgnoreCase(propertyKey, split[i]))
                propertyKey = StringUtils.join(split[i], propertyKey);
        }
        // https://github.com/uncefact/spec-jsonld/issues/144#issuecomment-1333493717
        // as id is widely used as an alias for @id in JSON-LD
        // it was agreed to rename `id` property to `identifier`
        if (propertyKey.equalsIgnoreCase("id")){
            propertyKey = "identifier";
        }
        return propertyKey;
    }

    static <T> String filterDuplicatesOut(T... elements){
        String[] array = (String[])elements;
        String result = array[array.length -1];
        for (int i = array.length -2; i >=0; i--){
            if(!StringUtils.startsWithIgnoreCase(result, array[i]))
                result = StringUtils.join(array[i], result);
        }
        return result;
    }

    public String getCefactBieDomainClass() {
        return (StringUtils.isNotBlank(getObjectClassTermQualifier()) ? getObjectClassTermQualifier().concat("_").concat(getObjectClassTerm()).concat(".Details") : getObjectClassTerm().concat(".Details")).replaceAll(" ", "");
    }

    public Set<String> getProperties() {
        return this.properties;
    }

    public void addProperty(String property) {
        if (this.properties == null) {
            this.properties = new TreeSet<>();
        }
        this.properties.add(property);
    }

    public String getTDED() {
        if (TDED == null)
            return "";
        if (TDED.length() > 4) {
            TDED = TDED.substring(1, 5);
        }
        return TDED;
    }

    public void setTDED(String TDED) {
        this.TDED = TDED;
    }

    public String getRangeIncludes() {
        return rangeIncludes;
    }

    public void setRangeIncludes(String rangeIncludes) {
        this.rangeIncludes = rangeIncludes;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public static void printRuleCounter(){
        System.out.println(ruleCount);
    }
}
