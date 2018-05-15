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

# Fix for java8 in jessie
# https://serverfault.com/questions/830636/cannot-install-openjdk-8-jre-headless-on-debian-jessie/830637#830637
# https://askubuntu.com/questions/190582/installing-java-automatically-with-silent-option
RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main\ndeb-src http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main" > /etc/apt/sources.list.d/webupd8team-java.list && \
     apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 && \
     apt-get update && \
     echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
     echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections && \
     apt-get install -y oracle-java8-installer && \
     update-java-alternatives -s java-8-oracle && \
     export JAVA_HOME=/usr/lib/jvm/java-8-oracle

# Compile edal to use required features in develop branch
ENV EDAL_VERSION edal-1.4.1
RUN mkdir /edal && \
    cd /edal && \
    git clone https://github.com/Reading-eScience-Centre/edal-java.git && \
    cd edal-java && \
    git checkout ${EDAL_VERSION} && \
    JAVA_HOME=/usr/lib/jvm/java-8-oracle mvn clean install

# Compile and install ncWMS
COPY . /ncWMS
RUN cd /ncWMS && \
    JAVA_HOME=/usr/lib/jvm/java-8-oracle mvn clean install && \
    unzip target/ncWMS2.war -d $CATALINA_HOME/webapps/ncWMS/ && \
    rm -rf /edal && \
    rm -rf /ncWMS/target

# Set login-config to BASIC since it is handled through Tomcat
RUN sed -i -e 's/DIGEST/BASIC/' $CATALINA_HOME/webapps/ncWMS/WEB-INF/web.xml && \
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
