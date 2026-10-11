# Rasoi server image (design-plan 4; docs/running-locally.md has the environment it needs).
# Build: docker build -t rasoi-server .   Run: see "Configuration for a production-like deployment".
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
# Only the server is built here; the client modules need Android and Apple toolchains.
RUN ./gradlew :server:app:installDist --no-daemon \
    -Prasoi.android=false -Prasoi.compose=false -Prasoi.ios=false

FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 10001 rasoi && mkdir /data && chown rasoi /data
COPY --from=build /src/server/app/build/install/app /opt/rasoi
USER rasoi
ENV PORT=8080 \
    RASOI_STORAGE_DIR=/data \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
VOLUME /data
ENTRYPOINT ["/opt/rasoi/bin/app"]
