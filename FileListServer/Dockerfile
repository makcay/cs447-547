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
COPY --from=build /app/target/lib /app/lib
COPY --from=build /app/tc /app/tc
# Comment line 16 and uncomment line 18 to apply your custom tc policies
# ADD tc /app/tc
COPY --from=build /app/files /app/files
# Comment line 19 and uncomment line 21 to add your files
# ADD files /app/files
RUN apk update && apk add bash iproute2 && apk add nano

