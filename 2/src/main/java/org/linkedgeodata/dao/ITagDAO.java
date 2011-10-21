package org.linkedgeodata.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public interface ITagDAO
	extends ISQLDAO
{
	boolean doesTagExist(Tag tag)
		throws SQLException;

	List<String> findKeys(Pattern pattern)
		throws SQLException;
}
