package org.unece.uncefact.vocab;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.unece.uncefact.vocab.transformers.BSPToJSONLDVocabulary;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class TransformationRunner {
    final static Options options = new Options();

    public static void main(String[] args) throws ParseException, IOException, InvalidFormatException {
        Attributes mainAttributes = readProperties();
        String version = mainAttributes.getValue("Implementation-Version");

        Option transformationTypeOption = new Option("t", true, "transformation type. Allowed values: CCL.");
        Option inputFileOption = new Option("i", true, "an input file to be transformed. Accepted format for CCL type is xls.");
        Option outputFileOption = new Option("o", true, "an output file to be created as a result of transformation.");
        Option versionOption = new Option("?", "version", false, "display this help.");

        options.addOption(transformationTypeOption);
        options.addOption(inputFileOption);
        options.addOption(outputFileOption);
        options.addOption(versionOption);

        HelpFormatter formatter = new HelpFormatter();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(versionOption.getOpt())) {
            formatter.printHelp(String.format("java -jar vocab-transformer-%s.jar", version), options);
            return;
        }

        String inputFileName = "BSP D20A Context CCL.xlsx";
        String outputFileName = "output.jsonld";
        String transformationType = "bsp";
        Transformer transformer = null;
        Iterator<Option> optionIterator = cmd.iterator();
        while (optionIterator.hasNext()) {
            Option option = optionIterator.next();
            String opt = StringUtils.defaultIfEmpty(option.getOpt(), "");
            if (opt.equals(inputFileOption.getOpt())) {
                inputFileName = option.getValue();
            } else if (opt.equals(outputFileOption.getOpt())) {
                outputFileName = option.getValue();
            } else if (opt.equals(transformationTypeOption.getOpt())) {
                transformationType = option.getValue();
            }
        }
        switch (transformationType.toLowerCase()) {
            case "bsp":
                transformer = new BSPToJSONLDVocabulary(inputFileName, outputFileName);
                break;
        }
        transformer.transform();
    }

    public static Attributes readProperties() throws IOException {
        final InputStream resourceAsStream = ClassLoader.getSystemResourceAsStream("META-INF/MANIFEST.MF");
        final Manifest manifest = new Manifest(resourceAsStream);
        final Attributes mainAttributes = manifest.getMainAttributes();
        return mainAttributes;
    }
}
