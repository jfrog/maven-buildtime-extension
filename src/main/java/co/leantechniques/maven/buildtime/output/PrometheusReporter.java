package co.leantechniques.maven.buildtime.output;

import co.leantechniques.maven.buildtime.*;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;
import org.apache.maven.execution.ExecutionEvent;
import org.slf4j.Logger;

import java.io.IOException;

public class PrometheusReporter implements Reporter {
    public void performReport(Logger logger, ExecutionEvent event, SessionTimer session) {
        if (!Boolean.parseBoolean(MavenHelper.getExecutionProperty(event, Constants.BUILDTIME_OUTPUT_PROM_PROPERTY, "false"))) {
            return;
        }
        CollectorRegistry registry = new CollectorRegistry();
        String builder = MavenHelper.getExecutionProperty(event, Constants.BUILDTIME_OUTPUT_PROM_BUILDER_LABEL_PROPERTY, "");
        session.accept(new PrometheusReporterVisitor(logger, registry, builder));
        logger.debug("Starting prometheus report");
        final String pgAddress = MavenHelper.getExecutionProperty(event, Constants.BUILDTIME_OUTPUT_PROM_PUSH_GATEWAY_PROPERTY, "");
        if (pgAddress.length() == 0) {
            throw new IllegalArgumentException("Push Gateway can not be empty please set " + Constants.BUILDTIME_OUTPUT_PROM_PUSH_GATEWAY_PROPERTY);
        }
        PushGateway pg = new PushGateway(pgAddress);
        try {
            logger.debug("Publishing to Prometheus Push Gateway on: {}", pgAddress);
            pg.pushAdd(registry, "mvn_build_time");
        } catch (IOException e) {
            logger.error("Could not push to Prometheus Gateway", e);
        }
    }


    public static class PrometheusReporterVisitor extends AbstractTimerVisitor {

        private final Logger logger;
        Gauge duration;

        public PrometheusReporterVisitor(Logger logger, CollectorRegistry registry, String builder) {
            this.logger = logger;
            String gaugeName = "mvn_build_time_" + builder;
            duration = Gauge.build().labelNames("module","mojo")
                    .name(gaugeName).help("Duration of mojo in seconds.").register(registry);
        }

        @Override
        public void visit(MojoTimer mojoTimer) {
            logger.trace("Visit {}, {}", mojoTimer.getProjectName(), mojoTimer.getName());
            duration.labels(normalize(mojoTimer.getProjectName()), normalize(mojoTimer.getName())).set(mojoTimer.getDuration() / 1000d);
        }

        private String normalize(String s) {
            return s.replace("(", "_")
                    .replace(")", "")
                    .replace(" ", "_")
                    .replace(":", "_")
                    .replace("-", "_");
        }

    }
}
