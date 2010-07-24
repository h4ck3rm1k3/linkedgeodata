package org.linkedgeodata.osm.mapping.impl;

import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public interface ISimpleOneOneTagMapper
	extends IOneOneTagMapper
{
	// TODO The following method should be renamed getBaseResource
	String getResource();
	SimpleTagPattern getTagPattern();
	
	
	// Returns the resource corresponding to a tag.
	String getResource(Tag tag);

	//Model map(String subject, Tag tag, Model model);
	
	
	<T> T accept(ISimpleOneOneTagMapperVisitor<T> visitor);
}
