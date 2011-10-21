package org.linkedgeodata.dao;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * 
 * 
 * @author raven
 *
 */
public class LGDModelAugmenter
{
	private Set<String> knownPredicates;	
	
	public LGDModelAugmenter(Set<String> knownPredicates)
	{
		this.knownPredicates = knownPredicates;
	}
	
	public int augment(Model model)
	{
		
		int result = 0;
		Iterator<Statement> it = model.listStatements();
		while(it.hasNext()) {
			Statement stmt = it.next();
			
			if(!knownPredicates.contains(stmt.getPredicate().toString())) {
				//model.createProperty(stmt.getPredicate())
				++result;
				stmt.getPredicate().addProperty(RDF.type, OWL.DatatypeProperty);
			}
		}

		return result;
	}

}
