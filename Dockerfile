# Etapa 1: compilar el proyecto con Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagen liviana solo con el JRE y el .jar
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 app && mkdir -p /app/efs && chown app /app/efs
COPY --from=build /build/target/eventos.jar app.jar
USER app
# Los tickets se generan en /app/efs, donde se monta el EFS de la instancia EC2
ENV EFS_PATH=/app/efs
EXPOSE 8080
# La wallet de Oracle Cloud se monta en /app/wallet al ejecutar el contenedor
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
