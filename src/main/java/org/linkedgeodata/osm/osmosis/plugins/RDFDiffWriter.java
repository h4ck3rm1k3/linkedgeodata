package org.linkedgeodata.osm.osmosis.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.linkedgeodata.util.IDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;


public class RDFDiffWriter
{
	private static final Logger logger = LoggerFactory.getLogger(RDFDiffWriter.class);
	
	private long sequenceId;	
	private File basePath;
	
	public RDFDiffWriter(File basePath, long sequenceId)
	{
		this.sequenceId = sequenceId;
	}
	
	public void write(IDiff<Model> diff)
		throws IOException
	{
		write(basePath, diff, sequenceId);
		
		++sequenceId;
	}

	
	public static void write(File basePath, IDiff<Model> diff, long sequenceId)
		throws IOException
	{
		String fileNameExtension = "tll";
		String jenaFormat = "N3";

		RDFWriter rdfWriter = ModelFactory.createDefaultModel().getWriter(jenaFormat);
		
		long majorId = sequenceId / 1000l;
		long minorId = sequenceId % 1000l;
		
		String seqPath = basePath.getAbsolutePath() + majorId + "/" + minorId;

		String addedFileName = seqPath + ".added." + fileNameExtension;		
		write(diff.getAdded(), rdfWriter, addedFileName);
		
		String removedFileName = seqPath + ".removed." + fileNameExtension;
		write(diff.getRemoved(), rdfWriter, removedFileName);
	}	

	public static void write(Model model, RDFWriter rdfWriter, String fileName)
		throws IOException
	{
		logger.info("Attempting to write diff-file: " + fileName);
		
		File file = new File(fileName);
		OutputStream out = new FileOutputStream(file);
		
		rdfWriter.write(model, out, "");
		
		out.flush();
		out.close();
	}
}
