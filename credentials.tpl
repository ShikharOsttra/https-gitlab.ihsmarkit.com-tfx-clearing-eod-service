spring.datasource.username=${SECRET:/etc/secrets/.db_user_secret/db-user-secret.json:username}
spring.datasource.password=${SECRET:/etc/secrets/.db_user_secret/db-user-secret.json:password}
spring.activemq.user=${SECRET:/etc/secrets/.amq_user_secret/amq-user-secret.json:username}
spring.activemq.password=${SECRET:/etc/secrets/.amq_user_secret/amq-user-secret.json:password}