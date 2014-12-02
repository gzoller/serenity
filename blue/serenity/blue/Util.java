package serenity.blue;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import java.util.stream.*;
import java.util.regex.*;

import org.w3c.dom.Document;

public class Util {
	
	private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private static Pattern pattern = Pattern.compile("T(\\d+)");
	
//	public static String httpGet( String url ) {
//		BufferedReader in = null;
//		try {
//			URL goGet = new URL(url);
//			in = new BufferedReader(new InputStreamReader(goGet.openStream()));
//			String inputLine;
//			StringBuilder response = new StringBuilder();
//			while ((inputLine = in.readLine()) != null)
//				response.append(inputLine);
//			return response.toString();
//		} catch( Exception e ) {
//			return "";
//		} finally {
//			try { if( in != null ) in.close(); } catch( Exception x ) {}
//		}
//	}
	
	public static TimeTemps parseNWSTemps( String nws ) {
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(nws);
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xpath = xPathFactory.newXPath();

			XPathExpression times = xpath.compile("/dwml/data/time-layout/start-valid-time");
			NodeList nl = (NodeList) times.evaluate(doc, XPathConstants.NODESET);
			Stream.Builder<Node> builder = Stream.builder();
			for( int i=0; i<nl.getLength(); i++ ) { builder.add(nl.item(i)); }
			Stream<Node> s = builder.build();
			Integer[] tempTimes = s.map(n -> {
					Matcher m = pattern.matcher(n.getTextContent());
					m.find();
					return Integer.parseInt( m.group(1) );
				}).toArray(size -> new Integer[size]);

			XPathExpression temps = xpath.compile("/dwml/data/parameters/temperature/value");
			NodeList nl2 = (NodeList) temps.evaluate(doc, XPathConstants.NODESET);
			Stream.Builder<Node> builder2 = Stream.builder();
			for( int i=0; i<nl2.getLength(); i++ ) { builder2.add(nl2.item(i)); }
			Stream<Node> s2 = builder2.build();
			Integer[] tempArr = s2.map(n -> Integer.parseInt( n.getTextContent() ) ).toArray(size -> new Integer[size]);
			return new TimeTemps( tempTimes, tempArr );
		} catch( Exception x ) {
			return new TimeTemps(null,null);
		}
	}
}

/*
<?xml version="1.0" encoding="UTF-8"?>
<dwml xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0" xsi:noNamespaceSchemaLocation="http://www.nws.noaa.gov/forecasts/xml/DWMLgen/schema/DWML.xsd">
   <head>
      <product srsName="WGS 1984" concise-name="time-series" operational-mode="official">
         <title>NOAA's National Weather Service Forecast Data</title>
         <field>meteorological</field>
         <category>forecast</category>
         <creation-date refresh-frequency="PT1H">2014-11-18T00:39:00Z</creation-date>
      </product>
      <source>
         <more-information>http://www.nws.noaa.gov/forecasts/xml/</more-information>
         <production-center>
            Meteorological Development Laboratory
            <sub-center>Product Generation Branch</sub-center>
         </production-center>
         <disclaimer>http://www.nws.noaa.gov/disclaimer.html</disclaimer>
         <credit>http://www.weather.gov/</credit>
         <credit-logo>http://www.weather.gov/images/xml_logo.gif</credit-logo>
         <feedback>http://www.weather.gov/feedback.php</feedback>
      </source>
   </head>
   <data>
      <location>
         <location-key>point1</location-key>
         <point latitude="32.96" longitude="-96.99" />
      </location>
      <moreWeatherInformation applicable-location="point1">http://forecast.weather.gov/MapClick.php?textField1=32.96&amp;textField2=-96.99</moreWeatherInformation>
      <time-layout time-coordinate="local" summarization="none">
         <layout-key>k-p3h-n9-1</layout-key>
         <start-valid-time>2014-11-17T21:00:00-06:00</start-valid-time>
         <start-valid-time>2014-11-18T00:00:00-06:00</start-valid-time>
         <start-valid-time>2014-11-18T03:00:00-06:00</start-valid-time>
         <start-valid-time>2014-11-18T06:00:00-06:00</start-valid-time>
         <start-valid-time>2014-11-18T09:00:00-06:00</start-valid-time>
         <start-valid-time>2014-11-18T12:00:00-06:00</start-valid-time>
         <start-valid-time>2014-11-18T15:00:00-06:00</start-valid-time>
         <start-valid-time>2014-11-18T18:00:00-06:00</start-valid-time>
         <start-valid-time>2014-11-18T21:00:00-06:00</start-valid-time>
      </time-layout>
      <parameters applicable-location="point1">
         <temperature type="hourly" units="Fahrenheit" time-layout="k-p3h-n9-1">
            <name>Temperature</name>
            <value>33</value>
            <value>29</value>
            <value>26</value>
            <value>25</value>
            <value>33</value>
            <value>44</value>
            <value>49</value>
            <value>46</value>
            <value>40</value>
         </temperature>
      </parameters>
   </data>
</dwml>
 */
