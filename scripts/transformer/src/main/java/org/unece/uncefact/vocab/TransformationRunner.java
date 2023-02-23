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
                String.format("- %s type requires a XLSX file with Recommendation 20 code list\n", REC20_NS) +
                String.format("- %s type requires a XLS file with Recommendation 21 code list\n", REC21_NS) +
                String.format("- %s type requires a XLS file with Recommendation 24 code list\n", REC24_NS) +
                String.format("- %s type requires a XLS file with Recommendation 28 code list\n", REC28_NS) +
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
            case "bsp":
                BSPJSONSchemaToJSONLDVocabulary bspSchema = new BSPJSONSchemaToJSONLDVocabulary(null, "/D22A/UNECE-BSPContextCCL.json");
                bspSchema.transform();
                JSONLDVocabulary bspSchemaVocabulary = bspSchema.getJsonldVocabulary();
                new FileGenerator().generateFile(bspSchemaVocabulary.getContextObjectBuilder(), bspSchemaVocabulary.getGraphJsonArrayBuilder(), true, String.format("%s-from-schema-fixed.jsonld", UNECE_NS));

                break;

            case UNECE_NS:
                String uneceFile = null;
                String unclFile = null;
                Iterator<String> iterator = inputFileNames.iterator();
                if (!inputFileNames.isEmpty()){
                    while (iterator.hasNext()){
                        String fileName = iterator.next();
                        if (fileName.endsWith(".xlsx")){
                            uneceFile = fileName;
                        } else {
                            unclFile = iterator.next();
                        }
                    }
                }
                BSPToJSONLDVocabulary bspTransformer = new BSPToJSONLDVocabulary(uneceFile, "/BSP D20A Context CCL.xlsx");
                UNCLToJSONLDVocabulary unclTransformer = new UNCLToJSONLDVocabulary(unclFile, "/UNCL.21B");
                bspTransformer.transform();
                unclTransformer.transform();
                JSONLDVocabulary bspVocabulary = bspTransformer.getJsonldVocabulary();
                JSONLDVocabulary unclVocabulary = unclTransformer.getJsonldVocabulary();
                JSONLDVocabulary combinedVocabulary = new JSONLDVocabulary();
                combinedVocabulary.setContextObjectBuilder(bspVocabulary.getContextObjectBuilder().addAll(unclVocabulary.getContextObjectBuilder()));
                combinedVocabulary.setGraphJsonArrayBuilder(bspVocabulary.getGraphJsonArrayBuilder().addAll(unclVocabulary.getGraphJsonArrayBuilder()));
                new FileGenerator().generateFile(combinedVocabulary.getContextObjectBuilder(), combinedVocabulary.getGraphJsonArrayBuilder(), true, String.format("%s.jsonld", UNECE_NS));
                JSONLDContext bspContext = bspTransformer.getJsonldContext();
                JSONLDContext unclContext = unclTransformer.getJsonldContext();
                JSONLDContext combinedContext = new JSONLDContext();
                combinedContext.setContextObjectBuilder(bspContext.getContextObjectBuilder().addAll(unclContext.getContextObjectBuilder()));
                new FileGenerator().generateFile(combinedContext.getContextObjectBuilder(), null, true, String.format("%s-context.jsonld", UNECE_NS));
                break;
            case REC20_NS:
                transformer = new REC20ToJSONLDVocabulary(inputFileName, "/rec20_Rev17e-2021.xlsx");
                break;
            case REC21_NS:
                transformer = new REC21ToJSONLDVocabulary(inputFileName, "/rec21_Rev12e_Annex-V-VI_2021.xls");
                break;
            case REC24_NS:
                transformer = new REC24ToJSONLDVocabulary(inputFileName, "/rec24_Rev6e_2017.xls");
                break;
            case REC28_NS:
                transformer = new REC28ToJSONLDVocabulary(inputFileName, "/Rec28_Rev4.2e_2018.xls");
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
