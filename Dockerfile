FROM gradle:8.7.0-jdk17 AS build

WORKDIR /home/gradle/src

COPY build.gradle settings.gradle gradle/ ./
COPY src ./src
COPY .editorconfig ./
COPY fakeEnv .env

ARG NEW_RELIC_LICENSE_KEY

ENV NEW_RELIC_LICENSE_KEY=$NEW_RELIC_LICENSE_KEY

# Read secrets from the mounted secrets files and export them as environment variables
RUN --mount=type=secret,id=username,target=/run/secrets/username \
    --mount=type=secret,id=token,target=/run/secrets/token \
    sh -c 'USERNAME=$(cat /run/secrets/username) && \
           TOKEN=$(cat /run/secrets/token) && \
           gradle build -PUSERNAME=$USERNAME -PTOKEN=$TOKEN'

WORKDIR /app

EXPOSE ${PORT}

COPY newrelic/newrelic.jar /app/newrelic.jar

ENTRYPOINT ["java", "-jar", "-javaagent:/app/newrelic.jar", "-Dnewrelic.config.license_key=${NEW_RELIC_LICENSE_KEY}", "/home/gradle/src/build/libs/service-0.0.1-SNAPSHOT.jar"]
