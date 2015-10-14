package io.cloudsoft.spark;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.reflect.TypeToken;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.core.annotation.Effector;
import org.apache.brooklyn.core.annotation.EffectorParam;
import org.apache.brooklyn.entity.group.DynamicCluster;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.core.sensor.Sensors;

@ImplementedBy(SparkClusterImpl.class)
public interface SparkCluster extends DynamicCluster {

    AttributeSensor<AtomicBoolean> RECONFIGURING_SPARK_CLUSTER = Sensors.newSensor(AtomicBoolean.class, "spark.cluster.reconfiguringSparkCluster", "Is the cluster being reconfigured");
    AttributeSensor<String> MASTER_NODE_HOSTNAME = Sensors.newStringSensor("spark.cluster.masterNodeHostname", "The hostname of the master node in the cluster");
    AttributeSensor<String> MASTER_NODE_CONNECTION_URL = Sensors.newStringSensor("spark.cluster.masterNodeConnectionUrl", "The connection url of the master node in the cluster that workers use to join");

    AttributeSensor<Integer> MASTER_NODE_SERVICE_PORT = Sensors.newIntegerSensor("spark.cluster.masterNodeServicePort", "The service port of the master node in the cluster");

    AttributeSensor<Entity> MASTER_SPARK_NODE = Sensors.newSensor(new TypeToken<Entity>() {
    }, "spark.cluster.masterSparkNode", "The master node of the Spark Cluster");

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
