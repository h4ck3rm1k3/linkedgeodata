package org.linkedgeodata.osm.osmosis.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.util.CollectionUtils;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
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
	
	public static Set<Resource> getInvolvedResources(Iterable<? extends Entity> entities, ILGDVocab vocab)
	{
		Set<Resource> result = new HashSet<Resource>();
		
		for(Entity entity : entities) {
			Resource[] resources = getInvolvedResources(entity, vocab);
			
			for(Resource res : resources) {
				//Resource res = ResourceFactory.createResource(str);
				result.add(res);
			}
		}
		
		
		return result;
	}
	
	public static Set<Resource> getInvolvedResources(Collection<EntityContainer> ecs, ILGDVocab vocab) {
		Set<Resource> result = new HashSet<Resource>();
		
		for(EntityContainer ec : ecs) {
			for(Resource res : getInvolvedResources(ec.getEntity(), vocab)) {
				//Resource res = ResourceFactory.createResource(str);
				
				result.add(res);
			}
		}
		
		
		return result;
	}
	
	public static Resource[] getInvolvedResources(Entity entity, ILGDVocab vocab)
	{
		long entityId = entity.getId();
		if(entity instanceof Node) {
			return new Resource[]{vocab.createNIRNodeURI(entityId)};
			
		} else if(entity instanceof Way) {
			return new Resource[]{
					vocab.createNIRWayURI(entityId),
					vocab.getHasNodesResource(entityId)};
		}
		
		return new Resource[]{};
	}
	
	public static List<String> constructQuery(Iterable<Entity> entities, ILGDVocab vocab, String graphName, int batchSize)
	{
		Set<Resource> uris = new TreeSet<Resource>();
		
		for(Entity entity : entities) {
			uris.addAll(Arrays.asList(getInvolvedResources(entity, vocab)));
		}
		
		return constructQuery(uris, graphName, batchSize);
	}
	

	public static List<String> constructQuery(Collection<Resource> subjects, String graphName, int batchSize)
	{
		List<String> result = new ArrayList<String>();
		if(subjects.isEmpty())
			return result;
		
		List<List<Resource>> chunks = CollectionUtils.chunk(subjects, batchSize);
		
		for(List<Resource> chunk : chunks) {
			String resources = "<" + StringUtil.implode(">,<", chunk) + ">";
			
			String fromPart = (graphName != null)
				? "From <" + graphName + "> "
				: "";
	
			String query =
				"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";
	
			result.add(query);
		}
		
		return result;
	}
	
	public static String constructBySubject(Collection<Resource> subjects, String graphName)
	{
		String resources = "<" + StringUtil.implode(">,<", subjects) + ">";
			
		String fromPart = (graphName != null)
			? "From <" + graphName + "> "
			: "";
	
		String result =
			"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";
			
		return result;
	}

		
	
	//public String lookup(
	
	/*
	public String lookupWays(Collection<String> subjects) {
	
		String query =
			"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";
		
	}
	*/
	
	
	

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
		//throw new NotImplementedException();
		throw new RuntimeException("Not implemented");
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
