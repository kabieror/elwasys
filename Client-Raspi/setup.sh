#!/bin/bash
set -e

# Read user input securely
function read_secure() {
  prompt=$1
  stty -echo
  read $prompt
  stty echo
  echo
}

# Generate a random password
function generate_password() {
  password=$(date +%s | sha256sum | base64 | head -c 32 ; echo)
  echo "$password"
}

# Ask for SSH password
read_secure -p "Enter SSH password: " ssh_password

# Ask for database information
read -p "Enter database server address (e.g., localhost:5432): " db_server
read -p "Enter database name: " db_name
read -p "Enter database username: " db_user
read_secure -p "Enter database password: " db_password
read -p "Should the database connection use SSL? (true/false): " db_use_ssl
read -p "Enter CA certificate (PEM format) for database encryption: " db_ca_cert

# Ask for client location and portal URL
read -p "Enter client location: " location
read -p "Enter portal URL: " portal_url

# Ask for SMTP settings
read -p "Enter SMTP server: " smtp_server
read -p "Enter SMTP port: " smtp_port
read -p "Enter SMTP username: " smtp_user
read_secure -p "Enter SMTP password: " smtp_password
read -p "Should the SMTP connection use SSL? (true/false): " smtp_use_ssl
read -p "Enter SMTP sender address: " smtp_sender

# Install Liberica JRE
sudo apt update
sudo apt install -y liberica-jre-full

mkdir -p /opt/elwasys

VER=$(curl --silent -qI https://github.com/kabieror/elwasys/releases/latest | awk -F '/' '/^location/ {print  substr($NF, 1, length($NF)-1)}')
wget https://github.com/kabieror/elwasys/releases/download/$VER/raspi-client-${VER}.jar -O /opt/elwasys/raspi-client-${VER}.jar
ln -s /opt/elwasys/raspi-client-${VER}.jar /opt/elwasys/raspi-client.latest.jar

# Populate the Config file
config_file="/opt/elwasys/elwasys.properties"
sudo tee "$config_file" > /dev/null <<EOT
# The address of the postgresql server
database.server: $db_server

# Name of the database
database.name: $db_name

# Username for the database connection
database.user: $db_user

# Password for the database connection
database.password: $db_password

# Whether the database connection is to be encrypted
database.useSsl: $db_use_ssl

# Location of this client, as specified in elwaPortal
# Only devices at this location will be available.
location: $location

# The URL of the elwasys web portal
portalUrl: $portal_url

# Settings for outgoing mails
smtp.server: $smtp_server
smtp.port: $smtp_port
smtp.user: $smtp_user
smtp.password: $smtp_password
smtp.useSSL: $smtp_use_ssl
smtp.senderAddress: $smtp_sender
EOT
sudo chmod 600 "$config_file"

# configure logging
logback_config="/opt/elwasys/logback.xml"
tee "$logback_config" > /dev/null <<EOT
<configuration scan="true" debug="true">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
              <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>%d %level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <appender name="FILE-DEBUG" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- daily rollover -->
            <fileNamePattern>log/elwasys.%d{yyyy-MM-dd}.debug.log</fileNamePattern>

            <!-- keep 30 days' worth of history -->
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d %level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
              <level>INFO</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- daily rollover -->
            <fileNamePattern>log/elwasys.%d{yyyy-MM-dd}.log</fileNamePattern>

            <!-- keep 300 days' worth of history -->
            <maxHistory>300</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d %level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="debug">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="FILE-DEBUG"/>
    </root>
</configuration>
EOT

# create ca-db.pem
ca_db="/opt/elwasys/ca-db.pem"
echo -e "$db_ca_cert" > $ca_db

truststore_password=$(generate_password)
truststore_file="/opt/elwasys/.truststore"
sudo keytool -import -trustcacerts -keystore "$truststore_file" -storepass "$truststore_password" -alias ca_cert -file "$ca_db" -noprompt

# run.sh script
run_script="/opt/elwasys/run.sh"
tee "$run_script" > /dev/null <<EOT
#!/bin/bash

sudo killall java 2> /dev/null

java -Djavafx.platform=gtk -Dlogback.configurationFile=/opt/elwasys/logback.xml \
        -Djavax.net.ssl.trustStore=/opt/elwasys/.truststore -Djavax.net.ssl.trustStorePassword=$truststore_password \
        -jar raspi-client.latest.jar -verbose > log/stdout 2> log/errout
EOT
chmod +x "$run_script"

# Auto-Start
tee "~/.xsession" > /dev/null <<EOT
cd /opt/elwasys
./run.sh
EOT

echo
echo
echo "> Installation completed!"
echo "> Please reboot now to complete installation."
