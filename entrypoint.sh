#!/usr/bin/env bash

CREDENTIALS_TPL=/opt/app/credentials.tpl
CREDENTIALS_FILE=/etc/.creds/credentials.properties
export LOG_PATH=/logs/${KUBERNETES_NAMESPACE}

if [[ -f "${CREDENTIALS_TPL}" ]]; then
        echo "Processing ${CREDENTIALS_TPL} file..."
        while IFS='=' read -r key value || [[ "$key" ]]; do
                secret_file=$(echo ${value} | sed 's/^\${SECRET:\(.*\):\(.*\)}$/\1/')
                secret_field=$(echo ${value} | sed 's/^\${SECRET:\(.*\):\(.*\)}$/\2/')
                case ${secret_field} in
                    username|password)
                        secret=$(cat ${secret_file} | jq -r ".connectionData.basicAuth.${secret_field}")
                    ;;
                    token)
                        secret=$(cat ${secret_file} | jq -r ".connectionData.${secret_field}")
                    ;;
                    *)
                        echo "Wrong ${CREDENTIALS_TPL}."
                    ;;
                esac
                echo "$key=${secret}" >> ${CREDENTIALS_FILE}
        done < "${CREDENTIALS_TPL}"
else
        echo "${CREDENTIALS_TPL} not found."
fi

[[ -d ${LOG_PATH} ]] || mkdir -p ${LOG_PATH}

java ${JAVA_OPTS} -jar /opt/app/${JAVA_APP_JAR} --spring.config.location=classpath:/application.properties,classpath:/application-env.properties,file:${CREDENTIALS_FILE},file:/opt/app/config/application.properties