package io.cloudsoft.spark;

import static java.lang.String.format;

import java.net.URI;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityInternal;
import brooklyn.entity.basic.lifecycle.ScriptHelper;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.entity.software.SshEffectorTasks;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.net.Urls;
import brooklyn.util.os.Os;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.task.DynamicTasks;
import brooklyn.util.time.Duration;
import brooklyn.util.time.Time;

public class SparkNodeSshDriver extends JavaSoftwareProcessSshDriver implements SparkNodeDriver {

    private String scalaSaveAs;
    private String sparkSaveAs;
    private String sparkHome;

    public SparkNodeSshDriver(final SparkNodeImpl entity, final SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void preInstall() {
        //assign the first node in the cluster to be master if master hasn't been set yet.
        if (!Optional.fromNullable(entity.getAttribute(SparkNode.IS_MASTER)).isPresent()) {
            if (entity.getAttribute(SparkCluster.FIRST_MEMBER)) {
                entity.setAttribute(SparkNode.IS_MASTER, Boolean.TRUE);
            } else {
                entity.setAttribute(SparkNode.IS_MASTER, Boolean.FALSE);
            }
        }
        resolver = Entities.newDownloader(this);
        setExpandedInstallDir(getInstallDir());
    }

    @Override
    public void install() {

        List<String> urls = resolver.getTargets();
        sparkSaveAs = resolver.getFilename();

        String scalaVersion = entity.getConfig(SparkNode.SCALA_VERSION);

        DownloadResolver scalaDownloadResolver = ((EntityInternal) entity).getManagementContext().getEntityDownloadsManager()
                .newDownloader(this, "scala", ImmutableMap.of("addonversion", scalaVersion));

        List<String> scalaModuleUrls = scalaDownloadResolver.getTargets();
        scalaSaveAs = scalaDownloadResolver.getFilename();

        List<String> commands = ImmutableList.<String>builder()
                .addAll(BashCommands.commandsToDownloadUrlsAs(scalaModuleUrls, scalaSaveAs))
                .addAll(BashCommands.commandsToDownloadUrlsAs(urls, sparkSaveAs))
                .add(BashCommands.INSTALL_TAR)
                .build();

        newScript(INSTALLING)
                .body.append(commands)
                .execute();
    }

    @Override
    public void customize() {

        String scalaVersion = entity.getConfig(SparkNode.SCALA_VERSION);
        String sparkVersion = entity.getConfig(SparkNode.SUGGESTED_VERSION);
        String scalaHome = Os.mergePaths(getRunDir(), format("scala-%s", scalaVersion));
        sparkHome = Os.mergePathsUnix(getRunDir(), format("spark-%s", sparkVersion));
        entity.setAttribute(SparkNode.SPARK_HOME_DIR, sparkHome);

        List<String> commands = ImmutableList.<String>builder()
                .add(BashCommands.sudo("mkdir -p " + scalaHome))
                .add("mkdir -p " + sparkHome)
                .add(BashCommands.sudo(format("tar xzfv %s/%s -C %s --strip-components 1", getInstallDir(), scalaSaveAs, scalaHome)))
                .add(format("tar xzfv %s/%s -C %s --strip-components 1", getInstallDir(), sparkSaveAs, sparkHome))
                .add(format("touch %s/conf/metrics.properties", sparkHome))
                .build();

        newScript(CUSTOMIZING)
                .body.append(commands)
                .execute();

        String metricsPropsTemplate = processTemplate(entity.getConfig(SparkNode.SPARK_METRICS_PROPS_TEMPLATE_URL));
        String saveAsMetricsProps = Urls.mergePaths(sparkHome, "/conf/metrics.properties");
        DynamicTasks.queueIfPossible(SshEffectorTasks.put(saveAsMetricsProps).contents(metricsPropsTemplate));

        String saveAsSparkEnv = Urls.mergePaths(sparkHome, "/conf/spark-env.sh");

        String sparkEnvTemplate = processTemplate(entity.getConfig(SparkNode.SPARK_ENV_TEMPLATE_URL));
        DynamicTasks.queueIfPossible(SshEffectorTasks.put(saveAsSparkEnv).contents(sparkEnvTemplate));

    }

    @Override
    public void launch() {

        //The set the launch if the entity is part of a spark cluster, otherwise do not launch.
        if (entity.getAttribute(SparkCluster.CLUSTER_MEMBER)) {
            if (entity.getAttribute(SparkCluster.FIRST_MEMBER)) {

                // `hostname` reported on the box is not reliable, eg in SL, so force it in /etc/hosts
                ScriptHelper internalHostScriptFull = newScript("getInternalHostnameFull")
                    .body.append("hostname -f").gatherOutput(true);
                internalHostScriptFull.execute();
                String internalHostnameFull = internalHostScriptFull.getResultStdout().split("\\n")[0];
                entity.setAttribute(SparkNode.MASTER_FULL_HOSTNAME, internalHostnameFull);

                ScriptHelper internalHostScriptShort = newScript("getInternalHostnameShort")
                    .body.append("hostname").gatherOutput(true);
                internalHostScriptShort.execute();
                String internalHostnameShort = internalHostScriptShort.getResultStdout().split("\\n")[0];
                entity.setAttribute(SparkNode.MASTER_SHORT_HOSTNAME, internalHostnameShort);

                String sparkConnectionUrl = format("spark://%s:%s", entity.getAttribute(Attributes.SUBNET_ADDRESS), entity.getAttribute(SparkNode.SPARK_MASTER_SERVICE_PORT));
                entity.setAttribute(SparkNode.MASTER_CONNECTION_URL, sparkConnectionUrl);

                newScript(LAUNCHING)
                        .body.append(format("export SPARK_MASTER_IP=%s ; %s/sbin/start-master.sh", entity.getAttribute(Attributes.SUBNET_ADDRESS), sparkHome))
                        .execute();

                //give time for master to start
                Time.sleep(Duration.THIRTY_SECONDS);
                entity.setAttribute(SparkNode.IS_MASTER_INITIALIZED, Boolean.TRUE);
                ((EntityInternal) entity.getAttribute(SparkCluster.CLUSTER)).setAttribute(SparkCluster.MASTER_SPARK_NODE, (SparkNode) entity);
                ((EntityInternal) entity.getAttribute(SparkCluster.CLUSTER)).setAttribute(SparkCluster.MASTER_NODE_CONNECTION_URL, sparkConnectionUrl);

                entity.setAttribute(Attributes.MAIN_URI, URI.create("http://"+entity.getAttribute(Attributes.HOSTNAME)+":8080/"));
                ((EntityInternal) entity.getAttribute(SparkCluster.CLUSTER)).setAttribute(Attributes.MAIN_URI, URI.create("http://"+entity.getAttribute(Attributes.HOSTNAME)+":8080/"));

                entity.setDisplayName(format("Spark Master Node:%s", entity.getId()));
            } else {
                //wait for the master to be initialized before joining the cluster
                entity.setAttribute(SparkNode.IS_MASTER, Boolean.FALSE);

                Entity masterNode = DependentConfiguration.waitInTaskForAttributeReady(entity.getAttribute(SparkCluster.CLUSTER), SparkCluster.MASTER_SPARK_NODE, Predicates.notNull());

                Entities.waitForServiceUp(masterNode, Duration.ONE_HOUR);

                entity.setAttribute(SparkNode.IS_MASTER_INITIALIZED, Boolean.TRUE);
                Long workerInstanceId = entity.getAttribute(SparkCluster.CLUSTER).getAttribute(SparkCluster.SPARK_WORKER_INSTANCE_ID_TRACKER).getAndIncrement();

                ScriptHelper internalHostScriptFull = newScript("getInternalHostnameFull")
                    .body.append("hostname -f").gatherOutput(true);
                internalHostScriptFull.execute();
                String internalHostnameFull = internalHostScriptFull.getResultStdout().split("\\n")[0];

                ScriptHelper internalHostScriptShort = newScript("getInternalHostnameShort")
                    .body.append("hostname").gatherOutput(true);
                internalHostScriptShort.execute();
                String internalHostnameShort = internalHostScriptShort.getResultStdout().split("\\n")[0];

                String subnetAddress = entity.getAttribute(Attributes.HOSTNAME);//entity.getAttribute(SparkNode.SUBNET_ADDRESS);

                // update etc hosts on master
                brooklyn.util.task.DynamicTasks.queue(brooklyn.entity.software.SshEffectorTasks.ssh("sudo sh -c '"
                    + "echo "+subnetAddress+" "+internalHostnameFull+" "+internalHostnameShort+" "
                    + ">> /etc/hosts'").machine(brooklyn.location.basic.Locations.findUniqueSshMachineLocation(masterNode.getLocations()).get()));
                // and on worker to know of master
                brooklyn.util.task.DynamicTasks.queue(brooklyn.entity.software.SshEffectorTasks.ssh("sudo sh -c '"
                    + "echo "+masterNode.getAttribute(Attributes.HOSTNAME)+" "+masterNode.getAttribute(SparkNode.MASTER_FULL_HOSTNAME)+" "+masterNode.getAttribute(SparkNode.MASTER_SHORT_HOSTNAME)+" "
                    + ">> /etc/hosts'"));

                newScript(LAUNCHING)
                        .body.append(format("%s/sbin/start-slave.sh %s %s 2>&1 &", sparkHome, workerInstanceId, getMasterConnectionUrl()))
                        .execute();

                if (!Optional.fromNullable(getInstanceIds()).isPresent()) {
                    entity.setAttribute(SparkNode.WORKER_INSTANCE_IDS, Lists.newArrayList(workerInstanceId));

                } else {
                    getInstanceIds().add(workerInstanceId);
                }
                entity.setAttribute(Attributes.MAIN_URI, URI.create("http://"+entity.getAttribute(Attributes.HOSTNAME)+":8081/"));
                entity.setDisplayName(format("Spark Worker Node:%s", entity.getId()));
            }
        }
    }

    @Override
    public boolean isRunning() {
        //no CLI tools to check if the Spark node is running through SSH. See connectSensors() in SparkNodeImpl for http polling for SERVICE_UP.
        return true;
    }

    @Override
    public void stop() {
        List<Long> listOfInstanceIds = getInstanceIds();
        ImmutableList.Builder<String> killCmdsBuilder = ImmutableList.<String>builder();
        if (isMaster()) {
            killCmdsBuilder.add(format("%s/spark-daemon.sh stop org.apache.spark.deploy.master.Master 1", sparkHome));
            //kill worker instances residing on the master node
            if (!listOfInstanceIds.isEmpty()) {
                for (Long id : listOfInstanceIds) {
                    killCmdsBuilder.add(format("kill -9 `cat %s/spark-%s-org.apache.spark.deploy.worker.Worker-%s.pid`", entity.getConfig(SparkNode.SPARK_PID_DIR), getMachine().getUser(), id));
                }
            }
        } else {
            //kill all instances
            if (!listOfInstanceIds.isEmpty()) {
                for (Long id : listOfInstanceIds) {
                    killCmdsBuilder.add(format("kill -9 `cat %s/spark-%s-org.apache.spark.deploy.worker.Worker-%s.pid`", entity.getConfig(SparkNode.SPARK_PID_DIR), getMachine().getUser(), id));
                }
            }
        }
        newScript(STOPPING)
                .body.append(killCmdsBuilder.build())
                .execute();
    }

    @Override
    public void addSparkWorkerInstances(Integer noOfInstances) {

        if (noOfInstances instanceof Integer && noOfInstances > 0 && isMasterInitialized()) {
            ImmutableList.Builder cmdsBuilder = ImmutableList.<String>builder();

            for (int i = 0; i < noOfInstances; i++) {
                Long workerInstanceId = entity.getAttribute(SparkCluster.CLUSTER).getAttribute(SparkCluster.SPARK_WORKER_INSTANCE_ID_TRACKER).getAndIncrement();
                cmdsBuilder.add(format("%s/sbin/start-slave.sh %s %s 2>&1 &", sparkHome, workerInstanceId, getMasterConnectionUrl()));

                if (!Optional.fromNullable(getInstanceIds()).isPresent()) {
                    entity.setAttribute(SparkNode.WORKER_INSTANCE_IDS, Lists.newArrayList(workerInstanceId));

                } else {
                    getInstanceIds().add(workerInstanceId);
                }
            }
            List<String> cmds = cmdsBuilder.build();

            newScript("addSparkWorkerInstances")
                    .body.append(cmds)
                    .execute();
        }
    }

    @Override
    public void startMasterNode() {

        //starts a master node process on the node

        ScriptHelper internalHostScript = newScript("getInternalHostname")
                .body.append("hostname").gatherOutput(true);

        internalHostScript.execute();
        String internalHostname = internalHostScript.getResultStdout().split("\\n")[0];

        String sparkConnectionUrl = format("spark://%s:%s",
                entity.getAttribute(SparkNode.HOSTNAME),
                entity.getAttribute(SparkNode.SPARK_MASTER_SERVICE_PORT));
        entity.setAttribute(SparkNode.MASTER_CONNECTION_URL, sparkConnectionUrl);

        newScript(LAUNCHING)
                .body.append(format("%s/sbin/start-master.sh", sparkHome))
                .execute();

        //give time for master to start
        Time.sleep(Duration.THIRTY_SECONDS);
        entity.setAttribute(SparkNode.IS_MASTER_INITIALIZED, Boolean.TRUE);
        ((EntityInternal) entity.getAttribute(SparkCluster.CLUSTER)).setAttribute(SparkCluster.MASTER_SPARK_NODE, (SparkNode) entity);
        ((EntityInternal) entity.getAttribute(SparkCluster.CLUSTER)).setAttribute(SparkCluster.MASTER_NODE_CONNECTION_URL, sparkConnectionUrl);
        entity.setDisplayName(format("Spark Master Node:%s", entity.getId()));
    }

    @Override
    public String getSparkHome() {
        return Optional.fromNullable(sparkHome).or("");
    }

    private boolean isMaster() {
        return Optional.fromNullable(entity.getAttribute(SparkNode.IS_MASTER)).or(false);
    }

    private String getMasterConnectionUrl() {
        return entity.getAttribute(SparkCluster.CLUSTER).getAttribute(SparkCluster.MASTER_NODE_CONNECTION_URL);
    }

    @Override
    protected String getLogFileLocation() {
        return format("%s/logs/", sparkHome);
    }

    private List<Long> getInstanceIds() {
        return Optional.fromNullable(entity.getAttribute(SparkNode.WORKER_INSTANCE_IDS))
                .or(Lists.<Long>newArrayList());
    }

    private boolean isMasterInitialized() {
        return Optional.fromNullable(entity.getAttribute(SparkNode.IS_MASTER_INITIALIZED)).or(false);
    }
}
