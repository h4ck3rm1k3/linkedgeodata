package org.linkedgeodata.jtriplify.methods;

import java.util.Map;

public class Pair<K, V>
	implements Map.Entry<K, V>
{
	private final K key;
	private final V value;

	public Pair(K key, V value)
	{ 
		this.key = key;
		this.value = value;   
	}
 
	public K getKey()
	{
		return key;
	}
 
	public V getValue()
	{
		return value;
	}
 
	public String toString()
	{ 
		return "(" + key + ", " + value + ")"; 
	}

	@Override
	public V setValue(V arg0)
	{
		throw new UnsupportedOperationException();
	}
}
