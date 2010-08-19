package org.linkedgeodata.osm.osmosis.plugins;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

public class LiveRDFDeltaPluginLoader
	implements PluginLoader
{
	public static void main(String[] args) 
	{
		LiveRDFDeltaPluginLoader loader = new LiveRDFDeltaPluginLoader();
		Map<String, TaskManagerFactory> taskFactories = loader.loadTaskFactories();
		
		/*
		for(Map.Entry<String, TaskManagerFactory> entry : taskFactories) {
			entry.getValue().createTaskManager(arg0)
		}*/
	}
	

	@Override
	public Map<String, TaskManagerFactory> loadTaskFactories()
	{
		Map<String, TaskManagerFactory> result = new HashMap<String, TaskManagerFactory>(
			Collections.singletonMap("liveRDFPluginFactory", new LiveRDFDeltaPluginFactory()));
			
		return result;
	}

}
