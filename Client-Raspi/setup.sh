#!/bin/bash
set -e

# Check if the script is run as root
if [[ $EUID -eq 0 ]]
then
  echo "This script should not be run as root."
  exit 1
fi

function log_state() {
  local cyan='\033[0;36m'
  local reset='\033[0m'
  echo -e "\n${cyan}> $@${reset}"
}
function log_success() {
  local green='\033[0;32m'
  local reset='\033[0m'
  echo -e "\n${green}> $@${reset}"
}
function generate_password() {
  password=$(date +%s | sha256sum | base64 | head -c 32 ; echo)
  echo "$password"
}

function collect_data() {
    echo
    echo
    echo === Database Connection ===
    echo
    read -p "Enter database server address (e.g., localhost:5432): " db_server
    echo
    read -p "Enter database name: " db_name
    echo
    read -p "Enter database username: " db_user
    echo
    read -s -p "Enter database password: " db_password
    echo
    echo
    read -p "Should the database connection use SSL? (true/false): " db_use_ssl
    echo
    echo "Please enter the CA certificate for verifying the server SSL certificate."
    echo "Provide a file in PEM format."
    echo "When you're done, type #"
    read -d '#' db_ca_cert

    echo
    echo
    echo === Email Settings ===
    echo
    read -p "Enter SMTP server: " smtp_server
    echo
    read -p "Enter SMTP port: " smtp_port
    echo
    read -p "Enter SMTP username: " smtp_user
    echo
    read -s -p "Enter SMTP password: " smtp_password
    echo
    echo
    read -p "Should the SMTP connection use SSL? (true/false): " smtp_use_ssl
    echo
    read -p "Enter SMTP sender address: " smtp_sender

    echo
    echo
    echo === elwasys Configuration ===
    echo
    read -p "Enter client location: " location
    echo
    read -p "Enter portal URL: " portal_url
    echo
    echo
    echo
}

function install_dependencies() {
    sudo apt-get update
    sudo apt-get install -y curl jq
}

# # # # # # #
function install_java() {
    log_state Installing Java Runtime Environment...
    wget -q -O - https://download.bell-sw.com/pki/GPG-KEY-bellsoft | sudo apt-key add -
    echo "deb [arch=arm64] https://apt.bell-sw.com/ stable main" | sudo tee /etc/apt/sources.list.d/bellsoft.list
    sudo apt-get update
    sudo apt-get install -y bellsoft-java17-runtime-full
}


# # # # # # #
function setup_firewall() {
    log_state Installing Firewall...

    # Install UFW
    sudo apt-get update
    sudo apt-get install -y ufw

    # Enable UFW
    sudo ufw enable

    sudo ufw default deny incoming
    sudo ufw allow 22
    sudo ufw --force enable
    sudo ufw status
}


# # # # # # #
function install_deconz() {
    log_state Installing deCONZ Zigbee Gateway...

    # Validations
    [ "$DECONZ_PASSWORD" ] || { echo "DECONZ_PASSWORD must be set"; exit 1; }

    sudo gpasswd -a $USER dialout
    wget -O - http://phoscon.de/apt/deconz.pub.key | \
            sudo apt-key add -
    sudo sh -c "echo 'deb http://phoscon.de/apt/deconz \
                $(lsb_release -cs) main' > \
                /etc/apt/sources.list.d/deconz.list"

    sudo apt-get update
    sudo apt-get install -y deconz

    otp="elwasysinstalleronetimetoken"
    curl -XPOST \
        -H 'Authorization: Basic ZGVsaWdodDpkZWxpZ2h0' \
        -H "Content-type: application/json" \
        -d "{ \"username\": \"$otp\", \"devicetype\": \"installer\" }" \
        http://localhost/api
    old_hash=`echo "$dc_user:$dc_user" | base64`
    new_hash=`echo "$dc_user:$DECONZ_PASSWORD" | base64`
    curl -XPUT \
        -H "Content-type: application/json" \
        -d "{ \"username\": \"$dc_user\", \"oldhash\": \"$old_hash\", \"newhash\": \"$new_hash\" }" \
        http://localhost/api/$otp/config/password
}

# # # # # # #
function install_elwasys() {
    log_state Installing elwasys...
    sudo mkdir -p $ELWA_ROOT
    sudo chown "$USER:$USER" $ELWA_ROOT

    cd $ELWA_ROOT

    VER=$(curl --silent -qI https://github.com/kabieror/elwasys/releases/latest | awk -F '/' '/^location/ {print  substr($NF, 1, length($NF)-1)}')
    jar_file=./raspi-client-${VER}.jar
    if [ ! -f "$jar_file" ]
    then
        wget https://github.com/kabieror/elwasys/releases/download/$VER/raspi-client-${VER}.jar -O $jar_file
        ln -s ./raspi-client-${VER}.jar ./raspi-client.latest.jar
    else
        echo "Skipping downloading raspi-client JAR. File already exists: $jar_file"
    fi
}

# # # # # # #
function config_elwasys() {
    log_state Configuring elwasys
    # Populate the Config file
    config_file="./elwasys.properties"
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
    logback_config="./logback.xml"
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
    ca_db="./ca-db.pem"
    echo -e "$db_ca_cert" > $ca_db

    truststore_password=$(generate_password)
    truststore_file="./.truststore"
    # Remove truststore file if it already exists
    [ -f "$truststore_file" ] && rm -f $truststore_file
    sudo keytool -import -trustcacerts -keystore "$truststore_file" -storepass "$truststore_password" -alias ca_cert -file "$ca_db" -noprompt

    # run.sh script
    run_script="./run.sh"
    tee "$run_script" > /dev/null <<EOT
#!/bin/bash

sudo killall java 2> /dev/null

java -Djavafx.platform=gtk -Dlogback.configurationFile=$ELWA_ROOT/logback.xml \
        -Djavax.net.ssl.trustStore=$ELWA_ROOT/.truststore -Djavax.net.ssl.trustStorePassword=$truststore_password \
        -jar raspi-client.latest.jar -verbose > log/stdout 2> log/errout
EOT
    chmod +x "$run_script"

    # Create log output folder
    mkdir -p ./log

    # Auto-Start
    cat > ~/.xsession <<EOT
cd $ELWA_ROOT
./run.sh
EOT
}

# collect_data

ELWA_ROOT=/opt/elwasys
DECONZ_PASSWORD=$(generate_password)

log_state Starting Installation...
install_dependencies

# setup_firewall

# install_java

install_deconz

# install_elwasys

# config_elwasys

# # # # # # #
echo
echo
log_success Installation completed!
echo Please reboot now to complete installation.
echo
echo "  $ sudo reboot"
echo
echo Please be sure to change the default password of the user $USER.
echo Run this command to change the password:
echo "  $ passwd"
echo
echo
