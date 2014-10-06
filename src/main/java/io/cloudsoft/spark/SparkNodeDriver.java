package io.cloudsoft.spark;

import java.util.List;

import brooklyn.entity.basic.SoftwareProcessDriver;

public interface SparkNodeDriver extends SoftwareProcessDriver {

    public void addSparkWorkerInstances(Integer noOfInstances);
    public void startMasterNode();
    public void submitSparkApp(String appName);
    public String getSparkHome();
}
