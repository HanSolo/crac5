# Build stage
FROM azul/zulu-openjdk:21-jdk-crac-latest AS builder
RUN apt-get update -y
RUN mkdir -p /opt/crac-files
COPY build/libs/crac5-21.0.0.jar /opt/app/crac5-21.0.0.jar
#RUN java -XX:CRaCEngine=warp -XX:CPUFeatures=generic -XX:CRaCCheckpointTo=/opt/crac-files -jar /opt/app/crac5-21.0.0.jar
RUN java -XX:CRaCEngine=warp -XX:CRaCCheckpointTo=/opt/crac-files -XX:CPUFeatures=ignore -jar /opt/app/crac5-21.0.0.jar

# Runtime stge
FROM azul/zulu-openjdk:21-jdk-crac-latest
RUN apt-get update -y
RUN mkdir -p /crac-files
COPY --from=builder /opt/app/crac5-21.0.0.jar .
COPY --from=builder /opt/crac-files/.* /crac-files
RUN java -XX:CRaCRestoreFrom=/crac-files
#CMD ["java -XX:CRaCCheckpointFrom=/crac-files"]
