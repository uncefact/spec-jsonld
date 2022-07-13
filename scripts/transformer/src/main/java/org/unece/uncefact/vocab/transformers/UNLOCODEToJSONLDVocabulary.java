package org.unece.uncefact.vocab.transformers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.unece.uncefact.vocab.Transformer;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class UNLOCODEToJSONLDVocabulary extends Transformer {
    protected static String UNLOCODE_NS = "unlocode";

    public UNLOCODEToJSONLDVocabulary(String inputFile, String outputFile, boolean prettyPrint) {
        super(null, outputFile, prettyPrint);

    }

    protected void setContext (){
        super.setContext();
        contextObjectBuilder.add(UNLOCODE_NS, "https://service.unece.org/trade/uncefact/vocabulary/unlocode/");
    }

    public void transform() throws IOException {
        try {
            Files.createDirectory(Paths.get(UNLOCODE_NS));
        }catch (FileAlreadyExistsException e){
            System.err.println(String.format("Output directory %s already exists, please remove it and repeat.", UNLOCODE_NS));
            throw e;
        }
        for (String file:inputFiles) {
            Reader reader = Files.newBufferedReader(Paths.get(file), Charset.forName("ISO-8859-1"));
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
            readInputFileToGraphArray(csvParser.getRecords());
            csvParser.close();
        }
    }
    public void readInputFileToGraphArray(final Object object) {
            Map<String, String> countryMapping = new TreeMap<>();
            List<CSVRecord> records = (List<CSVRecord>) object;
            String[][] data = new String[(int) records.size()][11];
            for (int i=0;i<records.size();i++){
                for (int j=0;j<11;j++)
                    data[i][j] = records.get(i).get(j);
            }
            for (int i=0; i<data.length; i++){
                if (StringUtils.isEmpty(data[i][2])) {
                    String country = data[i][3];
                    if (country.startsWith("."))
                        country = country.substring(1);
                    countryMapping.put(data[i][1], country);
                }
                else if (!data[i][1].equalsIgnoreCase("=")){
                    String locode = data[i][1].concat(data[i][2]);
                    String description = data[i][3];
                    String descriptionWithoutSpecialSymbols = data[i][4];
                    String region = data[i][5];
                    JsonObjectBuilder rdfClass = Json.createObjectBuilder();
                    rdfClass.add(ID, StringUtils.join(UNLOCODE_NS, ":", locode));
                    rdfClass.add(TYPE, StringUtils.join(UNLOCODE_NS, ":", "UNLOCODE"));
                    rdfClass.add(RDFS_COMMENT, description);
                    rdfClass.add(RDF_VALUE, locode);
                    if(StringUtils.isNotEmpty(region)) {
                        rdfClass.add(StringUtils.join(UNECE_NS, ":", "region"), region);
                    }
                    rdfClass.add(StringUtils.join(UNECE_NS, ":", "country"), countryMapping.get(data[i][1]));
                    outputFile = "unlocode/".concat(locode).concat(".jsonld");
                    graphJsonArrayBuilder.add(rdfClass);
                    try {
                        super.transform();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InvalidFormatException e) {
                        throw new RuntimeException(e);
                    }
                    graphJsonArrayBuilder = Json.createArrayBuilder();
                    setContext();
                }
            }

            /*Iterator<Row> rowIterator = sheet.rowIterator();

            Set<String> codes = new HashSet<>();

            for (int i = 0; i < 5; i++) {
                rowIterator.next();
            }
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // Accessing Values by Column Index
                // Status,"Common Code",Name,Description,"Level /Category",Symbol,Conversion Factor
                String status = getCellValue(row, 0);
                String code = getCellValue(row, 1);
                String name = getCellValue(row, 2);
                if (codes.contains(code)) {
                    System.err.println("Duplicated name - ".concat(name));
                } else codes.add(code);
                String description = getCellValue(row, 3);
                String levelCategory = getCellValue(row, 4);
                String symbol = getCellValue(row, 5);
                String conversionFactor = getCellValue(row, 6);

                JsonObjectBuilder rdfClass = Json.createObjectBuilder();
                rdfClass.add(ID, StringUtils.join(UNLOCODE_NS, ":", code));
                rdfClass.add(TYPE, StringUtils.join(UNECE_NS, ":", "UNECERec20Code"));
                rdfClass.add(RDFS_COMMENT, description);
                rdfClass.add(RDFS_LABEL, name);
                rdfClass.add(RDF_VALUE, code);
                rdfClass.add(StringUtils.join(UNECE_NS, ":", "levelCategory"), levelCategory);
                rdfClass.add(StringUtils.join(UNECE_NS, ":", "symbol"), symbol);
                rdfClass.add(StringUtils.join(UNECE_NS, ":", "conversionFactor"), conversionFactor);
                rdfClass.add(StringUtils.join(UNECE_NS, ":", "status"), status);
                graphJsonArrayBuilder.add(rdfClass);
            }*/
    }

}
