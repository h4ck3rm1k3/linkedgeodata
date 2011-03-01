package org.linkedgeodata.osm.osmosis.plugins;

import java.util.Map;
import java.util.Set;

import org.aksw.commons.jena.ModelUtils;
import org.aksw.commons.util.collections.MultiMaps;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.vocab.WGS84Pos;
import org.linkedgeodata.util.ITransformer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class TransitiveInferredModelEnricher
	implements ITransformer<Model, Model>
{
	private Map<Resource, Set<Resource>> classHierarchy; 
	private ILGDVocab vocab;
	
	public TransitiveInferredModelEnricher(ILGDVocab vocab, Model schema)
	{
		this.vocab = vocab;
		classHierarchy = ModelUtils.extractDirectSuperClassMap(schema);
		MultiMaps.transitiveClosureInPlace(classHierarchy);
	}
	
	@Override
	public Model transform(Model in)
	{
		//Model result = ModelFactory.createDefaultModel();
		
		//transform(result, in);
		
		//return result;
		return transform(in, in);
	}

	@Override
	public Model transform(Model out, Model in)
	{
		return transformUsingDomainSpecificCode(out, in);
	}

	
	
	private Model transformUsingDomainSpecificCode(Model out, Model in)
	{
		{
			StmtIterator it = in.listStatements(null, WGS84Pos.geometry, (RDFNode)null);
			while(it.hasNext()) {
				Statement stmt = it.next();
				out.add(stmt.getSubject(), RDF.type, vocab.getNodeClass());
			}
			it.close();
		}

		{
			StmtIterator it = in.listStatements(null, vocab.getHasNodesPred(), (RDFNode)null);
			while(it.hasNext()) {
				Statement stmt = it.next();
				out.add(stmt.getSubject(), RDF.type, vocab.getWayClass());
			}
			it.close();
		}
	
		{
			Model tmp = ModelFactory.createDefaultModel();
			StmtIterator it = in.listStatements(null, RDF.type, (RDFNode)null);
			while(it.hasNext()) {
				Statement stmt = it.next();
				
				for(Resource parentClass : MultiMaps.safeGet(classHierarchy, stmt.getObject())) {
					tmp.add(stmt.getSubject(), RDF.type, parentClass);
				}
			}
			it.close();
			
			out.add(tmp);
			
			if(out != in) {
				out.add(in);
			}
		}
		
		return out;
	}
}
