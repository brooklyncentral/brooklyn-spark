package io.cloudsoft.spark;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

import brooklyn.config.ConfigKey;
import brooklyn.config.render.RendererHints;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.location.basic.PortRanges;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(SparkNodeImpl.class)
public interface SparkNode extends SoftwareProcess {

    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "http://d3kbcqa49mib13.cloudfront.net/spark-${version}-bin-hadoop1.tgz");

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION,
            "1.1.0");

    @SetFromFlag("downloadAddonUrls")
    BasicAttributeSensorAndConfigKey<Map<String, String>> DOWNLOAD_ADDON_URLS = new BasicAttributeSensorAndConfigKey<Map<String, String>>(
            SoftwareProcess.DOWNLOAD_ADDON_URLS, ImmutableMap.of(
            "scala", "http://www.scala-lang.org/files/archive/scala-${addonversion}.final.tgz"));

    @SetFromFlag("stickyVersion")
    ConfigKey<String> SCALA_VERSION = ConfigKeys.newStringConfigKey(
            "spark.scala.version", "Version of scala to be installed, if required", "2.8.1");

    @SetFromFlag("masterWebPort")
    PortAttributeSensorAndConfigKey SPARK_MASTER_WEB_PORT = new PortAttributeSensorAndConfigKey("spark.master.webPort", "Spark Master node Web Interface Port", PortRanges.fromString("8080+"));

    @SetFromFlag("masterServicePort")
    PortAttributeSensorAndConfigKey SPARK_MASTER_SERVICE_PORT = new PortAttributeSensorAndConfigKey("spark.master.servicePort", "Spark Master node Service Port", PortRanges.fromString("7077+"));

    @SetFromFlag("workerServicePort")
    PortAttributeSensorAndConfigKey SPARK_WORKER_SERVICE_PORT = new PortAttributeSensorAndConfigKey("spark.worker.servicePort", "Spark Worker node Service Port");

    @SetFromFlag("masterDriverPort")
    PortAttributeSensorAndConfigKey SPARK_MASTER_DRIVER_PORT = new PortAttributeSensorAndConfigKey("spark.master.driverPort", "Spark Master driver Port", PortRanges.fromString("4040+"));

    @SetFromFlag("workerWebPortRangeStart")
    PortAttributeSensorAndConfigKey SPARK_WORKER_WEB_PORT_RANGE = new PortAttributeSensorAndConfigKey("spark.worker.webPortRangeStart", "Spark Worker node start Web Interface Port", PortRanges.fromString("8081+"));

    @SetFromFlag("sparkMetricsPropertiesTempalteUrl")
    ConfigKey<String> SPARK_METRICS_PROPS_TEMPLATE_URL = ConfigKeys.newStringConfigKey(
            "spark.metricsPropertiesTempalteUrl", "Template file (in freemarker format) for the metrics.properties config file",
            "classpath://metrics.properties.template");

    @SetFromFlag("sparkEnvTemplateUrl")
    ConfigKey<String> SPARK_ENV_TEMPLATE_URL = ConfigKeys.newStringConfigKey(
            "spark.envTemplateUrl", "Template file (in freemarker format) for the spark-env.sh config file to configure the Spark Node",
            "classpath://spark-env.sh.template");

    @SetFromFlag("sparkWorkerCores")
    ConfigKey<Integer> SPARK_WORKER_CORES = ConfigKeys.newIntegerConfigKey("spark.workerCores", " sets the number of cores to use on this worker node", 1);

    @SetFromFlag("sparkWorkerMemory")
    ConfigKey<String> SPARK_WORKER_MEMORY = ConfigKeys.newStringConfigKey("spark.workerMemory", "sets how much total memory workers have to give executors (e.g. 1000m, 2g)", "1000m");

    @SetFromFlag("sparkPidDir")
    ConfigKey<String> SPARK_PID_DIR = ConfigKeys.newStringConfigKey("spark.pidDir", "The directory location of the Spark PID files", "/tmp");

    AttributeSensor<String> SPARK_HOME_DIR = Sensors.newStringSensor("spark.homeDir", "Home directory for Spark");
    AttributeSensor<Boolean> IS_MASTER = Sensors.newBooleanSensor("spark.isMaster", "flag to determine if the current spark node is the master node for the cluster");
    AttributeSensor<Boolean> IS_MASTER_INITIALIZED = Sensors.newBooleanSensor("spark.isMasterInitialized", "flag to determine if the master node has been initialized");
    AttributeSensor<String> MASTER_CONNECTION_URL = Sensors.newStringSensor("spark.masterConnectionUrl", "url that is used by workers to connect to the masternode");
    AttributeSensor<String> MASTER_FULL_HOSTNAME = Sensors.newStringSensor("spark.masterFullHostname", "short hostname at master");
    AttributeSensor<String> MASTER_SHORT_HOSTNAME = Sensors.newStringSensor("spark.masterShortHostname", "full hostname at master");
    AttributeSensor<List<Long>> WORKER_INSTANCE_IDS = Sensors.newSensor(new TypeToken<List<Long>>() {
    }, "spark.wokerInstanceIds", "The Spark worker instances IDs initialized on this node");

    //TODO: aggregate the sensors to include all the current instances in the cluster.
    AttributeSensor<String> SPARK_WORKER_ID_SENSOR = Sensors.newStringSensor("spark.workerId", "The assigned worker Id by the Spark cluster");
    AttributeSensor<Integer> SPARK_WORKER_CORES_SENSOR = Sensors.newIntegerSensor("spark.workerCores", "Number of cores available for the worker");
    AttributeSensor<Integer> SPARK_WORKER_CORES_USED_SENSOR = Sensors.newIntegerSensor("spark.workerCoresUsed", "Number of cores used in the worker");
    AttributeSensor<Integer> SPARK_WORKER_MEMORY_SENSOR = Sensors.newIntegerSensor("spark.workerMemory", "Amount of memory available in the worker");
    AttributeSensor<Integer> SPARK_WORKER_MEMORY_USED_SENSOR = Sensors.newIntegerSensor("spark.workerMemoryUsed", "Amount of memory used by worker");
    AttributeSensor<String> SPARK_STATUS_SENSOR = Sensors.newStringSensor("spark.status", "Status of the Spark Cluster");

    public static final MethodEffector<Void> ADD_SPARK_WORKER_INSTANCES = new MethodEffector<Void>(SparkNode.class, "addSparkWorkerInstances");
    public static final MethodEffector<Void> START_MASTER_NODE = new MethodEffector<Void>(SparkNode.class, "startMasterNode");

    @Effector(description = "add this worker instances to this spark node")
    public void addSparkWorkerInstances(@EffectorParam(name = "noOfInstances") Integer numberOfInstances);

    @Effector(description = "initialize master node if this node is promoted to be the spark master node")
    public void startMasterNode();

    public Integer getMasterServicePort();

    public Integer getMasterWebPort();

    public Integer getWorkerWebPort();

    public Integer getWorkerServicePort();

    public String getHostname();

    public String getPidDir();

    public Integer getSparkWorkerCores();

    public String getSparkWorkerMemory();

    public String getAddress();

    public String getSubnetAddress();

    AttributeSensor<String> SPARK_NODE_URL = SparkNodeUrl.SPARK_NODE_URL;

    class SparkNodeUrl {
        public static final AttributeSensor<String> SPARK_NODE_URL = Sensors.newStringSensor("spark.node.url", "URL");

        static {
            RendererHints.register(SPARK_NODE_URL, RendererHints.namedActionWithUrl());
        }
    }
}
