package com.github.spring_batch_smell_detector.smells;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;

public interface SmellDetector {

	Set<UUID> analyse(Map<UUID, CKClassResultSpringBatch> results);
	
}
