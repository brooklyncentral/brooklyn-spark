Brooklyn Apache Spark Cluster
=======

([Apache Spark](https://spark.apache.org/)) is an engine for processing large datasets (big data). Spark has the advantage of fast data processing using in-memory computations and runs map reduce jobs much faster than Hadoop.

This repository contains the Apache Spark Brooklyn entity for deploying a Spark cluster with a master and worker nodes. The Apache Spark cluster will be monitored and managed by [Apache Brooklyn](https://brooklyn.incubator.apache.org/).

## Sample Deployment

```yaml
name: Spark Cluster Deployment
location: jclouds:softlayer:ams01
services:
- type: io.cloudsoft.spark.SparkCluster
  initialSize: 3
```

----

Copyright 2013-2014 by Cloudsoft Corporation Limited

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