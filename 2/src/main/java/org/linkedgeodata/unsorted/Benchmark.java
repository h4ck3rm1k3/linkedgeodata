package org.linkedgeodata.unsorted;

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
		//String prefix = "http://localhost:80/triplify/node/";

		String prefix = "http://localhost:7000/triplify/near/51.2,13.0/1000";
		//String prefix = "http://localhost:80/triplify/near/51.0,13.0/1000";
		
		
		//int maxNodeCount = 41101455
		//long maxNodeId = 699000559
		//int minNodeId = 104936;
		
		int offset = 104936;
		//int offset = 24948370;
		
		for(int i = 0; i < 50; ++i) {
			Random r = new Random();
			long id = offset + r.nextInt(1000000);

			//URL url = new URL(prefix + id);
			URL url = new URL(prefix);
			
			StopWatch sw = new StopWatch();
			sw.start();
			
			String str = StreamUtil.toString(url.openStream());
			sw.stop();
			
			System.out.println(str);
			System.out.println("Time taken: " + sw.getTime());
			
			sw.reset();
		}
	}
}
