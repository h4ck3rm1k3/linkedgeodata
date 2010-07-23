package org.linkedgeodata.dao;

import java.util.Arrays;

public class TagLabelDAO
	extends AbstractDAO
{
	enum Query
		implements IQuery
	{
		INSERT      ("INSERT INTO lgd_tags_labels(k, v, language, label) VALUES (?, ?, ?, ?)"),
		DELETE_EXACT("DELETE FROM lgd_tags_labels WHERE (k, v, language, label) = (?, ?, ?, ?)"),
		DELETE_LANG ("DELETE FROM lgd_tags_labels WHERE language = ?"),
		DELETE_ALL  ("DELETE FROM lgd_tags_labels"),
		SELECT_KVL  ("SELECT label FROM lgd_tags_labels WHERE (k, v, language) = (?, ?, ?)"),
		SELECT_KV  ("SELECT language, label FROM lgd_tags_labels WHERE (k, v) = (?, ?)"),
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
}
