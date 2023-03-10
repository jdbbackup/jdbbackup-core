![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/jdbbackup-core)
![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jdbbackup_jdbbackup-core&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jdbbackup_jdbbackup-core)
[![javadoc](https://javadoc.io/badge2/com.fathzer/jdbbackup-core/javadoc.svg)](https://javadoc.io/doc/com.fathzer/jdbbackup-core)

# JDbBackup-core
A helper library to backup database dump to various destinations.

*WORK in progress*

The [JDbBackup](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/JDbBackup.html) class uses a [DBDumper](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/DBDumper.html) to dump a database to a temporary file. Then it delegates to a [DestinationManager](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/DestinationManager.html) the task to save this temporary file to its final destination.

Source and destinations are defined by strings that may start with respectively the DBDumper or the DestinationManager identifier. For instance, *sftp://* identifies the DestinationManager able to send a file through the sftp protocol.  
The rest of the source and destination strings depends on the **DBDumper**/**DestinationManager**.  
Have a look to their javadoc to have more details.

This library contains the following implementation of *DBDumper*:  
* [mysql](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/dumpers/MySQLDumper.html). Please note it uses the mysqldump command which must be installed on the machine running this library.

This library contains the following implementation of *DestinationManager*:  
* [file](https://javadoc.io/doc/com.fathzer/jdbbackup/com/fathzer/jdbbackup/managers/local/FileManager.html).

**DBDumper**s and **DestinationManager**s are loaded through the [Java service loader](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html) standard mechanism. So, it's quite easy to implement your own and use it with this library.  
Examples can be found in [jdbbackup repositories](https://github.com/jdbbackup).

The library also includes classes that provides command line support. If you don't plan to use this manager, you can exclude the info.picocli:picocli dependency.

It requires java 11+.

## How to use it