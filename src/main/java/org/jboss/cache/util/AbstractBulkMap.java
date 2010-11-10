package org.jboss.cache.util;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

public class AbstractBulkMap<K, V>
	extends AbstractMap<K, V>
	implements IBulkMap<K, V>
{
	@Override
	public Set<Map.Entry<K, V>> entrySet()
	{
		throw new NotImplementedException();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<K, V> getAll(Collection<?> keys)
	{
		Map<K, V> result = new HashMap<K, V>();
		
		for(Object key : keys) {
			if(containsKey(key)) {
				result.put((K)key, get(key));
			}
		}
		
		return result;		
	}


	@Override
	public void removeAll(Collection<?> keys)
	{
		for(Object key : keys) {
			remove(key);
		}
	}
}
