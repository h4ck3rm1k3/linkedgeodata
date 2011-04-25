package org.linkedgeodata.evaluation;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.function.FunctionRegistry;
import com.hp.hpl.jena.util.FileManager;

public class VirtuosoExtensions
{
	public static void load() {
			//FunctionRegistry.get().put("http://example.org/function#myFunction", MyFunc.class);
			Query query = QueryFactory.create("Prefix bif:<http://example.org/function#> Select * From <http://linkedgeodata.org> { ?s ?p ?o . FILTER(bif:st_intersects(?o, bif:st_point (4.892222, 52.373056), 5)) . Filter(Regex(?s, 'test', 'i')) . }");
	}
}
