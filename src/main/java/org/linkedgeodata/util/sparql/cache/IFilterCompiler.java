package org.linkedgeodata.util.sparql.cache;

import java.util.Collection;
import java.util.List;


interface IFilterCompiler
{
	List<String> compileFilter(Collection<List<Object>> keys, List<String> columnNames);
	
	//List<String> compileFilter(Collection<TripleMatch> patterns);
}
