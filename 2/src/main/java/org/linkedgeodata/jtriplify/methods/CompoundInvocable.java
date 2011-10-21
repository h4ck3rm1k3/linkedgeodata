package org.linkedgeodata.jtriplify.methods;

import java.util.Arrays;

public class CompoundInvocable
	implements IInvocable
{
	private final IInvocable main;
	private final IInvocable[] coercions;

	
	public CompoundInvocable(IInvocable main, IInvocable[] coercions)
	{
		this.main = main;
		this.coercions = coercions;
	}
	
	@Override
	public Object invoke(Object... args)
		throws Exception
	{
		if(coercions == null)
			return main.invoke(args);
			
		Object[] transformed = new Object[args.length];
		for(int i = 0; i < args.length; ++i) {
			Object arg = args[i];
			IInvocable coercion = coercions[i];
			
			transformed[i] = (coercion == null) ? arg : coercion.invoke(arg);
		}
		
		return main.invoke(transformed);
	}
	
	@Override
	public String toString()
	{
		return main + " - " + Arrays.toString(coercions);
	}
}
