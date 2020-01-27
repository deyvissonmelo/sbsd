package com.github.spring_batch_smell_detector.report;

import java.util.Set;
import java.util.UUID;

import com.github.spring_batch_smell_detector.model.SmellType;

public interface SmellReport {

	void print(Set<UUID> components, String filePath);
	
}
