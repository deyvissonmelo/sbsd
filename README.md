# SBSD - Spring Batch Smell Detector
Spring Batch Smell Detector - Program to detect Design Smells specific to the context of batch processing applications.

## Getting start with SBSD

> Important: This project was developed using Java 13

> You can obtain the version from: https://www.oracle.com/java/technologies/javase-jdk13-downloads.html

> You also will need to Maven to execute the project. You can download it in https://maven.apache.org/download.cgi

## Configuring the project:
### 1. Edit the application.properties
* Into directory sbsd/src/main/resources/ open the file application.properties
* Change the values of the variables batch_role* with the name of the classes or intefaces for each architecture role:
  * batch_role_service_classes: Classes or intefaces with architecture role Service
  * batch_role_reader_classes: Classes or intefaces with architecture role Reader
  * batch_role_writer_classes: Classes or intefaces with architecture role Writer
  * batch_role_processor_classes: Classes or intefaces with architecture role Processor

> For example:

    batch_role_service_classes=AbstractService
    batch_role_reader_classes=ItemReader
    batch_role_writer_classes=ItemWriter,ProcessWriter
    batch_role_processor_classes=ItemProcessor
    
## 2. Edit the metric files:
* Into directory sbsd/src/main/resources/ open the file metric_statistics.properties
* Change the values of the variables according to the architecture role and the metric.

> For example:

> reader.statistic.loc.lower=2.5

* reader.statistic = Reader architecture role
* loc = Metric line of codes
* lower = Limit value considered low 

## Running the project:

### 1. Build the project
* Into the root directory execute the comand *mvn clean install*. This command will create the application's jar file into the directory target with the name spring_batch_smell_detector.

### 2. Run the project:
* For run the project you will need inform the following parameters:
  * project_path: The path of Spring Batch project for analyse - Required. 
  * job_file_path: The path of the Job definition XML file - Required. 
  * query_fle_path: The path of file with queries definitions - Optional.
* For run the project execute the command line:
  * *java -jar spring_batch_smell_detector <project_path> <job_file_path> <query_fle_path>* 
  
> For example:

> *java -jar spring_batch_smell_detector /user/user001/project001 /user/user001/project001/main/job/job.xml /user/user001/project001/main/queries/sql.xml*
