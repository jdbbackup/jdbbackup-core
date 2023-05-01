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

Additionally, here are the officially supported extra plugins:

| Type | id | maven artifact | comment |
| --- | --- | --- | --- |
| src | fake | [com.fathzer::jdbbackup-fakesource](https://github.com/jdbbackup/jdbbackup-fakesource) | A fake source to help implement destination manager's tests |
| dest | sftp | [com.fathzer::jdbbackup-sftp](https://github.com/jdbbackup/jdbbackup-sftp) | Sends backup to a remote server through sftp |
| dest | dropbox | [com.fathzer::jdbbackup-dropbox](https://github.com/jdbbackup/jdbbackup-dropbox) |Sends backup to a dropbox account |
| dest | s3 | [com.fathzer::jdbbackup-s3](https://github.com/jdbbackup/jdbbackup-s3) | Sends backup to an Amazon S3 bucket |
| dest | gcs | [com.fathzer::jdbbackup-gcs](https://github.com/jdbbackup/jdbbackup-gcs) | Sends backup to a Google Cloud Storage bucket |


**SourceManager**s and **DestinationManager**s are loaded through the [Java service loader](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html) standard mechanism. So, it's quite easy to implement your own and use it with this library (Official managers can be used as example).

A class that can be used as command line programs is also available at [https://github.com/jdbbackup/jdbbackup-cli](https://github.com/jdbbackup/jdbbackup-cli).  
A class that schedules backups is available at [https://github.com/jdbbackup/jdbbackup-cli](https://github.com/jdbbackup/jdbbackup-docker). This project also contains a ready to work Docker image able to schedule backups.

## How to use it
This library requires java 11+.

Here is a usage example:  
```java
final JDbBackup bckp = new JDbBackup();
bckp.backup("mysql://{f=dblogin.txt}@db.mycompany.com:3306/mydb", "file://{e=HOME}/backup/db-{d=yyyy}")
```

This example will backup the *db* database of *db.mycompany.com* mysql server in a file contained in the backup folder of user's home directory. The name of the file will ends with the current year. The login used to connect to the database is stored in the *dblogin.txt* file.

## Security notice
The data backed up by DBBackup passes through a temporary file. This makes it possible not to re-extract the data when you want to save them in two different destinations.  
The counterpart of this architecture is that it may be necessary, depending on the level of confidentiality of the saved data, to secure access to this file.

This temporary file is created in the *JDBBackup.createTempFile()* method. It creates the file in the default temporary directory and attempts to ensure that it is readable only by the owner of the account running the program.  
If you think the implementation is not safe enough, you can override this method.  
You may also encrypt the backup by using your own Source manager that encrypts content on the fly.

## TODO
- In a future release implement tmp file as a Path and not a File in order to allow tmp file as a memory file, for instance