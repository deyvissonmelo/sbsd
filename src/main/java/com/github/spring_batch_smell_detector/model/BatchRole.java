package com.github.spring_batch_smell_detector.model;

import java.io.IOException;
import java.util.List;

import com.github.spring_batch_smell_detector.config.SpringApplicationProperties;

public enum BatchRole {
	SERVICE("batch_role_service_classes"), 
	READER("batch_role_reader_classes"), 
	WRITER("batch_role_writer_classes"),
	PROCESSOR("batch_role_processor_classes");

	private String springClass;

	BatchRole(String springClass) {
		this.springClass = springClass;
	}

	public List<String> getSpringClasses() throws IOException {
		SpringApplicationProperties appProps;
		appProps = SpringApplicationProperties.getInstance();
		return appProps.getAppPropertyAsList(springClass);
	}
}
