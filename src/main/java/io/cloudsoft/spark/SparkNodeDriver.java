package io.cloudsoft.spark;

import java.util.List;

import brooklyn.entity.basic.SoftwareProcessDriver;

public interface SparkNodeDriver extends SoftwareProcessDriver {

    public void joinSparkCluster(String masterNodeConnectionUrl);
    public void startMasterNode();
    public void submitSparkApp(String appName);
    public String getSparkHome();
}
