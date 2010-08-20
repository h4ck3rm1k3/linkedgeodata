package org.linkedgeodata.osm.osmosis.plugins;

import org.apache.log4j.Logger;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.util.Diff;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Update Strategy which ignores the information of deleted tags - and in consequence triples -
 * from modified elements.
 * Therefore this strategy always performs a diff.
 * 
 * @author raven
 *
 */
public class IgnoreModifyDeleteDiffUpdateStrategy
	implements IUpdateStrategy
{
	private static final Logger logger = Logger.getLogger(IUpdateStrategy.class);
	
	private ILGDVocab vocab; 
	private ITransformer<Entity, Model> entityTransformer;
	private ISparulExecutor graphDAO;	
	private String graphName;


	public IgnoreModifyDeleteDiffUpdateStrategy(
			ILGDVocab vocab,
			ITransformer<Entity, Model> entityTransformer,
			ISparulExecutor graphDAO,
			String graphName)
	{
		this.vocab = vocab;
		this.entityTransformer = entityTransformer;
		this.graphDAO = graphDAO;
		this.graphName = graphName;
	}


	/*
	private static String constructBySubject(String iri, String graphName)
	{
		String fromPart = (graphName != null)
			? "From <" + graphName + "> "
			: "";

		String result =
			"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s = <" + iri + ">) . }";
		
		return result;
	}
	*/

	private static String constructNodeModelQuery(ILGDVocab vocab, long nodeId, String graphName)
	{
		String nodeIRI = vocab.createNIRNodeURI(nodeId);
		
		String fromPart = (graphName != null)
			? "From <" + graphName + "> "
					: "";

		String result =
			"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s = <" + nodeIRI + ">) . }";
	
	return result;
	}
	
	private static String constructWayModelQuery(ILGDVocab vocab, long wayId, String graphName)
	{
		String wayIRI = vocab.createNIRWayURI(wayId);
		String wayNodesIRI = vocab.getHasNodesResource(wayId).toString();
		
		String fromPart = (graphName != null)
			? "From <" + graphName + "> "
			: "";

		String result =
			"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s = <" + wayIRI + "> || ?s = <" + wayNodesIRI + ">) . }";
	
		return result;
	}
	

	/*
	private static Model getBySubject(ISparqlExecutor graphDAO, String iri, String graphName)
		throws Exception
	{
		String query = constructBySubject(iri, graphName);
		
		//logger.info("Created query: " + query);
		
		Model model = graphDAO.executeConstruct(query);
		
		return model;
	}*/
	
	
	/**
	 * NOTE Does not set retained triples
	 * 
	 * @param o
	 * @param n
	 * @return
	 */
	private static IDiff<Model> diff(Model o, Model n)
	{
		Model added = ModelFactory.createDefaultModel();
		added.add(n);
		added.remove(o);
		
		Model removed = ModelFactory.createDefaultModel();
		removed.add(o);
		removed.remove(n);
		
		IDiff<Model> result = new Diff<Model>(added, removed, null);
		
		return result;
	}
	
	
	@Override
	public void update(ChangeContainer c)
	{
		try { 
			_update(c);
		} catch(Exception e) {
			logger.error("Error processing an element", e);
		}
	}


	public void _update(ChangeContainer c)
		throws Exception
	{
		Entity entity = c.getEntityContainer().getEntity();
		String subject = vocab.createResource(entity);

		// If there is no subject the entity is implicitely not supported
		if(subject == null)
			return;
		
		Model oldModel;
		
		if(entity instanceof Node) {
			oldModel = graphDAO.executeConstruct(constructNodeModelQuery(vocab, entity.getId(), graphName));
		} else if (entity instanceof Way) {
			oldModel = graphDAO.executeConstruct(constructWayModelQuery(vocab, entity.getId(), graphName));			
		} else {
			oldModel = ModelFactory.createDefaultModel();
		}

		Model newModel = entityTransformer.transform(entity);
		
		IDiff<Model> diff = diff(oldModel, newModel);
		
		graphDAO.remove(diff.getRemoved(), graphName);
		graphDAO.insert(diff.getAdded(), graphName);

		
/*		
		ChangeAction action = c.getAction();
		if(action.equals(ChangeAction.Create)) {
			System.out.println("Create");
		}
		else if(action.equals(ChangeAction.Modify)) {
			System.out.println("Modify");
		}
		else if(action.equals(ChangeAction.Delete)) {
			System.out.println("Delete");			
		}
		System.out.println(    "-> " + c.getEntityContainer().getEntity());
*/	
	}


	@Override
	public void complete()
	{
		// TODO Auto-generated method stub
		
	}	
}
