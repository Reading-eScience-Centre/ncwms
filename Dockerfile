FROM unidata/tomcat-docker:8.5
MAINTAINER Kyle Wilcox <kyle@axiomdatascience.com>

RUN \
    apt-get update && \
    apt-get install -y \
        git \
        maven \
        unzip \
        && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Compile edal to use required features in develop branch
ENV EDAL_VERSION edal-1.5.2
# Remove this for releases
#ENV EDAL_VERSION develop
RUN mkdir /edal && \
    cd /edal && \
    git clone https://github.com/Reading-eScience-Centre/edal-java.git && \
    cd edal-java && \
    git checkout ${EDAL_VERSION} && \
    JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn clean install

# Compile and install ncWMS
COPY . /ncWMS
RUN cd /ncWMS && \
    JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn clean install && \
    unzip target/ncWMS2.war -d $CATALINA_HOME/webapps/ncWMS2/ && \
    rm -rf /edal && \
    rm -rf /ncWMS/target

# Set login-config to BASIC since it is handled through Tomcat
RUN sed -i -e 's/DIGEST/BASIC/' $CATALINA_HOME/webapps/ncWMS2/WEB-INF/web.xml && \
    cp /ncWMS/config/setenv.sh $CATALINA_HOME/bin/setenv.sh && \
    cp /ncWMS/config/ecache.xml $CATALINA_HOME/conf/ecache.xml && \
    cp /ncWMS/config/tomcat-users.xml $CATALINA_HOME/conf/tomcat-users.xml && \
    mkdir -p $CATALINA_HOME/conf/Catalina/localhost/ && \
    cp /ncWMS/config/ncWMS.xml $CATALINA_HOME/conf/Catalina/localhost/ncWMS.xml && \
    mkdir -p $CATALINA_HOME/.ncWMS2 && \
    cp /ncWMS/config/config.xml $CATALINA_HOME/.ncWMS2/config.xml

ENTRYPOINT ["/ncWMS/entrypoint.sh"]

EXPOSE 8080 8443 9090
CMD ["catalina.sh", "run"]
