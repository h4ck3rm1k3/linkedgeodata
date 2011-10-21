package org.linkedgeodata.jtriplify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.linkedgeodata.jtriplify.methods.FunctionUtil;
import org.linkedgeodata.jtriplify.methods.IInvocable;


public class RegexInvocationContainer
{
	private static final Logger logger = Logger.getLogger(RegexInvocationContainer.class);

	private Map<Pattern, ICPair> patternToInvocable = new HashMap<Pattern, ICPair>();
	
	public void put(String regex, IInvocable invocable, Object ...argMap)
	{
		Pattern pattern = Pattern.compile(regex);
		
		patternToInvocable.put(pattern, new ICPair(invocable, argMap));
	}
	
	private static Object invoke(Matcher matcher, IInvocable invocable, Object[] argMap)
		throws Exception
	{
		logger.info("Invoking: " + invocable);
		int groupCount = matcher.groupCount() + 1;
		Object[] matches = new Object[groupCount];
		
		for(int i = 0; i < groupCount; ++i) {
			matches[i] = matcher.group(i);
		}
		
		Object[] args = new Object[argMap.length];
		for(int i = 0; i < argMap.length; ++i) {
			
			if(argMap[i] != null) {
				String argStr = argMap[i].toString();
				
				if(argStr.startsWith("$")) {
					String indexStr = argStr.substring(1);
					int index = Integer.parseInt(indexStr);
					
					args[i] = matches[index];
					continue;
				}
			}
		
			args[i] = argMap[i];
		}

		logger.debug("Args: " + Arrays.toString(args) + ", Types: " + Arrays.toString(FunctionUtil.getTypes(args)));
		
		Object result = invocable.invoke(args);
		return result;
	}
	
	public Object invoke(String arg)
		throws Exception
	{
		for(Map.Entry<Pattern, ICPair> entry : patternToInvocable.entrySet()) {
			Pattern pattern = entry.getKey();
			ICPair icPair = entry.getValue();
			
			Matcher matcher = pattern.matcher(arg);
			if(matcher.matches()) {
				logger.info("Value '" + arg + "' matched the pattern '" + pattern + "'");

				return invoke(matcher, icPair.getInvocable(), icPair.getArgMap());
			}
		}
		
		logger.info("No match found for '" + arg + "'");
		
		return null;
	}
}
