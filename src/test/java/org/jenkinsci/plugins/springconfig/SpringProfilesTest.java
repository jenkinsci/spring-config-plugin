package org.jenkinsci.plugins.springconfig;

import com.cloudbees.hudson.plugins.folder.Folder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class SpringProfilesTest {

	@Rule
	public JenkinsRule r = new JenkinsRule();

	@Test
	public void testReadSpringProfileFromFolder() throws Exception {
		Jenkins jenkins = r.jenkins;
		Folder folder = jenkins.createProject(Folder.class, "f1");
		SpringProfilesFolderProperty profiles = new SpringProfilesFolderProperty();
		profiles.setSpringProfiles("profile1,profile2");
		folder.addProperty(profiles);
		WorkflowJob p = folder.createProject(WorkflowJob.class, "p1");
		p.setDefinition(new CpsFlowDefinition("print springProfiles()", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("profile1, profile2", b);
	}

	@Test
	public void testReadSpringProfileFromTopFolder() throws Exception {
		Jenkins jenkins = r.jenkins;

		Folder top = jenkins.createProject(Folder.class, "top");
		SpringProfilesFolderProperty profiles1 = new SpringProfilesFolderProperty();
		profiles1.setSpringProfiles("profileUnix,profileLinux");
		top.addProperty(profiles1);

		Folder folder = top.createProject(Folder.class, "child");
		SpringProfilesFolderProperty profiles2 = new SpringProfilesFolderProperty();
		profiles2.setSpringProfiles("profileProduction");
		folder.addProperty(profiles2);

		WorkflowJob p = folder.createProject(WorkflowJob.class, "p2");
		p.setDefinition(new CpsFlowDefinition("print springProfiles()", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("profileUnix, profileLinux, profileProduction", b);

	}

	@Test
	public void testReadSpringProfileFromProject() throws Exception {
		Jenkins jenkins = r.jenkins;

		WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p2");
		SpringProfilesJobProperty profiles3 = new SpringProfilesJobProperty("profileJob1");
		p.addProperty(profiles3);
		p.setDefinition(new CpsFlowDefinition("print springProfiles()", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("profileJob1", b);

	}

	@Test
	public void testReadSpringProfileFromProjectAndPage() throws Exception {
		Jenkins jenkins = r.jenkins;

		Folder top = jenkins.createProject(Folder.class, "top");
		SpringProfilesFolderProperty profiles1 = new SpringProfilesFolderProperty();
		profiles1.setSpringProfiles("profileUnix,profileLinux");
		top.addProperty(profiles1);

		Folder folder = top.createProject(Folder.class, "child");
		SpringProfilesFolderProperty profiles2 = new SpringProfilesFolderProperty();
		profiles2.setSpringProfiles("profileProduction");
		folder.addProperty(profiles2);

		WorkflowJob p = folder.createProject(WorkflowJob.class, "profileProduction");
		SpringProfilesJobProperty profiles3 = new SpringProfilesJobProperty("profileRegion1");
		p.addProperty(profiles3);

		p.setDefinition(new CpsFlowDefinition("print springProfiles()", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		r.assertLogContains("profileUnix, profileLinux, profileProduction, profileRegion1", b);
	}

}