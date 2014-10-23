package io.cloudsoft.spark;

import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import brooklyn.entity.BrooklynAppLiveTestSupport;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.test.EntityTestUtils;

public class SparkClusterLiveTest extends BrooklynAppLiveTestSupport {

    private static final Logger log = LoggerFactory.getLogger(SparkClusterLiveTest.class);

    private String provider =
            "named:softlayer-ams01";
//            "aws-ec2:eu-west-1";
//            "rackspace-cloudservers-uk";
//            "named:hpcloud-compute-at";
//            "localhost";
//            "jcloudsByon:(provider=\"aws-ec2\",region=\"us-east-1\",user=\"aled\",hosts=\"i-6f374743,i-35324219,i-1135453d\")";

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
        List<SparkNode> members = (List<SparkNode>) Iterables.transform(cluster.getMembers(), new Function<Entity, SparkNode>() {

            @Nullable
            @Override
            public SparkNode apply(@Nullable Entity entity) {
                return (SparkNode) entity;
            }
        });

        // Resize
        cluster.resize(3);
        Assert.assertEquals(cluster.getMembers().size(), 3, "members=" + cluster.getMembers());
    }
}
