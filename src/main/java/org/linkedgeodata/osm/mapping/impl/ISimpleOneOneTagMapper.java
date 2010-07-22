package org.linkedgeodata.osm.mapping.impl;

import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public interface ISimpleOneOneTagMapper
	extends IOneOneTagMapper
{
	// TODO The following two methods should be removed from the interface
	String getResource();
	SimpleTagPattern getTagPattern();
	
	
	//Model map(String subject, Tag tag, Model model);
	
	
	<T> T accept(ISimpleOneOneTagMapperVisitor<T> visitor);
}
