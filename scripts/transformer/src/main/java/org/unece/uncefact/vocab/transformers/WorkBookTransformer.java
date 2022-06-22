package org.unece.uncefact.vocab.transformers;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.unece.uncefact.vocab.Transformer;

import java.io.File;
import java.io.IOException;

public abstract class WorkBookTransformer extends Transformer {
    protected WorkBookTransformer(String inputFile, String outputFile, boolean prettyPrint) {
        super(inputFile, outputFile, prettyPrint);
    }

    public void transform() throws IOException, InvalidFormatException {
        Workbook workbook = WorkbookFactory.create(new File(inputFile));
        readInputFileToGraphArray(workbook);
        super.transform();
    }
}
