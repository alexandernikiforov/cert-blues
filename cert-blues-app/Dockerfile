FROM azul/zulu-openjdk-debian:11.0.13
ARG APP_VERSION
ADD build/context/cert-blues-app-${APP_VERSION}.tar .
WORKDIR /cert-blues-app-${APP_VERSION}
CMD ["./bin/cert-blues-app"]
