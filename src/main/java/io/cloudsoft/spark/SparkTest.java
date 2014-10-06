package io.cloudsoft.spark;

import java.util.List;

import com.google.common.collect.Lists;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.util.CommandLineUtil;

public class SparkTest extends AbstractApplication {

    public static String DEFAULT_LOCATION_SPEC = "AWS Virginia (us-east-1)";

    @Override
    public void init() {
        addChild(EntitySpec.create(SparkCluster.class)
                .configure(SparkCluster.INITIAL_SIZE, 2)
                .configure(SparkCluster.MEMBER_SPEC, EntitySpec.create(SparkNode.class).configure(SparkNode.SPARK_WORKER_MEMORY, "100m")));
    }

    public static void main(String[] argv) {
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", DEFAULT_LOCATION_SPEC);

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpec.create(StartableApplication.class, SparkTest.class)
                        .displayName("Spark Test"))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}