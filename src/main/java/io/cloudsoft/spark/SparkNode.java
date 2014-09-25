package io.cloudsoft.spark;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(SparkNodeImpl.class)
public interface SparkNode extends SoftwareProcess {

    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "http://d3kbcqa49mib13.cloudfront.net/spark-${version}.tgz");

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

    AttributeSensor<Boolean> IS_MASTER = Sensors.newBooleanSensor("spark.isMaster", "flag to determine if the current spark node is the master node for the cluster");
    AttributeSensor<Boolean> IS_WORKER = Sensors.newBooleanSensor("spark.isWorker", "flag to determine if the current spark node is the worker node for the cluster");


}
