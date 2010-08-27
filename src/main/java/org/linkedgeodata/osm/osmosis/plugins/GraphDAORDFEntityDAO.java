package org.linkedgeodata.osm.osmosis.plugins;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


public class GraphDAORDFEntityDAO
	implements IRDFEntityDAO
{
	private ISparulExecutor graphDAO;
	private ILGDVocab vocab;
	private String graphName;
	
	public static Set<Resource> getInvolvedResources(Iterable<Entity> entities, ILGDVocab vocab)
	{
		Set<Resource> result = new HashSet<Resource>();
		
		for(Entity entity : entities) {
			String[] resources = getInvolvedResources(entity, vocab);
			
			for(String str : resources) {
				Resource res = ResourceFactory.createResource(str);
				result.add(res);
			}
		}
		
		return result;
	}
	
	public static String[] getInvolvedResources(Entity entity, ILGDVocab vocab)
	{
		long entityId = entity.getId();
		if(entity instanceof Node) {
			return new String[]{vocab.createNIRNodeURI(entityId)};
			
		} else if(entity instanceof Way) {
			return new String[]{
					vocab.createNIRWayURI(entityId),
					vocab.getHasNodesResource(entityId).toString()};
		}
		
		return new String[]{};
	}
	
	public static List<String> constructQuery(Iterable<Entity> entities, ILGDVocab vocab, String graphName)
	{
		Set<String> uris = new HashSet<String>();
		
		for(Entity entity : entities) {
			uris.addAll(Arrays.asList(getInvolvedResources(entity, vocab)));
		}

		
		if(uris.isEmpty())
			return Collections.emptyList();
		
		String resources = "<" + StringUtil.implode(">,<", uris) + ">";
		
		String fromPart = (graphName != null)
			? "From <" + graphName + "> "
			: "";

		String result =
			"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";
	
		return Collections.singletonList(result);
	}

	public GraphDAORDFEntityDAO(ISparulExecutor graphDAO)
	{
		this.graphDAO = graphDAO;
	}

	@Override
	public Model fetchData(Iterable<Entity> entities)
		throws Exception
	{
		return null;
		/*
		String query = constructQuery(entities, vocab, graphName);
		
		Model result = graphDAO.executeConstruct(query);
		
		return result;
		*/
	}

	@Override
	public void delete(Iterable<Entity> entities)
	{
		throw new NotImplementedException();
	}

	@Override
	public void add(Model model)
		throws Exception
	{
		graphDAO.insert(model, graphName);
	}

	@Override
	public void delete(Model model)
		throws Exception
	{
		graphDAO.remove(model, graphName);
	}
}
