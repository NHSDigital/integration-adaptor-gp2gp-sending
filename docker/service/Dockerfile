FROM gradle:8.5-jdk21 AS build

COPY --chown=gradle:gradle service /home/gradle/service

WORKDIR /home/gradle/service

RUN gradle classes

FROM build AS package

RUN gradle --build-cache bootJar

FROM eclipse-temurin:21-jre-jammy

EXPOSE 8080

RUN mkdir /app

COPY --from=package /home/gradle/service/build/libs/gp2gp-fhir-send-adaptor.jar /app/gp2gp-fhir-send-adaptor.jar

USER 65534

ENTRYPOINT ["java", "-XX:+PrintCommandLineFlags", "-cp", "/app/gp2gp-fhir-send-adaptor.jar", "-Dloader.main=uk.nhs.adaptors.gp2gp.Gp2gpApplication", "org.springframework.boot.loader.launch.PropertiesLauncher"]

