package org.linkedgeodata.i18n.gettext;

import java.util.Iterator;

import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class EntityResolver2
	implements IEntityResolver
{
	private InMemoryTagMapper tagMapper;
	
	public EntityResolver2(InMemoryTagMapper tagMapper)
	{
		this.tagMapper = tagMapper;
	}
	
	@Override
	public Resource resolve(String key, String value)
	{
		Model model = tagMapper.map("http://ex.org", new Tag(key, value), null);
	
		Iterator<Statement> it = model.listStatements();

		while(it.hasNext()) {
			Statement stmt = it.next();
			
			//if(!stmt.getPredicate().equals(RDF.type))
			//	continue;
			if(!stmt.getObject().isURIResource()) {
				continue;
			}
			
			Resource classRes = stmt.getObject().as(Resource.class);
			
			return classRes;			
		}
		
		return null;
	}
	
}