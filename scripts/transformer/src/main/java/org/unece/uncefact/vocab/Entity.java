package org.unece.uncefact.vocab;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Entity {
    static List<String> codes = Arrays.asList(new String[]{"1001", "1049", "1073", "1153", "1159", "1225", "1227", "1229", "1373", "1501", "1503", "1505", "1507", "2005", "2009", "2013", "2015", "2017", "2023", "2151", "2155", "2379", "2475", "3035", "3045", "3055", "3077", "3079", "3083", "3131", "3139", "3153", "3155", "3227", "3237", "3279", "3285", "3289", "3295", "3299", "3301", "3397", "3401", "3403", "3405", "3455", "3457", "3475", "3477", "3479", "3493", "4017", "4025", "4027", "4035", "4037", "4043", "4049", "4051", "4053", "4055", "4059", "4065", "4071", "4079", "4153", "4183", "4215", "4219", "4221", "4233", "4237", "4277", "4279", "4295", "4343", "4347", "4383", "4401", "4403", "4405", "4407", "4419", "4431", "4435", "4437", "4439", "4447", "4451", "4453", "4455", "4457", "4461", "4463", "4465", "4471", "4475", "4487", "4493", "4499", "4501", "4503", "4505", "4507", "4509", "4511", "4513", "4517", "4525", "5007", "5013", "5025", "5027", "5039", "5047", "5049", "5125", "5153", "5189", "5213", "5237", "5243", "5245", "5249", "5261", "5267", "5273", "5283", "5305", "5307", "5315", "5375", "5379", "5387", "5393", "5419", "5463", "5495", "5501", "6029", "6063", "6069", "6071", "6077", "6079", "6085", "6087", "6113", "6145", "6155", "6167", "6173", "6245", "6311", "6313", "6321", "6331", "6341", "6343", "6347", "6353", "6415", "7001", "7007", "7009", "7011", "7039", "7041", "7045", "7047", "7059", "7073", "7075", "7077", "7081", "7083", "7085", "7111", "7133", "7139", "7143", "7161", "7171", "7173", "7187", "7233", "7273", "7293", "7295", "7297", "7299", "7365", "7383", "7405", "7429", "7431", "7433", "7449", "7451", "7455", "7459", "7491", "7493", "7495", "7497", "7511", "7515", "8015", "8025", "8035", "8051", "8053", "8077", "8101", "8155", "8169", "8179", "8249", "8273", "8281", "8323", "8335", "8339", "8341", "8393", "8395", "8457", "8459", "9003", "9013", "9015", "9017", "9023", "9025", "9029", "9031", "9035", "9037", "9039", "9043", "9045", "9051", "9141", "9143", "9153", "9161", "9169", "9175", "9213", "9285", "9303", "9353", "9411", "9415", "9417", "9421", "9437", "9441", "9443", "9447", "9453", "9501", "9507", "9509", "9601", "9623", "9625", "9635", "9641", "9643", "9645", "9649"});
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
    String associatedObjectClassTerm;
    String businessTerm;
    String context;
    String publicationComment;
    String TDED;
    Set<String> properties;

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
        this.objectClassTermQualifier = StringUtils.defaultIfEmpty(objectClassTermQualifier, "");
    }

    public String getObjectClassTerm() {
        return objectClassTerm;
    }

    public void setObjectClassTerm(String objectClassTerm) {
        this.objectClassTerm = objectClassTerm;
    }

    public String getPropertyTermQualifier() {
        return propertyTermQualifier;
    }

    public void setPropertyTermQualifier(String propertyTermQualifier) {
        this.propertyTermQualifier = StringUtils.defaultIfEmpty(propertyTermQualifier, "");
        ;
    }

    public String getPropertyTerm() {
        return propertyTerm;
    }

    public void setPropertyTerm(String propertyTerm) {
        this.propertyTerm = propertyTerm;
    }

    public String getDataTypeQualifier() {
        return dataTypeQualifier;
    }

    public void setDataTypeQualifier(String dataTypeQualifier) {
        this.dataTypeQualifier = dataTypeQualifier;
    }

    public String getRepresentationTermForNDRRules() {
        if (representationTerm.equalsIgnoreCase("Text")) {
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
        this.representationTerm = representationTerm;
    }

    public String getQualifiedDataTypeId() {
        return qualifiedDataTypeId;
    }

    public void setQualifiedDataTypeId(String qualifiedDataTypeId) {
        this.qualifiedDataTypeId = qualifiedDataTypeId;
    }

    public String getAssociatedObjectClassTermQualifier() {
        return associatedObjectClassTermQualifier;
    }

    public void setAssociatedObjectClassTermQualifier(String associatedObjectClassTermQualifier) {
        this.associatedObjectClassTermQualifier = StringUtils.defaultIfEmpty(associatedObjectClassTermQualifier, "");
        ;
    }

    public String getAssociatedObjectClassTerm() {
        return associatedObjectClassTerm;
    }

    public void setAssociatedObjectClassTerm(String associatedObjectClassTerm) {
        this.associatedObjectClassTerm = StringUtils.defaultIfEmpty(associatedObjectClassTerm, "");
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
        return this.associatedObjectClassTermQualifier.concat(this.associatedObjectClassTerm);
    }

    public String getPropertyTermWithQualifier() {
        return this.propertyTermQualifier.concat(this.propertyTerm);
    }

    public String getPropertyKey(boolean checkTDED) {
        if (checkTDED)
            return getPropertyKey();
        else
            return StringUtils.join(getPropertyTermWithQualifier(), getRepresentationTermForNDRRules()).replaceAll(" ", "");
    }

    public String getPropertyKey() {
        /*        return StringUtils.join(getPropertyTerm(), getRepresentationTerm()).replaceAll(" ", "");
         */

        if (StringUtils.isBlank(getTDED()) || !codes.contains(getTDED())) {
            if (StringUtils.isBlank(getAssociatedObjectClassTerm())) {
                return StringUtils.join(getPropertyTermWithQualifier(), getRepresentationTermForNDRRules()).replaceAll(" ", "");
            } else {
                return StringUtils.join(getPropertyTermWithQualifier(), getRepresentationTermForNDRRules(), getAssociatedObjectClassTerm()).replaceAll(" ", "");
            }
        } else {
            if (StringUtils.isBlank(getDataTypeQualifier())) {
                return StringUtils.join(getObjectClassTermQualifier(), getObjectClassTerm(), getPropertyTermWithQualifier(), getRepresentationTermForNDRRules()).replaceAll(" ", "");
            } else {
                if (StringUtils.contains(getDataTypeQualifier(), getPropertyTerm())) {
                    if (StringUtils.equalsIgnoreCase(getDataTypeQualifier(), getPropertyTerm()))
                        return StringUtils.join(getObjectClassTermQualifier(), getObjectClassTerm(), getDataTypeQualifier(), getRepresentationTermForNDRRules()).replaceAll(" ", "");
                    else
                        return StringUtils.join(getDataTypeQualifier(), getRepresentationTermForNDRRules()).replaceAll(" ", "");
                } else {
                    return StringUtils.join(getDataTypeQualifier(), getPropertyTermWithQualifier(), getRepresentationTermForNDRRules()).replaceAll(" ", "");
                }
            }
        }
    }

    public String getClassKey() {
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
        if (TDED.length() > 4) {
            TDED = TDED.substring(1, 5);
        }
        return TDED;
    }

    public void setTDED(String TDED) {
        this.TDED = TDED;
    }
}
