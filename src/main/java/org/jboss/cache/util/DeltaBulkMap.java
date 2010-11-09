package org.jboss.cache.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This is a slightly modified version of the DeltaBulkMap found in
 * org.jboss.cache.util.DeltaBulkMap.
 * 
 * This difference is, that there is a bulkGet method.
 * (So that multiple lookups could be packed into one query in the case of
 * a database-backed implementation)
 * 
 */

/**
 * Wraps an existing map, which is not modified, reflecting modifications in an
 * internal modification set.
 * 
 * This is to minimize the amount of data copying, for instance in the case few
 * changes are applied.
 * 
 * Example usage:
 * 
 * <pre>
 * HashMap&lt;String, String&gt; hm = new HashMap&lt;String, String&gt;();
 * hm.put(&quot;a&quot;, &quot;apple&quot;);
 * DeltaBulkMap&lt;String, String&gt; dm = DeltaBulkMap.create(hm);
 * dm.remove(&quot;a&quot;);
 * assert hm.containsKey(&quot;a&quot;);
 * assert !dm.containsKey(&quot;a&quot;);
 * dm.commit();
 * assert !hm.containsKey(&quot;a&quot;);
 * </pre>
 * 
 * @author Elias Ross
 * @param <K>
 *            key type
 * @param <V>
 *            value type
 */
public class DeltaBulkMap<K, V>
		extends AbstractMap<K, V>
		implements IBulkMap<K, V>
{

	/**
	 * Wrapped instance.
	 */
	private IBulkMap<K, V>	original;

	/**
	 * Keys excluded.
	 */
	private Set<K>		exclude;

	/**
	 * Keys changed. This may contain new entries or entries modified.
	 */
	private Map<K, V>	changed	= new HashMap<K, V>();

	/**
	 * Constructs a new DeltaBulkMap.
	 */
	private DeltaBulkMap(IBulkMap<K, V> original, Set<K> exclude)
	{
		if (original == null)
			throw new NullPointerException("original");
		if (exclude == null)
			throw new NullPointerException("exclude");
		this.original = original;
		this.exclude = exclude;
	}

	/**
	 * Creates and returns a DeltaBulkMap for an original map.
	 * 
	 * @param original
	 *            will not be modified, except by {@link #commit()}
	 * @return a new instance
	 */
	public static <K, V> DeltaBulkMap<K, V> create(IBulkMap<K, V> original)
	{
		return new DeltaBulkMap<K, V>(original, new HashSet<K>());
	}

	/**
	 * Creates and returns a DeltaBulkMap for an empty map.
	 * 
	 * @return a new instance
	 */
	public static <K, V> DeltaBulkMap<K, V> create()
	{
		return create(new BulkMapWrapper<K, V>(new HashMap<K, V>(0)));
	}

	/**
	 * Creates and returns a DeltaBulkMap for an original map, excluding some key
	 * mappings.
	 * 
	 * @param original
	 *            will not be modified, except by {@link #commit()}
	 * @param excluded
	 *            entries not to include
	 * @return a new instance
	 */
	public static <K, V> DeltaBulkMap<K, V> excludeKeys(IBulkMap<K, V> original,
			Set<K> exclude)
	{
		return new DeltaBulkMap<K, V>(original, exclude);
	}

	/**
	 * Creates and returns a DeltaBulkMap for an original map, excluding some key
	 * mappings.
	 */
	public static <K, V> DeltaBulkMap<K, V> excludeKeys(IBulkMap<K, V> original,
			K... exclude)
	{
		return excludeKeys(original, new HashSet<K>(Arrays.asList(exclude)));
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet()
	{
		return new AbstractSet<Entry<K, V>>() {

			@Override
			public Iterator<java.util.Map.Entry<K, V>> iterator()
			{
				return new WrappedIterator();
			}

			@Override
			public int size()
			{
				return DeltaBulkMap.this.size();
			}

		};
	}

	@Override
	public int size()
	{
		int size = original.size();
		for (Object o : changed.keySet()) {
			if (!original.containsKey(o))
				size++;
		}
		for (Object o : exclude) {
			if (original.containsKey(o))
				size--;
		}
		return size;
	}

	@Override
	public boolean containsKey(Object key)
	{
		if (exclude.contains(key))
			return false;
		if (changed.containsKey(key))
			return true;
		return original.containsKey(key);
	}

	@Override
	public V get(Object key)
	{
		if (exclude.contains(key))
			return null;
		if (changed.containsKey(key))
			return changed.get(key);
		return original.get(key);
	}

	@Override
	public V put(K key, V value)
	{
		V old;
		if (changed.containsKey(key))
			old = changed.get(key);
		else
			old = original.get(key);
		changed.put(key, value);
		if (exclude.contains(key)) {
			exclude.remove(key);
			return null;
		}
		return old;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key)
	{
		if (changed.containsKey(key)) {
			if (original.containsKey(key))
				exclude.add((K) key);
			return changed.remove(key);
		}
		if (exclude.contains(key)) {
			return null;
		}
		if (original.containsKey(key)) {
			exclude.add((K) key);
			return original.get(key);
		}
		return null;
	}

	/**
	 * Commits the changes to the original map. Clears the list of removed keys.
	 */
	public void commit()
	{
		//original.keySet().removeAll(exclude);
		original.removeAll(exclude);
		original.putAll(changed);
		exclude.clear();
		changed.clear();
	}

	/**
	 * Returns true if the internal map was modified.
	 */
	public boolean isModified()
	{
		return !changed.isEmpty() || !exclude.isEmpty();
	}

	/**
	 * Iterator that skips over removed entries.
	 * 
	 * @author Elias Ross
	 */
	private class WrappedIterator
			implements Iterator<Entry<K, V>>
	{

		private boolean					orig	= true;
		private boolean					nextSet	= false;
		private Entry<K, V>				next;
		private Iterator<Entry<K, V>>	i		= original.entrySet()
														.iterator();

		private boolean redef(Entry<K, V> e)
		{
			K key = e.getKey();
			return exclude.contains(key) || changed.containsKey(key);
		}

		public boolean hasNext()
		{
			if (nextSet)
				return true;
			if (orig) {
				while (true) {
					if (!i.hasNext()) {
						orig = false;
						i = changed.entrySet().iterator();
						return hasNext();
					}
					next = i.next();
					if (!redef(next)) {
						nextSet = true;
						return true;
					}
				}
			}
			if (!i.hasNext())
				return false;
			next = i.next();
			nextSet = true;
			return true;
		}

		public java.util.Map.Entry<K, V> next()
		{
			if (!hasNext())
				throw new NoSuchElementException();
			try {
				return next;
			} finally {
				nextSet = false;
			}
		}

		public void remove()
		{
			DeltaBulkMap.this.remove(next.getKey());
		}

	}

	/**
	 * Returns a debug string.
	 */
	public String toDebugString()
	{
		return "DeltaBulkMap original=" + original + " exclude=" + exclude
				+ " changed=" + changed;
	}

	@Override
	public void clear()
	{
		exclude.addAll(original.keySet());
		changed.clear();
	}

	/**
	 * Returns the original wrapped Map.
	 */
	public Map<K, V> getOriginal()
	{
		return original;
	}

	/**
	 * Sets the original values of this delta map.
	 */
	public void setOriginal(IBulkMap<K, V> original)
	{
		if (original == null)
			throw new NullPointerException("original");
		this.original = original;
	}

	/**
	 * Returns a Map of the entries changed, not including those removed.
	 */
	public Map<K, V> getChanged()
	{
		return changed;
	}

	/**
	 * Returns the entries removed, including entries excluded by the
	 * constructor.
	 */
	public Set<K> getRemoved()
	{
		return exclude;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<K, V> getAll(Collection<?> keys)
	{
		Map<K, V> result = new HashMap<K, V>();
		Set<Object> remainingKeys = new HashSet<Object>(keys);

		for(Object key : keys) {
			if (exclude.contains(key)) {
				continue;
			}
			else if (changed.containsKey(key)) {
				result.put((K)key, changed.get(key));
				continue;
			} else {
				remainingKeys.add(key);
			}
		}

		result.putAll(original.getAll(remainingKeys));
		
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void removeAll(Collection<?> keys)
	{
		Set<K> tmp = new HashSet<K>();
		for(Object key : keys) {
			tmp.add((K)key);
		}
		
		exclude.addAll(tmp);
		
		for(Object key : keys) {
			changed.remove(key);
		}
	}
}
