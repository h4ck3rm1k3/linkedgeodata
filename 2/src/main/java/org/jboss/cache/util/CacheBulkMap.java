package org.jboss.cache.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.linkedgeodata.util.sparql.cache.TripleIndexUtils;

public class CacheBulkMap<K, V>
	extends AbstractBulkMap<K, V> 
{
	private IBulkMap<K, V> baseMap;
	private Map<K, V> cachedValues;
	private Set<Object> exclude;
	
	
	public CacheBulkMap(IBulkMap<K, V> baseMap, Map<K, V> cachedValues, Set<Object> exclude)
	{
		this.baseMap = baseMap;
		this.cachedValues = cachedValues;
		this.exclude = exclude;
	}

	public static <K, V> CacheBulkMap<K, V> create(IBulkMap<K, V> baseMap, Integer maxCacheSize, Integer maxExcludeSize)
	{
		return new CacheBulkMap<K, V>(
				baseMap,
				TripleIndexUtils.<K, V>createMap(maxCacheSize),
				TripleIndexUtils.<Object>createSet(maxExcludeSize));
	}
	
	public void putAll(Map<? extends K, ? extends V> map)
	{
		baseMap.putAll(map);
		exclude.removeAll(map.keySet());
		cachedValues.putAll(map);
	}
	
	public void removeAll(Collection<?> keys) {
		for(Object key : keys) {
			cachedValues.remove(key);
			exclude.add(key);
		}
	}
	
	public Map<K, V> getAll(Collection<?> keys)
	{
		Map<K, V> result = new HashMap<K, V>();
		
		Set<K> remainingKeys = new HashSet<K>();

		// Perform a lookup on the cache
		for(Object key : keys) {
			if(exclude.contains(key)) {
				continue;
			} else if(cachedValues.containsKey(key)) {
				result.put((K)key, cachedValues.get(key));
			} else {
				remainingKeys.add((K)key);
			}
		}
		
		Map<K, V> tmp = baseMap.getAll(remainingKeys);
		
		cachedValues.putAll(tmp);
		result.putAll(tmp);
		
		return result;
	}
}
