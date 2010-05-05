package org.linkedgeodata.util;

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

}
