package org.linkedgeodata.jtriplify.methods;

public class DefaultArgsInvocable
	implements IInvocable
{
	private IInvocable delegate;
	private Object[] defaultArgs;

	public DefaultArgsInvocable(IInvocable delegate, Object... defaultArgs)
	{
		if(delegate == null)
			throw new NullPointerException();
		
		this.delegate = delegate;
		this.defaultArgs = defaultArgs;
	}
	
	@Override
	public Object invoke(Object... args)
		throws Exception
	{
		int n = Math.max(defaultArgs.length, args.length);
		Object[] effectiveArgs = new Object[n];
	
		for(int i = 0; i < args.length; ++i) {
			effectiveArgs[i] = args[i];
		}
		
		for(int i = args.length; i < defaultArgs.length; ++i) {
			effectiveArgs[i] = defaultArgs[i];
		}
		
		Object result = delegate.invoke(effectiveArgs);
		
		return result;
	}

}
