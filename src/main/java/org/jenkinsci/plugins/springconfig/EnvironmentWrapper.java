package org.jenkinsci.plugins.springconfig;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentWrapper {

	private StandardEnvironment environment;

	public EnvironmentWrapper(StandardEnvironment environment) {
		this.environment = environment;
	}

	@Whitelisted
	public String[] getActiveProfiles() {
		return environment.getActiveProfiles();
	}

	@Whitelisted
	public String[] getDefaultProfiles() {
		return environment.getDefaultProfiles();
	}

	@Whitelisted
	public boolean containsProperty(String key) {
		return environment.containsProperty(key);
	}

	@Nullable
	@Whitelisted
	public String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Whitelisted
	public String getProperty(String key, String defaultValue) {
		return environment.getProperty(key, defaultValue);
	}

	@Nullable
	@Whitelisted
	public <T> T getProperty(String key, Class<T> targetType) {
		return environment.getProperty(key, targetType);
	}

	@Whitelisted
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return environment.getProperty(key, targetType, defaultValue);
	}

	@Whitelisted
	public String getRequiredProperty(String key) throws IllegalStateException {
		return environment.getRequiredProperty(key);
	}

	@Whitelisted
	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		return environment.getRequiredProperty(key, targetType);
	}

	@Whitelisted
	public Map<String, Object> getNestedMap() {
		Map<String, Object> properties = getProperties();
		// The root map which holds all the first level properties
		Map<String, Object> rootMap = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			PropertyNavigator nav = new PropertyNavigator(key);
			nav.setMapValue(rootMap, value);
		}
		return rootMap;
	}

	@Whitelisted
	public Map<String, Object> getProperties() {
		// Map of unique keys containing full map of properties for each unique
		// key
		Map<String, Map<String, Object>> map = new LinkedHashMap<>();
		Map<String, Object> combinedMap = new LinkedHashMap<>();
		environment.getPropertySources().stream().filter(source -> source instanceof OriginTrackedMapPropertySource)
				.forEach(source -> {
					@SuppressWarnings("unchecked")
					Map<String, Object> value = (Map<String, Object>) source.getSource();
					for (String key : value.keySet()) {
						if (!key.contains("[")) {
							// Not an array, add unique key to the map
							combinedMap.put(key, value.get(key));

						}
						else {
							// An existing array might have already been added to the
							// property map
							// of an unequal size to the current array. Replace the array
							// key in
							// the current map.
							key = key.substring(0, key.indexOf("["));
							Map<String, Object> filtered = new LinkedHashMap<>();
							for (String index : value.keySet()) {
								if (index.startsWith(key + "[")) {
									filtered.put(index, value.get(index));
								}
							}
							map.put(key, filtered);
						}
					}
				});
		// Combine all unique keys for array values into the combined map
		for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
			combinedMap.putAll(entry.getValue());
		}
		postProcessProperties(combinedMap);
		return combinedMap;
	}

	private void postProcessProperties(Map<String, Object> propertiesMap) {
		propertiesMap.keySet().removeIf(key -> key.equals("spring.profiles"));
	}

	/**
	 * Class {@code PropertyNavigator} is used to navigate through the property key and
	 * create necessary Maps and Lists making up the nested structure to finally set the
	 * property value at the leaf node.
	 * <p>
	 * The following rules in yml/json are implemented: <pre>
	 * 1. an array element can be:
	 *    - a value (leaf)
	 *    - a map
	 *    - a nested array
	 * 2. a map value can be:
	 *    - a value (leaf)
	 *    - a nested map
	 *    - an array
	 * </pre>
	 */
	private static final class PropertyNavigator {

		private final String propertyKey;

		// Supports keys like org.x and org.x.y like in boot logging
		private String prefix = "";

		private int currentPos;

		private NodeType valueType;

		private PropertyNavigator(String propertyKey) {
			this.propertyKey = propertyKey;
			this.currentPos = -1;
			this.valueType = NodeType.MAP;
		}

		@SuppressWarnings("unchecked")
		private void setMapValue(Map<String, Object> map, Object value) {
			String key = getKey();
			if (NodeType.MAP.equals(this.valueType)) {
				Map<String, Object> nestedMap;
				if (map.get(key) instanceof Map) {
					nestedMap = (Map<String, Object>) map.get(key);
				}
				else if (map.get(key) != null) {
					// not an object, set prefix for later
					prefix = key + ".";
					nestedMap = map;
				}
				else {
					// value of key is null
					nestedMap = new LinkedHashMap<>();
					map.put(key, nestedMap);
				}
				setMapValue(nestedMap, value);
			}
			else if (NodeType.ARRAY.equals(this.valueType)) {
				List<Object> list = (List<Object>) map.get(key);
				if (list == null) {
					list = new ArrayList<>();
					map.put(key, list);
				}
				setListValue(list, value);
			}
			else {
				// use compound prefix
				map.put(prefix + key, value);
			}
		}

		private void setListValue(List<Object> list, Object value) {
			int index = getIndex();
			// Fill missing elements if needed
			while (list.size() <= index) {
				list.add(null);
			}
			if (NodeType.MAP.equals(this.valueType)) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list.get(index);
				if (map == null) {
					map = new LinkedHashMap<>();
					list.set(index, map);
				}
				setMapValue(map, value);
			}
			else if (NodeType.ARRAY.equals(this.valueType)) {
				@SuppressWarnings("unchecked")
				List<Object> nestedList = (List<Object>) list.get(index);
				if (nestedList == null) {
					nestedList = new ArrayList<>();
					list.set(index, nestedList);
				}
				setListValue(nestedList, value);
			}
			else {
				list.set(index, value);
			}
		}

		private int getIndex() {
			// Consider [
			int start = this.currentPos + 1;
			for (int i = start; i < this.propertyKey.length(); i++) {
				char c = this.propertyKey.charAt(i);
				if (c == ']') {
					this.currentPos = i;
					break;
				}
				else if (!Character.isDigit(c)) {
					throw new IllegalArgumentException("Invalid key: " + this.propertyKey);
				}
			}
			// If no closing ] or if '[]'
			if (this.currentPos < start || this.currentPos == start) {
				throw new IllegalArgumentException("Invalid key: " + this.propertyKey);
			}
			else {
				int index = Integer.parseInt(this.propertyKey.substring(start, this.currentPos));
				// Skip the closing ]
				this.currentPos++;
				if (this.currentPos == this.propertyKey.length()) {
					this.valueType = NodeType.LEAF;
				}
				else {
					switch (this.propertyKey.charAt(this.currentPos)) {
					case '.':
						this.valueType = NodeType.MAP;
						break;
					case '[':
						this.valueType = NodeType.ARRAY;
						break;
					default:
						throw new IllegalArgumentException("Invalid key: " + this.propertyKey);
					}
				}
				return index;
			}
		}

		private String getKey() {
			// Consider initial value or previous char '.' or '['
			int start = this.currentPos + 1;
			for (int i = start; i < this.propertyKey.length(); i++) {
				char currentChar = this.propertyKey.charAt(i);
				if (currentChar == '.') {
					this.valueType = NodeType.MAP;
					this.currentPos = i;
					break;
				}
				else if (currentChar == '[') {
					this.valueType = NodeType.ARRAY;
					this.currentPos = i;
					break;
				}
			}
			// If there's no delimiter then it's a key of a leaf
			if (this.currentPos < start) {
				this.currentPos = this.propertyKey.length();
				this.valueType = NodeType.LEAF;
				// Else if we encounter '..' or '.[' or start of the property is . or [
				// then it's invalid
			}
			else if (this.currentPos == start) {
				throw new IllegalArgumentException("Invalid key: " + this.propertyKey);
			}
			return this.propertyKey.substring(start, this.currentPos);
		}

		private enum NodeType {

			LEAF, MAP, ARRAY

		}

	}

}
