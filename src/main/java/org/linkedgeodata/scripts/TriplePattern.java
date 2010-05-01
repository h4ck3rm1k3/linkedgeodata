package org.linkedgeodata.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TriplePattern
{
	public Pattern s;
	public Pattern p;
	public Pattern o;

	public TriplePattern(String s, String p, String o)
	{
		this.s = Pattern.compile(s);
		this.p = Pattern.compile(p);
		this.o = Pattern.compile(o);
	}
	
	public List<String> match(String x, String y, String z)
	{
		Matcher[] ms = new Matcher[]{s.matcher(x), p.matcher(y), o.matcher(z)};

		for(Matcher m : ms) {
			if(!m.matches())
				return null;
		}

		List<String> result = new ArrayList<String>();
		for(Matcher m : ms) {
			System.out.println("groupCount: " + m.groupCount());
			for(int i = 1; i <= m.groupCount(); ++i) {
				result.add(m.group(i));
			}
		}
		
		return result;
	}
}
