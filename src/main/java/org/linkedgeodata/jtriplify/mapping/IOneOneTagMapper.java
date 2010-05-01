package org.linkedgeodata.jtriplify.mapping;

import java.net.URI;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;

public interface IOneOneTagMapper
{
	URI getResource();
	Tag getTagPattern();
	Model map(URI subject, Tag tag);
}
