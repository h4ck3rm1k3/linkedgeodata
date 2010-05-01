package org.linkedgeodata.jtriplify.mapping;

import java.net.URI;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;


public class SimpleClassTagMapper
	extends AbstractOneOneTagMapper
{		
	private static final Logger logger = Logger.getLogger(SimpleDataTypeTagMapper.class);
	
	
	/**
	 * Valid types are:
	 * type=int
	 * type=float
	 * type=bool
	 * type=uri
	 * type=lang, langTag=<>
	 * type=class
	 * 
	 * @param resource
	 * @param method
	 * @param tag
	 */
	public SimpleClassTagMapper(URI clazz, Tag tag)
		throws Exception
	{
		super(clazz, tag);	
	}
	
	public Model map(URI subject, Tag tag)
	{
		String suffix = "";
		
		if(super.getTagPattern().getValue() == null) {
			suffix = tag.getValue();
		}
		
		Model result = ModelFactory.createDefaultModel();
		result.add(
				result.getResource(subject.toString()),
				RDF.type,
				result.getResource(super.getResource().toString() + suffix)
		);
		
		return result;
	}
	
	@Override
	public String toString()
	{
		try {
			return TagMapperFactory.buildClassURI(super.getResource().toString(), super.getTagPattern().getKey(), super.getTagPattern().getValue()).toString();
		} catch(Exception e) {
			e.printStackTrace();
			return "";
		}
	}
}
