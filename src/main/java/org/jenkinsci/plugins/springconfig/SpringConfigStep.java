package org.jenkinsci.plugins.springconfig;

import com.google.common.collect.ImmutableSet;
import hudson.CloseProofOutputStream;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.RemoteOutputStream;
import jenkins.security.MasterToSlaveCallable;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.boot.context.config.ConfigFileApplicationListener.CONFIG_LOCATION_PROPERTY;

@Setter
@Getter
public class SpringConfigStep extends Step implements Serializable {

	private String[] profiles;

	private String location;

	@DataBoundSetter
	public void setProfiles(String[] profiles) {
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
			return ImmutableSet.of(TaskListener.class, FilePath.class);
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
		@SneakyThrows
		protected EnvironmentWrapper run() {
			FilePath ws = getContext().get(FilePath.class);
			TaskListener listener = getContext().get(TaskListener.class);
			assert ws != null;

			Launcher launcher = getContext().get(Launcher.class);
			if (launcher != null) {
				String[] profilesArray = step.getProfiles() != null ? step.getProfiles() : new String[0];
				return launcher.getChannel()
						.call(new Execution(profilesArray, step.getLocation(), listener.getLogger(), ws));
			}
			else {
				return null;
			}

		}

		public static class Execution extends MasterToSlaveCallable<EnvironmentWrapper, RuntimeException> {

			private final String[] profilesArray;

			private final String location;

			private final RemoteOutputStream remoteLogger;

			private final FilePath ws;

			private transient PrintStream localLogger;

			@SneakyThrows
			public Execution(String[] profilesArray, String location, PrintStream logger, FilePath ws) {
				this.profilesArray = profilesArray;
				this.location = location;
				localLogger = logger;
				this.remoteLogger = new RemoteOutputStream(new CloseProofOutputStream(logger));
				this.ws = ws;
			}

			@Override
			@SneakyThrows
			public EnvironmentWrapper call() {

				StandardEnvironment environment = new StandardEnvironment();
				Map<String, Object> configFilesMap = new HashMap();
				if (location != null && !location.equals("")) {
					configFilesMap.put(CONFIG_LOCATION_PROPERTY, ws.getRemote() + "/" + location);
				}
				else {
					configFilesMap.put(CONFIG_LOCATION_PROPERTY, ws.getRemote() + "/");
				}
				MapPropertySource mapPropertySource = new MapPropertySource("configfiles", configFilesMap);
				environment.getPropertySources().addFirst(mapPropertySource);
				SpringApplicationBuilder builder = new SpringApplicationBuilder().profiles(profilesArray)
						.bannerMode(Banner.Mode.OFF).environment(environment)
						// Don't use the default properties in this builder
						.registerShutdownHook(false).logStartupInfo(false).web(WebApplicationType.NONE)
						.sources(EmtpyConfiguration.class);
				builder.run();

				getLogger().print(environment);

				return new EnvironmentWrapper(environment);
			}

			@SneakyThrows
			private PrintStream getLogger() {
				if (localLogger == null) {
					localLogger = new PrintStream(remoteLogger, true, StandardCharsets.UTF_8.name());
				}
				return localLogger;
			}

		}

	}

	@Configuration
	public static class EmtpyConfiguration {

	}

}
