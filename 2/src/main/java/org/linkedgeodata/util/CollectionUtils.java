package org.linkedgeodata.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CollectionUtils
{
	@SuppressWarnings("unchecked")
	public static <T> List<T> filterByType(Iterable<?> col, Class<T> clazz)
	{
		List<T> result = new ArrayList<T>();
		for(Object item : col) {
			if(clazz.isAssignableFrom(item.getClass()))
				result.add((T)col);
		}
		
		return result;
	}
	
	public static <T> List<List<T>> chunk(Iterable<T> col, int batchSize)
	{
		List<List<T>> result = new ArrayList<List<T>>();
		
		List<T> chunk = new ArrayList<T>();
		
		Iterator<T> it = col.iterator();
		while(it.hasNext()) {
			chunk.add(it.next());

			if(chunk.size() >= batchSize || !it.hasNext()) {
				result.add(chunk);
				
				if(it.hasNext())
					chunk = new ArrayList<T>();
			}
		}

		return result;
	}
}
