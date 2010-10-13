package org.linkedgeodata.osm.osmosis.plugins;


import org.apache.commons.collections15.Predicate;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSinkChangeSource;


public class EntityFilterPlugin
	implements ChangeSinkChangeSource
{
	private ChangeSink sink;
	
	private Predicate<Entity> entityFilter;
	
	public EntityFilterPlugin(Predicate<Entity> entityFilter)
	{
		this.entityFilter = entityFilter;
	}

	@Override
	public void process(ChangeContainer c)
	{
		if(!entityFilter.evaluate(c.getEntityContainer().getEntity())) {
			// FIXME For now this filter simply removes all tags of entities
			// that fail the filter
			c.getEntityContainer().getEntity().getTags().clear();
		}
		sink.process(c);
	}

	@Override
	public void complete()
	{
		sink.complete();
	}

	@Override
	public void release()
	{
		sink.release();
	}

	@Override
	public void setChangeSink(ChangeSink sink)
	{
		this.sink = sink;
	}
}
