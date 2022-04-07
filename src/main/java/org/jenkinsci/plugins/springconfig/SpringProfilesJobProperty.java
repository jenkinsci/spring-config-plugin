package org.jenkinsci.plugins.springconfig;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class SpringProfilesJobProperty extends JobProperty<Job<?, ?>> implements SpringProfilesStore {

	@Getter
	@Setter
	@DataBoundSetter
	private String springProfiles = "";

	/**
	 * Constructor.
	 */
	@DataBoundConstructor
	public SpringProfilesJobProperty(String springProfiles) {
		this.springProfiles = springProfiles;
		log.log(Level.FINER, "Instantiating new SpringProfilesJobProperty\n");
	}

	@Override
	public SpringProfilesJobProperty reconfigure(StaplerRequest request, JSONObject formData) {
		if (formData == null) {
			return null;
		}
		springProfiles = formData.getString("springProfiles");
		return this;
	}

	/**
	 * Descriptor class.
	 */
	@Extension(ordinal = -1000)
	public static class DescriptorImpl extends JobPropertyDescriptor {

		@Nonnull
		@Override
		public String getDisplayName() {
			// return Messages.display_spring_profiles();
			return "z";
		}

	}

	static List<String> retrieveSpringProfilesFromJobConfig(Job job) {
		return Optional.ofNullable(job).map(thisJob -> thisJob.getProperty(SpringProfilesJobProperty.class))
				.map(SpringProfilesJobProperty.class::cast).map(SpringProfilesLoader::retrieveSpringProfilesFromStore)
				.orElseGet(Collections::emptyList);

	}

}
