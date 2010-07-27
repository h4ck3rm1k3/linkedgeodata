package org.linkedgeodata.core;

import org.ini4j.Profile.Section;
import org.linkedgeodata.util.ConnectionConfig;

public class IniUtils
{
	public static ConnectionConfig getConnectionConfig(Section section)
	{
		return new ConnectionConfig(
				section.get("uri"),
				section.get("name"),
				section.get("userName"),
				section.get("passWord"));
	}
}
