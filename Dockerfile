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

# Explode the fat JAR so the shift module can be added as a regular file in
# BOOT-INF/lib/ rather than a nested-ZIP entry. Running in exploded mode avoids
# classpath.idx and nested-URL issues introduced in Spring Boot 4.x.
RUN mkdir -p /olog-app && \
    cd /olog-app && \
    jar xf /olog-target/service-olog-*.jar && \
    cp /olog-target/shift-module.jar BOOT-INF/lib/ && \
    echo '- "BOOT-INF/lib/shift-module.jar"' >> BOOT-INF/classpath.idx

USER olog
WORKDIR /olog-app
EXPOSE 8080 8181
CMD java org.springframework.boot.loader.launch.JarLauncher
