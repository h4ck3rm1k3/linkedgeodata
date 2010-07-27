package org.linkedgeodata.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.linkedgeodata.util.SQLUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public class TagLabelDAO
	extends AbstractDAO
{
	enum Query
		implements IQuery
	{
		INSERT      ("INSERT INTO lgd_tag_labels(k, v, language, label) VALUES (?, ?, ?, ?)"),
		DELETE_EXACT("DELETE FROM lgd_tag_labels WHERE (k, v, language, label) = (?, ?, ?, ?)"),
		DELETE_LANG ("DELETE FROM lgd_tag_labels WHERE language = ?"),
		DELETE_ALL  ("DELETE FROM lgd_tag_labels"),
		SELECT_KVL  ("SELECT label FROM lgd_tag_labels WHERE (k, v, language) = (?, ?, ?)"),
		
		GET_PREFERRED_LABEL("SELECT label FROM lgd_tag_labels WHERE (k, v, language) = (?, ?, ?) LIMIT 1"),
		GET_LABELS_KV("SELECT language, label FROM lgd_tag_labels WHERE (k, v) = (?, ?)"),
		;

		private String sql;
		
		Query(String sql)
		{
			this.sql = sql;
		}
		
		@Override
		public String getSQL()
		{
			return sql;
		}
	}


	public TagLabelDAO()
	{
		super(Arrays.asList(Query.values()));
	}
	
	public TagLabelDAO(Connection conn)
		throws SQLException
	{
		super(Arrays.asList(Query.values()));

		setConnection(conn);
	}
	
	
	public void insert(String k, String v, String language, String label)
		throws Exception
	{
		super.execute(Query.INSERT, Void.class, k, v, language, label);
	}

	public void clear()
		throws Exception
	{
		super.execute(Query.DELETE_ALL, Void.class);
	}
	
	
	/**
	 * Returns the label in a specific languge for a certain tag.
	 * As multiple labels may exists, a list is returned.
	 * 
	 * In case that future implementation may allow multiple labels in the
	 * same language for a tag, this method should return the preferred one. 
	 * 
	 * @param tag
	 * @param language
	 * @return
	 * @throws Exception 
	 */
	public String getPreferredLabel(Tag tag, String language)
		throws Exception
	{		
		return super.execute(Query.GET_PREFERRED_LABEL, String.class, tag.getKey(), tag.getValue(), language);
	}
	
	/**
	 * 
	 * 
	 * @param language
	 * @param label
	 * @throws SQLException 
	 */
	public Set<Tag> find(String language, String label, boolean startsWith)
		throws SQLException
	{
		String langPart = (language == null)
			? ""
			: " AND language = ?";
				
		
		String matchingPart = (startsWith == true)
			? "label LIKE ? "
			: "label = ? ";
		
		if(startsWith)
			label += "%";
				
		String query =
			"SELECT k, v FROM lgs_tags_labels WHERE " + matchingPart + langPart; 
		
		ResultSet rs = (language == null)
			? SQLUtil.executeCore(conn, query, label)
			: SQLUtil.executeCore(conn, query, label, language);
			
		Set<Tag> result = new HashSet<Tag>();
		while(rs.next()) {
			String k = rs.getString(1);
			String v = rs.getString(2);
			
			result.add(new Tag(k, v));
		}
		
		return result;
	}
	
	public MultiMap<String, String> getLabels(Tag tag)
		throws SQLException
	{
		ResultSet rs = super.executeQuery(Query.GET_LABELS_KV, tag.getKey(), tag.getValue());
		
		MultiMap<String, String> result = new MultiHashMap<String, String>();
		
		try {
			while(rs.next()) {
				result.put(rs.getString(1), rs.getString(2));
			}
		} finally {
			rs.close();
		}
		
		return result;
	}
}
