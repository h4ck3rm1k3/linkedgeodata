package org.linkedgeodata.scripts;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;


/*
class PropertyMapDAO
{
	public Set<Tag> getDependencies() {
		
	}
	
	public Set<URI> getPropertiesByTags(Set<Tag> tags)
}
*/

public class VocabularyLauncher
{
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");

		/*
		Tag tag = new Tag("hi", null);
		Tag tag2 = new Tag("hi", null);
		
		System.out.println("Equals? " + tag.equals(tag2));
		
		Map<String, String> map = new HashMap<String, String>();
		map.put(null, "A null key");
		
		System.out.println(map.get(null));
		System.out.println("tag k:" + tag.getKey());
		System.out.println("tag v:" + tag.getValue());
		*/
		
		/*
		File file = new File("./data/test/test.groovy");
		if(!file.exists())
			throw new FileNotFoundException();
		* /
		Model model = ModelFactory.createDefaultModel();
		StmtIterator it = model.getProperty("").listProperties(RDFS.range);
		while(it.hasNext()) {
			Statement stmt = it.next();
			stmt.getObject();
		}
		Resource x = null;
		RDFNode y = null;
		y = x;
		*/
		//XSD.xin
		//XSD.x
		//x = y;
		//model.getPr
//XSD.integer
		
		
		String[] roots = new String[] { "./src/main/groovy" };
		GroovyScriptEngine gse = new GroovyScriptEngine(roots);
		Binding binding = new Binding();
		binding.setVariable("input", "hi");
		gse.run("vocabulary.groovy", binding);

		/*
		boolean b = XPathUtil.evalPredicate(doc,
				"//multimap[//k[contains(@value, 'type') and //v[contains(@value, 'node')]]]");
		*/
	}
}
