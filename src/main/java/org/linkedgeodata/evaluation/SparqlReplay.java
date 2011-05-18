package org.linkedgeodata.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import org.aksw.commons.util.strings.StringUtils;

public class SparqlReplay
{
	public static void main() throws Exception
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/home/raven/Desktop/LGDSparqlValid.txt"))));

		String line;
		while((line = reader.readLine()) != null) {
			System.out.println(StringUtils.decodeUtf8(line));
		}
		
	}
	                    	
}
