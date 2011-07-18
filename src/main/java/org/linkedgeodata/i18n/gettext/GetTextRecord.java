package org.linkedgeodata.i18n.gettext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class GetTextRecord
		extends HashMap<GetTextRecord.Msg, String>
{
	private List<String>	plainValues	= new ArrayList<String>();

	public List<String> getPlainValues()
	{
		return plainValues;
	}

	@Override
	public String get(Object msg)
	{
		String result = super.get(msg);

		return result == null ? "" : result;
	}

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 7884124527648846411L;

	enum Msg
	{
		COMMENT("#"), MSGCTXT("msgctxt"), MSGID("msgid"), MSGSTR("msgstr");

		private String	value;

		Msg(String value)
		{
			this.value = value;
		}

		public String getValue()
		{
			return value;
		}
	}
}