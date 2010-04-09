package org.linkedgeodata.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamUtil
{
	public static String toString(InputStream in)
		throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	
		String result = "";
		String line;
		while(null != (line = reader.readLine()))
			result += line + "\n";
	
		return result;
	}
}
