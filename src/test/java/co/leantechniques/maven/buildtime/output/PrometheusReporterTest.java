package co.leantechniques.maven.buildtime.output;

import co.leantechniques.maven.buildtime.Constants;
import co.leantechniques.maven.buildtime.MojoTimer;
import co.leantechniques.maven.buildtime.SessionTimer;
import co.leantechniques.maven.buildtime.TimerVisitor;
import co.leantechniques.maven.buildtime.output.PrometheusReporter.PrometheusReporterVisitor;
import io.prometheus.client.CollectorRegistry;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.Properties;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PrometheusReporterTest {

    @Mock
    Logger logger;

    @Mock
    CollectorRegistry registry;

    @Mock
    MojoTimer mojoTimer;

    @Mock
    ExecutionEvent event;

    @Mock
    SessionTimer session;

    @Mock
    MavenSession mvnSession;

    @Mock
    MavenProject mavenProject;

    @Test
    public void testVisit() {
        PrometheusReporterVisitor visitor = new PrometheusReporterVisitor(logger, registry, "mytestbuilder");
        when(mojoTimer.getName()).thenReturn("name");
        when(mojoTimer.getProjectName()).thenReturn("projectName");
        visitor.visit(mojoTimer);
        verify(mojoTimer, atLeastOnce()).getName();
        verify(mojoTimer, atLeastOnce()).getProjectName();
        verify(mojoTimer, atLeastOnce()).getDuration();
    }

    @Test
    public void testPerformReportPromDisabled() {
        PrometheusReporter reporter = new PrometheusReporter();
        setBasicMocks();
        reporter.performReport(logger, event, session);
        verify(session, times(0)).accept(any(TimerVisitor.class));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPerformReportPromEnabledPushGatewayNotConfigured() {
        PrometheusReporter reporter = new PrometheusReporter();
        setBasicMocks();
        Properties props = new Properties();
        props.put(Constants.BUILDTIME_OUTPUT_PROM_PROPERTY, "true");
        when(mavenProject.getProperties()).thenReturn(props);
        reporter.performReport(logger, event, session);
        verify(session, atLeastOnce()).accept(any(TimerVisitor.class));
    }

    private void setBasicMocks() {
        when(event.getSession()).thenReturn(mvnSession);
        when(mvnSession.getUserProperties()).thenReturn(new Properties());
        when(mvnSession.getSystemProperties()).thenReturn(new Properties());
        when(mvnSession.getTopLevelProject()).thenReturn(mavenProject);
        when(mavenProject.getProperties()).thenReturn(new Properties());
    }
}