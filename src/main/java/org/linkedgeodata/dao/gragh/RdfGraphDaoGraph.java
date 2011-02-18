package org.linkedgeodata.dao.gragh;

import org.linkedgeodata.dao.LGDRDFDAO;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;


/**
 * This class provides a Jena-Graph view on an LGDRDFDAO
 * 
 * @author raven
 *
 */
public class RdfGraphDaoGraph
	extends GraphBase
{
	private LGDRDFDAO dao;
	
	public RdfGraphDaoGraph(LGDRDFDAO dao)
	{
		this.dao = dao;
	}
	
	@Override
	protected ExtendedIterator<Triple> graphBaseFind(TripleMatch m)
	{
		Model model = ModelFactory.createDefaultModel();
	
		// Generate a prefiltered model from the dao
		// FIXME Ideally this should be pushed into the dao
		try {
			if(m.getMatchSubject() == null) {
				dao.getOntologyDAO().getOntology(model);
			} else {			
				dao.getOntologyDAO().describe(m.getMatchSubject().toString(), model);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Perform detailed filtering 
		return model.getGraph().find(m);
	}
}
