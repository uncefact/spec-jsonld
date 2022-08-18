package org.unece.uncefact.vocab.transformers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.unece.uncefact.vocab.Transformer;

import javax.json.*;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class UNLOCODEToJSONLDVocabulary extends Transformer {
    protected static String UNLOCODE_NS = "unlcd";
    protected static String UNLOCODE_SUBDIVISIONS_NS = "unlcds";
    protected static String UNLOCODE_COUNTRIES_NS = "unlcdc";
    protected static String UNLOCODE_FUNCTIONS_NS = "unlcdf";
    protected static String GEO_NS = "geo";

    protected static String FUNCTION_CLASS_NAME = "Function";
    protected static String FUNCTION_CLASS = StringUtils.join(UNLOCODE_FUNCTIONS_NS, ":", FUNCTION_CLASS_NAME);
    protected static String FUNCTIONS_PROPERTY_NAME = "functions";
    protected static String FUNCTIONS_PROPERTY = StringUtils.join(UNLOCODE_FUNCTIONS_NS, ":", FUNCTIONS_PROPERTY_NAME);
    protected static String SUBDIVISION_CLASS_NAME = "Subdivision";
    protected static String SUBDIVISION_CLASS = StringUtils.join(UNLOCODE_SUBDIVISIONS_NS, ":", SUBDIVISION_CLASS_NAME);
    protected static String COUNTRY_SUBDIVISION_PROPERTY_NAME = "countrySubdivision";
    protected static String COUNTRY_SUBDIVISION_PROPERTY = StringUtils.join(UNLOCODE_SUBDIVISIONS_NS, ":", COUNTRY_SUBDIVISION_PROPERTY_NAME);
    protected static String SUBDIVISION_TYPE_PROPERTY_NAME = "subdivisionType";
    protected static String SUBDIVISION_TYPE_PROPERTY = StringUtils.join(UNLOCODE_SUBDIVISIONS_NS, ":", SUBDIVISION_TYPE_PROPERTY_NAME);
    protected static String COUNTRY_CLASS_NAME = "Country";
    protected static String COUNTRY_CLASS = StringUtils.join(UNLOCODE_COUNTRIES_NS, ":", COUNTRY_CLASS_NAME);
    protected static String UNLOCODE_CLASS_NAME = "UNLOCODE";
    protected static String UNLOCODE_CLASS = StringUtils.join(UNLOCODE_NS, ":", UNLOCODE_CLASS_NAME);
    protected static String PROPERTY_COUNTRY_CODE_NAME = "countryCode";
    protected static String PROPERTY_COUNTRY_CODE = StringUtils.join(UNLOCODE_COUNTRIES_NS, ":", PROPERTY_COUNTRY_CODE_NAME);

    protected static Map<String, String> NS_MAP = Map.of(
            GEO_NS, "http://www.w3.org/2003/01/geo/wgs84_pos#",
            XSD_NS, "http://www.w3.org/2001/XMLSchema#",
            SCHEMA_NS, "http://schema.org/",
            UNLOCODE_NS, "https://service.unece.org/trade/uncefact/vocabulary/unlocode/",
            UNLOCODE_COUNTRIES_NS, "https://service.unece.org/trade/uncefact/vocabulary/unlocode-countries/",
            UNLOCODE_SUBDIVISIONS_NS, "https://service.unece.org/trade/uncefact/vocabulary/unlocode-subdivisions/",
            UNLOCODE_FUNCTIONS_NS, "https://service.unece.org/trade/uncefact/vocabulary/unlocode-functions/",
            RDF_NS, "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    );

    protected static Map<String, Function> functionsMap = new HashMap<>();
    Map<String, JsonObject> countriesGraph = new HashMap<>();
    Map<String, JsonObject> subdivisionsGraph = new HashMap<>();
    Map<String, JsonObject> functionsGraph = new HashMap<>();
    Map<String, JsonObject> locodesGraph = new HashMap<>();

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
        functionsMap.put("0", new Function("0", "Not officially functional", "Digit \"0\" means that the criteria for inclusion apply, but that no information is available or used which is recognized by the competent authority regarding the specific transport mode or function(s) of the location."));
        functionsMap.put("B", new Function("B", "Cross Border (former code; not to be used) ", "Any location that is located on the border with other"));
        functionsMap.put("A", new Function("A", "Special Economic Zone (SEZ)", "Any geographic region that has economic laws different from a country's typical economic laws for the purposes of trade operations and duties and tariffs."));
        for (String key : functionsMap.keySet()) {
            Function function = functionsMap.get(key);
            String id = function.getCode();
            JsonObjectBuilder rdfClass = Json.createObjectBuilder(Map.of(
                    ID, StringUtils.join(UNLOCODE_FUNCTIONS_NS, ":", id),
                    TYPE, FUNCTION_CLASS,
                    RDFS_LABEL, function.getFunction(),
                    RDFS_COMMENT, function.getDefinition(),
                    RDF_VALUE, id
            ));
            functionsGraph.put(id, rdfClass.build());
        }
    }


    public UNLOCODEToJSONLDVocabulary(boolean prettyPrint) {
        super(null, null, prettyPrint);

    }

    protected void setContext() {
        super.setMinimalContext();
        for (String ns :Arrays.asList(UNLOCODE_NS, UNLOCODE_COUNTRIES_NS, UNLOCODE_FUNCTIONS_NS, RDF_NS)){
            contextObjectBuilder.add(ns, NS_MAP.get(ns));
        }
    }


    public void transform() throws IOException {
        try {
            Files.createDirectory(Paths.get(UNLOCODE_NS));
            Files.createDirectory(Paths.get(UNLOCODE_NS.concat("/unlocode")));
            Files.createDirectory(Paths.get(UNLOCODE_NS.concat("/unlocode-countries")));
            Files.createDirectory(Paths.get(UNLOCODE_NS.concat("/unlocode-functions")));
            Files.createDirectory(Paths.get(UNLOCODE_NS.concat("/unlocode-subdivisions")));
        } catch (FileAlreadyExistsException e) {
            System.err.println(String.format("Output directory %s already exists, please remove it and repeat.", UNLOCODE_NS));
            throw e;
        }
        List<CSVRecord> locodes = new ArrayList();
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
        }
        readInputFileToGraphArray(locodes);
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
                                ID, StringUtils.join(UNLOCODE_FUNCTIONS_NS, ":", functionItem)
                        ));
                        functionArray.add(functionObj);
                    }
                    rdfClass.add(FUNCTIONS_PROPERTY, functionArray);
                } else {
                    JsonObjectBuilder functionObj = Json.createObjectBuilder(Map.of(
                            ID, StringUtils.join(UNLOCODE_FUNCTIONS_NS, ":", functions.get(0))
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
        try {
            JsonObjectBuilder unlocodeClass = Json.createObjectBuilder(Map.of(
                    ID, UNLOCODE_CLASS,
                    TYPE, RDFS_CLASS,
                    RDFS_COMMENT,
                    "Identifies an administrative or economic area, relevant to international trade and transport, as defined by the competent national authority in each country."
            ));
            locodesGraph.put(UNLOCODE_CLASS_NAME, unlocodeClass.build());

            for (String key : locodesGraph.keySet()) {
                graphJsonArrayBuilder = Json.createArrayBuilder();
                JsonObject jsonObject = locodesGraph.get(key);
                if (key.equalsIgnoreCase(UNLOCODE_CLASS_NAME)) {
                    setMinimalContext();
                    contextObjectBuilder.add(UNLOCODE_NS, NS_MAP.get(UNLOCODE_NS));
                } else {
                    setContext();
                    if(jsonObject.containsKey(COUNTRY_SUBDIVISION_PROPERTY)){
                        contextObjectBuilder.add(UNLOCODE_SUBDIVISIONS_NS, NS_MAP.get(UNLOCODE_SUBDIVISIONS_NS));
                    }
                    if(jsonObject.containsKey(StringUtils.join(GEO_NS, ":", "lat"))){
                        contextObjectBuilder.add(GEO_NS, NS_MAP.get(GEO_NS));
                        contextObjectBuilder.add(XSD_NS, NS_MAP.get(XSD_NS));
                    }
                }
                outputFile = StringUtils.join(UNLOCODE_NS, "/unlocode/",key,".jsonld");
                graphJsonArrayBuilder.add(locodesGraph.get(key));
                super.transform();
            }


            JsonObjectBuilder countryClass = Json.createObjectBuilder(Map.of(
                    ID, COUNTRY_CLASS,
                    TYPE, RDFS_CLASS,
                    RDFS_COMMENT, "The two-letter alphabetic country codes, adopted in International Standard ISO 3166-1."));
            countriesGraph.put(COUNTRY_CLASS_NAME, countryClass.build());

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

            countriesGraph.put(PROPERTY_COUNTRY_CODE_NAME, countryCodeProperty.build());

            for (String key : countriesGraph.keySet()) {
                graphJsonArrayBuilder = Json.createArrayBuilder();
                super.setMinimalContext();
                contextObjectBuilder.add(UNLOCODE_COUNTRIES_NS, NS_MAP.get(UNLOCODE_COUNTRIES_NS));
                if(!key.equalsIgnoreCase(COUNTRY_CLASS_NAME)){
                    contextObjectBuilder.add(RDF_NS, NS_MAP.get(RDF_NS));
                }
                if (key.equalsIgnoreCase(PROPERTY_COUNTRY_CODE_NAME)) {
                    contextObjectBuilder.add(SCHEMA_NS, NS_MAP.get(SCHEMA_NS));
                    contextObjectBuilder.add(UNLOCODE_SUBDIVISIONS_NS, NS_MAP.get(UNLOCODE_SUBDIVISIONS_NS));
                }
                outputFile = StringUtils.join(UNLOCODE_NS, "/unlocode-countries/",key,".jsonld");
                graphJsonArrayBuilder.add(countriesGraph.get(key));
                super.transform();
            }

            JsonObjectBuilder functionClass = Json.createObjectBuilder(Map.of(
                    ID, FUNCTION_CLASS,
                    TYPE, RDFS_CLASS,
                    RDFS_COMMENT, "1-character function classifier code which identifies the existence of " +
                            "either a facility providing a connection with a specific mode of transport 1 or some other " +
                            "significant function not directly related to any mode of transport at this location"
            ));
            functionsGraph.put(FUNCTION_CLASS_NAME, functionClass.build());

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
            functionsGraph.put(FUNCTIONS_PROPERTY_NAME, functionsProperty.build());

            for (String key : functionsGraph.keySet()) {
                graphJsonArrayBuilder = Json.createArrayBuilder();
                super.setMinimalContext();
                if(!key.equalsIgnoreCase(FUNCTION_CLASS_NAME)){
                    contextObjectBuilder.add(RDF_NS, NS_MAP.get(RDF_NS));
                }
                contextObjectBuilder.add(UNLOCODE_FUNCTIONS_NS, NS_MAP.get(UNLOCODE_FUNCTIONS_NS));
                if (key.equalsIgnoreCase(FUNCTIONS_PROPERTY_NAME)) {
                    contextObjectBuilder.add(UNLOCODE_NS, NS_MAP.get(UNLOCODE_NS));
                }
                outputFile = StringUtils.join(UNLOCODE_NS, "/unlocode-functions/",key,".jsonld");
                graphJsonArrayBuilder.add(functionsGraph.get(key));
                super.transform();
            }

            JsonObjectBuilder subdivisionClass = Json.createObjectBuilder(Map.of(
                    ID, SUBDIVISION_CLASS,
                    TYPE, RDFS_CLASS,
                    RDFS_COMMENT, "Code for the administrative division of the country concerned (state, province, department, etc.)."
            ));
            subdivisionsGraph.put(SUBDIVISION_CLASS_NAME, subdivisionClass.build());

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
            subdivisionsGraph.put(COUNTRY_SUBDIVISION_PROPERTY_NAME, countrySubdivProperty.build());

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

            subdivisionsGraph.put(SUBDIVISION_TYPE_PROPERTY_NAME, subdivTypeProperty.build());

            for (String key : subdivisionsGraph.keySet()) {
                graphJsonArrayBuilder = Json.createArrayBuilder();
                super.setMinimalContext();
                contextObjectBuilder.add(UNLOCODE_SUBDIVISIONS_NS, NS_MAP.get(UNLOCODE_SUBDIVISIONS_NS));
                if(!key.equalsIgnoreCase(SUBDIVISION_CLASS_NAME)){
                    contextObjectBuilder.add(RDF_NS, NS_MAP.get(RDF_NS));
                }
                if (!key.equalsIgnoreCase(SUBDIVISION_CLASS_NAME)
                        & !key.equalsIgnoreCase(COUNTRY_SUBDIVISION_PROPERTY_NAME)
                        & !key.equalsIgnoreCase(SUBDIVISION_TYPE_PROPERTY_NAME)) {
                    contextObjectBuilder.add(UNLOCODE_COUNTRIES_NS, NS_MAP.get(UNLOCODE_COUNTRIES_NS));
                }
                if (key.equalsIgnoreCase(COUNTRY_SUBDIVISION_PROPERTY_NAME)) {
                    contextObjectBuilder.add(UNLOCODE_NS, NS_MAP.get(UNLOCODE_NS));
                }
                if (key.equalsIgnoreCase(SUBDIVISION_TYPE_PROPERTY_NAME)) {
                    contextObjectBuilder.add(SCHEMA_NS, NS_MAP.get(SCHEMA_NS));
                    contextObjectBuilder.add(XSD_NS, NS_MAP.get(XSD_NS));
                }
                outputFile = StringUtils.join(UNLOCODE_NS, "/unlocode-subdivisions/",key,".jsonld");
                graphJsonArrayBuilder.add(subdivisionsGraph.get(key));
                super.transform();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
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
