package io.cloudsoft.spark;

import brooklyn.entity.basic.SoftwareProcessDriver;

public interface SparkNodeDriver extends SoftwareProcessDriver {

    public void addSparkWorkerInstances(Integer noOfInstances);

    public String getSparkHome();
}
