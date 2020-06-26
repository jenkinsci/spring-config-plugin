package org.jenkinsci.plugins.springconfig;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class SpringConfigTest {

	@Rule
	public JenkinsRule r = new JenkinsRule();

	@Test
	public void testReadSpringConfigWithoutProfile() throws Exception {
		WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition("print springConfig().getProperty('foo')", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("bar", b);
	}

	@Test
	public void testReadSpringConfigWithProfile() throws Exception {
		WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition("print springConfig(profiles: 'bar').getProperty('foo')", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("barporfile", b);
	}

	@Test
	@SneakyThrows
	public void testReadSpringConfigWithLocationAndProfile() throws Exception {
		File gooProfile = new File("custom-config/application-goo.yaml");
		FileUtils.forceMkdirParent(gooProfile);
		FileUtils.touch(gooProfile);
		try (InputStream gooInputStream = SpringConfigTest.class.getClassLoader().getResourceAsStream("goo.yaml");
				OutputStream gooOutputStream = new FileOutputStream(gooProfile)) {
			IOUtils.copy(gooInputStream, gooOutputStream);
		}
		WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition(
				"print springConfig(profiles: 'goo', location : 'custom-config/').getProperty('foo')", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("gooporfile", b);
	}

	@Test
	public void testReadSpringConfigAsProperties() throws Exception {
		WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition("" + "def config=springConfig() \n" + "print config.properties.foo \n"
				+ "print config.properties['a.b.c']", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("bar", b);
		r.assertLogContains("nestedvalue", b);
	}

	@Test
	public void testReadSpringConfigAsNestedMap() throws Exception {
		WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
		p.setDefinition(new CpsFlowDefinition("print springConfig().nestedMap.a.b.c", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("nestedvalue", b);
	}

}