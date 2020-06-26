package org.jenkinsci.plugins.springconfig;

import hudson.PluginWrapper;
import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpringConfigAction implements RunAction2 {

	Run run;

	private List<Map<String, Object>> propertiesList = new ArrayList();

	public void addProperties(Map<String, Object> properties) {
		propertiesList.add(properties);
	}

	@Override
	public void onAttached(Run<?, ?> r) {
		this.run = r;
	}

	@Override
	public void onLoad(Run<?, ?> r) {
		this.run = r;
	}

	@CheckForNull
	@Override
	public String getIconFileName() {
		PluginWrapper wrapper = Jenkins.get().getPluginManager().getPlugin(SpringConfigPlugin.class);
		return "/plugin/" + wrapper.getShortName() + "/images/spring-framework.png";
	}

	@CheckForNull
	@Override
	public String getDisplayName() {
		return "Spring Config";
	}

	@CheckForNull
	@Override
	public String getUrlName() {
		return "springconfig";
	}

	public List<List<KeyValue>> getAllProperties() {
		return propertiesList.stream()
				.map(properties -> properties.entrySet().stream()
						.map(property -> KeyValue.builder().key(property.getKey()).value(property.getValue()).build())
						.collect(Collectors.toList()))
				.collect(Collectors.toList());
	}

	@Setter
	@Getter
	@Builder
	public static class KeyValue {

		private String key;

		private Object value;

	}

}
