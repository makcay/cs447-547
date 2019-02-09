FROM alpine/git as clone
WORKDIR /app
RUN git clone https://github.com/makcay/cs547.git

FROM maven:3.5-jdk-8-alpine as build
WORKDIR /app
COPY --from=clone /app/cs547/FileListServer /app
RUN ["mvn", "compile"]
RUN ["mvn", "verify"]
RUN ["mvn", "package"]

FROM openjdk:8-jre-alpine
WORKDIR /app
COPY --from=build /app/target/FileListServer-1.0.0.jar /app
CMD ["java","-classpath","FileListServer-1.0.0.jar","server.FileListServer"]