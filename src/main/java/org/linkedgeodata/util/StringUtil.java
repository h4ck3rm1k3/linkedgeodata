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
}
