package com.github.spring_batch_smell_detector.metrics.metric;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.StringLiteral;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.metric.ClassLevelMetric;
import com.github.mauricioaniche.ck.metric.MethodLevelMetric;
import com.github.spring_batch_smell_detector.metrics.CKClassResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.CKMethodResultSpringBatch;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueriesFinder;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQuery;
import com.github.spring_batch_smell_detector.metrics.util.sql.SQLQueryFileType;

public class SQLComplexity extends ASTVisitor implements ClassLevelMetric, MethodLevelMetric {

	Set<UUID> sqlQueries = new HashSet<>();	
	List<Integer> sqlComplexities = new ArrayList<>();

	@Override
	public boolean visit(StringLiteral node) {
		SQLQueriesFinder sqlFinder = SQLQueriesFinder.getLoadedInstance();
		
		String varValue = node.getEscapedValue().replace("\"", "");
		Optional<SQLQuery> sqlQuery = sqlFinder.findByFileKey(varValue);				
		
		if (sqlQuery.isPresent()) {
			sqlQueries.add(sqlQuery.get().getId());
			sqlComplexities.add(sqlQuery.get().getComplexity());
		}
		else {
			int complexity = SQLQuery.calculateSQlComplexity(varValue);
			
			if(complexity > 0) {				
				String id = "String.Literal$" + UUID.randomUUID();
				sqlQueries.add(sqlFinder.addQuery(id, varValue, SQLQueryFileType.STRING_LITERAL));
				sqlComplexities.add(complexity);
			}
		}

		return true;
	}

	@Override
	public void setResult(CKClassResult result) {
		if (result instanceof CKClassResultSpringBatch) {
			((CKClassResultSpringBatch) result).setSqlQueries(sqlQueries);						
			((CKClassResultSpringBatch) result).setMaxSqlComplexity(calculateSQLComplexity());
		}
	}

	private int calculateSQLComplexity() {				
		OptionalInt max = sqlComplexities.stream().mapToInt(value -> value).max();		
		return max.isPresent() ? max.getAsInt() : 0;
	}

	@Override
	public void setResult(CKMethodResult result) {
		if (result instanceof CKMethodResultSpringBatch) {
			((CKMethodResultSpringBatch) result).setSqlQueries(sqlQueries);
			((CKMethodResultSpringBatch) result).setMaxSqlComplexity(calculateSQLComplexity());
		}
	}

}
