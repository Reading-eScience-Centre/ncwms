# Development

## Adding new colour palettes {#palettes}

To add new colour palettes to ncWMS, you must create palette files. These are text files with the extension ".pal". The name of the file will be the name of the palette. This files must contain one line for each colour in the palette. Intermediate colours will be interpolated. A colour is either of the form `#RRGGBB` or `#AARRGGBB` ('AA' representing the alpha-component), where values are in hexadecimal notation.

Once a palette file has been defined this will add 2 available palettes to ncWMS2 - the one you have defined as well as its inverse.

The palette files can be placed in a directory named `.palettes` within the main config directory (i.e. `~/.ncWMS2/.palettes/` by default) and will be picked up automatically after a server restart.  If you wish to place the palette files into a different directory, you need to define override the context parameter `paletteDirs` whose value is a comma-separated list of directories containing palette files. To do this on Tomcat, you should create a file named `$CATALINA_BASE/conf/[enginename]/[hostname]/[webappname].xml`.  For example, with the default webapp name running on localhost this is `$CATALINA_BASE/conf/Catalina/localhost/ncWMS2.xml`. Inside this file, create an entry of the form:

```
<Parameter name="paletteDirs" value="$CONFIGDIR/extra-palettes" override="false"/>
```

Note that you can use the variables `$CONFIGDIR` and `$HOME` which represent the config directory for ncWMS and the home directory of the user running the servlet container respectively.  These are special values - other environment variables cannot be used here. Since this setting is at the servlet container level, it will persist across redeployment of ncWMS2.


## Defining new style templates {#styles}

To create a new style for plotting, you will need to create an SLD template. The specification for EDAL-specific SLD documents can be found in [the appendix](../appendices/sld_spec/introduction.md). Within this template, you may use the following placeholders:

* $layerName
* $paletteName
* $scaleMin
* $scaleMax
* $logarithmic
* $numColorBands
* $bgColor
* $belowMinColor
* $aboveMaxColor
* $opacity

In addition to the `$layerName` parameter, you may add children of the named layer, according to the role they have to their parent layer. For example, if a named layer has two children with roles "mag" and "dir" (this is the case for a vector layer), you may specify them as `$layerName-mag` and `$layerName-dir` . This style will then be supported only for parent layers with such children.

For further examples, see the existing style templates in the [edal-graphics module](https://github.com/Reading-eScience-Centre/edal-java/tree/master/graphics/src/main/resources/styles).

Once you have created these templates, you may either place them in a subdirectory of the main config directory named `.styles`, or override the `styleDirs` context parameter, similar to the palettes directory above.

## Adding new data readers {#datareaders}

By default ncWMS supports reading of gridded NetCDF/GRIB/OPeNDAP data, and in-situ data from the EN3/4 UK Met Office dataset. To read additional types of data, a new data reader must be written. To do this, you must extend the class `uk.ac.rdg.resc.edal.dataset.DatasetFactory`. This has a single abstract method which returns a `uk.ac.rdg.resc.edal.dataset.Dataset` object given an ID and location.

The full details of how to implement this are beyond the scope of this guide, but it is recommended to example the two existing data readers: `uk.ac.rdg.resc.edal.dataset.cdm.CdmGridDatasetFactory` and `uk.ac.rdg.resc.edal.dataset.cdm.En3DatasetFactory`, which can be found in the edal-cdm module. The [EDAL Javadocs](http://reading-escience-centre.github.io/edal-java/apidocs/index.html) are reasonably complete and will be of great use. Additionally you may wish to contact the developers of ncWMS through the website, who will be happy to provide guidance and assistance.