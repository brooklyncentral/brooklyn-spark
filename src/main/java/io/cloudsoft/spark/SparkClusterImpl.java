package io.cloudsoft.spark;

import static java.lang.String.format;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import brooklyn.config.render.RendererHints;
import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.software.SshEffectorTasks;
import brooklyn.location.Location;
import brooklyn.location.basic.Machines;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.guava.Maybe;
import brooklyn.util.task.DynamicTasks;

public class SparkClusterImpl extends DynamicClusterImpl implements SparkCluster {
    private static final Logger log = LoggerFactory.getLogger(SparkClusterImpl.class);

    static {
        RendererHints.register(SparkCluster.MASTER_NODE_WEB_CONSOLE_URL, new RendererHints.NamedActionWithUrl("Open"));
    }

    @Override
    public void init() {
        setDisplayName(format("Spark Cluster:%s", getId()));

        if (!Optional.fromNullable(getAttribute(SparkCluster.SPARK_WORKER_INSTANCE_ID_TRACKER)).isPresent()) {
            setAttribute(SPARK_WORKER_INSTANCE_ID_TRACKER, new AtomicLong(0));
        }
        super.init();
    }

    @Override
    protected EntitySpec<?> getMemberSpec() {
        return getConfig(MEMBER_SPEC, EntitySpec.create(SparkNode.class));
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);
        connectSensors();
    }

    @Override
    protected void doStart() {
        super.doStart();
        log.info("Cluster:{} is initialized adding worker instances to each worker node", getId());
        addSparkWorkerInstances(getConfig(INITIAL_SIZE) - 1);

//        log.info("Cluster:{} is initialized running Java Pi Demo", getId());
//        runJavaSparkPiDemo();
    }

    protected void connectSensors() {

        //TODO: add logic to flag the cluster unavailable if master node is down.
        connectEnrichers();
    }

    protected void connectEnrichers() {

        addEnricher(Enrichers.builder()
                .transforming(SparkCluster.MASTER_SPARK_NODE)
                .from(this)
                .computing(new Function<Entity, String>() {

                    @Override
                    public String apply(Entity masterNode) {
                        return "http://" + masterNode.getAttribute(Attributes.HOSTNAME) + ":"
                                + masterNode.getAttribute(SparkNode.SPARK_MASTER_WEB_PORT);
                    }
                })
                .publishing(SparkCluster.MASTER_NODE_WEB_CONSOLE_URL)
                .build());

        addEnricher(Enrichers.builder()
                .propagating(SparkNode.MASTER_CONNECTION_URL)
                .from(getAttribute(SparkCluster.MASTER_SPARK_NODE))
                .build());



    }

    //TODO: passing an application url to this effector submits it to the spark cluster.
    @Override
    public void submitSparkApplication(String masterNodeConnectionUrl) {
        getMasterNode();
    }

    @Override
    public void addSparkWorkerInstances(Integer numberOfInstances) {
        //adds instances to the available nodes in round robin
        if (getAttribute(SERVICE_UP) && getMembers().size() > 0 && numberOfInstances > 0) {
            Iterator<Entity> sparkNodes = Iterables.cycle(Iterables.filter(getMembers(), Predicates.instanceOf(SparkNode.class))).iterator();

            for (int i = 0; i < numberOfInstances; i++) {
                Entities.invokeEffectorWithArgs(this, sparkNodes.next(), SparkNode.ADD_SPARK_WORKER_INSTANCES, 1);
            }
        }
    }

    @Override
    public void runJavaSparkPiDemo() {
        if (Maybe.fromNullable(getAttribute(SERVICE_UP)).or(Boolean.FALSE).equals(Boolean.TRUE)) {
            Entity masterNode = getMasterNode();

            SshMachineLocation loc = Machines.findUniqueSshMachineLocation(masterNode.getLocations()).get();
            String sparkHome = masterNode.getAttribute(SparkNode.SPARK_HOME_DIR);
            String sparkMasterUrl = masterNode.getAttribute(SparkNode.MASTER_CONNECTION_URL);

            DynamicTasks.queueIfPossible(new SshEffectorTasks.SshPutEffectorTaskFactory(loc, format("%s/javaSparkPi.jar", sparkHome))
                    .contents(this.getClass().getResourceAsStream("/simple-project-1.0.jar")).newTask());

            DynamicTasks.queueIfPossible(new SshEffectorTasks.SshEffectorTaskFactory(loc, format("%s/bin/spark-submit --class JavaSparkPi --master %s %s/javaSparkPi.jar", sparkHome, sparkMasterUrl, sparkHome)).newTask());
        }
    }

    private Entity getMasterNode() {
        return Optional.fromNullable(getAttribute(MASTER_SPARK_NODE)).get();
    }

    private String getMasterNodeHostname() {
        return Optional.fromNullable(getAttribute(MASTER_NODE_HOSTNAME)).get();
    }

    private Integer getMasterNodeServicePort() {
        return Optional.fromNullable(getAttribute(MASTER_NODE_SERVICE_PORT)).get();
    }


}