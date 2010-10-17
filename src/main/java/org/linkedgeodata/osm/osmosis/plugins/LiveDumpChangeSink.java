package org.linkedgeodata.osm.osmosis.plugins;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import org.linkedgeodata.dao.nodestore.NodePositionDAO;
import org.linkedgeodata.dao.nodestore.RDFNodePositionDAO;
import org.linkedgeodata.scripts.LiveSync;
import org.linkedgeodata.util.IDiff;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;

import com.hp.hpl.jena.rdf.model.Model;

public class LiveDumpChangeSink
	implements ChangeSink
{
	private IUpdateStrategy strategy;
	private int entityCount = 0;
	private OutputStream out;
	
	private int maxEntityCount = 1024;
	
	private NodePositionDAO nodePositionDao;
	
	public LiveDumpChangeSink(IUpdateStrategy strategy, NodePositionDAO nodePositionDao, OutputStream out)
	{
		this.strategy = strategy;
		this.nodePositionDao = nodePositionDao;
		this.out = out;
	}

	public LiveDumpChangeSink(IUpdateStrategy strategy, NodePositionDAO nodePositionDao, OutputStream out, int maxEntityCount)
	{
		this.strategy = strategy;
		this.nodePositionDao = nodePositionDao;
		this.out = out;
		this.maxEntityCount = maxEntityCount;
	}

	private void processBatch()
	{
		strategy.complete();
		
		
		// HACK Here we assume that the strategy can be reused after release
		// Ideas for cleaner solutions might be: strategy factory, reset method,
		// differnt interface for the strategy
		IDiff<Model> mainDiff = strategy.getMainGraphDiff();
		mainDiff.getAdded().write(out, "N-TRIPLE");
		try {
			out.flush();
		
			// Apply the node-diff
			applyNodeDiff(strategy.getNodeDiff());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		strategy.release();
		
		entityCount = 0;
	}

	private void applyNodeDiff(TreeSetDiff<Node> diff)
		throws SQLException
	{
		nodePositionDao.remove(LiveSync.getNodeToPositionMap(diff.getRemoved()).keySet());
		nodePositionDao.updateOrInsert(LiveSync.getNodeToPositionMap(diff.getAdded()));
	}
	
	
	@Override
	public void complete()
	{
		processBatch();
		
	}

	@Override
	public void release()
	{
	}

	@Override
	public void process(ChangeContainer ec)
	{
		strategy.process(ec);
		++entityCount;
		if(entityCount >= maxEntityCount) {
			processBatch();
		}
	}
	
}
