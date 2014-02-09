#!/bin/bash

export JAVA_HOME=/usr/lib/jvm/jdk-7-oracle-armhf/jre

EXEC="/usr/bin/jsvc"

PID="/var/run/flickring.pid"

CLASS="com.emf.flickring.Main"

ARGS="./conf"

CP="flickring-1.0.0-SNAPSHOT.jar:lib/*"

OUTFILE="./log/flickring.log"

ERRFILE="./log/flickring-error.log"

COMMAND=""

if [ "$1" = "stop" ]; then
  COMMAND="-stop"  
fi

$EXEC -home $JAVA_HOME -cp $CP -user pi -outfile $OUTFILE -errfile $ERRFILE -pidfile $PID $COMMAND $CLASS $ARGS