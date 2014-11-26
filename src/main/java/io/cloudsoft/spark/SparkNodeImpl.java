package io.cloudsoft.spark;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;

import brooklyn.entity.basic.EntityInternal;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.AttributeSensor;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.location.access.BrooklynAccessUtils;
import brooklyn.util.guava.TypeTokens;

public class SparkNodeImpl extends SoftwareProcessImpl implements SparkNode {

    private static final Logger log = LoggerFactory.getLogger(SparkNodeImpl.class);
    HttpFeed httpFeed;

    @Override
    public void init() {
        super.init();
    }

    @Override
    public Class getDriverInterface() {
        return SparkNodeDriver.class;
    }

    public SparkNodeDriver getDriver() {
        return (SparkNodeDriver) super.getDriver();
    }


    protected final static <T> HttpPollConfig<T> getSensorFromNodeStat(AttributeSensor<T> sensor, String metricName) {
        return new HttpPollConfig<T>(sensor)
                .onSuccess(HttpValueFunctions.jsonContents(metricName, TypeTokens.getRawRawType(sensor.getTypeToken())))
                .onFailureOrException(Functions.<T>constant(null));
    }

    public void connectSensors() {
        super.connectSensors();
        connectServiceUpIsRunning();

        //TODO: add sensors for worker nodes
        HostAndPort hp = null;
        if (getAttribute(IS_MASTER)) {
            Integer masterWebPort = getAttribute(SparkNode.SPARK_MASTER_WEB_PORT);
            Preconditions.checkNotNull(masterWebPort, "WEBUI Port is not set for %s", this);
            hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(this, masterWebPort);


            Preconditions.checkNotNull(hp, "Host and Port is not set for %s", this);
            String webUrl = String.format("http://%s", hp.toString());

            httpFeed = HttpFeed.builder()
                    .entity(this)
                    .period(1000)
                    .baseUri(webUrl + "/json/")
                    .poll(new HttpPollConfig<Boolean>(SERVICE_UP)
                            .onSuccess(HttpValueFunctions.responseCodeEquals(200))
                            .onFailureOrException(Functions.constant(false)))
                    .poll(getSensorFromNodeStat(SparkNode.SPARK_STATUS_SENSOR, "status"))
                    .poll(getSensorFromNodeStat(SparkNode.SPARK_WORKER_CORES_SENSOR, "cores"))
                    .poll(getSensorFromNodeStat(SparkNode.SPARK_WORKER_CORES_USED_SENSOR, "coresused"))
                    .poll(getSensorFromNodeStat(SparkNode.SPARK_WORKER_MEMORY_SENSOR, "memory"))
                    .poll(getSensorFromNodeStat(SparkNode.SPARK_WORKER_MEMORY_USED_SENSOR, "memoryused"))
                    .build();
        }
    }

    @Override
    protected void postStart() {
        super.postStart();
        if (getAttribute(IS_MASTER)) {
            setAttribute(SparkNode.IS_MASTER_INITIALIZED, Boolean.TRUE);
            ((EntityInternal) getAttribute(SparkCluster.CLUSTER)).setAttribute(SparkCluster.MASTER_SPARK_NODE, this);

            setDisplayName(format("Spark Master Node:%s", getId()));
        } else {
            setDisplayName(format("Spark Worker Node:%s", getId()));
        }

    }

    public void disconnectSensors() {
        super.disconnectSensors();
        disconnectServiceUpIsRunning();
        if (httpFeed != null) {
            httpFeed.stop();
        }
    }

    @Override
    public void addSparkWorkerInstances(Integer numberOfInstances) {
        getDriver().addSparkWorkerInstances(numberOfInstances);
    }

    @Override
    public Integer getMasterServicePort() {
        return getAttribute(SparkNode.SPARK_MASTER_SERVICE_PORT);
    }

    @Override
    public Integer getMasterWebPort() {
        return getAttribute(SparkNode.SPARK_MASTER_WEB_PORT);
    }

    @Override
    public Integer getWorkerWebPort() {
        return getAttribute(SparkNode.SPARK_WORKER_WEB_PORT_RANGE);
    }

    @Override
    public Integer getWorkerServicePort() {
        return getAttribute(SparkNode.SPARK_WORKER_SERVICE_PORT);
    }

    @Override
    public String getHostname() {
        return getAttribute(HOSTNAME);
    }

    @Override
    public String getPidDir() {
        return getConfig(SparkNode.SPARK_PID_DIR);
    }

    @Override
    public Integer getSparkWorkerCores() {
        return getConfig(SparkNode.SPARK_WORKER_CORES);
    }

    @Override
    public String getSparkWorkerMemory() {
        return getConfig(SparkNode.SPARK_WORKER_MEMORY);
    }

    @Override
    public String getSubnetAddress() {
        return getAttribute(SparkNode.SUBNET_ADDRESS);
    }

}