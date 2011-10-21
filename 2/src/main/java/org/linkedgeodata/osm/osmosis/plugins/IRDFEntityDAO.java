package org.linkedgeodata.osm.osmosis.plugins;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

import com.hp.hpl.jena.rdf.model.Model;

public interface IRDFEntityDAO
{
	Model fetchData(Iterable<Entity> entities)
		throws Exception;
	
	void delete(Iterable<Entity> entities)
		throws Exception;
	
	void add(Model model)
		throws Exception;

	void delete(Model model)
		throws Exception;
}
