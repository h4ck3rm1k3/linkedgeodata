package org.linkedgeodata.osm.osmosis.plugins;

import java.sql.SQLException;

import org.linkedgeodata.dao.nodestore.NodePositionDAO;
import org.linkedgeodata.scripts.LiveSync;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;

public class LiveDumpChangeSink
	implements ChangeSink
{
	private static final Logger logger = LoggerFactory.getLogger(LiveDumpChangeSink.class);
	
	private IUpdateStrategy strategy;
	private int entityCount = 0;
	
	private int maxEntityCount = 1024;
	
	private String graphName;
	private NodePositionDAO nodePositionDao;
	private ISparulExecutor sparqlEndpoint;
	
	
	private long totalEntityCount = 0;
	
	public LiveDumpChangeSink(IUpdateStrategy strategy, String graphName, ISparulExecutor sparqlEndpoint, NodePositionDAO nodePositionDao)
	{
		this.strategy = strategy;
		this.graphName = graphName;
		this.sparqlEndpoint = sparqlEndpoint;
		this.nodePositionDao = nodePositionDao;
	}

	public LiveDumpChangeSink(IUpdateStrategy strategy, String graphName, ISparulExecutor sparqlEndpoint, NodePositionDAO nodePositionDao, int maxEntityCount)
	{
		this.strategy = strategy;
		this.graphName = graphName;
		this.sparqlEndpoint = sparqlEndpoint;
		this.nodePositionDao = nodePositionDao;
		this.maxEntityCount = maxEntityCount;
	}

	private void processBatch()
	{
		long start = System.nanoTime();
		
		strategy.complete();
		
		
		// HACK Here we assume that the strategy can be reused after release
		// Ideas for cleaner solutions might be: strategy factory, reset method,
		// differnt interface for the strategy
		IDiff<Model> mainDiff = strategy.getMainGraphDiff();
		try {
			//out.flush();
			applyDiff(mainDiff);
		
			// Apply the node-diff
			applyNodeDiff(strategy.getNodeDiff());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		strategy.release();
		
		totalEntityCount += entityCount;
		entityCount = 0;
		
		logger.info("" + ((System.nanoTime() - start) / 1000000000.0) + "Completed processing batch of " + entityCount + " entities (total: " + totalEntityCount + ")");
	}

	private void applyDiff(IDiff<Model> diff)
		throws Exception
	{
		sparqlEndpoint.remove(diff.getRemoved(), graphName);
		sparqlEndpoint.insert(diff.getAdded(), graphName);
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
