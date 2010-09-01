package org.linkedgeodata.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections15.Predicate;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;


class Rule
{
	private boolean keepMode;
	private Set<String> exceptions;
	
	public Rule(boolean keepMode, Set<String> exceptions)
	{
		this.keepMode = keepMode;
		this.exceptions = exceptions;
	}

	public boolean isKeepMode()
	{
		return keepMode;
	}

	public Set<String> getExceptions()
	{
		return exceptions;
	}

	@Override
	public String toString()
	{
		return "Rule [keepMode=" + keepMode + ", exceptions=" + exceptions
				+ "]";
	}
	
}

/**
 * Format
 * 
 * 'highway'
 * '
 * 
 * <mode> <key> <values>
 * 
 * -'railway' 'station' '
 * 
 * keep tag railway, unless its station
 * remove tag, unless its station
 * 
 * @author raven
 *
 */
public class TagFilter
	implements Predicate<Tag>
{
	Map<String, Rule> keyToRule = new HashMap<String, Rule>();
	
	//*
	public static void main(String[] args) throws IOException {
		//File file = new File("/home/raven/Projects/Current/Eclipse/GoogleCodeLinkedGeoData/data/lgd/dump/ElementsEntityFilter.txt");
		File file = new File("/home/raven/Projects/Current/Eclipse/GoogleCodeLinkedGeoData/data/lgd/dump/ElementsTagFilter.txt");
		BufferedReader reader = new BufferedReader(new FileReader(file));
	
		TagFilter tf = new TagFilter();
		tf.read(reader);
	}
	//*/
	
	public static TagFilter create(File file)
		throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		TagFilter result = new TagFilter();
		result.read(reader);
		
		return result;
	}
	
	private void read(BufferedReader reader)
		throws IOException
	{
		Pattern stringPattern = Pattern.compile("'([^']*)'");
		
		String line;
		while(null != (line = reader.readLine())) {
			line = line.trim();
			
			if(line.isEmpty())
				continue;
			System.out.println("Line: " + line);
			
			Matcher m = stringPattern.matcher(line);

			m.find();
			
			String key = m.group(1);

			Set<String> values = new HashSet<String>();
			while(m.find()) {
				String value = m.group(1);
				values.add(value);
			}
			Rule rule = new Rule(false, values);
			
			keyToRule.put(key, rule);
		}
		
		System.out.println(keyToRule);
	}

	
	@Override
	public boolean evaluate(Tag tag)
	{
		Rule rule = keyToRule.get(tag.getKey());
		
		if(rule == null)
			return true;
		
		boolean result = rule.getExceptions().contains(tag.getValue());
		
		return result;
	}
}
