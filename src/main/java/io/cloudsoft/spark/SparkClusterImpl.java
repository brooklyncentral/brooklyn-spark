package io.cloudsoft.spark;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import brooklyn.entity.Entity;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;

public class SparkClusterImpl extends DynamicClusterImpl implements SparkCluster {
    private static final Logger log = LoggerFactory.getLogger(SparkClusterImpl.class);

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
//        subscribeToMembers(this, Attributes.SERVICE_UP ,new SensorEventListener<Boolean>() {
//            @Override
//            public void onEvent(SensorEvent<Boolean> booleanSensorEvent) {
//                if (booleanSensorEvent.getValue().equals(Boolean.FALSE))
//                {
//                    Entity sparkNode= booleanSensorEvent.getSource();
//                    Lifecycle serviceState = sparkNode.getAttribute(Attributes.SERVICE_STATE_ACTUAL);
//
//                    if (serviceState == Lifecycle.ON_FIRE || serviceState == Lifecycle.STOPPED)
//                    {
//                        if (sparkNode.getAttribute(SparkNode.IS_MASTER))
//                        {
//
//                        }
//                        else
//                        {
//
//                        }
//                    }
//
//                }
//            }
//        });
    }

    @Override
    public void submitSparkApplication(String masterNodeConnectionUrl) {
        getMasterNode();
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