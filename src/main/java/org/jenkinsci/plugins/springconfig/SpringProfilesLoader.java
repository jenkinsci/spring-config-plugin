package org.jenkinsci.plugins.springconfig;

import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log
public class SpringProfilesLoader {

	static List<String> retrieveSpringProfilesFromStore(@NonNull SpringProfilesStore store) {
		return Optional.ofNullable(store.getSpringProfiles()).filter(profile -> !Strings.isNullOrEmpty(profile))
				.map(profile -> Arrays.stream(profile.split(",")).collect(Collectors.toList()))
				.orElseGet(Collections::emptyList);
	}

}
