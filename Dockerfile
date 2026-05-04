# Stage 1: Detect required modules and build minimal JRE
FROM amazoncorretto:21.0.2-alpine AS jre-builder

RUN apk add --no-cache binutils

WORKDIR /build
COPY target/lmps-payment.jar .

# Extract JAR to inspect BOOT-INF, then let jdeps compute the exact module set
# needed by this app and all its dependencies — avoids ALL-MODULE-PATH bloat
RUN jar xf lmps-payment.jar && \
    MODULES=$($JAVA_HOME/bin/jdeps \
        --ignore-missing-deps \
        --print-module-deps \
        --multi-release 21 \
        --recursive \
        --class-path 'BOOT-INF/lib/*' \
        BOOT-INF/classes 2>/dev/null) && \
    echo "jlink modules: $MODULES" && \
    $JAVA_HOME/bin/jlink \
        --add-modules "$MODULES" \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-6 \
        --output /customjre

# Stage 2: Runtime
FROM alpine:3.21

RUN echo "Asia/Bangkok" > /etc/timezone

ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=jre-builder /customjre $JAVA_HOME

WORKDIR /usr/app

# Config before JAR: resources change less often than code, keeping this layer cached
RUN mkdir config_props
COPY src/main/resources/* ./config_props/
COPY target/*.jar ./

ENTRYPOINT ["java", "-jar", "lmps-payment.jar", "--spring.config.location=./config_props/application.yaml"]
