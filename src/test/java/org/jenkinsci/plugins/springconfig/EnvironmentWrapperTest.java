package org.jenkinsci.plugins.springconfig;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class EnvironmentWrapperTest {

	private StandardEnvironment standardEnvironment;

	@Before
	public void setUp() {
		Map map1 = new HashMap();
		map1.put("key", OriginTrackedValue.of("value1", null));
		map1.put("in.nested.key", OriginTrackedValue.of("value2", null));
		map1.put("arraykey[0]", OriginTrackedValue.of("value3", null));
		map1.put("arraykey[1]", OriginTrackedValue.of("value4", null));
		map1.put("noascii", OriginTrackedValue.of("\u4E2D\u6587", null));
		OriginTrackedMapPropertySource source1 = new OriginTrackedMapPropertySource("map1", map1);
		MutablePropertySources mutablePropertySources = new MutablePropertySources();
		mutablePropertySources.addLast(source1);
		standardEnvironment = mock(StandardEnvironment.class);
		when(standardEnvironment.getPropertySources()).thenReturn(mutablePropertySources);

	}

	@After
	public void tearDown() {
		reset(standardEnvironment);
	}

	@Test
	public void getProfilesAsString() {
		when(standardEnvironment.getActiveProfiles()).thenReturn(new String[] { "p1", "p2" });
		EnvironmentWrapper env = new EnvironmentWrapper(standardEnvironment);
		assertThat(env.getProfilesAsString()).endsWith("p1,p2");
	}

	@Test
	public void asProperties() {
		when(standardEnvironment.getActiveProfiles()).thenReturn(new String[] { "p1", "p2" });
		EnvironmentWrapper env = new EnvironmentWrapper(standardEnvironment);
		Map<String, Object> properties = env.asProperties();
		assertThat(properties).hasSize(5).containsKeys("key", "in.nested.key", "arraykey[0]", "arraykey[1]", "noascii");
	}

	@Test
	public void asPropertiesFileContent() {
		when(standardEnvironment.getActiveProfiles()).thenReturn(new String[] { "p1", "p2" });
		EnvironmentWrapper env = new EnvironmentWrapper(standardEnvironment);
		assertThat(env.asPropertiesFileContent()).contains("\\u4E2D\\u6587");
	}

	@Test
	public void asMap() {
		when(standardEnvironment.getActiveProfiles()).thenReturn(new String[] { "p1", "p2" });
		Map env = new EnvironmentWrapper(standardEnvironment);
		assertThat(env).hasSize(4);
		assertThat(env.get("arraykey")).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(2).contains("value3",
				"value4");
		assertThat(env.get("in")).asInstanceOf(InstanceOfAssertFactories.MAP).extractingByKey("nested")
				.asInstanceOf(InstanceOfAssertFactories.MAP).extractingByKey("key")
				.asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo("value2");
	}

}