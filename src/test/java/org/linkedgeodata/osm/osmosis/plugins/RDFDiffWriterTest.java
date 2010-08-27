package org.linkedgeodata.osm.osmosis.plugins;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.linkedgeodata.util.StringUtil;


public class RDFDiffWriterTest
{
	@Test
	@Ignore
	public void chunkValueTest()
	{
		List<Long> result;
		result = RDFDiffWriter.chunkValue(123456l, 1000l, 1000l);
		
		for(long i = 0; i < 20; ++i) {
			result = RDFDiffWriter.chunkValue(i, 2l, 5l);

			System.out.println(result);
		}
	}
	
	@Test
	public void chunkValueTest2()
	{
		for(long i = 0; i < 20; ++i) {
			List<Long> filePathIds = RDFDiffWriter.chunkValue(i, 1000l, 1000l);
			List<Long> dirIds = filePathIds.subList(0, filePathIds.size() - 1);
			Long fileId = filePathIds.get(filePathIds.size() - 1);
			
			String dirPath = "";
			
			if(!dirIds.isEmpty())
				dirPath += "/" + StringUtil.implode("/", dirIds);
			
			String seqPath = dirPath + "/" + fileId;
			
			System.out.println(seqPath);
		}
	}
	
}
