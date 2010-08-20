package org.linkedgeodata.osm.osmosis.plugins;

import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;

/**
 * This interface is more or less a duplicate of the osmosis ChangeSink
 * interface.
 * 
 * Maybe it should be removed.
 * 
 * @author raven
 *
 */
public interface IUpdateStrategy
{
	void update(ChangeContainer c);
	void complete();
}
