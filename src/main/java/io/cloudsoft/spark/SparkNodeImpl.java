package io.cloudsoft.spark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;

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

        if (isMaster()) {
            HostAndPort master = BrooklynAccessUtils.getBrooklynAccessibleAddress(this, getMasterWebPort());

            Preconditions.checkNotNull(master, "Host and Port is not set for %s", this);
            String webUrl = String.format("http://%s", master.toString());
            setAttribute(SPARK_NODE_URL, webUrl);

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
    public void startMasterNode() {
        getDriver().startMasterNode();
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
        return null;
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
    public String getAddress() {
        return getAttribute(SparkNode.ADDRESS);
    }

    @Override
    public String getSubnetAddress() {
        return getAttribute(SparkNode.SUBNET_ADDRESS);
    }

    private boolean isMaster() {
        return getAttribute(IS_MASTER) != null ? getAttribute(IS_MASTER) : false;
    }
}
