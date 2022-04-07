package org.jenkinsci.plugins.springconfig;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.google.common.collect.Ordering;
import hudson.FilePath;
import jenkins.model.Jenkins;
import lombok.SneakyThrows;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringConfigActionTest {

	@Rule
	public JenkinsRule r = new JenkinsRule();

	private int buildNumber;

	String jobName = "p";

	@Before
	@SneakyThrows
	public void setUp() {
		Jenkins jenkins = r.jenkins;

		WorkflowJob p = jenkins.createProject(WorkflowJob.class, jobName);
		FilePath ws = jenkins.getWorkspaceFor(p);
		FilePath applicationYaml = ws.child("application.yaml");
		applicationYaml.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application.yaml"));
		FilePath applicationBarYaml = ws.child("application-bar.yaml");
		applicationBarYaml
				.copyFrom(this.getClass().getClassLoader().getResourceAsStream("nodefault/application-bar.yaml"));
		p.setDefinition(new CpsFlowDefinition("node {springConfig(); springConfig(profiles: ['bar'])}", true));
		WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
		buildNumber = b.getNumber();
	}

	@Test
	public void testActionIndex() throws Exception {
		HtmlPage indexPage = r.createWebClient().goTo(String.format("job/%s/%d/springconfig/", jobName, buildNumber));
		DomElement springConfigPanel = indexPage.getElementById("springconfig-panel");
		List<HtmlTable> tables = springConfigPanel.getByXPath("table");
		assertThat(tables).hasSize(2);
		assertThat(tables.get(0).getRowCount()).isEqualTo(7);
		assertThat(tables.get(1).getRowCount()).isEqualTo(7);
		List<String> keys = IntStream.range(1, tables.get(0).getRowCount())
				.mapToObj(i -> tables.get(0).getRow(i).getCell(0).asText()).collect(Collectors.toList());
		assertThat(Ordering.<String>natural().isOrdered(keys)).isTrue();
	}

	@Test
	public void testJson() throws Exception {
		Map jsonObject = r.getJSON(String.format("job/%s/%d/springconfig/api/json", jobName, buildNumber))
				.getJSONObject();
		// @formatter:off
        assertThat(jsonObject)
                .hasSize(2)
                .containsKeys("_class", "properties")
                .extractingByKey("_class")
                .asInstanceOf(InstanceOfAssertFactories.STRING)
                .isEqualTo("org.jenkinsci.plugins.springconfig.SpringConfigAction");
        assertThat(jsonObject.get("properties"))
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .hasSize(2)
                .extractingByKey("")
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .extractingByKey("foo")
                .isEqualTo("bar");
    }

}