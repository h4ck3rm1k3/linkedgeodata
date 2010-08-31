package org.linkedgeodata.util;

import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.iterators.TransformIterator;

/**
 * Note: If the source collection is null, an empty collection
 * will be used instead
 * 
 * @author raven
 *
 * @param <I>
 * @param <O>
 */
public class TransformIterable<I, O>
	implements Iterable<O>
{
	private Iterable<I> src;
	private Transformer<I, O> transformer;
	
	public TransformIterable(Iterable<I> src, Transformer<I, O> transformer)
	{
		if(src == null)
			src = Collections.emptySet();

		this.src = src;
		this.transformer = transformer;
	}

	@Override
	public Iterator<O> iterator()
	{		
		return new TransformIterator<I, O>(src.iterator(), transformer);
	}
	
	public static <I, O> Iterable<O> transformedView(Iterable<I> src, Transformer<I, O> transformer)
	{
		return new TransformIterable<I, O>(src, transformer);
	}
}
