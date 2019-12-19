#!/usr/bin/env bash

CREDENTIALS_TPL=/opt/app/credentials.tpl
CREDENTIALS_FILE=/etc/.creds/credentials.properties
export LOG_PATH=/logs/${KUBERNETES_NAMESPACE}

if [[ ! -f "${CREDENTIALS_FILE}" ]]; then
        echo "Credentials file ${CREDENTIALS_FILE} not found, so creating one now..."
        if [[ -f "${CREDENTIALS_TPL}" ]]; then
                echo "Processing ${CREDENTIALS_TPL} file..."
                while IFS='=' read -r key value; do
                        echo "$key=`echo $value | sed 's/^\${SECRET:\(.*\)}$/\1/' | xargs cat`" >> ${CREDENTIALS_FILE}
                done < "${CREDENTIALS_TPL}"
        else
                echo "${CREDENTIALS_TPL} not found."
        fi
fi

[[ -d ${LOG_PATH} ]] || mkdir -p ${LOG_PATH}

java -jar /opt/app/${JAVA_APP_JAR} --spring.config.location=classpath:/application.properties,classpath:/application-env.properties,file:${CREDENTIALS_FILE}