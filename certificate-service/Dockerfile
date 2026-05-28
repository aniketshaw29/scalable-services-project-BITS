FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy parent POM and install it to local repo so the child can resolve it
COPY pom.xml ./pom.xml
RUN mvn -N install -q
# Copy and build just this service
COPY certificate-service/pom.xml ./certificate-service/pom.xml
COPY certificate-service/src ./certificate-service/src
RUN mvn -f certificate-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/certificate-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
