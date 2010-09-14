package org.linkedgeodata.osm.osmosis.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.TransformIterable;
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
	private Map<String, Rule> keyToRule = new HashMap<String, Rule>();
	private boolean inverted = false;
	
	public TagFilter()
	{
	}
	
	public TagFilter(boolean inverted)
	{
		this.inverted = inverted;
	}
	
	
	public Map<String, Rule> getRuleMap()
	{
		return keyToRule;
	}
	
	public boolean isInverted()
	{
		return inverted;
	}
	
	/**
	 * TODO Somehow deal with negation of filters 
	 * 
	 * @param tagFilter
	 * @param kName
	 * @param vName
	 * @return
	 */
	public static String createFilterSQL(TagFilter tagFilter, String kName, String vName)
	{
		Map<String, Rule> keyToRule = tagFilter.getRuleMap();
		
	//	-tf "k NOT IN ('created_by','ele','time','layer','source','tiger:tlid','tiger:county','tiger:upload_uuid','attribution','source_ref','KSJ2:coordinate','KSJ2:lat','KSJ2:long','KSJ2:curve_id','AND_nodes','converted_by','TMC:cid_58:tabcd_1:LocationCode','TMC:cid_58:tabcd_1:LCLversion','TMC:cid_58:tabcd_1:NextLocationCode','TMC:cid_58:tabcd_1:PrevLocationCode','TMC:cid_58:tabcd_1:LocationCode')"
		
	//	-ef "(filter.k IN ('highway', 'barrier', 'power') OR (filter.k = 'railway' AND filter.v NOT IN ('station')))"

		List<String> parts = new ArrayList<String>();
		
		List<String> withoutException = new ArrayList<String>();

		// Gather all conditions without exceptions
		for(Map.Entry<String, Rule> entry : keyToRule.entrySet()) {
			if(entry.getValue().getExceptions().isEmpty())
				withoutException.add(entry.getKey());
		}

		//String notIn = tagFilter.isInverted() ? " IN " : " NOT IN ";
		
		if(!withoutException.isEmpty()) {
			String part = "(" + kName + " IN (" + PostGISUtil.escapedList(withoutException) + "))"; 
			parts.add(part);
		}
		
		for(Map.Entry<String, Rule> entry : keyToRule.entrySet()) {
			if(entry.getValue().getExceptions().isEmpty())
				continue;
			
			String part = "(" + kName + "='" + SQLUtil.escapePostgres(entry.getKey()) + "' AND NOT " + vName + " IN (";

			part += PostGISUtil.escapedList(entry.getValue().getExceptions());
			
			part += "))";
			parts.add(part);
		}
		
		String result = "(" + StringUtil.implode(" OR ", parts) + ")";
		
		if(!tagFilter.isInverted())
			result = "NOT " + result;
		
		return result;
	}
	
	
	//*
	public static void main(String[] args) throws IOException {
		File file = new File("/home/raven/Projects/Current/Eclipse/GoogleCodeLinkedGeoData/data/lgd/dump/ElementsEntityFilter.txt");
		//File file = new File("/home/raven/Projects/Current/Eclipse/GoogleCodeLinkedGeoData/data/lgd/dump/ElementsTagFilter.txt");
		BufferedReader reader = new BufferedReader(new FileReader(file));
	
		TagFilter tf = new TagFilter(true);
		tf.read(reader);
		
		//String sql = createFilterSQL(tf, "k", "v");
		String sql = createFilterSQL(tf, "filter.k", "filter.v");
		System.out.println(sql);
		
	}
	//*/
	
	public void load(File file)
		throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		read(reader);
	}
	
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
		Pattern stringPattern = Pattern.compile("'((\\\\\\'|[^'])*)'");
		
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
