package org.jboss.cache.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class AbstractMapDecorator<K, V>
	implements Map<K, V>
{
	protected Map<K, V> decoratee;
	
	public AbstractMapDecorator(Map<K, V> decoratee)
	{
		this.decoratee = decoratee;
	}

	@Override
	public void clear()
	{
		decoratee.clear();
	}

	@Override
	public boolean containsKey(Object key)
	{
		return decoratee.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value)
	{
		return decoratee.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet()
	{
		return decoratee.entrySet();
	}

	@Override
	public V get(Object key)
	{
		return decoratee.get(key);
	}

	@Override
	public boolean isEmpty()
	{
		return decoratee.isEmpty();
	}

	@Override
	public Set<K> keySet()
	{
		return decoratee.keySet();
	}

	@Override
	public V put(K key, V value)
	{
		return decoratee.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m)
	{
		decoratee.putAll(m);		
	}

	@Override
	public V remove(Object key)
	{
		return decoratee.remove(key);
	}

	@Override
	public int size()
	{
		return decoratee.size();
	}

	@Override
	public Collection<V> values()
	{
		return decoratee.values();
	}
}
