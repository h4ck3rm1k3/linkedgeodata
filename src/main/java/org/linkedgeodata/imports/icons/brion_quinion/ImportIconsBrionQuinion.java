package org.linkedgeodata.imports.icons.brion_quinion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.aksw.commons.util.collections.MultiMaps;
import org.apache.commons.compress.tar.TarEntry;
import org.apache.commons.compress.tar.TarInputStream;
import org.linkedgeodata.util.URIUtil;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import com.google.common.io.Files;
import com.google.common.io.PatternFilenameFilter;


/**
 * Import and publish icons from the source
 *     http://www.sjjb.co.uk/mapicons/introduction
 *     
 *  Citations from the source:   
 *  """
 *   License:
 *     To the extent possible under law, SJJB Management has waived all
 *     copyright and related or neighboring rights to SJJB SVG Map Icons. This
 *     work is published from United Kingdom. That said, if you are making
 *     extensive use of these icons and your medium is suitable a credit would
 *     always be appreciated linking back to this page.
 *     For instance: "Map icons CC-0 from SJJB Management"
 *     
 *   Acknowlegdements:
 *    A number of these icons are derived from US National Park Service
 *    Cartography. Other symbols have been derived from Public Domain sources;
 *    details for individual symbols are available in SOURCES.txt which is
 *    included with the distribution. All sources are belived to be Public Domain
 *    and this has been check to best of my ability, however if you do have a
 *    concern over copyright please email me.
 *  """
 * 
 * TODO Use system calls for checking out the git repo
 * 
 * TODO Another source http://svn.openstreetmap.org/applications/share/map-icons/2020iconset/ * 
 * 
 * @author raven
 *
 */
public class ImportIconsBrionQuinion
{
	private File destDir = new File("/var/www/linkedgeodata.org/integration/icons/sjjb/"); 
	private String baseUrl = "http://linkedgeodata.org/integration/icons/sjjb/";
	
	public static void main(String[] args)
		throws Exception
	{
		Map<Tag, Set<File>> result = readMapping();
		
		// Publish the files and generate triples
		
		
		System.out.println(result);
	}
		
	
	public URL publishFile(File srcFile)
		throws IOException
	{
		Files.copy(srcFile, destDir);
		return new URL(baseUrl + srcFile.getName());
	}
	
	
	public static Map<Tag, Set<File>> readMapping()
		throws Exception
	{
		Map<Tag, Set<File>> result = new HashMap<Tag, Set<File>>();
		
		File file = new File("/tmp/SJJB-SVG-Icons.tar.gz");
		File dir = new File("/tmp/icons/");

		if(!file.exists()) {
			URIUtil.download(
				new URL(
						"http://www.sjjb.co.uk/mapicons/download/SJJB-SVG-Icons-20110406.tar.gz"),
				file);
		}
		
		if(!dir.exists()) {
			untar(file, dir);
		}
		
		// Iterate the dir
		File root = new File(dir.getPath() + "/svg/");
		//System.out.println(root);
		
		for(File keyFile : root.listFiles()) {
			if(!keyFile.isDirectory())
				continue;
			
			String key = keyFile.getName();
			
			for(File valueFile : keyFile.listFiles(new PatternFilenameFilter(".*\\.svg"))) {
				String value = valueFile.getName().substring(0, valueFile.getName().length() - 4);
				
				
				//System.out.println(key + " - " + value);
				MultiMaps.put(result, new Tag(key, value), valueFile);
			}
			
			
		}
		return result;
	}

	// Source: http://www.coderanch.com/t/416501/Streams/java/Untar-tar-gz-file
	public static void untar(File tarFile, File dest) throws IOException
	{

		// assuming the file you pass in is not a dir

		dest.mkdir();

		// create tar input stream from a .tar.gz file

		TarInputStream tin = new TarInputStream(new GZIPInputStream(
				new FileInputStream(tarFile)));

		// get the first entry in the archive

		TarEntry tarEntry = tin.getNextEntry();

		while (tarEntry != null) {// create a file with the same name as the
									// tarEntry

			File destPath = new File(dest.toString() + File.separatorChar
					+ tarEntry.getName());

			if (tarEntry.isDirectory()) {

				destPath.mkdir();

			} else {

				FileOutputStream fout = new FileOutputStream(destPath);

				tin.copyEntryContents(fout);

				fout.close();
			}
			tarEntry = tin.getNextEntry();
		}
		tin.close();
	}

}


