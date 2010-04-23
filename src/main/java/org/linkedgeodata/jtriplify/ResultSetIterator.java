package org.linkedgeodata.jtriplify;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.linkedgeodata.util.SinglePrefetchIterator;

class ResultSetIterator
	extends SinglePrefetchIterator<List<?>>
{
	private ResultSet rs;
	
	public ResultSetIterator(ResultSet rs)
	{
		this.rs = rs;
	}
	
	@Override
	protected List<?> prefetch()
		throws Exception
	{
		if(!rs.next()) {
			return finish();
		}
		
		List<Object> result = new ArrayList<Object>();
		for(int i = 1; i <= rs.getMetaData().getColumnCount(); ++i) {
			result.add(rs.getObject(i));
		}
		
		return result;
	}
}
