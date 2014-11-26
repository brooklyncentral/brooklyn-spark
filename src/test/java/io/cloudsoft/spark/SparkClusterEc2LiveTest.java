package io.cloudsoft.spark;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import brooklyn.entity.AbstractEc2LiveTest;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.test.EntityTestUtils;

@Test(groups = "Live")
public class SparkClusterEc2LiveTest extends AbstractEc2LiveTest {
//    private static final Logger log = LoggerFactory.getLogger(SparkClusterEc2LiveTest.class);

    @Override
    protected void doTest(Location location) throws Exception {
        SparkCluster sparkCluster = app.createAndManageChild(EntitySpec.create(SparkCluster.class)
                .configure(SparkCluster.INITIAL_SIZE, 2)
                .configure(SparkCluster.MEMBER_SPEC, EntitySpec.create(SparkNode.class).configure(SparkNode.SPARK_WORKER_MEMORY, "1000m")));

        app.start(ImmutableList.of(location));

        Iterator<Entity> clusterIterator = sparkCluster.getMembers().iterator();

        Entity node1 = clusterIterator.next();
        Entity node2 = clusterIterator.next();

        Entity masterNode = Iterables.find(sparkCluster.getMembers(), new Predicate<Entity>() {
            @Override
            public boolean apply(@Nullable Entity entity) {
                if (entity instanceof SparkNode && entity.getAttribute(SparkNode.IS_MASTER)) {
                    return true;
                }

                return false;
            }
        });

        EntityTestUtils.assertAttributeEqualsEventually(node1, SparkNode.SERVICE_UP, true);
        EntityTestUtils.assertAttributeEqualsEventually(node2, SparkNode.SERVICE_UP, true);
        EntityTestUtils.assertAttributeEqualsEventually(sparkCluster, SparkCluster.SERVICE_UP, true);

    }
}