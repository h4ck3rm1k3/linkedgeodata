package org.jboss.cache.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BulkMapWrapper<K, V>
	extends AbstractMapDecorator<K, V>
	implements IBulkMap<K, V>
{
	public BulkMapWrapper(Map<K, V> decoratee)
	{
		super(decoratee);
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
			decoratee.remove(key);
		}
	}	
}
