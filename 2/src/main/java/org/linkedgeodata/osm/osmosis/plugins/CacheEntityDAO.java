package org.linkedgeodata.osm.osmosis.plugins;

import java.util.List;
import java.util.Set;

import org.linkedgeodata.core.ILGDVocab;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class CacheEntityDAO
	implements IRDFEntityDAO
{
	private Model cache = ModelFactory.createDefaultModel();
	private ILGDVocab vocab;

	
	public CacheEntityDAO(ILGDVocab vocab)
	{
		this.vocab = vocab;
	}
	
	@Override
	public Model fetchData(Iterable<Entity> entities)
		throws Exception
	{
		Model result = ModelFactory.createDefaultModel();

		Set<Resource> resources = GraphDAORDFEntityDAO.getInvolvedResources(entities, vocab);
		
		for(Resource resource : resources) {
			List<Statement> stmts =
				cache.listStatements(resource, null, (RDFNode)null).toList();
			
			result.add(stmts);
		}
		
		return null;
	}

	@Override
	public void delete(Iterable<Entity> entities) throws Exception
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void add(Model model) throws Exception
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Model model) throws Exception
	{
		// TODO Auto-generated method stub
		
	}

}
