package org.unece.uncefact.vocab.transformers;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.unece.uncefact.vocab.Transformer;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class REC21ToJSONLDVocabulary extends Transformer {
    protected static String REC21_NS = "rec21";

    public REC21ToJSONLDVocabulary(String inputFile, String outputFile, boolean prettyPrint) {
        super(inputFile, outputFile, prettyPrint);

        contextObjectBuilder.add(REC21_NS, "https://service.unece.org/trade/uncefact/vocabulary/uncefact/rec21#");
    }

    public void readInputFileToGraphArray(final Workbook workbook){
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            rowIterator.next();
        }
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            // Accessing Values by Column Index
            // Status,Code,Name,Description,Numeric code
            String status = getCellValue(row, 0);
            String code = getCellValue(row, 1);
            String name = getCellValue(row, 2);
            if (codes.contains(code)) {
                System.err.println("Duplicated name - ".concat(name));
            } else codes.add(code);
            String description = getCellValue(row, 3);
            String numericCode = getCellValue(row, 4, true);

            JsonObjectBuilder rdfClass = Json.createObjectBuilder();
            rdfClass.add(ID, StringUtils.join(REC21_NS,":",code));
            rdfClass.add(TYPE, StringUtils.join(UNECE_NS,":","UNECERec21Code"));
            rdfClass.add(RDFS_COMMENT, description);
            rdfClass.add(RDFS_LABEL, name);
            rdfClass.add(RDF_VALUE, code);
            rdfClass.add(StringUtils.join(UNECE_NS,":","numericCode"), numericCode);
            graphJsonArrayBuilder.add(rdfClass);
        }
    }

}
