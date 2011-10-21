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
import org.junit.Assert;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.SQLUtil;
import org.linkedgeodata.util.StringUtil;
import org.linkedgeodata.util.TransformIterable;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;


class Rule
{
	// Either: Allow everything of that key except for the exception
	// Or: Allow nothing from that key except for the exceptions (Allow only the given values for the key) 
	private boolean blacklist = true;
	private Set<String> exceptions;
	
	public Rule(boolean keepMode, Set<String> exceptions)
	{
		this.blacklist = keepMode;
		this.exceptions = exceptions;
	}

	public boolean isKeepMode()
	{
		return blacklist;
	}

	public Set<String> getExceptions()
	{
		return exceptions;
	}

	@Override
	public String toString()
	{
		return "Rule [keepMode=" + blacklist + ", exceptions=" + exceptions
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
	
	// Whether the filter is to be interpreted as a white or blacklist
	private boolean blacklist = false;
	
	public TagFilter()
	{
	}
	
	public TagFilter(boolean inverted)
	{
		this.blacklist = inverted;
	}
	
	
	public Map<String, Rule> getRuleMap()
	{
		return keyToRule;
	}
	
	public boolean isInverted()
	{
		return blacklist;
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

			Rule rule = entry.getValue();
			String valueNot = rule.isKeepMode() ? " NOT" : "";
			
			String part = "(" + kName + "='" + SQLUtil.escapePostgres(entry.getKey()) + "' AND " + vName + valueNot + " IN (";

			part += PostGISUtil.escapedList(entry.getValue().getExceptions());
			
			part += "))";
			parts.add(part);
		}
		
		String result = "(" + StringUtil.implode(" OR ", parts) + ")";
		
		if(tagFilter.isInverted())
			result = "NOT " + result;
		
		return result;
	}
	
	
	//*
	public static void main(String[] args) throws IOException {
		File file = new File("/home/raven/Projects/Current/Eclipse/GoogleCodeLinkedGeoData/config/LiveSync/LiveEntityFilter.txt.dist");
		//File file = new File("/home/raven/Projects/Current/Eclipse/GoogleCodeLinkedGeoData/data/lgd/dump/ElementsTagFilter.txt");
		BufferedReader reader = new BufferedReader(new FileReader(file));
	
		TagFilter tf = new TagFilter(true);
		tf.read(reader);
		
		//String sql = createFilterSQL(tf, "k", "v");
		String sql = createFilterSQL(tf, "filter.k", "filter.v");
		System.out.println(sql);
		
//		Assert.assertTrue(tf.evaluate(new Tag("foo", "bar")));
//		Assert.assertTrue(tf.evaluate(new Tag("railway", "station")));

		Assert.assertFalse(tf.evaluate(new Tag("power", "foo")));
		Assert.assertFalse(tf.evaluate(new Tag("railway", "foo")));
		Assert.assertFalse(tf.evaluate(new Tag("railway", "foo")));

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
		
		boolean areKeysBlackListed = true;
		
		while(null != (line = reader.readLine())) {
			line = line.trim();

			if(line.equalsIgnoreCase("whitelist")) {
				areKeysBlackListed = false;
				continue;
			}
			
			
			boolean areValuesBlacklisted = line.startsWith("-");
			
			if(line.isEmpty())
				continue;
			System.out.println("Line: " + line);
			
			Matcher m = stringPattern.matcher(line);

			if(!m.find()) {
				continue;
			}
			
			String key = m.group(1);

			Set<String> values = new HashSet<String>();
			while(m.find()) {
				String value = m.group(1);
				values.add(value);
			}
			Rule rule = new Rule(areValuesBlacklisted, values);
			
			keyToRule.put(key, rule);
		}
		
		
		this.blacklist = areKeysBlackListed;
		System.out.println(keyToRule);
	}

	@Override
	public boolean evaluate(Tag tag)
	{
		boolean result = evaluateAsWhiteList(tag);
		
		if(this.blacklist)
			result = !result;
			
		return result;
	}
	
	protected boolean evaluateAsWhiteList(Tag tag)
	{
		Rule rule = keyToRule.get(tag.getKey());
		
		if(rule == null)
			return false;
		
		boolean result = rule.getExceptions().contains(tag.getValue());

		if(rule.isKeepMode())
			result = !result;

		return result;
	}
}
