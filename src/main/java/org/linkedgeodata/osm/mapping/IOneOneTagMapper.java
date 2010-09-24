package org.linkedgeodata.osm.mapping;

import org.linkedgeodata.osm.mapping.impl.IOneOneTagMapperVisitor;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public interface IOneOneTagMapper
{
	// TODO The following two methods should be removed from the interface
	//String getResource();
	//ITagPattern getTagPattern();
	boolean matches(Tag tag);
	
	Model map(String subject, Tag tag, Model model);
	
	//<T> T accept(ISimpleOneOneTagMapperVisitor<T> visitor);
	
	<T> T accept(IOneOneTagMapperVisitor<T> visitor);
}
