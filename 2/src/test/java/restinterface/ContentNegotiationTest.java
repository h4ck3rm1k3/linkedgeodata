/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package restinterface;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.StreamUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class ContentNegotiationTest
{
	enum RDFFormat
		implements Map.Entry<String, String>
	{
		RDFXML("application/rdf+xml", "RDF/XML"),
		NTRIPLE("text/plain", "N-TRIPLE"),		
		TURTLE("text/turtle", "TURTLE"), // TTL
		N3("text/plain", "N3"),		
		;

		private String key;
		private String value;
		
		RDFFormat(String key, String value)
		{
			this.key = key;
			this.value = value;
		}
		
		@Override
		public String getKey()
		{
			return key;
		}

		@Override
		public String getValue()
		{
			return value;
		}

		@Override
		public String setValue(String value)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String toString()
		{
			return "(" + key + ", " + value + ")";
		}
	}

	public static Model tryLoadURL(String url, RDFFormat format)
		throws MalformedURLException, IOException
	{
		URLConnection c = new URL(url).openConnection();
		c.addRequestProperty("Accept", format.getKey());
		String data = StreamUtil.toString(c.getInputStream());

		Model model = ModelFactory.createDefaultModel();
		model.read(new ByteArrayInputStream(data.getBytes()), "", format.getValue());

		return model;
	}
	

	@Test
	@Ignore
	public void TestRDXML()
		throws Exception
	{
		Model expected = ModelFactory.createDefaultModel();
		expected.read(new FileInputStream(new File("data/test/restinterface/node20958816.n3")), "", "N3");

		for(RDFFormat format : RDFFormat.values()) {
			Model actual = tryLoadURL("http://localhost:7000/data/node20958816", format);
			
			System.out.println("ACTUAL " + format + "\n-------------------------");
			System.out.println(ModelUtil.toString(actual, format.getValue()));
			
			
			Assert.assertTrue(expected.containsAll(actual));
			Assert.assertTrue(actual.containsAll(expected));
		}

		/*
		System.out.println("EXPECTED:\n-------------------------");
		System.out.println(ModelUtil.toString(expected));
		System.out.println("ACTUAL:\n-------------------------");
		System.out.println(ModelUtil.toString(actual));
		 */
		
	}
	
	
}
