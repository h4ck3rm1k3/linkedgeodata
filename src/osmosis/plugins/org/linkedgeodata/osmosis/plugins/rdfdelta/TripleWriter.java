package org.linkedgeodata.osmosis.plugins.rdfdelta;


import java.io.File;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * Osmosis-task to export data as RDF
 * @author <a href="mailto:CStadler@informatik.uni-leipzig.de">Claus Stadler</a>
 */
public class TripleWriter
	implements Sink
{
	//private TagMapper tagMapper;
	
	
    /**
     * The RDF store to retrieve data from
     */
	
	
	public static void createDirectory(File dir)
	{
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IllegalArgumentException("Cannot create directory "
                        + dir.getAbsolutePath());
            }
        }

        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory "
                    + dir.getAbsolutePath());
        }		
	}

    /**
     * @param aDir the directory with the osmbin-database
     */
    public TripleWriter() {
        //myDataSet = new OsmBinDataSet(aDir);
    }

    @Override
    public void process(final EntityContainer container) {
    	System.out.println(container.getEntity());
    	
    	/*
        Entity entity = aContainer.getEntity();
        if (entity instanceof Node) {
            myDataSet.addNode((Node) entity);
        } else if (entity instanceof Way) {
            myDataSet.addWay((Way) entity);
        } else if (entity instanceof Relation) {
            myDataSet.addRelation((Relation) entity);
        }*/
    }

    @Override
    public void complete() {
    }

    @Override
    public void release() {
        //myDataSet.shutdown();
    }

}
