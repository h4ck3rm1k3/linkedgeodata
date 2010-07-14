package org.linkedgeodata.util;

public class ConnectionConfig
{
	private String hostName;
	private String passWord;
	private String userName;
	private String dataBaseName;
	
	public ConnectionConfig()
	{
		super();
	}
	
	public ConnectionConfig(String hostName, String dataBaseName, String userName, String passWord)
	{
		super();
		this.hostName = hostName;
		this.passWord = passWord;
		this.userName = userName;
		this.dataBaseName = dataBaseName;
	}

	public String getHostName()
	{
		return hostName;
	}
	public String getPassWord()
	{
		return passWord;
	}
	public String getUserName()
	{
		return userName;
	}
	public String getDataBaseName()
	{
		return dataBaseName;
	}

	public void setHostName(String hostName)
	{
		this.hostName = hostName;
	}

	public void setPassWord(String passWord)
	{
		this.passWord = passWord;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public void setDataBaseName(String dataBaseName)
	{
		this.dataBaseName = dataBaseName;
	}
}
