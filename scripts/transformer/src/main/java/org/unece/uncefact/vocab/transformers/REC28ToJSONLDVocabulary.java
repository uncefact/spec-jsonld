package org.unece.uncefact.vocab.transformers;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class REC28ToJSONLDVocabulary extends WorkBookTransformer {
    public REC28ToJSONLDVocabulary(String inputFile, String outputFile, boolean prettyPrint) {
        super(inputFile, outputFile, prettyPrint);

        contextObjectBuilder.add(REC28_NS, NS_MAP.get(REC28_NS));
    }

    public void readInputFileToGraphArray(final Object object) {
        Workbook workbook = (Workbook) object;
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 4; i++) {
            rowIterator.next();
        }
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            // Accessing Values by Column Index
            /*CI,Mode,Code-A,Code-B,Name,Description*/

            String mode = getCellValue(row, 1, true);
            String codeA = getCellValue(row, 2, true);
            String codeB = getCellValue(row, 3, true);
            String name = getCellValue(row, 4);
            String code = StringUtils.join(mode, codeA, codeB);
            if (codes.contains(code)) {
                System.err.println("Duplicated name - ".concat(name));
            } else codes.add(code);
            String description = getCellValue(row, 5);

            JsonObjectBuilder rdfClass = Json.createObjectBuilder();
            rdfClass.add(ID, StringUtils.join(REC28_NS,":",code));
            rdfClass.add(TYPE, StringUtils.join(UNECE_NS,":","UNECERec28Code"));
            rdfClass.add(RDFS_COMMENT, description);
            rdfClass.add(RDFS_LABEL, name);
            rdfClass.add(RDF_VALUE, code);
            graphJsonArrayBuilder.add(rdfClass);
        }
    }

}
