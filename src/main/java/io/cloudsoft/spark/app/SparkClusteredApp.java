package io.cloudsoft.spark.app;

import org.apache.brooklyn.enricher.stock.Enrichers;
import org.apache.brooklyn.core.entity.AbstractApplication;
import org.apache.brooklyn.core.entity.StartableApplication;
import org.apache.brooklyn.api.entity.EntitySpec;
import io.cloudsoft.spark.SparkCluster;

public class SparkClusteredApp extends AbstractApplication implements StartableApplication {

    @Override
    public void init() {

        setDisplayName("Apache Spark clustered (3-node) deployment");

        SparkCluster sparkCluster = addChild(EntitySpec.create(SparkCluster.class)
                .configure(SparkCluster.INITIAL_SIZE, 3));

        addEnricher(Enrichers.builder()
                .propagating(SparkCluster.MASTER_NODE_HOSTNAME)
                .from(sparkCluster)
                .build());
    }

}
