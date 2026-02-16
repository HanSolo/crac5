#!/bin/bash

sleep 45; jcmd /opt/app/crac5-25.0.0.jar JDK.checkpoint
java -XX:CRaCEngine=warp -XX:CRaCCheckpointTo=/opt/crac-files -XX:CPUFeatures=ignore -jar /opt/app/crac5-25.0.0.jar