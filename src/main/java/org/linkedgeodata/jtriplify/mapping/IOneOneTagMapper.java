package org.linkedgeodata.jtriplify.mapping;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public interface IOneOneTagMapper
{
	// TODO The following two methods should be removed from the interface
	String getResource();
	TagPattern getTagPattern();
	
	
	Model map(String subject, Tag tag, Model model);
	
	
	<T> T accept(IOneOneTagMapperVisitor<T> visitor);
}
