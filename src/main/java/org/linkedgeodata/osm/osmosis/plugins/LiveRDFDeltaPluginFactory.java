/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.linkedgeodata.osm.osmosis.plugins;

import java.sql.Connection;

import org.apache.log4j.Logger;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.VirtuosoUtils;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.linkedgeodata.util.sparql.VirtuosoJdbcSparulExecutor;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.ChangeSinkManager;

public class LiveRDFDeltaPluginFactory
	extends TaskManagerFactory
{
	private static final Logger logger = Logger.getLogger(LiveRDFDeltaPluginFactory.class);
	
	private static final String ARG_GRAPH_NAME = "graph";
	private static final String ARG_HOST_NAME = "host";
	private static final String ARG_USER_NAME = "user";
	private static final String ARG_PASSWORD = "password";
	
	
	private static final String DEFAULT_GRAPH_NAME = "linkedgeodata.org";
	private static final String DEFAULT_HOST_NAME = "localhost";
	private static final String DEFAULT_USER_NAME = "dba";
	private static final String DEFAULT_PASSWORD = "dba";
	
	public LiveRDFDeltaPluginFactory()
	{
		super();
	}


	private ConnectionConfig extractConnectionConfig(TaskConfiguration taskConfig)
	{
		// FIXME The graph name becomes the database name
		// This is a small hack
		String graphName =
			getStringArgument(taskConfig, ARG_GRAPH_NAME,
			getDefaultStringArgument(taskConfig, DEFAULT_GRAPH_NAME));
		
		String hostName =
			getStringArgument(taskConfig, ARG_HOST_NAME,
			getDefaultStringArgument(taskConfig, DEFAULT_HOST_NAME));
		
		String userName =
			getStringArgument(taskConfig, ARG_USER_NAME,
			getDefaultStringArgument(taskConfig, DEFAULT_USER_NAME));
		
		String passWord =
			getStringArgument(taskConfig, ARG_PASSWORD,
			getDefaultStringArgument(taskConfig, DEFAULT_PASSWORD));
		
		ConnectionConfig result =
			new ConnectionConfig(hostName, graphName, userName, passWord);
		
		return result;
	}
	
	private TaskManager _createTaskManagerImpl(TaskConfiguration taskConfig)
		throws Exception
	{
		ConnectionConfig cConfig = extractConnectionConfig(taskConfig);
				
		Connection conn = VirtuosoUtils.connect(
				cConfig.getHostName(),
				cConfig.getUserName(),
				cConfig.getPassWord());

		ISparulExecutor graphDAO =
			new VirtuosoJdbcSparulExecutor(conn, cConfig.getDataBaseName());


		LiveRDFDeltaPlugin task = new LiveRDFDeltaPlugin(graphDAO, cConfig.getDataBaseName());
		
		TaskManager result =
			new ChangeSinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
		
		return result;
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