package org.linkedgeodata.scripts;

import org.apache.commons.collections15.Predicate;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;


/**
 * Rejects entities if at least one tag is rejected.
 * 
 * @author raven
 *
 */
public class EntityFilter
	implements Predicate<Entity>
{
	private Predicate<Tag> tagFilter;

	public EntityFilter(Predicate<Tag> tagFilter)
	{
		this.tagFilter = tagFilter;
	}
	
	@Override
	public boolean evaluate(Entity entity)
	{
		for(Tag tag : entity.getTags()) {
			if(tagFilter.evaluate(tag) == false)
				return false;
		}
		
		return true;
	}
}
