package io.cloudsoft.spark;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityInternal;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.group.StopFailedRuntimeException;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.MemberReplaceable;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.location.Location;
import brooklyn.policy.ha.ServiceReplacer;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.exceptions.Exceptions;

public class SparkClusterImpl extends DynamicClusterImpl implements SparkCluster {
    private static final Logger log = LoggerFactory.getLogger(SparkClusterImpl.class);

    @Override
    public void init() {

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

            @Override
            public void onEvent(SensorEvent sensorEvent) {
                Entity node = sensorEvent.getSource();
                Boolean serviceUp = (Boolean) sensorEvent.getValue();
                if (node.getAttribute(SparkNode.IS_MASTER) && serviceUp.equals(Boolean.FALSE)) {
                    log.info("Master node is Down, reconfiguring the spark cluster");
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

    private Entity getMasterNode() {
        return Optional.fromNullable(getAttribute(MASTER_SPARK_NODE)).get();
    }

    private String getMasterNodeHostname() {
        return Optional.fromNullable(getAttribute(MASTER_NODE_HOSTNAME)).get();
    }

    private Integer getMasterNodeServicePort() {
        return Optional.fromNullable(getAttribute(MASTER_NODE_SERVICE_PORT)).get();
    }

    public static class SparkClusterResilliencePolicy extends ServiceReplacer {
        @Override
        protected synchronized void onDetectedFailure(SensorEvent<Object> event) {
            final Entity failedEntity = event.getSource();
            final Entity failedEntityCluster = failedEntity.getAttribute(SparkCluster.CLUSTER);
            final Object reason = event.getValue();

            if (isSuspended()) {
                log.warn("ServiceReplacer suspended, so not acting on failure detected at " + failedEntity + " (" + reason + ", child of " + entity + ")");
                return;
            }

//        if (super.isRepeatedlyFailingTooMuch()) {
//            log.error("ServiceReplacer not acting on failure detected at "+failedEntity+" ("+reason+", child of "+entity+"), because too many recent replacement failures");
//            return;
//        }

            log.warn("ServiceReplacer acting on failure detected at " + failedEntity + " (" + reason + ", child of " + entity + ")");
            ((EntityInternal) entity).getManagementSupport().getExecutionContext().submit(MutableMap.of(), new Runnable() {

                @Override
                public void run() {
                    try {

                        if (Optional.fromNullable(entity.getAttribute(SparkNode.IS_MASTER)).or(false)) {
                            log.info("----- MASTER IS DOWN! SHUTTING DOWN ALL WORKER NODES -----");
                            log.info("reconfiguring the Spark Cluster...");

                            Entities.invokeEffectorWithArgs(entity, entity, MemberReplaceable.REPLACE_MEMBER, failedEntity.getId()).get();
                            consecutiveReplacementFailureTimes.clear();

                            ((EntityInternal) failedEntityCluster).setAttribute(SparkCluster.FIRST, entity);
                        } else {

                        }
                    } catch (Exception e) {
                        if (Exceptions.getFirstThrowableOfType(e, StopFailedRuntimeException.class) != null) {
                            log.info("ServiceReplacer: ignoring error reported from stopping failed node " + failedEntity);
                            return;
                        }
                        onReplacementFailed("Replace failure (error " + e + ") at " + entity + ": " + reason);
                    }
                }
            });
        }
    }

    ;
}