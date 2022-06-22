package org.unece.uncefact.vocab.transformers;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.json.*;
import java.util.*;

public class REC20ToJSONLDVocabulary extends WorkBookTransformer {
    protected static String REC20_NS = "rec20";

    public REC20ToJSONLDVocabulary(String inputFile, String outputFile, boolean prettyPrint) {
        super(inputFile, outputFile, prettyPrint);

        contextObjectBuilder.add(REC20_NS, "https://service.unece.org/trade/uncefact/vocabulary/uncefact/rec20#");
    }

    public void readInputFileToGraphArray(final Object object) {
        Workbook workbook = (Workbook) object;
        Sheet sheet = workbook.getSheetAt(2);
        Iterator<Row> rowIterator = sheet.rowIterator();

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
            rdfClass.add(ID, StringUtils.join(REC20_NS,":",code));
            rdfClass.add(TYPE, StringUtils.join(UNECE_NS,":","UNECERec20Code"));
            rdfClass.add(RDFS_COMMENT, description);
            rdfClass.add(RDFS_LABEL, name);
            rdfClass.add(RDF_VALUE, code);
            rdfClass.add(StringUtils.join(UNECE_NS,":","levelCategory"), levelCategory);
            rdfClass.add(StringUtils.join(UNECE_NS,":","symbol"), symbol);
            rdfClass.add(StringUtils.join(UNECE_NS,":","conversionFactor"), conversionFactor);
            rdfClass.add(StringUtils.join(UNECE_NS,":","status"), status);
            graphJsonArrayBuilder.add(rdfClass);
        }
    }

}
