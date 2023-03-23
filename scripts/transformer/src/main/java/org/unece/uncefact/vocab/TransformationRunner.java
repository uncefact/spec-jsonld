package org.unece.uncefact.vocab;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.unece.uncefact.vocab.transformers.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.unece.uncefact.vocab.Transformer.*;

public class TransformationRunner {
    final static Options options = new Options();

    public static void main(String[] args) throws ParseException, IOException, InvalidFormatException {
        Attributes mainAttributes = readProperties();
        String version = mainAttributes.getValue("Implementation-Version");

        Option transformationTypeOption = new Option("t", true, "transformation type.\n" +
                String.format("Allowed values: %s, %s, %s, %s, %s, %s.\n", UNECE_NS, REC20_NS, REC21_NS, REC24_NS, REC28_NS, UNLOCODE_NS) +
                String.format("Default value: %s.", UNECE_NS));
        Option inputFileOption = new Option("i", true, "an input file or files to be transformed. \n" +
                String.format("- %s type requires two files - XLSX with BSP subset and text file with UNCL code lists\n", UNECE_NS) +
                String.format("- %s type requires CSV files with UN/LOCODE and Subdivision codes\n", UNLOCODE_NS));
        Option prettyPrintOption = new Option("p", "pretty-print",false, "an output file to be created as a result of transformation. Default value: output.jsonld.");
        Option versionOption = new Option("?", "version", false, "display this help.");

        options.addOption(transformationTypeOption);
        options.addOption(inputFileOption);
        options.addOption(prettyPrintOption);
        options.addOption(versionOption);

        HelpFormatter formatter = new HelpFormatter();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(versionOption.getOpt())) {
            formatter.printHelp(String.format("java -jar vocab-transformer-%s.jar", version), options);
            return;
        }

        Set<String> inputFileNames = new TreeSet<>();
        boolean prettyPrint = false;
        String transformationType = UNECE_NS;
        Transformer transformer = null;
        Iterator<Option> optionIterator = cmd.iterator();
        while (optionIterator.hasNext()) {
            Option option = optionIterator.next();
            String opt = StringUtils.defaultIfEmpty(option.getOpt(), "");
            if (opt.equals(inputFileOption.getOpt())) {
                inputFileNames.add(option.getValue());
            } else if (opt.equals(prettyPrintOption.getOpt())) {
                prettyPrint = Boolean.TRUE;
            } else if (opt.equals(transformationTypeOption.getOpt())) {
                transformationType = option.getValue();
            }
        }
        String inputFileName = null;
        if (inputFileNames.isEmpty()){
            //default value
            inputFileName = null;
        } else {
            inputFileName = inputFileNames.iterator().next();
        }

        switch (transformationType.toLowerCase()) {
            case UNECE_NS:
                BSPJSONSchemaToJSONLDVocabulary bspSchema = new BSPJSONSchemaToJSONLDVocabulary(null, "/D22A/UNECE-BSPContextCCL.json");
                bspSchema.transform();
                JSONLDVocabulary bspSchemaVocabulary = bspSchema.getJsonldVocabulary();
                new FileGenerator().generateFile(bspSchemaVocabulary.getContextObjectBuilder(), bspSchemaVocabulary.getGraphJsonArrayBuilder(), true, String.format("%s-from-schema-final.jsonld", UNECE_NS));
                break;

            case UNLOCODE_NS:
                Set<String> defaultInputNames = new TreeSet<>();
                defaultInputNames.add("/loc221csv/2022-1 UNLOCODE CodeListPart1.csv");
                defaultInputNames.add("/loc221csv/2022-1 UNLOCODE CodeListPart2.csv");
                defaultInputNames.add("/loc221csv/2022-1 UNLOCODE CodeListPart3.csv");
                defaultInputNames.add("/loc221csv/2022-1 SubdivisionCodes.csv");
                transformer = new UNLOCODEToJSONLDVocabulary(inputFileNames, defaultInputNames, prettyPrint);
                break;
        }
        if (transformer!=null)
            transformer.transform();
    }

    public static Attributes readProperties() throws IOException {
        final InputStream resourceAsStream = ClassLoader.getSystemResourceAsStream("META-INF/MANIFEST.MF");
        final Manifest manifest = new Manifest(resourceAsStream);
        final Attributes mainAttributes = manifest.getMainAttributes();
        return mainAttributes;
    }
}
