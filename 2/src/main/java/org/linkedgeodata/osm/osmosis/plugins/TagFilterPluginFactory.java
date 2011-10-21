package org.linkedgeodata.osm.osmosis.plugins;

import java.io.File;
import java.io.IOException;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.ChangeSinkChangeSourceManager;
import org.openstreetmap.osmosis.core.pipeline.v0_6.ChangeSinkManager;

public class TagFilterPluginFactory
	extends TaskManagerFactory
{
	private static final String ARG_FILE_NAME = "fileName";
	
	//private static final String DEFAULT_GRAPH_NAME = "linkedgeodata.org";

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig)
	{
		String fileName = getStringArgument(taskConfig, ARG_FILE_NAME);
			//getDefaultStringArgument(taskConfig, DEFAULT_OUTPUT_BASE_NAME));

		File file = new File(fileName);
		
		TagFilter tagFilter = null;
		try {
			tagFilter = TagFilter.create(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TagFilterPlugin task = new TagFilterPlugin(tagFilter);

		TaskManager result =
			new ChangeSinkChangeSourceManager(taskConfig.getId(), task, taskConfig.getPipeArgs());

		return result;
	}

}
