package org.linkedgeodata.osm.osmosis.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.linkedgeodata.util.IDiff;
import org.linkedgeodata.util.ModelUtil;
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
	
	public RDFDiff read()
		throws IOException
	{
		return read(baseName, zip);
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
	
	public static File createFile(String baseName, boolean zip, boolean added)
	{
		String fileNameExtension = "nt";

		if(zip == true)
			fileNameExtension += ".gz";
		
		String type = (added == true) ? "added" : "removed";
		
		
		String fileName = baseName + "." + type + "." + fileNameExtension;
					
		return new File(fileName);	
	}

	private static String jenaFormat = "N-TRIPLE";
	
	public static void write(String baseName, IDiff<Model> diff, boolean zip)
		throws IOException
	{
		File file = new File(baseName);
		File parentDir = file.getParentFile();
		if(parentDir != null)
			parentDir.mkdir();
			

		
		RDFWriter rdfWriter = ModelFactory.createDefaultModel().getWriter(jenaFormat);

		File addedFile = createFile(baseName, zip, true);		
		write(diff.getAdded(), rdfWriter, addedFile, zip);
		
		File removedFile = createFile(baseName, zip, false);
		write(diff.getRemoved(), rdfWriter, removedFile, zip);
	}	

	public static InputStream getInputStream(File file, boolean zip)
		throws IOException
	{
		InputStream result = new FileInputStream(file);
		
		if(zip) {
			result = new GZIPInputStream(result);
		}
		
		return result;
	}
	
	public static RDFDiff read(String baseName, boolean zip)
		throws IOException
	{
		File addedFile = createFile(baseName, zip, true);
		Model addedModel = ModelUtil.read(getInputStream(addedFile, zip), jenaFormat);

		File removedFile = createFile(baseName, zip, false);
		Model removedModel = ModelUtil.read(getInputStream(removedFile, zip), jenaFormat);

		RDFDiff result = new RDFDiff(addedModel, removedModel, null);
		
		return result;
	}
	
	public static void write(Model model, RDFWriter rdfWriter, File file, boolean zip)
		throws IOException
	{
		logger.info("Attempting to write diff-file: " + file.getAbsolutePath());
				
		OutputStream tmp = new FileOutputStream(file);
		
		OutputStream out;
		if(zip) {
			out = new GzipCompressorOutputStream(tmp);
			//out = new BZip2CompressorOutputStream(tmp);
		}
		else {
			out = tmp;
		}
		
		rdfWriter.write(model, out, "");
		
		out.flush();
		out.close();
	}
}
