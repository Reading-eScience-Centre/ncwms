[![Build Status](https://travis-ci.org/yosoyjay/ncwms.svg?branch=test_hook)](https://travis-ci.org/yosoyjay/ncwms)
# ncWMS2

ncWMS is a [Web Map Service](https://en.wikipedia.org/wiki/Web_Map_Service) for geospatial data that are stored in CF-compliant NetCDF files. The intention is to create a WMS that requires minimal configuration: the source data files should already contain most of the necessary metadata. ncWMS is developed and maintained by the Reading e-Science Centre ([ReSC](http://www.met.reading.ac.uk/resc/home/)) at the University of Reading, UK.

ncWMS v2 is build on top of the [EDAL]((https://reading-escience-centre.gitbooks.io/edal-user-guide/content/)) libraries developed by ReSC

# ncWMS on Docker

A feature full Tomcat (SSL over APR, etc.) running [ncWMS](http://www.resc.rdg.ac.uk/trac/ncWMS/)

### tl;dr

**Quickstart**

```bash
$ docker run \
    -d \
    -p 80:8080 \
    -p 443:8443 \
    -v /path/to/this/dir/config/config.xml:/usr/local/tomcat/.ncWMS2/config.xml
    ncwms:dockerize 
```

Note: `-v` arguments require absolute path.

**Production**

```bash
$ docker run \
    -d \
    -p 80:8080 \
    -p 443:8443 \
    -v /path/to/your/ssl.crt:/usr/local/tomcat/conf/ssl.crt \
    -v /path/to/your/ssl.key:/usr/local/tomcat/conf/ssl.key \
    -v /path/to/your/tomcat-users.xml:/usr/local/tomcat/conf/tomcat-users.xml \
    -v /path/to/your/ncwms/config:/usr/local/tomcat/.ncWMS2 \
    -e "ADVERTISED_PALETTES=div-RdBu" \
    -e "DEFAULT_PALETTE=div-RdBu" \
    --name ncwms \
    ncwms:dockerize 
```

## Configuration

### Tomcat

See [these instructions](https://github.com/axiom-data-science/docker-tomcat) for configuring Tomcat


### ncWMS

Mount your own `config` directory:

```bash
$ docker run \
    -v /path/to/your/ncwms/config/directory:/usr/local/tomcat/.ncWMS2 \
    ... \
    axiom/docker-ncwms
```

Set your own [default palette](https://github.com/Reading-eScience-Centre/edal-java/blob/cef96a148ea37be4dbfdf24096396607f5fe8b96/ncwms/src/main/webapp/WEB-INF/web.xml#L115-L121)

```bash
$ docker run \
    -e "DEFAULT_PALETTE=seq-BuYl" \
    ... \
    axiom/docker-ncwms
```

Set your own [advertised palettes](https://github.com/Reading-eScience-Centre/edal-java/blob/cef96a148ea37be4dbfdf24096396607f5fe8b96/ncwms/src/main/webapp/WEB-INF/web.xml#L122-L129)

```bash
$ docker run \
    -e "ADVERTISED_PALETTES=div-RdBu,div-RdBu-inv,seq-cubeYF" \
    ... \
    axiom/docker-ncwms
```


### Users

By default, Tomcat will start with [two user accounts](https://github.com/axiom-data-science/docker-ncwms/blob/master/files/tomcat-users.xml). The passwords are equal to the user name.

* `ncwms` - used to admin ncWMS
* `admin` - can be used by everything else (has full privileges)

## Licence
Copyright (c) 2010 The University of Reading
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University of Reading, nor the names of the
   authors or contributors may be used to endorse or promote products
   derived from this software without specific prior written permission.
4. If you wish to use, with or without modification, the Godiva web
   interface, the logo of the Reading e-Science Centre must be retained
   on the web page.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Authors and Contributors

[@guygriffiths](https://github.com/guygriffiths)
