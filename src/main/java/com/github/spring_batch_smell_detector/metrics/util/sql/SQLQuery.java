package com.github.spring_batch_smell_detector.metrics.util.sql;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.base.Strings;

public class SQLQuery {

	private static final String[] SQL_KEYWORDS = { " AND ", "BETWEEN ", "CASE ", "DELETE ", " DESC", " DISTINCT ",
			"EXISTS ", "FROM ", "FULL ", "GROUP ", "HAVING ", " IN ", "INNER ", "INSERT ", " INTO ", " NULL", "NOT ",
			"JOIN ", "LEFT ", "LIKE ", "LIMIT ", " OR ", "ORDER ", "OUTER ", "RIGTH ", "ROWNUM ", "SELECT ", "TOP ",
			"UNION", "UPDATE ", "VALUES ", "WHERE ", " = ", " > ", " < " };

	private static final String[] SQL_WRITER_KEYWORDS = { "INSERT", "UPDATE", "DELETE" };

	private UUID id;

	private String fileKey;

	private String query;

	private SQLQueryType type;

	private SQLQueryFileType fileType;

	private String filePath;

	private int complexity;

	public SQLQuery(UUID id, String fileKey, SQLQueryFileType fileType, String filePath) {
		this.id = id;
		this.fileKey = fileKey;
		this.fileType = fileType;
		this.filePath = filePath;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getFileKey() {
		return fileKey;
	}

	public void setFileKey(String fileKey) {
		this.fileKey = fileKey;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
		this.complexity = calculateSQlComplexity(query);
		this.type = getQueryType(query);
	}

	public SQLQueryType getType() {
		return type;
	}

	public SQLQueryFileType getFileType() {
		return fileType;
	}

	public int getComplexity() {
		return complexity;
	}

	public String getFilePath() {
		return filePath;
	}

	public static Integer calculateSQlComplexity(String value) {
		if (Strings.isNullOrEmpty(value))
			return 0;

		Integer sqlComplexity = 0;

		for (String keyword : SQL_KEYWORDS) {
			Pattern pattern = Pattern.compile(keyword);
			Matcher matcher = pattern.matcher(value.toUpperCase());

			while (matcher.find()) {
				sqlComplexity++;
			}
		}

		return sqlComplexity;
	}

	public static SQLQueryType getQueryType(String query) {
		if (Stream.of(SQL_WRITER_KEYWORDS).anyMatch(sqlKeyword -> query.contains(sqlKeyword))) {
			return SQLQueryType.WRITE_SQL;
		} else {
			return SQLQueryType.READ_SQL;
		}
	}

	public static boolean isSQLValid(String query) {
		Pattern selectPattern = Pattern.compile("(SELECT|select)\\s.*\\s(FROM|from)\\s.*");
		Pattern insertPattern = Pattern.compile("(INSERT|insert)\\s(INTO|into).*\\s(VALUES|values)\\s.*");
		Pattern updatePattern = Pattern.compile("(UPDATE|update)\\s.*\\s(SET|set).*");
		Pattern deletePattern = Pattern.compile("(DELETE|delete)\\s(FROM|from).*");

		Matcher selectMatcher = selectPattern.matcher(query);
		Matcher insertMatcher = insertPattern.matcher(query);
		Matcher updateMatcher = updatePattern.matcher(query);
		Matcher deleteMatcher = deletePattern.matcher(query);

		return selectMatcher.find() || insertMatcher.find() || updateMatcher.find() || deleteMatcher.find();

	}
}
