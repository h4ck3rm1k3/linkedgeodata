package org.linkedgeodata.util;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

public class URIUtil
{
	public static MultiMap<String, String> getQueryMap(String query)  
	{  
	    MultiMap<String, String> result = new MultiHashMap<String, String>();  
	    if(query == null)
	    	return result;
	    
	    String[] params = query.split("&");  
	    for (String param : params)  
	    {
	    	String[] kv = param.split("=", 2);
	        String key = kv[0];  
	        String value = kv.length == 2 ? kv[1] : null;

	        result.put(key, value);
	    }  
	    return result;  
	}
	
	public static <K, V> Collection<V> safeGet(MultiMap<K, V> map, K key)
	{
		Collection<V> value = map.get(key);
		if(value == null)
			return Collections.emptyList();
		
		return value;
	}
}
