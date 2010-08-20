package org.linkedgeodata.osm.osmosis.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

public class LiveRDFDeltaPluginLoader
	implements PluginLoader
{
	@Override
	public Map<String, TaskManagerFactory> loadTaskFactories()
	{
		Map<String, TaskManagerFactory> result = new HashMap<String, TaskManagerFactory>(
			Collections.singletonMap("liveRDFPluginFactory", new LiveRDFDeltaPluginFactory()));
			
		return result;
	}
}
