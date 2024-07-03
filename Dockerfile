FROM gradle:8.7.0-jdk17 AS build

WORKDIR /home/gradle/src

COPY build.gradle settings.gradle gradle/ ./
COPY src ./src
COPY .editorconfig ./
COPY fakeEnv .env

# Read secrets from the mounted secrets files and export them as environment variables
RUN --mount=type=secret,id=username,target=/run/secrets/username \
    --mount=type=secret,id=token,target=/run/secrets/token \
    sh -c 'USERNAME=$(cat /run/secrets/username) && \
           TOKEN=$(cat /run/secrets/token) && \
           gradle build -PUSERNAME=$USERNAME -PTOKEN=$TOKEN'



WORKDIR /app
COPY fakeEnv .env

EXPOSE ${PORT}

ENTRYPOINT ["java", "-jar", "/home/gradle/src/build/libs/service-0.0.1-SNAPSHOT.jar"]
