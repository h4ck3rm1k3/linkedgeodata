package org.linkedgeodata.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface ISQLDAO
{
	void setConnection(Connection conn)
		throws SQLException;
}
