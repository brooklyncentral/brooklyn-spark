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


        Integer workerWebPort = getAttribute(SparkNode.SPARK_WORKER_WEB_PORT);
        Preconditions.checkNotNull(workerWebPort, "WEBUI Port is not set for %s", this);
        HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(this, workerWebPort);
        String workerWebUrl = String.format("http://%s", hp.toString());

        HttpFeed.Builder httpFeedBuilder = HttpFeed.builder()
                .entity(this)
                .period(1000)
                .baseUri(workerWebUrl + "/json/")
                .poll(getSensorFromNodeStat(SparkNode.SPARK_WORKER_ID, "id"))
                .poll(getSensorFromNodeStat(SparkNode.SPARK_WORKER_CORES, "cores"))
                .poll(getSensorFromNodeStat(SparkNode.SPARK_WORKER_MEMORY, "memory"))
                .poll(getSensorFromNodeStat(SparkNode.SPARK_WORKER_MEMORY_USED, "memoryused"));

        if (isMaster()) {
            httpFeedBuilder.poll(getSensorFromNodeStat(SparkNode.SPARK_STATUS, "status"));
        }
        httpFeed = httpFeedBuilder.build();
    }

    public void disconnectSensors() {
        super.disconnectSensors();
        disconnectServiceUpIsRunning();
        if (httpFeed != null) {
            httpFeed.stop();
        }
    }

    @Override
    public void joinSparkCluster(String masterNodeConnectionUrl) {
        getDriver().joinSparkCluster(masterNodeConnectionUrl);
    }

    @Override
    public void startMasterNode() {
        getDriver().startMasterNode();
    }

    @Override
    public void submitSparkApp(String sparkJarLocation) {
        getDriver().submitSparkApp(sparkJarLocation);
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
        return getAttribute(SparkNode.SPARK_WORKER_WEB_PORT);
    }

    @Override
    public Integer getWorkerServicePort() {
        return null;
    }

    @Override
    public String getHostname() {
        return getAttribute(HOSTNAME);
    }

    private boolean isMaster() {
        return getAttribute(IS_MASTER) != null ? getAttribute(IS_MASTER) : false;
    }

}