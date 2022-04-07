package org.jenkinsci.plugins.springconfig;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jenkinsci.plugins.springconfig.SpringProfilesFolderProperty.retrieveSpringProfileFromFolderConfig;
import static org.jenkinsci.plugins.springconfig.SpringProfilesJobProperty.retrieveSpringProfilesFromJobConfig;

@Setter
@Getter
public class SpringProfilesStep extends Step implements Serializable {

	private static final long serialVersionUID = 1L;

	@DataBoundConstructor
	public SpringProfilesStep() {
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new SpringProfileExecution(context, this);
	}

	@Extension
	public static class DescriptorImpl extends StepDescriptor {

		@Override
		public Set<? extends Class<?>> getRequiredContext() {
			return ImmutableSet.of(TaskListener.class);
		}

		@Override
		public String getFunctionName() {
			return "springProfiles";
		}

		@Override
		public String getDisplayName() {
			return "A step to retrieve spring profiles defined in Job";
		}

	}

	public static class SpringProfileExecution extends SynchronousStepExecution<List<String>> {

		private final SpringProfilesStep step;

		protected SpringProfileExecution(@Nonnull StepContext context, SpringProfilesStep step) {
			super(context);
			this.step = step;
		}

		@Override
		@SneakyThrows
		protected List<String> run() {
			Job job = getContext().get(Run.class).getParent();
			List<String> retrieveSpringProfilesFromJobConfig = retrieveSpringProfilesFromJobConfig(job);
			List<String> retrievedSpringProfilesFromSingleFolder = retrieveSpringProfileFromFolderConfig(job);
			return Stream.of(retrievedSpringProfilesFromSingleFolder, retrieveSpringProfilesFromJobConfig)
					.flatMap(List::stream).distinct().collect(Collectors.toList());
		}

	}

}
