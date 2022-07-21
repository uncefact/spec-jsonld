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

public class TransformationRunner {
    final static Options options = new Options();

    public static void main(String[] args) throws ParseException, IOException, InvalidFormatException {
        Attributes mainAttributes = readProperties();
        String version = mainAttributes.getValue("Implementation-Version");

        Option transformationTypeOption = new Option("t", true, "transformation type.\n" +
                "Allowed values: BSP, REC20.\n" +
                "Default value: BSP.");
        Option inputFileOption = new Option("i", true, "an input file to be transformed. Accepted format for BSP type is xls. Required.");
        Option outputFileOption = new Option("o", true, "an output file to be created as a result of transformation. Default value: output.jsonld.");
        Option prettyPrintOption = new Option("p", "pretty-print",false, "an output file to be created as a result of transformation. Default value: output.jsonld.");
        Option versionOption = new Option("?", "version", false, "display this help.");

        options.addOption(transformationTypeOption);
        options.addOption(inputFileOption);
        options.addOption(outputFileOption);
        options.addOption(prettyPrintOption);
        options.addOption(versionOption);

        HelpFormatter formatter = new HelpFormatter();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(versionOption.getOpt())) {
            formatter.printHelp(String.format("java -jar vocab-transformer-%s.jar", version), options);
            return;
        } else if(!cmd.hasOption(inputFileOption.getOpt())){
            throw new MissingOptionException(inputFileOption.getOpt());
        }

        Set<String> inputFileNames = new TreeSet<>();
        String outputFileName = "output.jsonld";
        boolean prettyPrint = false;
        String transformationType = "bsp";
        Transformer transformer = null;
        Iterator<Option> optionIterator = cmd.iterator();
        while (optionIterator.hasNext()) {
            Option option = optionIterator.next();
            String opt = StringUtils.defaultIfEmpty(option.getOpt(), "");
            if (opt.equals(inputFileOption.getOpt())) {
                inputFileNames.add(option.getValue());
            } else if (opt.equals(outputFileOption.getOpt())) {
                outputFileName = option.getValue();
            } else if (opt.equals(prettyPrintOption.getOpt())) {
                prettyPrint = Boolean.TRUE;
            } else if (opt.equals(transformationTypeOption.getOpt())) {
                transformationType = option.getValue();
            }
        }
        String inputFileName = inputFileNames.iterator().next();
        switch (transformationType.toLowerCase()) {
            case "bsp":
                transformer = new BSPToJSONLDVocabulary(inputFileName, outputFileName, prettyPrint);
                break;
            case "rec20":
                transformer = new REC20ToJSONLDVocabulary(inputFileName, outputFileName, prettyPrint);
                break;
            case "rec21":
                transformer = new REC21ToJSONLDVocabulary(inputFileName, outputFileName, prettyPrint);
                break;
            case "rec24":
                transformer = new REC24ToJSONLDVocabulary(inputFileName, outputFileName, prettyPrint);
                break;
            case "rec28":
                transformer = new REC28ToJSONLDVocabulary(inputFileName, outputFileName, prettyPrint);
                break;
            case "uncl":
                transformer = new UNCLToJSONLDVocabulary(inputFileName, outputFileName, prettyPrint);
                break;
            case "unlocode":
                transformer = new UNLOCODEToJSONLDVocabulary(null, null, prettyPrint);
                transformer.setInputFiles(inputFileNames);
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
