package org.linkedgeodata.i18n.gettext;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class EntityResolverImpl
		implements IEntityResolver
{
	private static final Logger logger = Logger.getLogger(EntityResolverImpl.class);
	
	private Map<List<String>, Resource>	map	= new HashMap<List<String>, Resource>();

	public EntityResolverImpl() throws Exception
	{
		URL url = new URL("http://linkedgeodata.org/vocabulary/core");
		InputStream in = null;
		try {
			in = url.openStream();

			Model model = ModelFactory.createDefaultModel();
			model.read(in, "", "N-TRIPLE");

			process(model);
		} finally {
			if (in != null)
				in.close();
		}
	}

	private void process(Model model)
	{
		Property subClassOf = model.getProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf");
		Iterator<Statement> it = model.listStatements((Resource)null, subClassOf, (RDFNode)null);
	
		int prefixLength = "http://linkedgeodata.org/vocabulary#".length();
		
		while(it.hasNext()) {
			Statement stmt = it.next();
			
			Resource uri = stmt.getSubject();
			
			String v = stmt.getSubject().getURI().substring(prefixLength);
			String k = stmt.getObject().asNode().getURI().substring(prefixLength);
			
			k = k.trim().toLowerCase();
			v = v.trim().toLowerCase();
			
			List<String> list = new ArrayList<String>();
			list.add(k);
			list.add(v);
			
			logger.trace("Loaded Mapping: (" + k + ", " + v + ") -> " + uri); 
			
			map.put(list, uri);
		}
	}

	@Override
	public Resource resolve(String key, String value)
	{		
		String k = key.trim().toLowerCase();
		String v = value.trim().toLowerCase();
		
		List<String> list = new ArrayList<String>();
		list.add(k);
		list.add(v);
		
		Resource result = map.get(list);

		return result;
	}
}