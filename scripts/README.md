Transform scripts go here.

Transformation tool is a java program, binary is released as a jar file and can be run with the following command:
```
java -jar vocab-transformer.jar -?
```
The option `-?` will list the available types for transformation:

- `unece` - the default type to transform BSP subset from XLS input file and text file with UNCL code lists.
- `rec20` - produces the output for Recommendation 20
- `rec21` - produces the output for Recommendation 21
- `rec24` - produces the output for Recommendation 24
- `rec28` - produces the output for Recommendation 24
- `unlcd` - UN/LOCODE output generation

<i>Please note that UN/LOCODE is big set of data, and it takes time and resource to produce the output. 
The size of the output file is about 40Mb.</i>

By default, the input data is taken from the src/main/resources directory, but you can override the input files with `-i` option:

```
java -jar vocab-transformer.jar -t rec20 -i /path/to/your/file
```

But keep in mind the file format and data structure should be the same as for the default input files.

The output files are the JSON-LD vocabulry definitions along with context files.
Currently, we have two contexts:
- `unece` for BSP subset and code lists
- `unlcd` for UN/LOCODEs

To get the latest version from main branch go to https://github.com/uncefact/vocab/actions/workflows/package.yml and checkout the artifacts for the latest run. The complete release process to be set up soon.
