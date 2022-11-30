package org.unece.uncefact.vocab.transformers;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.unece.uncefact.vocab.Transformer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class WorkBookTransformer extends Transformer {

    public WorkBookTransformer(String inputFile, String defaultFile) {
        super(inputFile, defaultFile);
    }

    public void transform() throws IOException, InvalidFormatException {
        Workbook workbook;
        if (inputFile == null){
            workbook = WorkbookFactory.create(getClass().getResourceAsStream(defaultFile));
        } else {
            workbook = WorkbookFactory.create(new File(inputFile));
        }
        readInputFileToGraphArray(workbook);
        super.transform();
    }
}
