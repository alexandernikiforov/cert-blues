FROM azul/zulu-openjdk-alpine:11.0.13-jre-headless
ARG APP_VERSION
ADD build/context/cert-blues-app-${APP_VERSION}.tar .
WORKDIR /cert-blues-app-${APP_VERSION}
CMD ["./bin/cert-blues-app"]
