package org.linkedgeodata.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil
{
	public static void copy(InputStream in, OutputStream out)
		throws IOException
	{
		copy(in, out, 4096);
	}
	
	public static void copy(InputStream in, OutputStream out, int bufferSize)
		throws IOException
	{
		byte[] buffer = new byte[bufferSize];
		int n;
		while((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
	}
}
