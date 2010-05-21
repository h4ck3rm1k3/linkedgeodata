package org.linkedgeodata.util.stats;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

/**
 * A helper class for keeping track of "counts or something" per time.
 * 
 * @author Claus Stadler
 */
public class SimpleStatsTracker
{
	private static final Logger logger = Logger.getLogger(SimpleStatsTracker.class);

	// TODO Make this a weak map
	private static Map<String, SimpleStatsTracker> nameToTracker =
		new HashMap<String, SimpleStatsTracker>();
	
	public static SimpleStatsTracker get(String name)
	{
		SimpleStatsTracker result = nameToTracker.get(name);
		if(result == null) {
			result = new SimpleStatsTracker();
			nameToTracker.put(name, result);
		}
		
		return result;
	}

	
	private StopWatch stopWatch = new StopWatch();
	private float lastTime = 0.0f;
	private long counter = 0;
	boolean isStarted = false;
	
	
	public void update(int delta)
	{
		if(!isStarted)
			start();
		
		counter += delta;
		float time = stopWatch.getTime() / 1000.0f;
		if(time - lastTime > 5.0f) {
			float ratio = counter / time;	
			logger.info("Time: " + time + ", Counter: " + counter + ", ratio = " + ratio);
			lastTime = time;
		}		
	}
	
	public void start()
	{
		stopWatch.start();
		isStarted = true;
	}
}
