package org.linkedgeodata.util;

import java.util.Arrays;
import java.util.Iterator;

public class StringUtil
{	
	public static String strip(String str, String ... chars)
	{
		for(String c : chars) {
			if(str.length() < 2)
				return str;
			
			if(str.startsWith(c) && str.endsWith(c))
				str = str.substring(1, str.length() - 1);
		}
		
		return str;
	}

	
	public static <T> T coalesce(T ...args)
	{
		for(T arg : args) {
			if(arg != null) {
				return arg;
			}
		}
		
		return null;
	}

	public static String ucFirst(String str)
	{
		return str.length() == 0
			? ""
			: str.substring(0,1).toUpperCase() + str.substring(1);
	}

	public static String lcFirst(String str)
	{
		return str.length() == 0
			? ""
			: str.substring(0,1).toLowerCase() + str.substring(1); 
	}
	
	public static String implode(Object separator, Object ... items)
	{
		return implode(separator, Arrays.asList(items));
	}

	/**
	 * Returns null if the iterator is null.
	 * If an empty string is desired, use StringUtil.trim() 
	 * 
	 * @param separator
	 * @param it
	 * @return
	 */
	public static String implode(Object separator, Iterator<? extends Object> it)
	{
		String result = "";
		
		if(it == null)
			return null;
		
		while(it.hasNext()) {
			result += it.next();
			
			if(it.hasNext())
				result += separator;
		}
		
		return result;		
	}
	
	public static String implode(Object separator, Iterable<? extends Object> items)
	{
		return implode(separator, items.iterator());
	}

	
	public static String toCamelCase(String s)
	{
		int offset = 0;
		String result = "";
		for(;;) {
			int newOffset = s.indexOf('_', offset);
			if(newOffset == -1) {
				result += StringUtil.ucFirst(s.substring(offset));
				break;
			}
			
			result += StringUtil.ucFirst(s.substring(offset + 1));
			offset = newOffset;
		}
		
		return result;
	}
}
