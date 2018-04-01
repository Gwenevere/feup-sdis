#!/bin/sh
if [ "$#" -ne 1 ]; then
  echo "Usage: peer.sh <peer_num>"
  exit 1
fi

java -classpath bin service.Peer 1.0 "$1" //localhost/ 224.0.0.0:8000 224.0.0.0:8001 224.0.0.0:8002