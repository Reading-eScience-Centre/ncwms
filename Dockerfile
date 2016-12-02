FROM unidata/tomcat-docker:8
MAINTAINER Kyle Wilcox <kyle@axiomdatascience.com>

RUN \
    apt-get update && \
    apt-get install -y \
    unzip \
    openjdk-8-jdk \
    maven \
    git

# Fix for maven missing sting library
WORKDIR /usr/share/maven/lib
RUN ln -s ../../java/commons-lang.jar .

# Change java version for edal 
RUN update-java-alternatives --set java-1.8.0-openjdk-amd64

# Compile edal to avoid the broken version 1.2.4
# - default WORKDIR is /usr/local/tomcat
WORKDIR /usr/local/edal
RUN git clone https://github.com/yosoyjay/edal-java.git
WORKDIR /usr/local/edal/edal-java
RUN git checkout dev
RUN mvn clean install

# Compile and install ncWMS
WORKDIR /usr/local/ncWMS
COPY . ./
RUN mvn clean install
RUN unzip target/ncWMS2.war -d $CATALINA_HOME/webapps/ncWMS/

# Set login-config to BASIC since it is handled through Tomcat
RUN sed -i -e 's/DIGEST/BASIC/' $CATALINA_HOME/webapps/ncWMS/WEB-INF/web.xml

# Tomcat users
COPY config/tomcat-users.xml $CATALINA_HOME/conf/tomcat-users.xml
# Java options
COPY config/javaopts.sh $CATALINA_HOME/bin/javaopts.sh

# Create context config file
COPY config/ncWMS.xml $CATALINA_HOME/conf/Catalina/localhost/ncWMS.xml

# Set permissions
RUN chown -R tomcat:tomcat "$CATALINA_HOME"

COPY entrypoint.sh /
ENTRYPOINT ["/entrypoint.sh"]

EXPOSE 8080 8443 9090
CMD ["catalina.sh", "run"]
