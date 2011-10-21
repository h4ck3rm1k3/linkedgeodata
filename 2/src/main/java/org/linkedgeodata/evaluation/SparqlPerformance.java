package org.linkedgeodata.evaluation;

import org.aksw.commons.sparql.core.SparqlEndpoint;
import org.aksw.commons.sparql.core.impl.HttpSparqlEndpoint;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;


public class SparqlPerformance
{
	public static void main(String[] args) {
		SparqlEndpoint sparqlEndpoint = new HttpSparqlEndpoint("http://dbpedia.org/sparql", "http://dbpedia.org");
		
		//sparqlEndpoint = (SparqlEndpoint)MonProxyFactory.monitor(sparqlEndpoint);
		
		
		Monitor mon = MonitorFactory.start();
		ResultSet rs = sparqlEndpoint.executeSelect("Select * {?s ?p ?o . } Limit 500");
		
		while(rs.hasNext()) {
			QuerySolution qs = rs.next();
			//System.out.println(rs.next());
		}
		mon.stop();
		
		System.out.println(mon);

		System.out.println("Done");
	}
}
