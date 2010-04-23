package org.linkedgeodata.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.linkedgeodata.core.dao.IQuery;


//http://stackoverflow.com/questions/1497569/how-to-execute-sql-script-file-using-jdbc
public class SQLUtil
{
	private static final Logger logger = Logger.getLogger(SQLUtil.class);
	
	public static String placeHolder(int n, int m)
	{
		String part = "";
		for(int i = m - 1; i >= 0; --i) {
			part += "?";
			
			if(i != 0)
				part += ", ";
		}

		if(m > 1)
			part = "(" + part + ")";
		
		String result = "";
		for(int j = n - 1; j >= 0; --j) {
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

	public static <T> T single(ResultSet rs, Class<T> clazz)
		throws SQLException
	{
		return single(rs, clazz, true);
	}

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
	public static <T> T single(ResultSet rs, Class<T> clazz, boolean bClose)
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

		if(bClose)
			rs.close();
		
		return result;
	}
	
	
	public static <T> List<T> list(ResultSet rs, Class<T> clazz)
		throws SQLException
	{
		return list(rs, clazz, true);
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> list(ResultSet rs, Class<T> clazz, boolean bClose)
		throws SQLException
	{		
		List<T> result = new ArrayList<T>();
		
		while(rs.next()) {
			Object o = rs.getObject(1);;
			//System.out.println("Result = " + o);
			T item = (T)o;
			result.add(item);
		}
	
		if(bClose)
			rs.close();
		return result;
		
	}
	
	
	public static <T> void executeSetArgs(PreparedStatement stmt, Object ...args)
		throws SQLException
	{
		for(int i = 0; i < args.length; ++i) {
			stmt.setObject(i + 1, args[i]);
		}
		
		// Pad with nulls
		int n = stmt.getParameterMetaData().getParameterCount();
		//System.out.println("x = " + n);
		for(int i = args.length; i < n; ++i) {
			stmt.setObject(i + 1, null);
		}
		//System.out.println("y = " + n);
	}
	
	public static <T> T execute(Connection conn, String sql, Class<T> clazz, Object ...args)
		throws SQLException
	{
		PreparedStatement stmt = conn.prepareStatement(sql);
		
		T result = execute(stmt, clazz, args);
		
		stmt.close();
		
		return result;
	}
	
	public static ResultSet executeCore(Connection conn, String sql, Object ...args)
		throws SQLException
	{
		logger.trace("Executing statement '" + sql + "' with args " + Arrays.asList(args));
		
		PreparedStatement stmt = conn.prepareStatement(sql);

		executeSetArgs(stmt, args);
		ResultSet result = stmt.executeQuery();
		
		//stmt.close();
		
		return result;
	}
	
	public static <T> T execute(PreparedStatement stmt, Class<T> clazz, Object ...args)
		throws SQLException
	{
		executeSetArgs(stmt, args);
	
		T result = null;
		if(clazz == null || Void.class.equals(clazz)) {
			stmt.execute();
		}
		else {
			ResultSet rs = stmt.executeQuery();		
			result = SQLUtil.single(rs, clazz);
			rs.close();
		}
	
		return result;
	}

	public static <T> List<T> executeList(Connection conn, String sql, Class<T> clazz, Object ...args)
		throws SQLException
	{
		PreparedStatement stmt = conn.prepareStatement(sql);
		
		List<T> result = executeList(stmt, clazz, args);
		
		stmt.close();
		
		return result;
	}
	
	public static <T> List<T> executeList(PreparedStatement stmt, Class<T> clazz, Object ...args)
		throws SQLException
	{
		executeSetArgs(stmt, clazz, args);
	
		ResultSet rs = stmt.executeQuery();		
		List<T> result = SQLUtil.list(rs, clazz);

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
