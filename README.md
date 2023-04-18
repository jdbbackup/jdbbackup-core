![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/jdbbackup-core)
![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jdbbackup_jdbbackup-core&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jdbbackup_jdbbackup-core)
[![javadoc](https://javadoc.io/badge2/com.fathzer/jdbbackup-core/javadoc.svg)](https://javadoc.io/doc/com.fathzer/jdbbackup-core)

# JDbBackup-core
A helper library to backup data to various destinations.

The [JDbBackup](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/JDbBackup.html) class uses a [SourceManager](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/SourceManager.html) to save data to a temporary file. Then it delegates to a [DestinationManager](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/DestinationManager.html) the task to save this temporary file to its final destination.

Source and destinations are defined by strings that may start with respectively the SourceManager or the DestinationManager identifier. For instance, *sftp://* identifies the DestinationManager able to send a file through the sftp protocol.  
The rest of the source and destination addresses depends on the **SourceManager**/**DestinationManager**.  
Have a look to their javadoc to have more details.

That said, unless otherwise specified, all managers support the use of the following patterns in addresses (except in the SourceManager/DestinationManager identifier):  
- {d=*dateFormat*}: dateFormat must be a valid date time pattern as described in [SimpleDateFormat documentation](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/text/SimpleDateFormat.html).  
For example, the pattern {d=yyyy} will be replaced by the year on 4 characters at runtime.
- {e=*envVar*}: envVar must be an existing environment variable whose value will replace the pattern.
- {p=*property*}: property must be an existing java System property whose value will replace the pattern.
- {f=*filePath*}: filePath must be an existing file whose value will replace the pattern.

## SourceManager and DestinationManagers implementations

This library contains the following implementation of *SourceManager*:  
* [mysql](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/sources/MySQLDumper.html). Please note it uses the mysqldump command which must be installed on the machine running this library.

This library contains the following implementation of *DestinationManager*:  
* [file](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/managers/local/FileManager.html).

**SourceManager**s and **DestinationManager**s are loaded through the [Java service loader](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html) standard mechanism. So, it's quite easy to implement your own and use it with this library.  
Examples can be found in [jdbbackup repositories](https://github.com/jdbbackup).

A class that can be used as command line programs is also available [here](https://github.com/jdbbackup/jdbbackup-cli).  
A class that schedules backups is available [here](https://github.com/jdbbackup/jdbbackup-docker). This project also contains a ready to work Docker image able to schedule backups.

## How to use it
This library requires java 11+.

*WORK in progress*

## TODO
- test proxy is set on source manager that implements ProxyCompliant
