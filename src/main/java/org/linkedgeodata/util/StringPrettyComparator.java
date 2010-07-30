package org.linkedgeodata.util;

import java.util.Comparator;

/**
 * Identifies integers in strings and applies integer comparision for those
 * parts.
 *
 * 
 * @author raven
 *
 */
public class StringPrettyComparator
	implements Comparator<String>
{
	boolean isDigitPrefix(String s)
	{
		if(s.isEmpty())
			return false;
	
		boolean result = Character.isDigit(s.charAt(0)); 
		return result;
	}
	
	String getPrefix(String s, boolean digitMode)
	{
		if(s.isEmpty())
			return "";
		
		int i = 0;
		for(i = 0; i < s.length(); ++i) {
			if(Character.isDigit(s.charAt(i)) != digitMode)
				break;
		}
		//while(Character.isDigit(s.charAt(i)) == digitMode && (i < s.length() - 1))
			//++i;

		String part = s.substring(0, i);

		return part;
	}
	
	
	
	boolean isDigitSuffix(String s)
	{
		if(s.isEmpty())
			return false;
	
		boolean result = Character.isDigit(s.charAt(s.length() - 1)); 
		return result;
	}

	String getSuffix(String s, boolean digitMode)
	{
		if(s.isEmpty())
			return "";
		
		int i = s.length() - 1;
		
		for(; Character.isDigit(s.charAt(i)) == digitMode && i > 0; --i);

		String part = s.substring(i);

		return part;
	}
	
	@Override
	public int compare(String a, String b)
	{
		while(true) {
			if(a.isEmpty() && b.isEmpty())
				return 0;
			
			// Sort empty strings before non-empty ones
			int da = a.isEmpty() ? -1 : 0;
			int db = b.isEmpty() ? 1 : 0;
			
			int d = db + da;
			if(d != 0)
				return d;
			
			
			// Sort values before strings
			da = isDigitPrefix(a) ? -1 : 0;
			db = isDigitPrefix(b) ? 1 : 0;
			
			d = db + da;
			if(d != 0)
				return d;
			
			String sa = getPrefix(a, da != 0);
			String sb = getPrefix(b, db != 0);
	
			d = (da != 0)
				? ((Long)Long.parseLong(sa)).compareTo(Long.parseLong(sb))
				: sa.compareTo(sb);
				
			if(d != 0)
				return d;
				
			a = a.substring(sa.length());
			b = b.substring(sb.length());
			//a = a.substring(0, a.length() - sa.length());
			//b = b.substring(0, b.length() - sb.length());
		}
	}
}
