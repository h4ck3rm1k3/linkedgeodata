package org.linkedgeodata.osm.osmosis.plugins;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.ChangeSinkManager;

public class LiveRDFDeltaPluginFactory
	extends TaskManagerFactory
{
	private static final Logger logger = Logger.getLogger(LiveRDFDeltaPluginFactory.class);
	
	private static final String TEST_ARG_NAME = "test";
	private static final String DEFAULT_TEST_ARG = "foo";
	
	public LiveRDFDeltaPluginFactory()
	{
		super();
		
		System.out.println("Weee: Loading " + this.getClass());
	}

	private TaskManager _createTaskManagerImpl(TaskConfiguration taskConfig)
		throws Exception
	{		
		String fileName = getStringArgument(taskConfig,
				TEST_ARG_NAME,
				 getDefaultStringArgument(taskConfig, DEFAULT_TEST_ARG)
				 ); 

		LiveRDFDeltaPlugin task = new LiveRDFDeltaPlugin(fileName);
		return new ChangeSinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
	}
	
	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
	{
		try {
			return _createTaskManagerImpl(taskConfig);
		}
		catch(Throwable t) {
			logger.fatal("An Error occurred while creating a LinkedGeoData task factory", t);
			throw new RuntimeException(t);
		}
	}
}


/**
2 	*
3 	* /
4 	package org.openstreetmap.osm.data.osmbin.v1_0;
5 	
6 	import java.io.File;
7 	
8 	import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
9 	import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
10 	import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
11 	import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;
12 	
13 	public class OsmBinV10WriterFactory extends TaskManagerFactory {
14 	
15 	
16 	
17 	private static final String ARG_DIR_NAME = "dir";
18 	private static final String DEFAULT_DIR_NAME = "osmbin";
19 	
20 	/**
21 	* {@inheritDoc}
22 	* /
23 	protected TaskManager createTaskManagerImpl(final TaskConfiguration taskConfig) {
24 	String fileName;
25 	File dir;
26 	OsmBinV10Writer task;
27 	
28 	// Get the task arguments.
29 	fileName = getStringArgument(
30 	taskConfig,
31 	ARG_DIR_NAME,
32 	getDefaultStringArgument(taskConfig, DEFAULT_DIR_NAME)
33 	);
34 	
35 	// Create a file object from the file name provided.
36 	dir = new File(fileName);
37 	
38 	// Build the task object.
39 	task = new OsmBinV10Writer(dir);
40 	
41 	return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
42 	}
43 	} 
*/