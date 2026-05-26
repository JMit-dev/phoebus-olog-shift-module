# Stage 1: Build the shift module JAR
FROM maven:3.9-eclipse-temurin-17 AS module-build
WORKDIR /build
COPY pom.xml .
COPY src/ src/
RUN mvn clean package -DskipTests

# Stage 2: Inject the module into the published phoebus-olog image
FROM ghcr.io/olog/org-phoebus-service-olog:latest
USER root

COPY --from=module-build /build/target/olog-shift-module-*.jar /olog-target/shift-module.jar

# Switch the fat JAR launcher from JarLauncher to PropertiesLauncher so that
# loader.path is respected at startup, allowing the module JAR to be appended
# to the classpath without rebuilding the olog JAR.
# Uses the JDK jar tool (already present) — no extra packages needed.
RUN OLOG_JAR=$(ls /olog-target/service-olog-*.jar) && \
    mkdir -p /tmp/mf && cd /tmp/mf && \
    jar xf "$OLOG_JAR" META-INF/MANIFEST.MF && \
    sed -i 's/JarLauncher/PropertiesLauncher/g' META-INF/MANIFEST.MF && \
    jar uf "$OLOG_JAR" META-INF/MANIFEST.MF && \
    rm -rf /tmp/mf

USER olog
WORKDIR /olog-target
EXPOSE 8080 8181
CMD java -Dloader.path=/olog-target/shift-module.jar -jar service-olog-*.jar
