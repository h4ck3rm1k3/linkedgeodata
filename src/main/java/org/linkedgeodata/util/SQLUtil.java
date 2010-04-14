package org.linkedgeodata.util;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.ctc.wstx.util.StringUtil;


//http://stackoverflow.com/questions/1497569/how-to-execute-sql-script-file-using-jdbc
public class SQLUtil
{
	private static final Logger logger = Logger.getLogger(SQLUtil.class);
	
	public static String placeHolder(int n, int m)
	{
		String part = "(";
		for(int i = m; i >= 0; --i) {
			part += "?";
			
			if(i != 0)
				part += ", ";
		}
		part += ")";
		
		String result = "";
		for(int j = n; j >= 0; --j) {
			result += part;
			
			if(j != 0)
				result += ", ";
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param in
	 */
	/*
	public static void importSQLScript(InputStream in, ISimpleConnection conn)
		throws Exception
	{
		String data = StringUtil.toString(in);
		
		data = data.replaceAll("/\\*.*\\*<remove>/", "");
		
		String[] sqls = data.split(";");
		
		for(String sql : sqls) {
			sql = sql.trim();
			
			if(sql.isEmpty())
				continue;
			
			logger.debug("Executing SQL Statement:\n" + sql);
			conn.update(sql);
		}
	}
	
	
	public static String multiImplode(Collection<? extends Collection<?>> rows)
	{
		MultiRowString result = new MultiRowString();

		for(Collection<?> row : rows) {
			result.nextRow();

			for(Object o : row) {
				result.add(o);
			}
			
			result.endRow();
		}
		
		return result.toString();
	}
	
	
	public static String implodeEscaped(String separator, Collection<Object> items)
	{
		return StringUtil.implode(
				separator,
				new TransformIterator<Object, String>(
						items.iterator(),
						SQLEscapeTransformer.getInstance()));
	}
	*/
	/**
	 * Returns the 1st column of the first row or null of there is no row.
	 * Also throws exception if there is more than 1 row and 1 column.
	 * 
	 * @param connection
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T single(ResultSet rs, Class<T> clazz)
		throws SQLException
	{
		if(rs.getMetaData().getColumnCount() != 1)
			throw new RuntimeException("only a single column expected");
		
		T result = null;

		if(rs.next()) {
			Object o = rs.getObject(1);;
			//System.out.println("Result = " + o);
			result = (T)o;

			if(rs.next()) 
				throw new RuntimeException("only at most 1 row expected");
		}
		
		return result;
	}
	
	/*
	public static String escape(Object value)
	{
		String v;
		if(value == null) {
			v = "NULL";
		}
		else {
			if(value instanceof String)
				v = "'" + StringEscapeUtils.escapeSql(value.toString()) + "'";
			else
				v = value.toString();
		}
		
		return v;
	}
	*/	
}
