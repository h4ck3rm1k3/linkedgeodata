package org.linkedgeodata.osm.osmosis.plugins;

import java.util.Iterator;

import org.apache.commons.collections15.Predicate;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSinkChangeSource;


public class TagFilterPlugin
	implements ChangeSinkChangeSource
{
	private ChangeSink sink;
	private Predicate<Tag> tagFilter;
	
	public TagFilterPlugin(Predicate<Tag> tagFilter)
	{
		this.tagFilter = tagFilter;
	}
	
	@Override
	public void process(ChangeContainer c)
	{
		Iterator<Tag> it = c.getEntityContainer().getEntity().getTags().iterator();
		while(it.hasNext()) {
			Tag tag = it.next();
			if(!tagFilter.evaluate(tag)) {
				it.remove();
			}
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
