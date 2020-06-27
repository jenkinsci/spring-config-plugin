package org.jenkinsci.plugins.springconfig;

import hudson.model.Api;
import hudson.model.Run;
import jenkins.model.RunAction2;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ExportedBean
public class SpringConfigAction implements RunAction2 {

	private Run run;

	private Map<String, Map<String, Object>> allProperties = new HashMap();

	public void addProperties(String profiles, Map<String, Object> properties) {
		allProperties.put(profiles, properties);
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
		return "/plugin/spring-config/images/spring-framework.png";
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

	public Api getApi() {
		return new Api(this);
	}

	@Exported(visibility = 2)
	public Map<String, Map<String, Object>> getProperties() {
		return allProperties;
	}

	public List<ProfileConfig> getAllProperties() {
		return allProperties.entrySet().stream().map(profileProperties -> {
			Map<String, Object> properties = profileProperties.getValue();
			List<KeyValue> propertiesAsList = properties.entrySet().stream()
					.map(property -> KeyValue.builder().key(property.getKey()).value(property.getValue()).build())
					.collect(Collectors.toList());
			String profiles = profileProperties.getKey();
			return ProfileConfig.builder().profiles(profiles).properties(propertiesAsList).build();
		}).collect(Collectors.toList());
	}

	@Setter
	@Getter
	@Builder
	public static class ProfileConfig {

		private String profiles;

		private List<KeyValue> properties;

	}

	@Setter
	@Getter
	@Builder
	public static class KeyValue {

		private String key;

		private Object value;

	}

}
