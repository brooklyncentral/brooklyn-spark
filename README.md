Brooklyn Apache Spark Cluster
=======

[![Build Status](https://api.travis-ci.org/brooklyncentral/brooklyn-spark.svg?branch=master)](https://travis-ci.org/brooklyncentral/brooklyn-spark)


[Apache Spark](https://spark.apache.org/) is an engine for processing large datasets (big data). Spark has the
advantage of fast data processing using in-memory computations and runs map reduce jobs much faster than Hadoop.

This repository contains the Apache Spark Brooklyn entity for deploying a Spark cluster with a master and worker nodes. The Apache Spark cluster will be monitored and managed by [Apache Brooklyn](https://brooklyn.incubator.apache.org/).

## Opening in an IDE

To open this project in an IDE, you will need maven support enabled
(e.g. with the relevant plugin).  You should then be able to develop
it and run it as usual.  For more information on IDE support, visit:

    http://brooklyncentral.github.io/dev/build/ide.html

## Sample Blueprint

Below is a sample YAML blueprint that can be used to deploy Apache Spark to SoftLayer, adding the list of services
shown:

```yaml
name: Spark Cluster Deployment
location: jclouds:softlayer:ams01
services:
- type: io.cloudsoft.spark.SparkCluster
  initialSize: 3
```

## Building and Running

There are several options available for building and running.

### Building a standalone distro

To build an assembly, simply run:

    mvn clean install

This creates a tarball with a full standalone application which can be installed in any *nix machine at:
    target/brooklyn-spark-dist.tar.gz

It also installs an unpacked version which you can run locally:

     cd target/brooklyn-spark-dist/brooklyn-spark

To run Apache Brooklyn with a simple Apache Spark 2-nodes application:

     ./start.sh launch -l <location> --spark

For more information see the README (or `./start.sh help`) in that directory.

### Adding to Brooklyn dropins

An alternative is to build a single jar and to add that to an existing Brooklyn install.

First install Brooklyn. There are instructions at https://brooklyn.incubator.apache.org/v/latest/start/index.html

Then simply run:

    mvn clean install

You can copy the jar to your Brooklyn dropins folder, and then launch Brooklyn:

    cp target/brooklyn-spark-0.1-SNAPSHOT.jar $BROOKLYN_HOME/lib/dropins/
    nohup $BROOKLYN_HOME/bin/brooklyn launch &

### Adding to Brooklyn catalog on-the-fly

*TODO: this is work in progress; the project is still to be converted to build an OSGi bundle*

A third alternative is to build an OSGi bundle, which can then be deployed to
a running Brooklyn server. The Spark blueprint can be added to the catalog
(referencing the required OSGi bundle), which makes the blueprint available
to Brooklyn users.

General instructions for how to do this are available at:
https://brooklyn.incubator.apache.org/v/latest/ops/catalog/index.html

First create the OSGi bundle:

    mvn clean install

Copy the OSGi bundle to a stable location. This could be something like Artifactory, or
for test purposes it could be just on your local file system:

    cp target/brooklyn-spark-0.1-SNAPSHOT.jar /path/to/artifacts/brooklyn-spark-0.1-SNAPSHOT.jar

Add the `SparkCluster` to the catalog. Here we assume Brooklyn is running at https://localhost:8443,
with credentials admin:password.

First create the catalog definition, which is a YAML file (assumed here to be at `/path/to/spark-catalog.yaml`):

    brooklyn.catalog:
      id: io.cloudsoft.spark.SparkCluster
      version: 0.1-SNAPSHOT
      iconUrl: http://spark.apache.org/images/spark-logo.png
      description: Apache Spark Cluster
      libraries:
      - url: file:///path/to/artifacts/brooklyn-spark-0.1-SNAPSHOT.jar

    services:
    - type: io.cloudsoft.spark.SparkCluster
      name: Spark Cluster

Upload this to the Brooklyn server:

    curl https://127.0.0.1:8443/v1/catalog --data-binary @/path/to/spark-catalog.yaml

Users can then provision an Ambari cluster using the YAML shown in the first section.

----

Copyright 2014-2015 by Cloudsoft Corporation Limited

> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
> 
> http://www.apache.org/licenses/LICENSE-2.0
> 
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
