package org.linkedgeodata.osmosis.plugins.rdfdelta;

/**
*
*/

import java.io.File;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;

public class TripleWriterFactory extends TaskManagerFactory  {



   private static final String ARG_DIR_NAME = "dir";
   private static final String DEFAULT_DIR_NAME = "osmbin";
   //private static final String DEFAULT_DIR_NAME = "osmbin";

   /**
    * {@inheritDoc}
    */
   protected TaskManager createTaskManagerImpl(final TaskConfiguration taskConfig) {
       String fileName;
       File dir;
       TripleWriter task;

       // Get the task arguments.
       fileName = getStringArgument(
           taskConfig,
           ARG_DIR_NAME,
           getDefaultStringArgument(taskConfig, DEFAULT_DIR_NAME)
       );


       // Build the task object.
       task = new TripleWriter();

       return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
   }
}