package org.unece.uncefact.vocab.transformers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.unece.uncefact.vocab.JSONLDContext;
import org.unece.uncefact.vocab.JSONLDVocabulary;
import org.unece.uncefact.vocab.Transformer;

import javax.json.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class UNLOCODEToJSONLDVocabulary extends Transformer {

    protected static String FUNCTION_CLASS_NAME = "Function";
    protected static String FUNCTION_CLASS = StringUtils.join(UNLOCODE_VOCAB_NS, ":", FUNCTION_CLASS_NAME);
    public static final String FUNCTIONS_PROPERTY_NAME = "functions";
    public static String FUNCTIONS_PROPERTY = StringUtils.join(UNLOCODE_VOCAB_NS, ":", FUNCTIONS_PROPERTY_NAME);
    public static String SUBDIVISION_CLASS_NAME = "Subdivision";
    public static String SUBDIVISION_CLASS = StringUtils.join(UNLOCODE_VOCAB_NS, ":", SUBDIVISION_CLASS_NAME);
    public static final String COUNTRY_SUBDIVISION_PROPERTY_NAME = "countrySubdivision";
    public static String COUNTRY_SUBDIVISION_PROPERTY = StringUtils.join(UNLOCODE_VOCAB_NS, ":", COUNTRY_SUBDIVISION_PROPERTY_NAME);
    public final static String SUBDIVISION_TYPE_PROPERTY_NAME = "subdivisionType";
    public static String SUBDIVISION_TYPE_PROPERTY = StringUtils.join(UNLOCODE_VOCAB_NS, ":", SUBDIVISION_TYPE_PROPERTY_NAME);
    public static String COUNTRY_CLASS_NAME = "Country";
    public static String COUNTRY_CLASS = StringUtils.join(UNLOCODE_VOCAB_NS, ":", COUNTRY_CLASS_NAME);
    public static String UNLOCODE_CLASS_NAME = "UNLOCODE";
    public static String UNLOCODE_CLASS = StringUtils.join(UNLOCODE_VOCAB_NS, ":", UNLOCODE_CLASS_NAME);
    public final static String PROPERTY_COUNTRY_CODE_NAME = "countryCode";
    public static String PROPERTY_COUNTRY_CODE = StringUtils.join(UNLOCODE_VOCAB_NS, ":", PROPERTY_COUNTRY_CODE_NAME);


    protected static Map<String, Function> functionsMap = new TreeMap<>();
    Map<String, JsonObject> countriesGraph = new TreeMap<>();
    Map<String, JsonObject> subdivisionsGraph = new TreeMap<>();
    Map<String, JsonObject> vocabGraph = new TreeMap<>();
    Map<String, JsonObject> functionsGraph = new TreeMap<>();
    Map<String, JsonObject> locodesGraph = new TreeMap<>();

    /**
     * Static block to initialise functions graph
     * Functions are not coming within the outputs for UNLOCODE, so we are defining them inline
     * The source file "Recommendation No. 16 – 2020 Edition" - https://unece.org/sites/default/files/2020-12/ECE-TRADE-459E.pdf
     */
    {
        functionsMap.put("1", new Function("1", "Maritime transport (sea port or maritime port)", "Any location with permanent facilities at which seagoing vessels can load or discharge cargo moving in maritime traffic."));
        functionsMap.put("2", new Function("2", "Rail transport", "Any location that has one or more railway terminals like cargo terminals or train stations (excluding passenger terminals). Specific terminals located inside a location shall not be considered individually as a location."));
        functionsMap.put("3", new Function("3", "Road transport", "Any location that is connected to other ones by means of roads. Specific terminals located inside a location shall not be considered individually as a location."));
        functionsMap.put("4", new Function("4", "Air transport (airport) or space transport (spaceport)", "Any location with permanent facilities at which aircraft can load or discharge cargo moving in air traffic."));
        functionsMap.put("5", new Function("5", "International Mail Processing Centre (IMPC) recognized by the Universal Postal Union (UPU)", "A mail processing facility, recognized by UPU, that has significance for the processing of inter-operator mail, either because they generate or receive dispatches or because they act as transit centres for mail exchanged between other IMPCs. Each IMPC has a well-defined physical location, is operated by or under the responsibility of a single organization and handles a specific set of mail flows. (This was known as a postal exchange office in the former edition of the Recommendation.)"));
        functionsMap.put("6", new Function("6", "Multimodal transport facility", "Any location where one or more of the below facilities can be found: Inland Clearance Depot (ICD): a multimodal transport facility, other than a sea port or an airport, which is approved by a competent body, equipped with fixed installations and offering services for the handling and temporary storage of any kind of goods (including containers) carried under customs transit by any applicable mode of transport, placed under customs control and, with customs and other agencies, competent to clear goods for home use, warehousing, temporary admission, re-export, temporary storage for onward transit and outright export. (Definition applies also to synonyms like Dry Port, Inland Clearance Terminal, etc.) Container Depot: a multimodal transport facility which offers services for storage, repair and maintenance of containers. Inland freight terminal: a multimodal transport facility, other than a sea port or an airport, operated on a common- user basis, at which trade cargo is received or dispatched."));
        functionsMap.put("7", new Function("7", "Fixed Transport Installation (oil pipeline terminal, electric power lines, ropeway terminals, etc.)", "Any location with permanent facilities to load or discharge cargo that doesn’t fit in the previous definitions (e.g. oil platform)."));
        functionsMap.put("8", new Function("8", "Inland water transport (river ports, and lake ports)", "Any location with permanent facilities at which vessels can load or discharge cargo moving in inland waterway traffic."));
        functionsMap.put("0", new Function("0", "Not officially functional", "Digit '0' means that the criteria for inclusion apply, but that no information is available or used which is recognized by the competent authority regarding the specific transport mode or function(s) of the location."));
        functionsMap.put("B", new Function("B", "Cross Border (former code; not to be used) ", "Any location that is located on the border with other"));
        functionsMap.put("A", new Function("A", "Special Economic Zone (SEZ)", "Any geographic region that has economic laws different from a country's typical economic laws for the purposes of trade operations and duties and tariffs."));
        for (String key : functionsMap.keySet()) {
            Function function = functionsMap.get(key);
            String id = function.getCode();
            JsonObjectBuilder rdfClass = Json.createObjectBuilder(Map.of(
                    ID, StringUtils.join(UNLOCODE_FUNC_NS, ":", id),
                    TYPE, FUNCTION_CLASS,
                    RDFS_LABEL, function.getFunction(),
                    RDFS_COMMENT, function.getDefinition(),
                    RDF_VALUE, id
            ));
            functionsGraph.put(id, rdfClass.build());
        }
    }


    public UNLOCODEToJSONLDVocabulary(Set<String> inputFiles, Set<String> defaultInputFiles, boolean prettyPrint) {
        super(null);
        setInputFiles(inputFiles);
        setDefaultInputFiles(defaultInputFiles);

    }

    protected JsonObjectBuilder getContext() {
        JsonObjectBuilder result = super.getMinimalContext();
        for (String ns :Arrays.asList(UNLOCODE_VOCAB_NS, RDF_NS)){
            result.add(ns, NS_MAP.get(ns));
        }
        return result;
    }


    public void transform() throws IOException, InvalidFormatException {
        List<CSVRecord> locodes = new ArrayList();
        if (inputFiles.isEmpty()){
            for (String file : defaultInputFiles) {
                InputStream in = getClass().getResourceAsStream(file);
                Reader reader = new BufferedReader(new InputStreamReader(in, Charset.forName("ISO-8859-1")));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
                List<CSVRecord> records = csvParser.getRecords();
                if (records.get(0).size() > 4) {
                    locodes.addAll(records);
                } else {
                    processSubdivisionCodes(records);
                }
                csvParser.close();
                reader.close();
                in.close();
            }
        }
        else {
            for (String file : inputFiles) {
                Reader reader = Files.newBufferedReader(Paths.get(file), Charset.forName("ISO-8859-1"));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
                List<CSVRecord> records = csvParser.getRecords();
                if (records.get(0).size() > 4) {
                    locodes.addAll(records);
                } else {
                    processSubdivisionCodes(records);
                }
                csvParser.close();
                reader.close();
            }
        }
        readInputFileToGraphArray(locodes);
        super.transform();
    }

    private void processSubdivisionCodes(List<CSVRecord> records) {
        for (int i = 0; i < records.size(); i++) {
            SubDivisionCode code = new SubDivisionCode(
                    records.get(i).get(0),
                    records.get(i).get(1),
                    records.get(i).get(2),
                    records.get(i).get(3)
            );
            String id = StringUtils.join(code.getCountry(), code.getCode());
            JsonObjectBuilder rdfClass = Json.createObjectBuilder(Map.of(
                    ID, StringUtils.join(UNLOCODE_SUBDIVISIONS_NS, ":", id),
                    TYPE, SUBDIVISION_CLASS,
                    RDFS_LABEL, code.getName(),
                    RDF_VALUE, id,
                    SUBDIVISION_TYPE_PROPERTY, code.getType()
            ));
            JsonObjectBuilder countryObj = Json.createObjectBuilder(Map.of(ID, StringUtils.join(UNLOCODE_COUNTRIES_NS, ":", code.getCountry())));
            rdfClass.add(PROPERTY_COUNTRY_CODE, countryObj);

            subdivisionsGraph.put(id, rdfClass.build());
        }
    }

    public void readInputFileToGraphArray(final Object object) {
        List<CSVRecord> records = (List<CSVRecord>) object;
        String[][] data = new String[(int) records.size()][11];
        for (int i = 0; i < records.size(); i++) {
            for (int j = 0; j < 11; j++)
                data[i][j] = records.get(i).get(j);
        }

        for (int i = 0; i < data.length; i++) {
            String changeIndicator = data[i][1].concat(data[i][0]);
            String country = data[i][1];
            String locode = country.concat(data[i][2]);
            String name = data[i][3];
            String internationalName = data[i][4];
            String subdivision = data[i][5];
            String function = data[i][6];
            String coordinates = data[i][10];

            if (StringUtils.isEmpty(data[i][2]) && !data[i][0].equalsIgnoreCase("=")) {
                // read the country name
                String countryName = data[i][3];
                if (countryName.startsWith("."))
                    countryName = countryName.substring(1);

                JsonObjectBuilder rdfClass = Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(UNLOCODE_COUNTRIES_NS, ":", country),
                        TYPE, COUNTRY_CLASS,
                        RDFS_LABEL, countryName,
                        RDF_VALUE, country
                ));
                countriesGraph.put(locode, rdfClass.build());
            } else if (!data[i][0].equalsIgnoreCase("=")) {
                //read the UN/LOCODE
                JsonObjectBuilder rdfClass = Json.createObjectBuilder(Map.of(
                        ID, StringUtils.join(UNLOCODE_NS, ":", locode),
                        TYPE, UNLOCODE_CLASS,
                        RDF_VALUE, locode
                ));
                if (StringUtils.equals(name, internationalName)) {
                    JsonObjectBuilder labelObj = Json.createObjectBuilder(Map.of(
                            VALUE, name,
                            LANGUAGE, "en"
                    ));
                    rdfClass.add(RDFS_LABEL, labelObj);
                } else {
                    JsonArrayBuilder labelArray = Json.createArrayBuilder();
                    JsonObjectBuilder labelObj = Json.createObjectBuilder(Map.of(
                            VALUE, name
                    ));
                    labelArray.add(labelObj);
                    JsonObjectBuilder labelObjEn = Json.createObjectBuilder(Map.of(
                            VALUE, internationalName,
                            LANGUAGE, "en"
                    ));
                    labelArray.add(labelObjEn);
                    rdfClass.add(RDFS_LABEL, labelArray);
                }

                List<String> functions = new ArrayList<>();
                for (char functionChar : function.toCharArray()) {
                    if (functionChar != '-') {
                        functions.add(String.valueOf(functionChar));
                    }
                }
                if (functions.size() > 1) {
                    JsonArrayBuilder functionArray = Json.createArrayBuilder();
                    for (String functionItem : functions) {
                        JsonObjectBuilder functionObj = Json.createObjectBuilder(Map.of(
                                ID, StringUtils.join(UNLOCODE_FUNC_NS, ":", functionItem)
                        ));
                        functionArray.add(functionObj);
                    }
                    rdfClass.add(FUNCTIONS_PROPERTY, functionArray);
                } else {
                    JsonObjectBuilder functionObj = Json.createObjectBuilder(Map.of(
                            ID, StringUtils.join(UNLOCODE_FUNC_NS, ":", functions.get(0))
                    ));
                    rdfClass.add(FUNCTIONS_PROPERTY, functionObj);
                }
                if (StringUtils.isNotEmpty(subdivision)) {
                    String subdivisionString = StringUtils.join(country, subdivision);
                    JsonObjectBuilder subdivisionObj = Json.createObjectBuilder(Map.of(
                            ID, StringUtils.join(UNLOCODE_SUBDIVISIONS_NS, ":", subdivisionString)
                    ));
                    rdfClass.add(COUNTRY_SUBDIVISION_PROPERTY, subdivisionObj);
                }
                JsonObjectBuilder countryObj = Json.createObjectBuilder(Map.of(ID, StringUtils.join(UNLOCODE_COUNTRIES_NS, ":", country)));
                rdfClass.add(StringUtils.join(PROPERTY_COUNTRY_CODE), countryObj);

                if (StringUtils.isNotEmpty(coordinates)) {
                    float latFloat;
                    float longFloat;
                    String latDMS = coordinates.split(" ")[0];
                    String longDMS = coordinates.split(" ")[1];
                    latFloat = Float.valueOf(latDMS.substring(0, 2)) + Float.valueOf(latDMS.substring(2, 4)) / 60;
                    if (latDMS.endsWith("S")) {
                        latFloat *= -1;
                    }
                    longFloat = Float.valueOf(longDMS.substring(0, 3)) + Float.valueOf(longDMS.substring(3, 5)) / 60;
                    if (longDMS.endsWith("W")) {
                        longFloat *= -1;
                    }
                    JsonObjectBuilder latObj = Json.createObjectBuilder(Map.of(
                            VALUE, String.valueOf(latFloat),
                            TYPE, StringUtils.join(XSD_NS, ":float")
                    ));
                    rdfClass.add(StringUtils.join(GEO_NS, ":", "lat"), latObj);

                    JsonObjectBuilder longObj = Json.createObjectBuilder(Map.of(
                            VALUE, String.valueOf(longFloat),
                            TYPE, StringUtils.join(XSD_NS, ":float")
                    ));
                    rdfClass.add(StringUtils.join(GEO_NS, ":", "long"), longObj);
                }
                locodesGraph.put(locode, rdfClass.build());
            }
        }

        JsonObjectBuilder countryClass = Json.createObjectBuilder(Map.of(
                ID, COUNTRY_CLASS,
                TYPE, RDFS_CLASS,
                RDFS_COMMENT, "The two-letter alphabetic country codes, adopted in International Standard ISO 3166-1."));
        vocabGraph.put(COUNTRY_CLASS_NAME, countryClass.build());

        JsonObjectBuilder countryCodeProperty = Json.createObjectBuilder(Map.of(
                ID, PROPERTY_COUNTRY_CODE,
                TYPE, RDF_PROPERTY,
                RDFS_COMMENT, "Related ISO 3166-1 country code."
        ));
        countryCodeProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, COUNTRY_CLASS
        )));
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        arrayBuilder.add(Json.createObjectBuilder(Map.of(
                ID, UNLOCODE_CLASS
        )));
        arrayBuilder.add(Json.createObjectBuilder(Map.of(
                ID, SUBDIVISION_CLASS
        )));
        countryCodeProperty.add(SCHEMA_DOMAIN_INCLUDES, arrayBuilder);

        vocabGraph.put(PROPERTY_COUNTRY_CODE_NAME, countryCodeProperty.build());

        JsonObjectBuilder unlocodeClass = Json.createObjectBuilder(Map.of(
                ID, UNLOCODE_CLASS,
                TYPE, RDFS_CLASS,
                RDFS_COMMENT,
                "Identifies an administrative or economic area, relevant to international trade and transport, as defined by the competent national authority in each country."
        ));
        vocabGraph.put(UNLOCODE_CLASS_NAME, unlocodeClass.build());

        JsonObjectBuilder subdivisionClass = Json.createObjectBuilder(Map.of(
                ID, SUBDIVISION_CLASS,
                TYPE, RDFS_CLASS,
                RDFS_COMMENT, "Code for the administrative division of the country concerned (state, province, department, etc.)."
        ));
        vocabGraph.put(SUBDIVISION_CLASS_NAME, subdivisionClass.build());

        JsonObjectBuilder countrySubdivProperty = Json.createObjectBuilder(Map.of(
                ID, COUNTRY_SUBDIVISION_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_COMMENT, "Related ISO 3166-2 country subdivision code."
        ));
        countrySubdivProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, SUBDIVISION_CLASS
        )));
        countrySubdivProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, UNLOCODE_CLASS
        )));
        vocabGraph.put(COUNTRY_SUBDIVISION_PROPERTY_NAME, countrySubdivProperty.build());

        JsonObjectBuilder subdivTypeProperty = Json.createObjectBuilder(Map.of(
                ID, SUBDIVISION_TYPE_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_COMMENT, "The administrative division (state," +
                        "province, department, etc.).",
                SCHEMA_RANGE_INCLUDES, StringUtils.join(XSD_NS, ":string")
        ));
        subdivTypeProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, SUBDIVISION_CLASS
        )));

        vocabGraph.put(SUBDIVISION_TYPE_PROPERTY_NAME, subdivTypeProperty.build());

        JsonObjectBuilder functionClass = Json.createObjectBuilder(Map.of(
                ID, FUNCTION_CLASS,
                TYPE, RDFS_CLASS,
                RDFS_COMMENT, "1-character function classifier code which identifies the existence of " +
                        "either a facility providing a connection with a specific mode of transport 1 or some other " +
                        "significant function not directly related to any mode of transport at this location"
        ));
        vocabGraph.put(FUNCTION_CLASS_NAME, functionClass.build());

        JsonObjectBuilder functionsProperty = Json.createObjectBuilder(Map.of(
                ID, FUNCTIONS_PROPERTY,
                TYPE, RDF_PROPERTY,
                RDFS_COMMENT, "Related function codes."
        ));
        functionsProperty.add(SCHEMA_RANGE_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, FUNCTION_CLASS
        )));
        functionsProperty.add(SCHEMA_DOMAIN_INCLUDES, Json.createObjectBuilder(Map.of(
                ID, UNLOCODE_CLASS
        )));
        vocabGraph.put(FUNCTIONS_PROPERTY_NAME, functionsProperty.build());

        JSONLDVocabulary jsonldVocabulary = new JSONLDVocabulary(StringUtils.join("unlocode-vocab.jsonld"), true);
        jsonldVocabulary.setContextObjectBuilder(getContext());
        jsonldVocabulary.getContextObjectBuilder().add(SCHEMA_NS, NS_MAP.get(SCHEMA_NS));

        JSONLDContext jsonldContext = new JSONLDContext(StringUtils.join("unlocode-vocab-context.jsonld"), true);
        jsonldContext.getContextObjectBuilder().add(UNLOCODE_VOCAB_NS, NS_MAP.get(UNLOCODE_VOCAB_NS));
        jsonldContext.getContextObjectBuilder().add(RDF_NS, NS_MAP.get(RDF_NS));
        jsonldContext.getContextObjectBuilder().add(RDFS_NS, NS_MAP.get(RDFS_NS));
        jsonldContext.getContextObjectBuilder().add(XSD_NS, NS_MAP.get(XSD_NS));
        jsonldContext.getContextObjectBuilder().add(UNLOCODE_NS, NS_MAP.get(UNLOCODE_NS));
        jsonldContext.getContextObjectBuilder().add(UNLOCODE_COUNTRIES_NS, NS_MAP.get(UNLOCODE_COUNTRIES_NS));
        jsonldContext.getContextObjectBuilder().add(UNLOCODE_SUBDIVISIONS_NS, NS_MAP.get(UNLOCODE_SUBDIVISIONS_NS));
        jsonldContext.getContextObjectBuilder().add(GEO_NS, NS_MAP.get(GEO_NS));
        jsonldContext.getContextObjectBuilder().add(XSD_NS, NS_MAP.get(XSD_NS));
        jsonldContext.getContextObjectBuilder().add(SCHEMA_NS, NS_MAP.get(SCHEMA_NS));
        jsonldContext.getContextObjectBuilder().add("id", ID);
        jsonldContext.getContextObjectBuilder().add("type", TYPE);

        for (String key : vocabGraph.keySet()) {
            jsonldVocabulary.getGraphJsonArrayBuilder().add(vocabGraph.get(key));
            JsonObjectBuilder propertyObjectBuilder = Json.createObjectBuilder(Map.of(ID, StringUtils.join(UNLOCODE_VOCAB_NS,":", key)));
            switch (key){
                case PROPERTY_COUNTRY_CODE_NAME:
                case COUNTRY_SUBDIVISION_PROPERTY_NAME:
                case FUNCTIONS_PROPERTY_NAME:
                    propertyObjectBuilder.add(TYPE, "@vocab");
                    break;
                case SUBDIVISION_TYPE_PROPERTY_NAME:
                    propertyObjectBuilder.add(TYPE, "xsd:string");
                    break;
            }

            jsonldContext.getContextObjectBuilder().add(key, propertyObjectBuilder.build());
        }
        vocabularies.add(jsonldVocabulary);
        contexts.add(jsonldContext);

        jsonldVocabulary = new JSONLDVocabulary(StringUtils.join("unlocode-functions.jsonld"), true);
        jsonldVocabulary.setContextObjectBuilder(getContext());
        jsonldVocabulary.getContextObjectBuilder().add(UNLOCODE_FUNC_NS, NS_MAP.get(UNLOCODE_FUNC_NS));
        for (String key : functionsGraph.keySet()) {
            jsonldVocabulary.getGraphJsonArrayBuilder().add(functionsGraph.get(key));
        }
        vocabularies.add(jsonldVocabulary);

        jsonldVocabulary = new JSONLDVocabulary(StringUtils.join("unlocode.jsonld"), false);
        jsonldVocabulary.setContextObjectBuilder(getContext());
        jsonldVocabulary.getContextObjectBuilder().add(UNLOCODE_NS, NS_MAP.get(UNLOCODE_NS));
        jsonldVocabulary.getContextObjectBuilder().add(UNLOCODE_COUNTRIES_NS, NS_MAP.get(UNLOCODE_COUNTRIES_NS));
        jsonldVocabulary.getContextObjectBuilder().add(UNLOCODE_SUBDIVISIONS_NS, NS_MAP.get(UNLOCODE_SUBDIVISIONS_NS));
        jsonldVocabulary.getContextObjectBuilder().add(GEO_NS, NS_MAP.get(GEO_NS));
        jsonldVocabulary.getContextObjectBuilder().add(XSD_NS, NS_MAP.get(XSD_NS));
        for (String key : locodesGraph.keySet()) {
            jsonldVocabulary.getGraphJsonArrayBuilder().add(locodesGraph.get(key));
        }
        vocabularies.add(jsonldVocabulary);

        jsonldVocabulary = new JSONLDVocabulary(StringUtils.join("unlocode-countries.jsonld"), true);
        jsonldVocabulary.setContextObjectBuilder(getContext());
        jsonldVocabulary.getContextObjectBuilder().add(UNLOCODE_COUNTRIES_NS, NS_MAP.get(UNLOCODE_COUNTRIES_NS));
        for (String key : countriesGraph.keySet()) {
            jsonldVocabulary.getGraphJsonArrayBuilder().add(countriesGraph.get(key));
        }
        vocabularies.add(jsonldVocabulary);

        jsonldVocabulary = new JSONLDVocabulary(StringUtils.join("unlocode-subdivisions.jsonld"), true);
        jsonldVocabulary.setContextObjectBuilder(getContext());
        jsonldVocabulary.getContextObjectBuilder().add(UNLOCODE_COUNTRIES_NS, NS_MAP.get(UNLOCODE_COUNTRIES_NS));
        jsonldVocabulary.getContextObjectBuilder().add(UNLOCODE_SUBDIVISIONS_NS, NS_MAP.get(UNLOCODE_SUBDIVISIONS_NS));
        for (String key : subdivisionsGraph.keySet()) {
            jsonldVocabulary.getGraphJsonArrayBuilder().add(subdivisionsGraph.get(key));
        }
        vocabularies.add(jsonldVocabulary);

    }

    class SubDivisionCode {
        private String country;
        private String code;
        private String name;
        private String type;

        public SubDivisionCode(String country, String code, String name, String type) {
            this.country = country;
            this.code = code;
            this.name = name;
            this.type = type;
        }

        public String getCountry() {
            return country;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }

    class Function {
        private String code;
        private String function;
        private String definition;

        public Function(String code, String function, String definition) {
            this.code = code;
            this.function = function;
            this.definition = definition;
        }

        public String getCode() {
            return code;
        }

        public String getFunction() {
            return function;
        }

        public String getDefinition() {
            return definition;
        }
    }

}
