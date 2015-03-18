package io.cloudsoft.spark;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import brooklyn.entity.BrooklynAppLiveTestSupport;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.test.EntityTestUtils;

@Test(groups = "Integration")
public class SparkClusterIntegrationTest extends BrooklynAppLiveTestSupport {
    protected Location testLocation;
    protected SparkCluster sparkCluster;

    @Override
    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        super.setUp();
        testLocation = app.newLocalhostProvisioningLocation();
    }

    public void testStartAndShutdownClusterSizeOne() throws Exception {
        EntitySpec<SparkCluster> spec = EntitySpec.create(SparkCluster.class)
                .configure("initialSize", 1);

        runStartAndShutdownClusterSizeOne(spec);
    }

    protected void runStartAndShutdownClusterSizeOne(EntitySpec<SparkCluster> clusterSpec) throws Exception {
        sparkCluster = app.createAndManageChild(clusterSpec);
        Assert.assertEquals(sparkCluster.getCurrentSize().intValue(), 0);

        app.start(ImmutableList.of(testLocation));
        Entities.dumpInfo(app);

        final SparkNode node = (SparkNode) Iterables.get(sparkCluster.getMembers(), 0);

        EntityTestUtils.assertAttributeEqualsEventually(sparkCluster, SparkCluster.GROUP_SIZE, 1);
        EntityTestUtils.assertAttributeEqualsEventually(node, SparkNode.SERVICE_UP, true);

//        Entities.waitForServiceUp(sparkCluster, Duration.minutes(20));

//        String sparkMasterUrl = node.getAttribute(SparkNode.MASTER_CONNECTION_URL);
//        SparkConf conf = new SparkConf().setAppName("TestApp").setMaster(sparkMasterUrl);
//        JavaSparkContext sc = new JavaSparkContext(conf);
//
//        Assert.assertEquals(sc.isLocal(), Boolean.TRUE);
    }
}