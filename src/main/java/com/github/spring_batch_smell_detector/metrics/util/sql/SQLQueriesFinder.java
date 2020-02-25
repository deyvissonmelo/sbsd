package com.github.spring_batch_smell_detector.metrics.util.sql;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.spring_batch_smell_detector.util.XMLFileReader;
import com.google.common.base.Strings;

public class SQLQueriesFinder {	
	
	private SQLQueriesFinder() {
	}

	private static SQLQueriesFinder instance;

	private Map<UUID, SQLQuery> queries;

	public static SQLQueriesFinder initialize(String jobFilePath, String alternativeQueryFilePath)
			throws ParserConfigurationException, SAXException, IOException {
		if (instance == null) {
			instance = new SQLQueriesFinder();
			instance.setQueries(new HashMap<>());
			instance.loadSQLQueries(jobFilePath, alternativeQueryFilePath);
		}

		return instance;
	}

	public Map<UUID, SQLQuery> getQueries() {
		return queries;
	}
	
	public void setQueries(Map<UUID, SQLQuery> queries) {
		this.queries = queries;
	}
	
	public Map<UUID, Integer> getSqlQueriesComplexity() {
		Map<UUID, Integer> sqlComplexity = new HashMap<>();
		instance.queries.forEach((id, sqlQuery) -> sqlComplexity.put(id, sqlQuery.getComplexity()));
		return sqlComplexity;
	}

	public Map<UUID, String> getSqlQueries() {		
		Map<UUID, String> sqlQueries = new HashMap<>();
		instance.queries.forEach((id, sqlQuery) -> sqlQueries.put(id, sqlQuery.getQuery()));
		return sqlQueries;
	}	

	private void loadSQLQueries(String jobFilePath, String alternativeQueryFilePath)
			throws ParserConfigurationException, SAXException, IOException {		
		if (!Strings.isNullOrEmpty(alternativeQueryFilePath)) {
			loadAlternativeFileQueries(alternativeQueryFilePath);
		}

		if (!Strings.isNullOrEmpty(jobFilePath)) {
			loadJobFile(jobFilePath);
		}
	}

	private void loadJobFile(String jobFilePath)
			throws ParserConfigurationException, SAXException, IOException {				
		Document doc = XMLFileReader.readXmlFile(jobFilePath);

		NodeList elements = doc.getElementsByTagName("bean");

		for (int i = 0; i < elements.getLength(); i++) {
			Node idNode = elements.item(i).getAttributes().getNamedItem("id");

			if (idNode == null) {
				continue;
			}

			String key = idNode.getNodeValue();

			for (int j = 0; j < elements.item(i).getChildNodes().getLength(); j++) {
				Node node = elements.item(i).getChildNodes().item(j);

				if (node.getAttributes() == null) {
					continue;
				}

				Node propAttribute = node.getAttributes().getNamedItem("name");

				if (propAttribute == null || !propAttribute.getNodeValue().equals("sql")) {
					continue;
				}

				Node sqlAttribute = node.getAttributes().getNamedItem("value");
				addQuery(key, sqlAttribute.getNodeValue(), SQLQueryFileType.JOB_FILE, jobFilePath);					
			}
		}
	}

	private void loadAlternativeFileQueries(String alternativeQueryFilePath)
			throws ParserConfigurationException, SAXException, IOException {
		Document doc = XMLFileReader.readXmlFile(alternativeQueryFilePath);

		NodeList elements = doc.getElementsByTagName("entry");

		for (int i = 0; i < elements.getLength(); i++) {
			String key = elements.item(i).getAttributes().getNamedItem("key").getNodeValue();
			String value = "";

			for (int j = 0; j < elements.item(i).getChildNodes().getLength(); j++) {
				String tempValue = ((CharacterData) elements.item(i).getChildNodes().item(j)).getData();

				if (!Strings.isNullOrEmpty(tempValue.trim())) {
					value = tempValue;
				}
			}
			
			addQuery(key, value, SQLQueryFileType.ALTERNATIVE_FILE, alternativeQueryFilePath);					
		}
	}

	public static SQLQueriesFinder getLoadedInstance() {
		if(instance == null) {
			throw new RuntimeException("SQLQueriesFinder não foi inicializado.");
		}
		
		return instance;
	}

	public UUID addQuery(String key, String query, SQLQueryFileType fileType, String filePath) {
		SQLQuery sqlQuery = new SQLQuery(UUID.randomUUID(), key, fileType, filePath);
		sqlQuery.setQuery(query);	
		queries.put(sqlQuery.getId(), sqlQuery);
		
		return sqlQuery.getId();
	}

	public Optional<SQLQuery> findByFileKey(String fileKey) {		
		return queries.values().stream().filter(sqlQuery -> sqlQuery.getFileKey().equals(fileKey)).findFirst();
	}
	
	public List<SQLQuery> getByFileType(SQLQueryFileType fileType){
		return queries.values().stream().filter(q -> q.getFileType().equals(fileType)).collect(Collectors.toList());
	}
	
}
