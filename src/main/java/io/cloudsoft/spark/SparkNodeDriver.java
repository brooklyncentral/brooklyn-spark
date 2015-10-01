package io.cloudsoft.spark;

import org.apache.brooklyn.entity.software.base.SoftwareProcessDriver;

public interface SparkNodeDriver extends SoftwareProcessDriver {

    public void addSparkWorkerInstances(Integer noOfInstances);

    public void startMasterNode();

    public String getSparkHome();
}
