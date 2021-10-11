# Eye application

## About

This is a CLI (Command-Line Interface) application that can send data to server.


## Instructions for using Maven

Make sure that the parent POM was installed first.

To compile and run using _exec_ plugin:

```
mvn compile exec:java -Dserver.instance=1 -Deye.name=Tagus -Deye.latitude=37.737613 -Deye.longitude=-8.303164
```

To generate launch scripts for Windows and Linux
(the POM is configured to attach appassembler:assemble to the _install_ phase):

```
mvn install
```

To run using appassembler plugin on Linux:

```
./target/appassembler/bin/eye <zooHost> <zooPort> <path> <latitude> <longitude>
```

To run using appassembler plugin on Windows:

```
target\appassembler\bin\eye <zooHost> <zooPort> <path> <latitude> <longitude>
```


## To configure the Maven project in Eclipse

'File', 'Import...', 'Maven'-'Existing Maven Projects'

'Select root directory' and 'Browse' to the project base folder.

Check that the desired POM is selected and 'Finish'.


----

