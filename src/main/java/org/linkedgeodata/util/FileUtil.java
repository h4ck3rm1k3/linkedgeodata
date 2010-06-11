package org.linkedgeodata.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtil
{
	String getContent(File file)
		throws IOException
	{
		return StreamUtil.toString(new FileInputStream(file), true);
	}
}
