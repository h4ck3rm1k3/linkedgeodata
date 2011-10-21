package org.linkedgeodata.util;

import java.util.Comparator;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;

public class StatementPrettyComparator
	implements Comparator<Statement>
{
	private Comparator<RDFNode> nodeComparator = new RDFNodePrettyComparator();
	
	@Override
	public int compare(Statement a, Statement b)
	{
		int d = nodeComparator.compare(a.getSubject(), b.getSubject());
		if(d != 0)
			return d;
		
		
		d = nodeComparator.compare(a.getPredicate(), b.getPredicate());
		if(d != 0)
			return d;
		
		
		return nodeComparator.compare(a.getObject(), b.getObject());
	}

}
