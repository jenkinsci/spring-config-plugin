package org.jenkinsci.plugins.springconfig;

import hudson.FilePath;
import jenkins.model.Jenkins;
import lombok.SneakyThrows;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class SpringConfigTest {

	@Rule
	public JenkinsRule r = new JenkinsRule();

	@Test
	public void testReadSpringConfig() throws Exception {
		Jenkins jenkins = r.jenkins;
		WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
		FilePath applicationYaml = jenkins.getWorkspaceFor(p).child("application.yaml");
		applicationYaml.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application.yaml"));
		p.setDefinition(new CpsFlowDefinition("node {print springConfig().a.b.c}", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("nestedvalue", b);
	}

	@Test
	public void testReadSpringConfigInDir() throws Exception {
		Jenkins jenkins = r.jenkins;
		WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
		FilePath applicationGooYaml = jenkins.getWorkspaceFor(p).child("custom-config").child("application-goo.yaml");
		applicationGooYaml
				.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application-goo.yaml"));
		p.setDefinition(
				new CpsFlowDefinition("node{dir('custom-config'){print springConfig(profiles: ['goo']).foo}}", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("gooporfile", b);
	}

	@Test
	public void testReadSpringConfigArrayValue() throws Exception {
		Jenkins jenkins = r.jenkins;
		WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
		FilePath applicationYaml = jenkins.getWorkspaceFor(p).child("application.yaml");
		applicationYaml.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application.yaml"));
		p.setDefinition(new CpsFlowDefinition("node {print springConfig().d.array[0]}", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("a1", b);
	}

	@Test
	public void testReadSpringConfigWithProfile() throws Exception {
		Jenkins jenkins = r.jenkins;
		WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
		FilePath ws = jenkins.getWorkspaceFor(p);
		FilePath applicationYaml = ws.child("application.yaml");
		applicationYaml.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application.yaml"));
		FilePath applicationBarYaml = ws.child("application-bar.yaml");
		applicationBarYaml
				.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application-bar.yaml"));
		p.setDefinition(new CpsFlowDefinition("node {print springConfig(profiles: ['bar']).foo}", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("barporfile", b);
	}

	@Test
	@SneakyThrows
	public void testReadSpringConfigWithLocationAndProfile() throws Exception {
		Jenkins jenkins = r.jenkins;
		WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
		FilePath applicationGooYaml = jenkins.getWorkspaceFor(p).child("custom-config").child("application-goo.yaml");
		applicationGooYaml
				.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application-goo.yaml"));
		p.setDefinition(new CpsFlowDefinition(
				"node{print springConfig(profiles: ['goo'], location : 'custom-config/').foo}", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("gooporfile", b);
	}

	@Test
	@SneakyThrows
	public void testReadSpringConfigWithMultiLocationAndProfile() throws Exception {
		Jenkins jenkins = r.jenkins;
		WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
		FilePath applicationGooYaml = jenkins.getWorkspaceFor(p).child("custom-config").child("application-goo.yaml");
		FilePath applicationGoo2Yaml = jenkins.getWorkspaceFor(p).child("custom-config2")
				.child("application-goo2.yaml");
		applicationGooYaml
				.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application-goo.yaml"));
		applicationGoo2Yaml
				.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application-goo2.yaml"));
		p.setDefinition(new CpsFlowDefinition(
				"node{def config=springConfig(profiles: ['goo','goo2'], location : 'custom-config/,custom-config2/');\n"
						+ "print config.foo;\n" + "print config.bar;\n" + "}",
				true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("gooporfile", b);
		r.assertLogContains("barporfile", b);
	}

	@Test
	public void testReadSpringConfigAsProperties() throws Exception {
		Jenkins jenkins = r.jenkins;
		WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
		FilePath applicationYaml = jenkins.getWorkspaceFor(p).child("application.yaml");
		applicationYaml.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application.yaml"));
		p.setDefinition(new CpsFlowDefinition("node {print springConfig().asProperties()['a.b.c']}", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("nestedvalue", b);
	}

}