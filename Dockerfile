FROM unidata/tomcat-docker:8
MAINTAINER Kyle Wilcox <kyle@axiomdatascience.com>

RUN \
    apt-get update && \
    apt-get install -y \
    unzip \
#    openjdk-8-jdk \
    maven \
    git

# Fix for maven missing sting library
WORKDIR /usr/share/maven/lib
RUN ln -s ../../java/commons-lang.jar .

# Change java version for edal 
#RUN update-java-alternatives --set java-1.8.0-openjdk-amd64

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

# # Compile edal to avoid the broken version 1.2.4
# # - default WORKDIR is /usr/local/tomcat
 WORKDIR /usr/local/edal
 RUN git clone https://github.com/yosoyjay/edal-java.git
 WORKDIR /usr/local/edal/edal-java
 RUN git checkout dev
 ENV JAVA_HOME /usr/lib/jvm/java-8-oracle
# RUN update-java-alternatives -l 
 RUN mvn clean install

# 
# # Compile and install ncWMS
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
