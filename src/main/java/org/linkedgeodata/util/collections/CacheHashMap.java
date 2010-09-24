package org.linkedgeodata.util.collections;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * 
 * @author raven
 *
 * @param <K>
 * @param <V>
 */
public class CacheHashMap<K, V>
	extends HashMap<K, V>
{
	private static final long	serialVersionUID	= 6215902001517077010L;
	private CacheSet<K> cacheSet = new CacheSet<K>();

	@Override
	public V get(Object item)
	{
		cacheSet.renew(item);
		return super.get(item);		
	}


	@Override
	public V put(K key, V value)
	{
		K removed = cacheSet.addAndGetRemoved(key);
		if(removed != null)
			super.remove(removed);
		
		return super.put(key, value);
	}


	@Override
	public void putAll(Map<? extends K, ? extends V> map)
	{
		for(Map.Entry<? extends K, ? extends V> item : map.entrySet())
			put(item.getKey(), item.getValue());
	}


	@Override
	public V remove(Object key)
	{
		cacheSet.remove(key);
		return super.remove(key);
	}
}
