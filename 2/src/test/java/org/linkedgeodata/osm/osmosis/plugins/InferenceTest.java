package org.linkedgeodata.osm.osmosis.plugins;

import java.io.File;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Test;
import org.linkedgeodata.core.ILGDVocab;
import org.linkedgeodata.core.LGDVocab;
import org.linkedgeodata.util.ITransformer;
import org.linkedgeodata.util.ModelUtil;
import org.mindswap.pellet.jena.PelletReasoner;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.vocabulary.RDF;

public class InferenceTest
{
	//@Test
	public void reasonerCreationBenchmarkTest()
	{		
		System.out.println("public void reasonerCreationBenchmarkTest()");
		for(int i = 0; i < 5; ++i) {
			StopWatch sw = new StopWatch();
			sw.start();

			int count = 0;
			while(sw.getTime() < 1000) {
				Reasoner reasoner = new PelletReasoner();
				//reasoner.
				++count;
				//System.out.println(sw.getLastTaskTimeMillis());
			}
			sw.stop();
			
			System.out.printf("Run %d: intances = %d\n", i, count);
			//sw.prettyPrint();
		}
	}
	
	@Test
	public void inferenceTest()
		throws Exception
	{
		Model schema = ModelUtil.read(new File("data/test/inference/schema.n3"));
		Model instances = ModelUtil.read(new File("data/test/inference/instances.n3"));
		
		ILGDVocab vocab = new LGDVocab();
		
		//ITransformer<Model, Model> transformer = new InferredModelEnricher(schema);
		ITransformer<Model, Model> transformer = new TransitiveInferredModelEnricher(vocab, schema);
		
		Model result = transformer.transform(instances);
		
		System.out.println(RDF.Seq.toString());
		System.out.println("Result");
		//System.out.println(ModelUtil.toString(result, "N-TRIPLES"));
		System.out.println(ModelUtil.toString(result));
	}
}
