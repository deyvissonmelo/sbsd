package com.github.spring_batch_smell_detector.util;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLFileReader {
	
	private XMLFileReader() {}

	public static Document readXmlFile(String filePath) throws ParserConfigurationException, SAXException, IOException {				
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		dBuilder.setEntityResolver(new EntityResolver() {			
			@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				return new InputSource(new StringReader(""));
			}
		});
		
		Document doc = dBuilder.parse(filePath);		
		doc.getDocumentElement().normalize();
		return doc;
	}
	
}
