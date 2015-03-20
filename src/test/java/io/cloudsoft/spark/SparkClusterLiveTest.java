package io.cloudsoft.spark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import brooklyn.entity.BrooklynAppLiveTestSupport;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.test.EntityTestUtils;

@Test(groups="Live")
public class SparkClusterLiveTest extends BrooklynAppLiveTestSupport {

    private static final Logger log = LoggerFactory.getLogger(SparkClusterLiveTest.class);

    private String provider = "jclouds:softlayer:ams01";
    //private String provider = "byon:(user=\"andrea\",hosts=\"37.58.126.5, 37.58.126.6\")";

    protected Location testLocation;
    protected SparkCluster cluster;

    @BeforeMethod(alwaysRun = true)
    @Override
    public void setUp() throws Exception {
        super.setUp();
        testLocation = mgmt.getLocationRegistry().resolve(provider);
    }

    @AfterMethod(alwaysRun = true)
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test(groups = "Live")
    public void testCluster() throws Exception {
        EntitySpec<SparkCluster> spec = EntitySpec.create(SparkCluster.class)
                .configure("initialSize", 2)
                .configure("clusterName", "SparkClusterLive");

        cluster = app.createAndManageChild(spec);
        Assert.assertEquals(cluster.getCurrentSize().intValue(), 0);

        app.start(ImmutableList.of(testLocation));

        // Check cluster is up and healthy
        EntityTestUtils.assertAttributeEqualsEventually(cluster, SparkCluster.GROUP_SIZE, 2);
        Entities.dumpInfo(app);

        cluster.runJavaSparkPiDemo();
    }
}
