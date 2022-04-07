package org.jenkinsci.plugins.springconfig;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class SpringProfilesFolderProperty<C extends AbstractFolder<?>> extends AbstractFolderProperty<AbstractFolder<?>>
		implements SpringProfilesStore {

	@Getter
	@Setter
	@DataBoundSetter
	private String springProfiles = "";

	/**
	 * Constructor.
	 */
	@DataBoundConstructor
	public SpringProfilesFolderProperty() {
		log.log(Level.FINER, "Instantiating new SpringProfileFolderProperty\n");
	}

	@Override
	public AbstractFolderProperty<?> reconfigure(StaplerRequest request, JSONObject formData)
			throws Descriptor.FormException {
		if (formData == null) {
			return null;
		}

		springProfiles = formData.getString("springProfiles");
		return this;
	}

	/*
	 * The {@link AbstractFolder} object that owns this property. This value will be set
	 * by the folder. Derived classes can expect this value to be always set.
	 *
	 * protected transient C owner; /* Hook for performing post-initialization action.
	 *
	 * @param owner the owner.
	 *
	 * protected void setPropertyOwner(@NonNull C owner) { this.owner = owner; }
	 */

	/**
	 * Descriptor class.
	 */
	@Extension
	public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

		@Nonnull
		@Override
		public String getDisplayName() {
			return Messages.display_spring_profiles();
		}

	}

	static List<String> retrieveSpringProfileFromFolderConfig(Job job) {
		List<String> springProfiles = new ArrayList<>();
		ItemGroup parent = job.getParent();
		while (parent != null) {
			if (parent instanceof AbstractFolder) {
				List<String> folderProfiles = retrieveSpringProfilesFromSingleFolder((AbstractFolder) parent);
				springProfiles.addAll(0, folderProfiles);
			}
			else if (parent instanceof Jenkins) {
				break;
			}

			if (parent instanceof Item) {
				parent = ((Item) parent).getParent();
			}
			else {
				parent = null;
			}
		}

		return springProfiles;
	}

	static List<String> retrieveSpringProfilesFromSingleFolder(AbstractFolder folder) {
		return Optional.ofNullable(folder)
				.map(thisFolder -> thisFolder.getProperties().get(SpringProfilesFolderProperty.class))
				.map(SpringProfilesFolderProperty.class::cast)
				.map(SpringProfilesLoader::retrieveSpringProfilesFromStore).orElseGet(Collections::emptyList);

	}

}
