# Installation

## Getting ncWMS {#download}

ncWMS can be downloaded as either a Java webapp (to be run in a servlet container such as Tomcat), or as a standalone Java application. Generally speaking, the webapp version is recommended for most uses. However, the standalone version is recommended for users who wish to only use ncWMS on their own machine for exploring data, or for testing ncWMS to explore its functionality prior to installing the webapp version.

The latest release of ncWMS can be downloaded [here](https://github.com/Reading-eScience-Centre/edal-java/releases/latest).

## Standalone version {#standalone}

The standalone version of ncWMS requires no installation. It can be run from the command-line with the command `java -jar ncWMS2-standalone.jar` . This will run the WMS server locally with no security for administration and configuration. It will be available at [http://localhost:8080/](http://localhost:8080/). All configuration data is stored in a directory named `.ncWMS2` in the home directory of the user running the software.

## Servlet Container {#servlet}

ncWMS is a Java servlet which runs on a servlet container such as Tomcat, JBoss, or Glassfish.  Tomcat is the recommended servlet container and is what ncWMS is written and tested on. Installation is servlet-container dependent, but there are no ncWMS-specific procedures for installation.

### Servlet-level Configuration

Once ncWMS is up-and-running, on first launch it will create a configuration file and logging directory. By default this is located in a directory named `.ncWMS2` in the home directory of the user running the servlet container. **Note that the user running the servlet container must have write access to their home directory. This is not always the case for system users such as `tomcat7` or `nobody`.**

To change the location of the server configuration, you need to redefine the context parameter `configDir`. To do this on Tomcat, you should create a file named `$CATALINA_BASE/conf/[enginename]/[hostname]/[webappname].xml`.  For example, with the default webapp name running on localhost this is `$CATALINA_BASE/conf/Catalina/localhost/ncWMS2.xml`. Below is an example config with several options set, including `configDir`:

```
<?xml version='1.0' encoding='utf-8'?>
<Context>
    <Parameter name="configDir" value="$HOME/.ncWMS2-testserver" override="false"/>
    <Parameter name="paletteDirs" value="$HOME/.ncWMS2-testserver/palettes" override="false"/>
    <Parameter name="styleDirs" value="$HOME/.ncWMS2-testserver/styles" override="false"/>
    <Parameter name="defaultPalette" value="[whatever]" override="false"/>
    <Parameter name="advertisedPalettes" value="[whatever]" override="false"/>
</Context>
```

Note that `$HOME` represents the home directory of the user running **the servlet container** and is a special value - other environment variables cannot be used here. Since this setting is at the servlet container level, it will persist across redeploys of ncWMS2.


### Security configuration

Security for the administration of ncWMS is delegated to the servlet container (in standalone mode there is no security on any administration). You should define a security role with the name `ncWMS-admin`, and add users with that role. To do this on Tomcat, you could add the following to `tomcat-users.xml`:

```
<role rolename="ncWMS-admin" />
<user username="admin" password="ncWMS-password" roles="ncWMS-admin"/>
```

Access to the administration interface would then be granted to a user with the name `admin` and the password `ncWMS-password`
