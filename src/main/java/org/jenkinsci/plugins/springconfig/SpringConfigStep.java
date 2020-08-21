package org.jenkinsci.plugins.springconfig;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hudson.CloseProofOutputStream;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
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
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.boot.context.config.ConfigFileApplicationListener.CONFIG_LOCATION_PROPERTY;

@Setter
@Getter
public class SpringConfigStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<String> profiles = new ArrayList<String>();

	private String location;

	private boolean hideInBuildPage = false;

	@DataBoundSetter
	public void setProfiles(List<String> profiles) {
		this.profiles = ImmutableList.copyOf(profiles);
	}

	public List<String> getProfiles() {
		return ImmutableList.copyOf(profiles);
	}

	@DataBoundSetter
	public void setLocation(String location) {
		this.location = location;
	}

	@DataBoundSetter
	public void setHideInBuildPage(boolean hideInBuildPage) {
		this.hideInBuildPage = hideInBuildPage;
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
			assert ws != null;
			TaskListener listener = getContext().get(TaskListener.class);
			Run run = getContext().get(Run.class);
			Launcher launcher = getContext().get(Launcher.class);

			EnvironmentWrapper environmentWrapper = Optional.ofNullable(launcher).map(Launcher::getChannel)
					.map(channel -> {
						try {
							return channel.call(
									new Execution(step.getProfiles(), step.getLocation(), listener.getLogger(), ws));
						}
						catch (IOException | RuntimeException | InterruptedException e) {
							return null;
						}
					}).orElse(null);
			if (!step.isHideInBuildPage() && environmentWrapper != null) {
				SpringConfigAction springConfigAction = run.getAction(SpringConfigAction.class);
				if (springConfigAction == null) {
					springConfigAction = new SpringConfigAction();
					run.addAction(springConfigAction);
				}
				springConfigAction.addProperties(environmentWrapper.getProfilesAsString(),
						environmentWrapper.asProperties());
				run.save();
			}
			return environmentWrapper;
		}

		public static class Execution extends MasterToSlaveCallable<EnvironmentWrapper, RuntimeException> {

			private static final long serialVersionUID = 1L;

			private final List<String> profiles;

			private final String location;

			private final RemoteOutputStream remoteLogger;

			private final FilePath ws;

			private transient PrintStream localLogger;

			@SneakyThrows
			public Execution(List<String> profiles, String location, PrintStream logger, FilePath ws) {
				this.profiles = ImmutableList.copyOf(profiles);
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
					String newLocation = Arrays.stream(location.split(",")).map(single -> {
						String remoteLocation = new FilePath(ws, single).getRemote();
						char lastChar = single.charAt(single.length() - 1);
						if (lastChar == '/' || lastChar == '\\') {
							remoteLocation += lastChar;
						}
						return remoteLocation;
					}).collect(Collectors.joining(","));
					configFilesMap.put(CONFIG_LOCATION_PROPERTY, newLocation);
				}
				else {
					configFilesMap.put(CONFIG_LOCATION_PROPERTY, ws.getRemote() + "/");
				}
				MapPropertySource mapPropertySource = new MapPropertySource("configfiles", configFilesMap);
				environment.getPropertySources().addFirst(mapPropertySource);
				SpringApplicationBuilder builder = new SpringApplicationBuilder()
						.profiles(profiles.toArray(new String[0])).bannerMode(Banner.Mode.OFF).environment(environment)
						// Don't use the default properties in this builder
						.registerShutdownHook(false).logStartupInfo(false).web(WebApplicationType.NONE)
						.sources(EmtpyConfiguration.class);
				builder.run().close();

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
