package io.cloudsoft.spark;

import static java.lang.String.format;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import brooklyn.entity.Entity;
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
    private boolean isMasterSet;

    public SparkNodeSshDriver(final SparkNodeImpl entity, final SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void preInstall() {

        if (entity.getAttribute(SparkCluster.FIRST_MEMBER)) {
            entity.setAttribute(SparkNode.IS_MASTER, Boolean.TRUE);
        } else {
            entity.setAttribute(SparkNode.IS_MASTER, Boolean.FALSE);
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

        if (isMaster()) {
            String sparkEnvMasterTemplate = processTemplate(entity.getConfig(SparkNode.SPARK_ENV_MASTER_TEMPLATE_URL));
            DynamicTasks.queueIfPossible(SshEffectorTasks.put(saveAsSparkEnv).contents(sparkEnvMasterTemplate));
        } else {
            String sparkEnvWorkerTemplate = processTemplate(entity.getConfig(SparkNode.SPARK_ENV_WORKER_TEMPLATE_URL));
            DynamicTasks.queueIfPossible(SshEffectorTasks.put(saveAsSparkEnv).contents(sparkEnvWorkerTemplate));
        }
    }

    @Override
    public void launch() {

        //The set the launch if the entity is part of a spark cluster, otherwise do not launch.
        if (entity.getAttribute(SparkCluster.CLUSTER_MEMBER)) {
            if (entity.getAttribute(SparkCluster.FIRST_MEMBER)) {

                entity.getAttribute(SparkNode.SUBNET_HOSTNAME);
                ScriptHelper internalHostScript = newScript("getInternalHostname")
                        .body.append("hostname").gatherOutput(true);

                internalHostScript.execute();
                String internalHostname = internalHostScript.getResultStdout().split("\\n")[0];

                String sparkConnectionUrl = format("spark://%s:%s", internalHostname, entity.getAttribute(SparkNode.SPARK_MASTER_SERVICE_PORT));
                entity.setAttribute(SparkNode.MASTER_CONNECTION_URL, sparkConnectionUrl);

                newScript(LAUNCHING)
                        .body.append(format("%s/sbin/start-master.sh", sparkHome))
                        .execute();

                //give time for master to start
                Time.sleep(Duration.THIRTY_SECONDS);
                entity.setAttribute(SparkNode.IS_MASTER_INITIALIZED, Boolean.TRUE);
                ((EntityInternal) entity.getAttribute(SparkCluster.CLUSTER)).setAttribute(SparkCluster.MASTER_SPARK_NODE, (SparkNode) entity);
                ((EntityInternal) entity.getAttribute(SparkCluster.CLUSTER)).setAttribute(SparkCluster.MASTER_NODE_CONNECTION_URL, sparkConnectionUrl);
            } else {
                //wait for the master to be initialized before joining the cluster
                entity.setAttribute(SparkNode.IS_MASTER, Boolean.FALSE);

                Entity masterNode = DependentConfiguration.waitInTaskForAttributeReady(entity.getAttribute(SparkCluster.CLUSTER), SparkCluster.MASTER_SPARK_NODE, Predicates.notNull());

                Entities.waitForServiceUp(masterNode, Duration.ONE_HOUR);

                newScript(LAUNCHING)
                        .body.append(format("%s/bin/spark-class org.apache.spark.deploy.worker.Worker %s 2>&1 &", sparkHome, getMasterConnectionUrl()))
                        .execute();
            }
        }
    }

    @Override
    public boolean isRunning() {

//        return newScript(CHECK_RUNNING)
//                .body.append(format("%s/bin/run-example SparkPi 10", sparkHome))
//                .execute() == 0;
        return true;
    }

    @Override
    public void stop() {
        if (isMaster()) {
            newScript(STOPPING)
                    .body.append(format("%s/spark-daemon.sh stop org.apache.spark.deploy.master.Master 1", sparkHome))
                    .execute();
        } else {
            newScript(STOPPING)
                    .body.append(format("%s/spark-daemon.sh stop org.apache.spark.deploy.master.Master 1", sparkHome))
                    .execute();

        }
    }

    @Override
    public void joinSparkCluster(String masterNodeConnectionUrl) {
        if (!isMaster()) {
            newScript("joinSparkCluster")
                    .body.append(format("%s/bin/spark-class org.apache.spark.deploy.worker.Worker %s", sparkHome, masterNodeConnectionUrl))
                    .execute();
        }
    }

    @Override
    public void startMasterNode() {
        if (isMaster()) {
            entity.getAttribute(SparkNode.SUBNET_HOSTNAME);
            ScriptHelper internalHostScript = newScript("getInternalHostname")
                    .body.append("hostname").gatherOutput(true);

            internalHostScript.execute();
            String internalHostname = internalHostScript.getResultStdout().split("\\n")[0];

            String sparkConnectionUrl = format("spark://%s:%s", internalHostname, entity.getAttribute(SparkNode.SPARK_MASTER_SERVICE_PORT));
            entity.setAttribute(SparkNode.MASTER_CONNECTION_URL, sparkConnectionUrl);
            ((EntityInternal) entity.getAttribute(SparkCluster.CLUSTER)).setAttribute(SparkCluster.MASTER_NODE_CONNECTION_URL, sparkConnectionUrl);

            newScript("startMasterNode")
                    .body.append(format("%s/sbin/start-master.sh", sparkHome))
                    .execute();

            //give time for master to start
            Time.sleep(Duration.THIRTY_SECONDS);
            ((EntityInternal) entity).setAttribute(SparkNode.IS_MASTER_INITIALIZED, Boolean.TRUE);
        }
    }

    @Override
    public void submitSparkApp(String appName) {
        if (isMaster() && entity.getAttribute(SparkCluster.CLUSTER).getAttribute(SparkCluster.SERVICE_UP)) {
            newScript("submitJar")
                    .body.append()
                    .execute();
        }
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
        return sparkHome;
    }
}