FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B verify
FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/* && groupadd -r portal && useradd -r -g portal portal && mkdir -p /data /uploads && chown -R portal:portal /data /uploads /usr/local/tomcat
COPY --from=build /build/target/placement.war /usr/local/tomcat/webapps/ROOT.war
COPY deploy/server.xml /usr/local/tomcat/conf/server.xml
USER portal
EXPOSE 8080
CMD ["catalina.sh","run"]
