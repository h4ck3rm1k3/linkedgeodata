/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.linkedgeodata.osm.osmosis.plugins;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.util.Diff;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.ModelUtil;
import org.linkedgeodata.util.TransformIterable;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.sort.v0_6.EntityByTypeThenIdComparator;
import org.openstreetmap.osmosis.core.sort.v0_6.EntityByIdComparator;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/*
class ChunkIterator<T>
	implements Iterator<Collection<T>>
{
	private Collection<T> source;
	private int batchSize;

	public CollectionChunker(Collection<T> source, int batchSize)
	{
		this.source = source;
		this.batchSize = batchSize;
	}

	@Override
	public boolean hasNext()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<T> next()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}


class CollectionChunker<T>
	extends AbstractCollection<Collection<T>>
{
	private Collection<T> source;
	private int batchSize;
	
	public CollectionChunker(Collection<T> source, int batchSize)
	{
		this.source = source;
		this.batchSize = batchSize;
	}

	@Override
	public Iterator<Collection<T>> iterator()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size()
	{
		return source.size() / batchSize; 
	}
}
*/


class SetDiff<T>
	extends CollectionDiff<T, Set<T>>
{
	/*
	public SetDiff(Comparator<T> comparator)
	{
	}
	*/

	public SetDiff(Comparator<T> comparator)
	{
		super(
				new TreeSet<T>(comparator),
				new TreeSet<T>(comparator),
				new TreeSet<T>(comparator));
	}	
}

abstract class CollectionDiff<T, C extends Collection<T>>
	extends Diff<C>
{	
	public CollectionDiff(C added, C removed, C retained)
	{
		super(added, removed, retained);
	}

	public void add(T item) {
		getRemoved().remove(item);
		getAdded().add(item);
	}
	
	public void remove(T item) {
		getAdded().remove(item);
		getRemoved().add(item);
	}
	
	public void clear() {
		getAdded().clear();
		getRemoved().clear();
	}
	
	
	public int size()
	{
		return getAdded().size() + getRemoved().size();
	}
}

/**
 * Update Strategy which ignores the information of deleted tags - and in consequence triples -
 * from modified elements.
 * Therefore this strategy always performs a diff.
 * 
 * @author raven
 *
 * FIXME Somehow separate the store update code from the timely diff code
 *
 */
public class IgnoreModifyDeleteDiffUpdateStrategy
	implements IUpdateStrategy
{
	private static final Logger logger = Logger.getLogger(IUpdateStrategy.class);
	
	private ILGDVocab vocab; 
	private ITransformer<Entity, Model> entityTransformer;
	private ISparqlExecutor graphDAO;	
	private String graphName;

	private ITransformer<Model, Model> postStatmentTransformer = new VirtuosoStatementNormalizer();
	
	private RDFDiff timelyDiff;
	
	//private Set<Entity> entities = new HashSet<Entity>();
	
	SetDiff<EntityContainer> entityDiff = new SetDiff<EntityContainer>(new EntityByTypeThenIdComparator());
	
	// Number of entities that should be processed as a batch
	private int maxEntityBatchSize = 500;
	
	/*
	long entityDiffTimeSpan = 60000;	
	private Date timeStamp = null;
	*/
	
	public IgnoreModifyDeleteDiffUpdateStrategy(
			ILGDVocab vocab,
			ITransformer<Entity, Model> entityTransformer,
			ISparqlExecutor graphDAO,
			String graphName)
	{
		this.vocab = vocab;
		this.entityTransformer = entityTransformer;
		this.graphDAO = graphDAO;
		this.graphName = graphName;
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
	public void process(ChangeContainer c)
	{
		if(c.getAction().equals(ChangeAction.Delete)) {
			entityDiff.remove(c.getEntityContainer());
		}		
		else {
			entityDiff.add(c.getEntityContainer());
		}
	}

	
	private static Model executeConstruct(ISparqlExecutor graphDAO, Iterable<String> queries)
		throws Exception
	{
		Model result = ModelFactory.createDefaultModel();
		for(String query : queries) {
			Model tmp = graphDAO.executeConstruct(query);
			
			result.add(tmp);
		}
		
		return result;
	}


	public static <T> Iterable<Iterable<T>> chunk(Iterable<T> col, int batchSize)
	{
		List<Iterable<T>> result = new ArrayList<Iterable<T>>();
		
		List<T> chunk = new ArrayList<T>();
		
		Iterator<T> it = col.iterator();
		while(it.hasNext()) {
			chunk.add(it.next());

			if(chunk.size() >= batchSize || !it.hasNext()) {
				result.add(chunk);
				
				if(it.hasNext())
					chunk = new ArrayList<T>();
			}
		}

		return result;
	}
	
		
	private void process(IDiff<? extends Iterable<EntityContainer>> inDiff, RDFDiff outDiff, int batchSize)
		throws Exception
	{
		Iterable<Iterable<EntityContainer>> parts;

		Transformer<EntityContainer, Entity> entityExtractor = new Transformer<EntityContainer, Entity>() {
			@Override
			public Entity transform(EntityContainer input)
			{
				return input.getEntity();
			}
		};
		
		parts = chunk(inDiff.getRemoved(), batchSize);
		for(Iterable<EntityContainer> part : parts) {
			processBatch(outDiff, ChangeAction.Delete, TransformIterable.transformedView(part, entityExtractor));
		}		

		parts = chunk(inDiff.getAdded(), batchSize);
		for(Iterable<EntityContainer> part : parts) {
			processBatch(outDiff, ChangeAction.Create, TransformIterable.transformedView(part, entityExtractor));
		}
	}
	
	

	private void processBatch(RDFDiff outDiff, ChangeAction changeAction, Iterable<Entity> entityBatch)
		throws Exception
	{
		List<String> queries = GraphDAORDFEntityDAO.constructQuery(
				entityBatch,
				vocab,
				graphName);

		Model oldModel = executeConstruct(graphDAO, queries);
		
		Model newModel = ModelFactory.createDefaultModel();
		for(Entity entity : entityBatch) {
			entityTransformer.transform(newModel, entity);
		}
		
		// Transform the triples that were added
		Model processedNewModel = postStatmentTransformer.transform(newModel);
		//System.out.println(ModelUtil.toString(added));
		
		
		IDiff<Model> diff = diff(oldModel, processedNewModel);		
		
		outDiff.remove(diff.getRemoved());
		outDiff.add(diff.getAdded());
	}
		
	
	@Override
	public void complete()
	{
		//logger.info(this.getClass() + " completed");
		try {
			timelyDiff = new RDFDiff();
			process(entityDiff, timelyDiff, maxEntityBatchSize);
			entityDiff.clear();
		} catch(Exception e) {
			logger.error("An error occurred at the completion phase of a task", e);
		}
	}
	
	
	
	public RDFDiff getDiff()
	{
		return timelyDiff;
	}

	@Override
	public void release()
	{
		timelyDiff = null;
	}
	
	
}




/*
 * Following code can be removed as soon as the plugin is working - 
 * because then its definitely not needed anymore
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

/*
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
*/	

/*
private static String constructQuery(final ILGDVocab vocab, Iterable<Long> nodeIds, Iterable<Long> wayIds, String graphName)
{
	if(!wayIds.iterator().hasNext())
		return "";

	String resources = "";
	
	resources += StringUtil.implode(",",
			new TransformIterable<Long, String>(
					nodeIds,
					new Transformer<Long, String>() {
						@Override
						public String transform(Long nodeId)
						{
							return vocab.createNIRNodeURI(nodeId);
						}
					}));

	resources += StringUtil.implode(",",
			new TransformIterable<Long, String>(
					nodeIds,
					new Transformer<Long, String>() {
						@Override
						public String transform(Long wayId)
						{
							return
								"<" + vocab.createNIRWayURI(wayId) + ">,<" +
								vocab.getHasNodesResource(wayId).toString() + ">";
						}
					}));
	
		
	String fromPart = (graphName != null)
		? "From <" + graphName + "> "
		: "";

	String result =
		"Construct { ?s ?p ?o . } " + fromPart + "{ ?s ?p ?o . Filter(?s In (" + resources + ")) . }";

	return result;
}
*/

/*
private static Model getBySubject(ISparqlExecutor graphDAO, String iri, String graphName)
	throws Exception
{
	String query = constructBySubject(iri, graphName);
	
	//logger.info("Created query: " + query);
	
	Model model = graphDAO.executeConstruct(query);
	
	return model;
}*/

/*
if(timeStamp == null)
	timeStamp = new Date();

Date now = new Date();
*/

/*
if(timeStamp == null)
	timeStamp = entity.getTimestamp(); 

Date now = entity.getTimestamp();

if(timeStamp.getTime() > now.getTime()) {
	logger.warn("Warning: Entities arriving out of order: " + timeStamp + " > " + now);
}*/


/*
if(timeStamp == null)
	timeStamp = new Date();

Date now = new Date();

if(now.getTime() - timeStamp.getTime() > entityDiffTimeSpan) {
	process(timelyDiff);
	
	timeStamp = now;
	
	entities.clear();
}*/
