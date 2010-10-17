package org.linkedgeodata.osm.osmosis.plugins;

import java.io.IOException;
import java.io.OutputStream;

import org.linkedgeodata.util.IDiff;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;

import com.hp.hpl.jena.rdf.model.Model;

public class LiveDumpChangeSink
	implements ChangeSink
{
	private IUpdateStrategy strategy;
	private int entityCount = 0;
	private OutputStream out;
	
	private int maxEntityCount = 100000;
	
	public LiveDumpChangeSink(IUpdateStrategy strategy, OutputStream out)
	{
		this.strategy = strategy;
		this.out = out;
	}

	public LiveDumpChangeSink(IUpdateStrategy strategy, OutputStream out, int maxEntityCount)
	{
		this.strategy = strategy;
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		strategy.release();
		
		entityCount = 0;
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
