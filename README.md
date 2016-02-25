Brooklyn Apache Spark Cluster
=======

Simply install the BOM file into your Brooklyn instance and deploy.

See [brooklyn.io]() for more information.

## Nodes

* The older Java version of the blueprint is available in the java-brooklyn-spark branch.

* On SoftLayer you often need to specify the network ID's to use,
  otherwise it may provision nodes in different subnets.  For example:

       location:
         jclouds:softlayer:
           region: ams01
           templateOptions:
             primaryNetworkComponentNetworkVlanId: 1153481
             primaryBackendNetworkComponentNetworkVlanId: 1153483

