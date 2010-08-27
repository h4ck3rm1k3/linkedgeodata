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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.util.Diff;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.sparql.ISparqlExecutor;
import org.linkedgeodata.util.sparql.ISparulExecutor;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

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
	private ISparulExecutor graphDAO;	
	private String graphName;

	private List<Entity> entities = new ArrayList<Entity>();
	
	// Number of entities that should be processed as a batch
	private int maxEntityBatchSize = 100;
	
	long entityDiffTimeSpan = 60000;
	RDFDiff timelyDiff = new RDFDiff();
	
	private Date timeStamp = null;


	private RDFDiffWriter rdfDiffWriter;
	
	public IgnoreModifyDeleteDiffUpdateStrategy(
			ILGDVocab vocab,
			ITransformer<Entity, Model> entityTransformer,
			ISparulExecutor graphDAO,
			String graphName,
			RDFDiffWriter rdfDiffWriter)
	{
		this.vocab = vocab;
		this.entityTransformer = entityTransformer;
		this.graphDAO = graphDAO;
		this.graphName = graphName;
		
		this.rdfDiffWriter = rdfDiffWriter;
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

	/**
	 * Note: Hopefully entites come in in timely order :/
	 * Otherwise the diff output would get messed up
	 * 
	 * @param c
	 * @throws Exception
	 */
	public void _update(ChangeContainer c)
		throws Exception
	{
		/*
		if(timeStamp == null)
			timeStamp = new Date();

		Date now = new Date();
		*/

		Entity entity = c.getEntityContainer().getEntity();

		if(timeStamp == null)
			timeStamp = entity.getTimestamp(); 

		Date now = entity.getTimestamp();
		
		if(now.getTime() - timeStamp.getTime() <= entityDiffTimeSpan) {
			process();
			
			timeStamp = now;
		}

		entities.add(entity);
	}

	private void process()
		throws Exception
	{
		List<Entity> batch = new ArrayList<Entity>(); 
		for(Entity entity : entities) {
			batch.add(entity);

			if(batch.size() >= maxEntityBatchSize) {
				processBatch(batch);
				batch.clear();
			}
		}
		
		processBatch(batch);
		batch.clear();
		
		rdfDiffWriter.write(timelyDiff);
		timelyDiff.clear();
	}

	private void processBatch(Iterable<Entity> entityBatch)
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
		
		IDiff<Model> diff = diff(oldModel, newModel);
		
		graphDAO.remove(diff.getRemoved(), graphName);
		graphDAO.insert(diff.getAdded(), graphName);
		
		timelyDiff.remove(diff.getRemoved());
		timelyDiff.add(diff.getAdded());
	}
		
	
	@Override
	public void complete()
	{
		try {
			process();
		} catch(Exception e) {
			logger.error("An error occurred at the completion phase of a task", e);
		}
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
