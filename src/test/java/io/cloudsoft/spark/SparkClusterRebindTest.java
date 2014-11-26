package io.cloudsoft.spark;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Lifecycle;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.rebind.RebindTestFixtureWithApp;
import brooklyn.test.EntityTestUtils;

public class SparkClusterRebindTest extends RebindTestFixtureWithApp {

    @Test(groups = "Integration")
    public void testRebindToMachineEntity() throws Exception {
        SparkCluster cluster = origApp.createAndManageChild(EntitySpec.create(SparkCluster.class).configure(SparkCluster.INITIAL_SIZE, 2));
        origApp.start(ImmutableList.of(origManagementContext.getLocationRegistry().resolve("localhost")));
        EntityTestUtils.assertAttributeEqualsEventually(cluster, Attributes.SERVICE_STATE_ACTUAL, Lifecycle.RUNNING);
        rebind(false);
        Entity cluster2 = newManagementContext.getEntityManager().getEntity(cluster.getId());
        EntityTestUtils.assertAttributeEqualsEventually(cluster2, Attributes.SERVICE_STATE_ACTUAL, Lifecycle.RUNNING);
    }
}