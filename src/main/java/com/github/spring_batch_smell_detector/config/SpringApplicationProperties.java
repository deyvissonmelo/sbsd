package com.github.spring_batch_smell_detector.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SpringApplicationProperties {
	
	private static final String APPLICATION_PROPERTIES = "application.properties";

	private static SpringApplicationProperties instance;
	
	public final Map<String, String> appProperties = new HashMap<>();
	
	private SpringApplicationProperties() throws IOException {
		try(InputStream input = SpringApplicationProperties.class.getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES)){
			Properties props = new Properties();			
			props.load(input);			
			props.forEach((key, value) -> appProperties.put(key.toString(), value.toString()));
		}
	}
	
	public static SpringApplicationProperties getInstance() throws IOException {
		if(instance == null) {
			instance = new SpringApplicationProperties();
		}
		
		return instance;
	}
	
	public String getAppProperty(String key) {
		return appProperties.get(key);
	}		
	
	public List<String> getAppPropertyAsList(String key){
		String prop = getAppProperty(key);		
		return Arrays.asList(prop.split(","));
	}
}
