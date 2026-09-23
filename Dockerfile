# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-noble AS maven
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw \
    && ./mvnw -B -ntp -DskipTests package \
    && mkdir -p /out \
    && for m in gateway identity-service catalog-service cart-service inventory-service order-service payment-service review-service; do \
         jar=$(find "$m/target" -maxdepth 1 -name "$m-*.jar" ! -name '*.original'); \
         cp "$jar" "/out/$m.jar"; \
       done

FROM eclipse-temurin:25-jre-noble AS runtime
WORKDIR /app
ARG MODULE
RUN groupadd --system store && useradd --system --gid store --no-create-home store
COPY --from=maven /out/${MODULE}.jar app.jar
USER store
ENTRYPOINT ["java", "-jar", "app.jar"]
