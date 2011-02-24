package org.linkedgeodata.osm.osmosis.plugins;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.ModelUtil;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class InferenceTest
{
	@Test
	public void inferenceTest()
		throws IOException
	{
		Model schema = ModelUtil.read(new File("data/test/inference/schema.nt"));
		Model instances = ModelUtil.read(new File("data/test/inference/instances.nt"));
		
		ITransformer<Model, Model> transformer = new InferredModelEnricher(schema);
		
		Model result = transformer.transform(instances);
		
		System.out.println("Result");
		System.out.println(ModelUtil.toString(result));
	}
}
