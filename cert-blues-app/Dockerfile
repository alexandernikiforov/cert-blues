FROM azul/zulu-openjdk-debian:17.0.3 as build
RUN jlink \
    --add-modules \
      java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.sql,java.xml,jdk.httpserver,jdk.unsupported \
    --verbose \
    --strip-java-debug-attributes \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output /opt/java/jdk

ARG APP_VERSION
ADD build/context/cert-blues-app-${APP_VERSION}.tar .

RUN jpackage \
    --runtime-image /opt/java/jdk \
    --type app-image \
    --input /cert-blues-app-${APP_VERSION}/lib  \
    --name cert-blues  \
    --main-jar cert-blues-app-${APP_VERSION}.jar  \
    --main-class ch.alni.certblues.app.Runner  \
    --verbose \
    --dest /opt/image

FROM gcr.io/distroless/base:latest
# copy libraries missing in the distroless image
COPY --from=build /lib/x86_64-linux-gnu/libz.so.1 /lib/x86_64-linux-gnu/libz.so.1
COPY --from=build /lib/x86_64-linux-gnu/libcrypt.so.1 /lib/x86_64-linux-gnu/libcrypt.so.1
# copy the application itself
COPY --from=build /opt/image/cert-blues /opt/image/cert-blues

WORKDIR /opt/image/cert-blues
ENTRYPOINT ["./bin/cert-blues"]
