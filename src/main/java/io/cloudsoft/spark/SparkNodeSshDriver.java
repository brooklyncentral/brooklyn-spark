package io.cloudsoft.spark;

import static java.lang.String.format;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityInternal;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.location.OsDetails;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.ssh.BashCommands;

public class SparkNodeSshDriver extends AbstractSoftwareProcessSshDriver implements SparkNodeDriver {


    public SparkNodeSshDriver(final SparkNodeImpl entity, final SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void preInstall() {
        resolver = Entities.newDownloader(this);
        setExpandedInstallDir(getInstallDir());
    }

    @Override
    public void install() {


        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();

        OsDetails osDetails = getMachine().getMachineDetails().getOsDetails();

        String scalaVersion = entity.getConfig(SparkNode.SCALA_VERSION);
        DownloadResolver scalaDownloadResolver = ((EntityInternal) entity).getManagementContext().getEntityDownloadsManager()
                .newDownloader(this, "scala", ImmutableMap.of("addonversion", scalaVersion));

        List<String> scalaModuleUrls = scalaDownloadResolver.getTargets();
        String scalaSaveAs = scalaDownloadResolver.getFilename();

        String scalaDir = scalaDownloadResolver.getUnpackedDirectoryName(scalaSaveAs);
        String sparkDir = resolver.getUnpackedDirectoryName(saveAs);

        List<String> commands = ImmutableList.<String>builder()
                .addAll(BashCommands.commandsToDownloadUrlsAs(scalaModuleUrls, scalaSaveAs))
                .addAll(BashCommands.commandsToDownloadUrlsAs(urls, saveAs))
                .add(BashCommands.INSTALL_TAR)
                .add("tar xzfv " + scalaSaveAs)
                .add("tar xzfv " + saveAs)
                .add(format("export PATH=%s/bin/:$PATH", scalaDir))
                .add(format("export SCALA_HOME=%s", scalaDir))
                .add(format("cd %s", sparkDir))
                .add("sbt/sbt package")
                .build();

        newScript(INSTALLING)
                .body.append(commands)
                .execute();


    }


    @Override
    public void customize() {

    }

    @Override
    public void launch() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void stop() {

    }

}
