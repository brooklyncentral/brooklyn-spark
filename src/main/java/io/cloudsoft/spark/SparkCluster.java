package io.cloudsoft.spark;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.reflect.TypeToken;

import brooklyn.catalog.Catalog;
import brooklyn.entity.Entity;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;

@Catalog(name = "Apache Spark Cluster", description = "Apache Spark is an engine for processing large datasets. This Entity deploys" +
        "a cluster of Spark comprising a master and worker Apache Spark entities")
@ImplementedBy(SparkClusterImpl.class)
public interface SparkCluster extends DynamicCluster {

    AttributeSensor<String> MASTER_NODE_WEB_CONSOLE_URL = Sensors.newStringSensor("spark.cluster.masterNodeWebConsoleUrl", "The Url for accessing the master node web console");
    AttributeSensor<String> MASTER_NODE_HOSTNAME = Sensors.newStringSensor("spark.cluster.masterNodeHostname", "The hostname of the master node in the cluster");
    AttributeSensor<String> MASTER_NODE_CONNECTION_URL = Sensors.newStringSensor("spark.cluster.masterNodeConnectionUrl", "The connection url of the master node in the cluster that workers use to join");

    AttributeSensor<Integer> MASTER_NODE_SERVICE_PORT = Sensors.newIntegerSensor("spark.cluster.masterNodeServicePort", "The service port of the master node in the cluster");

    AttributeSensor<Entity> MASTER_SPARK_NODE = Sensors.newSensor(Entity.class, "spark.cluster.masterSparkNode", "The master node of the Spark Cluster");

    AttributeSensor<List<Entity>> SPARK_CLUSTER_NODES = Sensors.newSensor(
            new TypeToken<List<Entity>>() {
            }, "spark.cluster.nodes", "List of all active Spark nodes in the cluster");

    //required to launch Spark workers
    AttributeSensor<AtomicLong> SPARK_WORKER_INSTANCE_ID_TRACKER = Sensors.newSensor(AtomicLong.class, "spark.workerInstanceIdTracker", "An incrementing worker id to identify Spark workers instances in the cluster");

    @Effector(description = "submit a Spark app to the cluster")
    public void submitSparkApplication(@EffectorParam(name = "masterConnectionUrl") String masterNodeConnectionUrl);

    @Effector(description = "adds worker instances to this spark cluster members in round robin")
    public void addSparkWorkerInstances(@EffectorParam(name = "noOfInstances") Integer numberOfInstances);

    @Effector(description = "run JavaSparkPi demo")
    public void runJavaSparkPiDemo();
}
