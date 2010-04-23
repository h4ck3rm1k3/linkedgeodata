package org.linkedgeodata.jtriplify;

import java.net.URI;
import java.net.URL;
import java.util.Random;

import org.apache.commons.lang.time.StopWatch;
import org.linkedgeodata.util.StreamUtil;


public class Benchmark
{
	public static void main(String[] args)
		throws Exception
	{
		//String prefix = "http://localhost:7000/triplify/way/";
		//String prefix = "http://localhost:80/triplify/way/";

		
		//String prefix = "http://localhost:7000/triplify/node/";
		String prefix = "http://localhost:80/triplify/node/";
		
		
		//int maxNodeCount = 41101455
		//long maxNodeId = 699000559
		//int minNodeId = 104936;
		
		int offset = 104936;
		//int offset = 24948370;
		
		for(int i = 0; i < 1000; ++i) {
			Random r = new Random();
			long id = offset + r.nextInt(1000000);

			URL url = new URL(prefix + id);
			
			StopWatch sw = new StopWatch();
			sw.start();
			
			String str = StreamUtil.toString(url.openStream());
			
			sw.stop();
			
			System.out.println("Time taken: " + sw.getTime());
			
			sw.reset();
		}
	}
}
