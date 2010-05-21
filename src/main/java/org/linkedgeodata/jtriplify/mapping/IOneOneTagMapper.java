package org.linkedgeodata.jtriplify.mapping;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public interface IOneOneTagMapper
{
	String getResource();
	TagPattern getTagPattern();
	Model map(String subject, Tag tag);
}
