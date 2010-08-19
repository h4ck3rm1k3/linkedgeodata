package org.linkedgeodata.osm.osmosis.plugins;

import java.sql.Connection;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.osm.mapping.IOneOneTagMapper;
import org.linkedgeodata.osm.mapping.InMemoryTagMapper;
import org.linkedgeodata.osm.mapping.TagMapperInstantiator;
import org.linkedgeodata.osm.mapping.TagMappingDB;
import org.linkedgeodata.osm.mapping.impl.OSMEntityToRDFTransformer;
import org.linkedgeodata.tagmapping.client.entity.AbstractSimpleTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.AbstractTagMapperState;
import org.linkedgeodata.tagmapping.client.entity.IEntity;
import org.linkedgeodata.util.Diff;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.VirtuosoUtils;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.linkedgeodata.util.sparql.VirtuosoJdbcSparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

interface IUpdateStrategy
{
	void update(ChangeContainer c);
}

/**
 * Update Strategy which ignores the information of deleted tags - and in consequence triples -
 * from modified elements.
 * Therefore this strategy always performs a diff.
 * 
 * @author raven
 *
 */
class IgnoreModifyDeletesDiffUpdateStrategy
	implements IUpdateStrategy
{
	private static final Logger logger = Logger.getLogger(IUpdateStrategy.class);
	
	private ILGDVocab vocab; 
	private ITransformer<Entity, Model> entityTransformer;
	private ISparulExecutor graphDAO;	
	private String graphName;


	public IgnoreModifyDeletesDiffUpdateStrategy(
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


	private static String constructBySubject(String iri, String graphName)
	{
		String fromPart = (graphName != null)
			? "From <" + graphName + "> "
			: "";

		String result =
			"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s = <" + iri + ">) . }";
		
		return result;
	}


	private static Model getBySubject(ISparqlExecutor graphDAO, String iri, String graphName)
		throws Exception
	{
		String query = constructBySubject(iri, graphName);
		
		//logger.info("Created query: " + query);
		
		Model model = graphDAO.executeConstruct(query);
		
		return model;
	}
	
	
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

		Model oldModel = getBySubject(graphDAO, subject, graphName);
		Model newModel = entityTransformer.transform(entity);
		
		IDiff<Model> diff = diff(oldModel, newModel);
		
		graphDAO.insert(diff.getAdded(), graphName);
		graphDAO.remove(diff.getRemoved(), graphName);

		
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
}



public class LiveRDFDeltaPlugin
	implements ChangeSink 
{
	private IUpdateStrategy updateStrategy;	
	
	public LiveRDFDeltaPlugin(String fileName)
		throws Exception
	{
		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
		
		Session session = TagMappingDB.getSession();
		Transaction tx = session.beginTransaction();
		
		for(Object o : session.createCriteria(AbstractTagMapperState.class).list()) {
			IOneOneTagMapper item = TagMapperInstantiator.getInstance().instantiate((IEntity)o);
			
			tagMapper.add(item);
		}
		
		tx.commit();
		
		ILGDVocab vocab = new LGDVocab();
		ITransformer<Entity, Model> entityTransformer =
			new OSMEntityToRDFTransformer(tagMapper, vocab);

		String graphName = "http://example.org";
		
		Connection conn = VirtuosoUtils.connect("localhost", "dba", "dba");

		ISparulExecutor graphDAO =
			new VirtuosoJdbcSparulExecutor(conn, graphName);
		
		IUpdateStrategy updateStrategy = new IgnoreModifyDeletesDiffUpdateStrategy(
				vocab, entityTransformer, graphDAO, graphName);
		
		this.updateStrategy = updateStrategy;
		
		System.out.println("Constructing " + this.getClass() + ", arg=" + fileName);
	}


	@Override
	public void complete()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void release()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void process(ChangeContainer c)
	{
		updateStrategy.update(c);
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
}
