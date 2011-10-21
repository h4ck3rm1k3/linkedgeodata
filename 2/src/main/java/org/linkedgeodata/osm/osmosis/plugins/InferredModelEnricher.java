package org.linkedgeodata.osm.osmosis.plugins;

import java.util.Iterator;

import org.linkedgeodata.util.ITransformer;
import org.mindswap.pellet.jena.PelletReasoner;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class InferredModelEnricher
	implements ITransformer<Model, Model>
{
	private Model schema;
	private Reasoner reasoner = new PelletReasoner();
	
	public InferredModelEnricher(Model schema)
	{
		this.schema = schema;
	}
	
	@Override
	public Model transform(Model in)
	{
		Model result = ModelFactory.createDefaultModel();
		
		transform(result, in);
		
		return result;
	}

	@Override
	public Model transform(Model out, Model in)
	{
		Model model = ModelFactory.createInfModel(reasoner, schema, in);
		//Model model = ModelFactory.createRDFSModel(schema, in);		
		// FIXME Create tmp model as out could be the same as in
		// However, maybe that is not necessary, as we retrieve the set
		// of subjects first anyway
		//Model tmp = ModelFactory.createDefaultModel();
		Model tmp = out;
		
		for(Resource subject : in.listSubjects().toSet()) {
			tmp.add(model.listStatements(subject, null, (RDFNode)null));
		}
		
		tmp.remove(tmp.listStatements(null, RDF.type, OWL.Thing));
		
		Iterator<Statement> it = tmp.listStatements(null, OWL.sameAs, (RDFNode)null);
		while(it.hasNext()) {
			Statement stmt = it.next();
			
			if(stmt.getSubject().equals(stmt.getObject())) {
				it.remove();
			}
		}
		
		//out.add(tmp.listStatements());

		return out;
	}

}
