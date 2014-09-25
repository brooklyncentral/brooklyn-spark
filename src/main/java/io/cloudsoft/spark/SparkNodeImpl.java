package io.cloudsoft.spark;

import brooklyn.entity.basic.SoftwareProcessImpl;

public class SparkNodeImpl extends SoftwareProcessImpl implements SparkNode {
    @Override
    public Class getDriverInterface() {
        return SparkNodeDriver.class;
    }

    public SparkNodeDriver getDriver() {
        return (SparkNodeDriver) super.getDriver();
    }
}
