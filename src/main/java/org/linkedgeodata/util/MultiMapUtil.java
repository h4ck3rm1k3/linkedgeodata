package org.linkedgeodata.util;

import java.util.Collection;

import org.apache.commons.collections15.MultiMap;


public class MultiMapUtil
{
	public static <K, V> V getOne(MultiMap<K, V> map, K key, boolean throwOnNonNull)
	{
		V result = getOne(map, key);
		
		if(result == null && throwOnNonNull)
			throw new RuntimeException("Key '" + key + "' enforced, but not present in map");

		return result;
	}

	public static <K, V> V getOne(MultiMap<K, V> map, K key)
	{
		Collection<V> vs = map.get(key);
		if(vs == null)
			return null;
		
		if(vs.size() > 1)
			throw new RuntimeException("Too many elements");
		
		return vs.iterator().next();
	}
}
