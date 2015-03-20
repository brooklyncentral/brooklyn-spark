package io.cloudsoft.spark.app;

import brooklyn.enricher.Enrichers;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.proxying.EntitySpec;
import io.cloudsoft.spark.SparkCluster;

public class SparkClusteredApp extends AbstractApplication implements StartableApplication {

    @Override
    public void init() {

        setDisplayName("Apache Spark clustered (3-node) deployment");

        SparkCluster sparkCluster = addChild(EntitySpec.create(SparkCluster.class)
                .configure(SparkCluster.INITIAL_SIZE, 2));

        addEnricher(Enrichers.builder()
                .propagating(SparkCluster.MASTER_NODE_HOSTNAME)
                .from(sparkCluster)
                .build());
    }

}
