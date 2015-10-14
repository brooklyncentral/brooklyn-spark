package io.cloudsoft.spark;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects.ToStringHelper;

import org.apache.brooklyn.cli.Main;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.cloudsoft.spark.app.SparkClusteredApp;

/**
 * This class provides a static main entry point for launching a custom Brooklyn-based app.
 * <p>
 * It inherits the standard Brooklyn CLI options from {@link Main}, plus adds a few more shortcuts for
 * favourite blueprints to the {@link LaunchCommand}.
 */
public class SparkMain extends Main {

    private static final Logger log = LoggerFactory.getLogger(SparkMain.class);

    public static void main(String... args) {
        log.debug("CLI invoked with args " + Arrays.asList(args));
        new SparkMain().execCli(args);
    }

    @Override
    protected String cliScriptName() {
        return "start.sh";
    }

    @Override
    protected Class<? extends BrooklynCommand> cliLaunchCommand() {
        return LaunchCommand.class;
    }

    @Command(name = "launch", description = "Starts a brooklyn server, and optionally an application.")
    public static class LaunchCommand extends Main.LaunchCommand {

        @Option(name = {"--spark"}, description = "Launch a clustered Spark (3-nodes) deployment")
        public boolean spark;

        @Override
        public Void call() throws Exception {
            // process our CLI arguments
            if (spark) {
                setAppToLaunch(SparkClusteredApp.class.getCanonicalName());
            }

            // now process the standard launch arguments
            return super.call();
        }

        @Override
        public ToStringHelper string() {
            return super.string()
                    .add("spark", spark);
        }
    }
}
