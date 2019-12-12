#!/usr/bin/env bash

export LOG_PATH=/logs/${KUBERNETES_NAMESPACE}

[[ -d ${LOG_PATH} ]] || mkdir -p ${LOG_PATH}

java -jar /opt/app/${JAVA_APP_JAR} --spring.config.location=classpath:/application.properties,classpath:/application-env.properties