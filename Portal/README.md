# elwasys Web Portal

![Screenshot](https://raw.githubusercontent.com/kabieror/elwasys-portal/master/docs/screenshot-dashboard.png)

## Set up a Development Environment

The following tool stack is recommended:

-	Java Development Kit 8
	http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

-	Apache Maven
	https://maven.apache.org/

-	IntelliJ IDEA
	https://www.jetbrains.com/idea/

## Import into IntelliJ IDEA

1. Clone this repository and the [elwasys-common](https://github.com/kabieror/elwasys-common) repository
2. Within IntelliJ chose File > Open... and select the project folder `elwasys-portal`
3. Next, go to File > Project Structure
4. Click the plus icon, select _Import Module_ and browse to the project folder `elwasys-common`

## Create a configuration file

Copy the file elwaportal.example.properties to /etc/elwaportal/elwaportal.properties and replace the values.
Under Windows, the `/etc` must be located in the same drive letter as the project directory. E.g. if your project is located at `D:\code\elwasys-portal`, then the config folder has to be `D:\etc\elwaportal`.

## Start the development server

### From the command line

```
cd elwasys-common
mvn install
cd ../elwasys-portal
mvn jetty:run
```

### Within IntelliJ:

1. Run > Edit Configurations...
2. Click the Plus icon
3. Select "Maven"
4. Use the button all the way to the right to select the project `common`
5. Set "Name" to "Install Common"
5. Set "Command line" to "install"
6. Add another maven configuration and select the project `webportal`
6. Set "Name" to "Run Jetty"
7. Set "Command line" to "jetty:run"
6. Click "OK"
7. Execute the run configuration "Install Common"
8. Execute the run configuration "Run Jetty"

The web portal will be listening on port 8080. Access it via http://localhost:8080.
