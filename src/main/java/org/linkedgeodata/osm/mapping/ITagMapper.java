package org.linkedgeodata.osm.mapping;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;


/**
 * Interface for mapping tags to triples.
 * 
 * @author raven
 *
 */
public interface ITagMapper
{
	Model map(String subject, Tag tag, Model model);
}
