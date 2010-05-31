package org.linkedgeodata.util;

import org.apache.commons.collections15.Transformer;

/**
 * Interface intended for directly transforming into a target collection.
 * 
 * 
 * @author Claus Stadler
 *
 * @param <I>
 * @param <O>
 */
public interface ITransformer<I, O>
	extends Transformer<I, O>
{
	O transform(O out, I in);
}
