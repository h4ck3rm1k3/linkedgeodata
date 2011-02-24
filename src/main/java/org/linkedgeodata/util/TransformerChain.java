package org.linkedgeodata.util;

/**
 * Chains transformers together.
 * 
 * TODO This class chains in place "transformations" (I should have used a
 * different term as there is quite a difference between the concepts of
 * a transformation that retains the state of a source object and one that does not)
 * 
 * @author raven
 *
 * @param <I>
 * @param <T>
 * @param <O>
 */
public class TransformerChain<I, T, O>
	implements ITransformer<I, O>
{
	private ITransformer<I, T> a;
	private ITransformer<? super T, O> b;
	
	public TransformerChain(ITransformer<I, T> a, ITransformer<? super T, O> b) {
		this.a = a;
		this.b = b;
	}
	
	public static <I, T, O> TransformerChain<I, T, O> create(ITransformer<I, T> a, ITransformer<? super T, O> b) {
		return new TransformerChain<I, T, O>(a, b);
	}
	
	
	@Override
	public O transform(I in)
	{
		return b.transform(a.transform(in));
	}

	@Override
	public O transform(O out, I in)
	{
		return b.transform(out, a.transform(in)); 
	}
}
