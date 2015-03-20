package io.cloudsoft.spark;

import static java.lang.String.format;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.software.SshEffectorTasks;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.location.Location;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.guava.Maybe;
import brooklyn.util.task.DynamicTasks;

public class SparkClusterImpl extends DynamicClusterImpl implements SparkCluster {
    private static final Logger log = LoggerFactory.getLogger(SparkClusterImpl.class);

    @Override
    public void init() {
        setDisplayName(format("Spark Cluster:%s", getId()));
        setAttribute(RECONFIGURING_SPARK_CLUSTER, new AtomicBoolean(false));
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

    protected void connectSensors() {
        subscribeToMembers(this, SparkNode.SERVICE_UP, new SensorEventListener() {
            //TODO: add a reconfiguration policy if master fails.
            @Override
            public void onEvent(SensorEvent sensorEvent) {
                Entity node = sensorEvent.getSource();
                Boolean serviceUp = (Boolean) sensorEvent.getValue();
                if (node.getAttribute(SparkNode.IS_MASTER) && serviceUp.equals(Boolean.FALSE)) {
                    SparkClusterImpl.this.getAttribute(SparkCluster.RECONFIGURING_SPARK_CLUSTER).set(true);
//                    log.info("Master node is Down, reconfiguring the Spark cluster");
//                    SparkClusterImpl.this.getAttribute(RECONFIGURING_SPARK_CLUSTER).set(true);
//                    log.info("Stopping all Spark instances");
                    //Entities.invokeEffector(SparkClusterImpl.this, SparkClusterImpl.this.getMembers(), SparkNode.STOP);
                }
            }
        });


    }

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
            SshMachineLocation loc = (SshMachineLocation) Iterables.find(masterNode.getLocations(), Predicates.instanceOf(SshMachineLocation.class));
            String sparkHome = masterNode.getAttribute(SparkNode.SPARK_HOME_DIR);
            String sparkMasterUrl = masterNode.getAttribute(SparkNode.MASTER_CONNECTION_URL);

            DynamicTasks.queueIfPossible(new SshEffectorTasks.SshPutEffectorTaskFactory(loc, format("%s/javaSparkPi.jar", sparkHome))
                    .contents(this.getClass().getResourceAsStream("/simple-project-1.0.jar")).newTask());

            final String command = format("%s/bin/spark-submit --class JavaSparkPi --master %s %s/javaSparkPi.jar", sparkHome, sparkMasterUrl, sparkHome);
            log.info("command: " + command);
            DynamicTasks.queueIfPossible(new SshEffectorTasks.SshEffectorTaskFactory(loc, command).newTask());
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
