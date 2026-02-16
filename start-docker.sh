#!/bin/sh

# Output timestamp
echo "$(date +'%H:%M:%S.%3N') Call jar file"

# Check whether crac-files folder is present not empty and if so, restore from checkpoint, otherwise start from jar
#if [ -d "/crac-files" ]; then
#  if [ "$(ls -A /crac-files)" ]; then
#    echo "Restore from checkpoint"
#    java -XX:CRaCRestoreFrom=/crac-files
#  else
#    echo "Standard start from jar file"
#    java -XX:CRaCEngine=warp -XX:CRaCCheckpointTo=/crac-files -jar crac5-21.0.0.jar
#  fi
#else
#  echo "No crac-files folder found"
#  mkdir /crac-files
#  echo "Standard start from jar file"
#  java -XX:CRaCEngine=warp -XX:CRaCCheckpointTo=/crac-files -jar crac5-21.0.0.jar
#fi

echo "start-docker.sh"
java -version
java -XX:CRaCEngine=warp -XX:CRaCCheckpointTo=/crac-files -jar crac5-21.0.0.jar
