package org.unece.uncefact.vocab;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;

public abstract class Transformer {
    protected String inputFile;
    protected String outputFile;

    Transformer(String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public abstract void transform() throws IOException, InvalidFormatException;
}
