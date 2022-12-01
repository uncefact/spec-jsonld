package org.unece.uncefact.vocab.transformers;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.unece.uncefact.vocab.JSONLDContext;
import org.unece.uncefact.vocab.JSONLDVocabulary;
import org.unece.uncefact.vocab.Transformer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.IntPredicate;

public class UNCLToJSONLDVocabulary extends Transformer {
    protected static String UNCL_NS = "uncl";

    JSONLDContext jsonldContext;
    JSONLDVocabulary jsonldVocabulary;

    public JSONLDContext getJsonldContext() {
        return jsonldContext;
    }

    public JSONLDVocabulary getJsonldVocabulary() {
        return jsonldVocabulary;
    }

    public UNCLToJSONLDVocabulary(String inputFile, String defaultFile) {
        super(inputFile, defaultFile);
        this.jsonldContext = new JSONLDContext();
        this.jsonldVocabulary = new JSONLDVocabulary();
    }

    public void transform() throws IOException, InvalidFormatException {
        try {
            Files.createDirectory(Paths.get(UNCL_NS));
        } catch (IOException e) {
            System.err.printf("Output directory %s already exists, please remove it and repeat.%n", UNCL_NS);
        }
        BufferedReader reader;
        if (inputFile == null){
            reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(defaultFile),Charset.forName("ISO-8859-1")));
        } else {
            reader = Files.newBufferedReader(Paths.get(inputFile), Charset.forName("ISO-8859-1"));
        }
        readInputFileToGraphArray(reader);
        super.transform();
    }
    public void readInputFileToGraphArray(final Object object) {
        Map<String, JsonObject> classesGraph = new TreeMap<>();
        String codeNS = null;
        BufferedReader reader = (BufferedReader)object;
        JSONLDVocabulary vocabulary = new JSONLDVocabulary();
        try {
            String line = reader.readLine();
            while (line != null) {
                String codeListID = null;
                String codeListName = null;
                String codeListContext = null;
                String codeListDescString = null;
                if (line.startsWith("----------------------------------------------------------------------")) {


                    line = reader.readLine();
                    String codeListNameString = reader.readLine();
                    if (StringUtils.isNotBlank(codeListNameString.substring(0, 1))) {
                        /*                    System.out.println("Change indicator - " + codeListNameString);*/
                    }

/*
     1001  Document name code                                      [C]*/
                    codeListID = stripChars(codeListNameString.substring(5, 9), c -> c > '\u001F' && c < '\u007F');
                    codeListName = stripChars(codeListNameString.substring(11, 67).trim(), c -> c > '\u001F' && c < '\u007F');
                    codeListContext = stripChars(codeListNameString.substring(67, 70).trim(), c -> c > '\u001F' && c < '\u007F');

                    reader.readLine();
/*
     Desc: Code indicating an action associated with a line of a
*/
                    codeListDescString = reader.readLine().substring(11);
                    do {
                        line = reader.readLine();
                        if (StringUtils.isNotEmpty(line)) {
                            codeListDescString = codeListDescString.concat(" ").concat(line.substring(11));
                        }
                    }

                    while (StringUtils.startsWith(line.trim(), "Repr:"));


                    reader.readLine();
                    reader.readLine();
                    do {
/*
     3     Occurrence span
              The service was performed based on an event occurring
              over a period of time.
*/
                        String codeLine = null;
                        String code = null;
                        String name = null;
                        do {
                            codeLine = reader.readLine();
                            if (codeLine == null) {
                                jsonldVocabulary.setContextObjectBuilder(getContext());
                                JSONLDContext jsonldContext = new JSONLDContext();
                                for (String key: classesGraph.keySet()){
                                    jsonldVocabulary.getGraphJsonArrayBuilder().add(classesGraph.get(key));
                                    jsonldContext.getContextObjectBuilder().add(key, Json.createObjectBuilder(Map.of(ID, StringUtils.join(UNECE_NS,":", key))).build());
                                    this.jsonldContext.getContextObjectBuilder().add(key, Json.createObjectBuilder(Map.of(ID, StringUtils.join(UNECE_NS,":", key))).build());
                                }
                                return;
                            }
                        } while (StringUtils.isEmpty(codeLine));

                        String changeIndicator = null;
                        if (StringUtils.isNotEmpty(codeLine)) {
                            if (codeLine.startsWith("---")) {
                                line = codeLine;
                                break;
                            } else if (StringUtils.isNotBlank(codeLine.substring(0, 1))) {
                                /*                          System.out.println("Change indicator - " + codeLine);*/
                                changeIndicator = codeLine.substring(0, 1);
                            }
                            code = codeLine.substring(5, 11).trim();
                            name = codeLine.substring(11).trim();
                        }

                        String description = null;
                        do {
                            line = reader.readLine();
                            if (StringUtils.isNotEmpty(line)) {
                                if (StringUtils.isNotBlank(line.substring(11, 13))) {
                                    name = name.concat(" ").concat(line.substring(11));
                                } else {
                                    description = line;
                                }
                            }
                        } while (StringUtils.isEmpty(line));

                        if (description == null) {
                            do {
                                description = reader.readLine();
                            } while (StringUtils.isEmpty(description));
                        }
                        try {
                            description = description.substring(14);
                        } catch (StringIndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
/*
              A code to indicate a finish to start constraint
              relationship.
*/
                        do {
                            line = reader.readLine();
                            if (StringUtils.isNotEmpty(line)) {
                                description = description.concat(" ").concat(line.substring(14));
                            }
                        }
                        while (StringUtils.isNotEmpty(line));

                        Set<String> codes = new HashSet<>();
                        code = stripChars(code, c -> c > '\u001F' && c < '\u007F');
                        name = stripChars(name, c -> c > '\u001F' && c < '\u007F');
                        if (codes.contains(code)) {
                            System.err.println("Duplicated name - ".concat(name));
                        } else codes.add(code);
                        description = stripChars(description, c -> c > '\u001F' && c < '\u007F');
                        JsonObjectBuilder rdfClass = Json.createObjectBuilder();
                        codeNS = UNCL_NS.concat(codeListID);
                        rdfClass.add(ID, StringUtils.join(codeNS, ":", code));
                        rdfClass.add(TYPE, StringUtils.join(UNECE_NS, ":", String.format("UNCL%sCode", codeListID)));
                        rdfClass.add(RDFS_COMMENT, description);
                        rdfClass.add(RDFS_LABEL, name);
                        rdfClass.add(RDF_VALUE, code);
                        if (changeIndicator != null) {
                            if (changeIndicator.equalsIgnoreCase("+"))
                                rdfClass.add(StringUtils.join(UNECE_NS, ":", "status"), "added");
                        }
                        vocabulary.getGraphJsonArrayBuilder().add(rdfClass);
                    }
                    while (StringUtils.isEmpty(line));
                }
                // read next line
                if(!line.startsWith("---")){
                    line = reader.readLine();
                } else {
                    JsonObjectBuilder rdfClass = Json.createObjectBuilder();
                    rdfClass.add(ID, StringUtils.join(UNECE_NS, ":", String.format("UNCL%sCode", codeListID)));
                    rdfClass.add(TYPE, RDFS_CLASS);
                    rdfClass.add(RDFS_COMMENT, codeListDescString);
                    rdfClass.add(RDFS_LABEL, codeListName);
                    classesGraph.put(String.format("UNCL%sCode", codeListID), rdfClass.build());
                    vocabulary.setOutputFile(StringUtils.join(UNCL_NS, "/", codeNS, ".jsonld"));
                    vocabulary.setPrettyPrint(true);
                    vocabulary.setContextObjectBuilder(getContext());
                    vocabulary.getContextObjectBuilder().add(codeNS, String.format(NS_MAP.get(UNECE_NS).concat("%s#"), codeNS));
                    this.jsonldContext.getContextObjectBuilder().add(codeNS, String.format(NS_MAP.get(UNECE_NS).concat("%s#"), codeNS));
                    vocabularies.add(vocabulary);
                    vocabulary = new JSONLDVocabulary();
                }
            }
            reader.close();
        }catch (IOException e){
            System.err.println(e.getMessage());
        }
    }
    static String stripChars(String s, IntPredicate include) {
        return s.codePoints().filter(include::test).collect(StringBuilder::new,
                StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

}
