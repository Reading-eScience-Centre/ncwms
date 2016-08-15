# For ncWMS 1.x Users

## Changes from ncWMS 1.x {#changes}

The following is a list of some of the major changes from ncWMS 1.x:

*   Support for SLD styling
*   Easy configuration of additional styles based on SLD templates
*   Support for in-situ data visualisation from the Met Office EN3/4 databases
*   Many more palettes and a simple configuration for adding new ones
*   An improved Godiva3 web client
*   Moved security configuration to the servlet container
*   Moved build system to Maven - this makes it easier for 3rd parties to build the project and use the EDAL libraries in their own projects
*   Updated to the latest NetCDF libraries from Unidata
*   Some changes to the API:
    *   GetMap only produces animations with the addition of an extra URL parameter: "animation"
    *   GetFeatureInfo now only returns text/XML
    *   GetTimeseries, GetVerticalProfile, are new methods which replace the previous PNG implementations for GetFeatureInfo

## Migrating from ncWMS 1.x {#migration}

Configuration for ncWMS v2 is very similar to that for ncWMS v1.x. Whilst the dataset configuration has changed quite a bit, old `config.xml` files from 1.x versions can be used on v2 (but not the other way around). Therefore, to migrate from v1.x to v2, only two steps need to be taken:

*   Copy the `config.xml` file from its old location (`~/.ncWMS/`) to the v2 location (`~/.ncWMS2/` by default - see [configuration](./03-config.md) for details on how to change this)
*   Configure your servlet container to add a security role for the ncWMS admin user (see [installation](./02-installation.md))
