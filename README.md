# SBSD - Spring Batch Smell Detector
Spring Batch Smell Detector - Program to detect Design Smells specific to the context of batch processing applications.

## Getting start with SBSD

> Important: This project was developed using Java 13

> You can obtain the version from: https://www.oracle.com/java/technologies/javase-jdk13-downloads.html

## Configuring the project:
### 1. Edit the application.properties
* Into directory sbsd/src/main/resources/ open the file application.properties
* Change the values of the variables batch_role* with the name of the classes or intefaces for each archtectural role:
  * batch_role_service_classes: Classes or intefaces with archtectural role Service
  * batch_role_reader_classes: Classes or intefaces with archtectural role Reader
  * batch_role_writer_classes: Classes or intefaces with archtectural role Writer
  * batch_role_processor_classes: Classes or intefaces with archtectural role Processor

> For example:

    batch_role_service_classes=AbstractService
    batch_role_reader_classes=ItemReader
    batch_role_writer_classes=ItemWriter,ProcessWriter
    batch_role_processor_classes=ItemProcessor
    
## 2. Edit the metric files:
* Into directory sbsd/src/main/resources/ open the file metric_statistics.properties
* Change the values of the variables according to the archtectural role and the metric.

> For example:

> reader.statistic.loc.lower=2.5

* reader.statistic = Reader archtectural role
* loc = Metric line of codes
* lower = Limit value considered low 
