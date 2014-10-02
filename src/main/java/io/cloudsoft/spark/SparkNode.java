package io.cloudsoft.spark;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import brooklyn.config.ConfigKey;
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

    @SetFromFlag("workerWebPort")
    PortAttributeSensorAndConfigKey SPARK_WORKER_WEB_PORT = new PortAttributeSensorAndConfigKey("spark.worker.webPort", "Spark Worker node Web Interface Port", PortRanges.fromString("8081+"));

    @SetFromFlag("sparkEnvMasterTemplateUrl")
    ConfigKey<String> SPARK_ENV_MASTER_TEMPLATE_URL = ConfigKeys.newStringConfigKey(
            "spark.envMasterTemplateUrl", "Template file (in freemarker format) for the spark-env.sh config file to configure the Spark Master",
            "classpath://spark-env.sh.master.template");

    @SetFromFlag("sparkEnvWorkerTemplateUrl")
    ConfigKey<String> SPARK_ENV_WORKER_TEMPLATE_URL = ConfigKeys.newStringConfigKey(
            "spark.envWorkerTemplateUrl", "Template file (in freemarker format) for the spark-env.sh config file to configure the Spark Worker",
            "classpath://spark-env.sh.worker.template");

    @SetFromFlag("sparkMetricsPropertiesTempalteUrl")
    ConfigKey<String> SPARK_METRICS_PROPS_TEMPLATE_URL = ConfigKeys.newStringConfigKey(
            "spark.metricsPropertiesTempalteUrl", "Template file (in freemarker format) for the metrics.properties config file",
            "classpath://metrics.properties.template");


    AttributeSensor<String> SPARK_HOME_DIR = Sensors.newStringSensor("spark.homeDir", "Home directory for Spark");
    AttributeSensor<Boolean> IS_MASTER = Sensors.newBooleanSensor("spark.isMaster", "flag to determine if the current spark node is the master node for the cluster");
    AttributeSensor<Boolean> IS_MASTER_INITIALIZED = Sensors.newBooleanSensor("spark.isMasterInitialized", "flag to determine if the master node has been initialized");
    AttributeSensor<String> MASTER_CONNECTION_URL = Sensors.newStringSensor("spark.masterConnectionUrl", "url that is used by workers to connect to the masternode");

    /* Attributes gathered from polling the metrics servlet on spark workers
    * {
     "id" : "worker-20140929154440-ip-10-180-145-69.ec2.internal-36822",
    "masterurl" : "spark://ip-10-183-211-217:7077",
    "masterwebuiurl" : "http://ip-10-183-211-217:8080",
    "cores" : 1,
    "coresused" : 0,
    "memory" : 631,
    "memoryused" : 0,
    "executors" : [ ],
    "finishedexecutors" : [ ]
    }*/

    AttributeSensor<String> SPARK_WORKER_ID = Sensors.newStringSensor("spark.workerId", "The assigned worker Id by the Spark cluster");
    AttributeSensor<Integer> SPARK_WORKER_CORES = Sensors.newIntegerSensor("spark.workerCores", "Number of cores available for the worker");
    AttributeSensor<Integer> SPARK_WORKER_CORES_USED = Sensors.newIntegerSensor("spark.workerCoresUsed", "Number of cores used in the worker");
    AttributeSensor<Integer> SPARK_WORKER_MEMORY = Sensors.newIntegerSensor("spark.workerMemory", "Amount of memory available in the worker");
    AttributeSensor<Integer> SPARK_WORKER_MEMORY_USED = Sensors.newIntegerSensor("spark.workerMemoryUsed", "Amount of memory used by worker");
    AttributeSensor<Integer> SPARK_STATUS = Sensors.newIntegerSensor("spark.status", "Status of the Spark Cluster");

    public static final MethodEffector<Void> JOIN_SPARK_CLUSTER = new MethodEffector<Void>(SparkNode.class, "joinSparkCluster");
    public static final MethodEffector<Void> START_MASTER_NODE = new MethodEffector<Void>(SparkNode.class, "startMasterNode");
    public static final MethodEffector<Void> SUBMIT_SPARK_APP = new MethodEffector<Void>(SparkNode.class, "submitSparkApp");

    @Effector(description = "add this worker node to the spark cluster")
    public void joinSparkCluster(@EffectorParam(name = "masterConnectionUrl") String masterNodeConnectionUrl);

    @Effector(description = "initialize master node if this node is promoted to be the spark master node")
    public void startMasterNode();

    @Effector(description = "submit app to Spark")
    public void submitSparkApp(@EffectorParam(name = "appName") String sparkJarLocation);

    public Integer getMasterServicePort();

    public Integer getMasterWebPort();

    public Integer getWorkerWebPort();

    public Integer getWorkerServicePort();

    public String getHostname();
}