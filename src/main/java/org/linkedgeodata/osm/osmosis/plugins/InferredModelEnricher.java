package org.linkedgeodata.osm.osmosis.plugins;

import org.linkedgeodata.util.ITransformer;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class InferredModelEnricher
	implements ITransformer<Model, Model>
{
	private Model schema;
	
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
		Model model = ModelFactory.createRDFSModel(schema, in);		
		// FIXME Create tmp model as out could be the same as in
		// However, maybe that is not necessary, as we retrieve the set
		// of subjects first anyway
		//Model tmp = ModelFactory.createDefaultModel();
		Model tmp = out;
		
		for(Resource subject : in.listSubjects().toSet()) {
			tmp.add(model.listStatements(subject, null, (RDFNode)null));
		}
		
		//out.add(tmp.listStatements());

		return null;
	}

}
