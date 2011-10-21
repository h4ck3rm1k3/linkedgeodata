package org.linkedgeodata.osm.mapping;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public interface ITagPattern
{
	boolean matches(Tag given);
}
