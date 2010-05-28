package org.linkedgeodata.util;

import java.util.Arrays;
import java.util.Iterator;

public class StringUtil
{
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
}
