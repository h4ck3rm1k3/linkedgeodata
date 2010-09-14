package org.linkedgeodata.osm.osmosis.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;


public class RDFDiffWriter
{
	private static final Logger logger = LoggerFactory.getLogger(RDFDiffWriter.class);
	
	//private long sequenceId;
	//private File basePath;
	
	private String baseName;
	
	
	private boolean zip = true;
	
	/*
	public RDFDiffWriter(File basePath, long sequenceId)
	{
		this.basePath = basePath;
		this.sequenceId = sequenceId;
	}
	*/
	
	public RDFDiffWriter(String baseName)
	{
		this.baseName = baseName;
	}
	
	public void write(IDiff<Model> diff)
		throws IOException
	{
		//logger.info("Writing diff: +" + diff.getAdded().size() + ", -" + diff.getRemoved().size());
		//write(basePath, diff, sequenceId);
		
		write(baseName, diff, zip);
		
		//++sequenceId;
	}

	// 5 10 100
	public static List<Long> chunkValue(long id, long ...chunkSizes)
	{
		long denominator = 1;
		for(long chunkSize : chunkSizes)
			denominator *= chunkSize;
		
		List<Long> result = new ArrayList<Long>();

		long remainder = id;
		for(long chunkSize : chunkSizes) {			
			long div = remainder / denominator;
			remainder = remainder % denominator;

			result.add(div);

			denominator = denominator / chunkSize;
		}
		
		result.add(remainder);
		
		return result;
	}

	/*
	public static void write(File basePath, IDiff<Model> diff, long sequenceId)
		throws IOException
	{
		List<Long> filePathIds = chunkValue(sequenceId, 1000l, 1000l);
		List<Long> dirIds = filePathIds.subList(0, filePathIds.size() - 1);
		Long fileId = filePathIds.get(filePathIds.size() - 1);
		
		String dirPath = basePath.getAbsolutePath();
		
		if(!dirIds.isEmpty())
			dirPath += "/" + StringUtil.implode("/", dirIds);

		File dir = new File(dirPath);
		dir.mkdirs();
		
		String seqPath = dirPath + "/" + fileId;

	}
	*/
	
	public static void write(String baseName, IDiff<Model> diff, boolean zip)
		throws IOException
	{
		File file = new File(baseName);
		File parentDir = file.getParentFile();
		if(parentDir != null)
			parentDir.mkdir();
			
		String fileNameExtension = "nt";
		String jenaFormat = "N-TRIPLE";

		if(zip == true)
			fileNameExtension += ".gz";
		
		
		RDFWriter rdfWriter = ModelFactory.createDefaultModel().getWriter(jenaFormat);

		String addedFileName = baseName + ".added." + fileNameExtension;		
		write(diff.getAdded(), rdfWriter, addedFileName, zip);
		
		String removedFileName = baseName + ".removed." + fileNameExtension;
		write(diff.getRemoved(), rdfWriter, removedFileName, zip);
	}	

	public static void write(Model model, RDFWriter rdfWriter, String fileName, boolean zip)
		throws IOException
	{
		logger.info("Attempting to write diff-file: " + fileName);
		
		File file = new File(fileName);
		
		OutputStream tmp = new FileOutputStream(file);
		
		OutputStream out;
		if(zip) {
			out = new GzipCompressorOutputStream(tmp);
		}
		else {
			out = tmp;
		}
		
		rdfWriter.write(model, out, "");
		
		out.flush();
		out.close();
	}
}
