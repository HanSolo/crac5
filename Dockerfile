#FROM ubuntu:20.04
FROM azul/zulu-openjdk:21-jdk-crac-latest

RUN apt-get update -y

RUN mkdir -p /opt/crac-files

#COPY ./start-docker.sh /opt/app/start-docker.sh
#RUN  chmod +x /opt/app/start-docker.sh


COPY build/libs/crac5-21.0.0.jar /opt/app/crac5-21.0.0.jar

COPY start-docker.sh /opt/app/start-docker.sh

#CMD ["docker exec -it -u root crac5 java -XX:CRaCEngine=warp -XX:CRaCCheckpointTo=/opt/crac-files -jar /opt/app/crac5-21.0.0.jar"]

#CMD ["/bin/bash"]

#CMD ["docker run -it --rm --name crac5 crac5"]

#CMD ["docker exec -it -u root crac5 /bin/bash"]

