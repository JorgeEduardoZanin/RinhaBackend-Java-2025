
FROM ghcr.io/graalvm/native-image-community:24 AS builder
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -Pnative -DskipTests native:compile -B

FROM debian:bookworm-slim
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      ca-certificates \
      zlib1g && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/target/jorge.rinha /app/jorge.rinha

EXPOSE 8080
ENTRYPOINT ["/app/jorge.rinha"]
