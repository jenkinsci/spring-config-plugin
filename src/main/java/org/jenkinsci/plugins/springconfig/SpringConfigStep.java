package org.jenkinsci.plugins.springconfig;

import hudson.Extension;
import hudson.model.TaskListener;
import lombok.Getter;
import lombok.Setter;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;

import static org.springframework.boot.context.config.ConfigFileApplicationListener.CONFIG_LOCATION_PROPERTY;

@Setter
@Getter
public class SpringConfigStep extends Step implements Serializable {

	private String profiles;

	private String location;

	@DataBoundSetter
	public void setProfiles(String profiles) {
		this.profiles = profiles;
	}

	@DataBoundSetter
	public void setLocation(String location) {
		this.location = location;
	}

	@DataBoundConstructor
	public SpringConfigStep() {
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new SpringProfileExecution(context, this);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return Collections.<Class<?>>singleton(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "springConfig";
		}

		@Override
		public String getDisplayName() {
			return "A step to read spring style profile configs";
		}

	}

	public static class SpringProfileExecution extends SynchronousStepExecution<EnvironmentWrapper> {

		private SpringConfigStep step;

		protected SpringProfileExecution(@Nonnull StepContext context, SpringConfigStep step) {
			super(context);
			this.step = step;
		}

		@Override
		protected EnvironmentWrapper run() throws Exception {
			String[] profilesArray = Optional.ofNullable(step.getProfiles()).map(profiles -> profiles.split(","))
					.orElse(new String[0]);
			StandardEnvironment environment = new StandardEnvironment();
			Map<String, Object> configFilesMap = new HashMap();
			if (!StringUtils.isEmpty(step.getLocation())) {
				configFilesMap.put(CONFIG_LOCATION_PROPERTY, step.getLocation());
			}
			MapPropertySource mapPropertySource = new MapPropertySource("configfiles", configFilesMap);
			environment.getPropertySources().addFirst(mapPropertySource);
			SpringApplicationBuilder builder = new SpringApplicationBuilder().profiles(profilesArray)
					.bannerMode(Banner.Mode.OFF).environment(environment)
					// Don't use the default properties in this builder
					.registerShutdownHook(false).logStartupInfo(false).web(WebApplicationType.NONE)
					.sources(EmtpyConfiguration.class);
			builder.run();
			return new EnvironmentWrapper(environment);
		}

	}

	@Configuration
	public static class EmtpyConfiguration {

	}

}
