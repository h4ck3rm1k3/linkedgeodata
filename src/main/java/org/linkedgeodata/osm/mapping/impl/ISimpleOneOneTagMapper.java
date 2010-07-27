package org.linkedgeodata.osm.mapping.impl;

import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public interface ISimpleOneOneTagMapper
	extends IOneOneTagMapper
{
	String getProperty();
	SimpleTagPattern getTagPattern();
	
	
	// Returns the resource corresponding to a tag.
	String getObject(Tag tag);

	//Model map(String subject, Tag tag, Model model);
	
	
	<T> T accept(ISimpleOneOneTagMapperVisitor<T> visitor);
}
