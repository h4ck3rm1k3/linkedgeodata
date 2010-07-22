package org.linkedgeodata.scripts;
/**
 * Copyright (C) 2009-2010, LinkedGeoData team at the MOLE research
 * group at AKSW / University of Leipzig
 *
 * This file is part of LinkedGeoData.
 *
 * LinkedGeoData is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * LinkedGeoData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.h sllorg/licenses/>.
 *
 */ 
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.util.ConnectionConfig;
import org.linkedgeodata.util.PostGISUtil;
import org.linkedgeodata.util.SQLUtil;



public class NodeTagsTriggerBenchmark
{
	private static final Logger logger = Logger.getLogger(LGDDumper.class);	
    protected static Options cliOptions;

    
    public static ConnectionConfig parseConnectionConfig(CommandLine commandLine)
    {
		return new ConnectionConfig(
				commandLine.getOptionValue("h", "localhost"),
				commandLine.getOptionValue("d", "lgd"),
				commandLine.getOptionValue("u", "lgd"),
				commandLine.getOptionValue("w", "lgd"));
    }

	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
	
		initCLIOptions();
	
		CommandLineParser cliParser = new GnuParser();
		CommandLine commandLine = cliParser.parse(cliOptions, args);
	
		ConnectionConfig config = parseConnectionConfig(commandLine);

		Connection conn = PostGISUtil.connectPostGIS(config);


		try {
			conn.createStatement().execute("INSERT INTO nodes(id, version, user_id, tstamp, changeset_id, geom) VALUES(0, 0, 0, now(), 0, ST_SetSRID(ST_MakePoint(50.0, 50.0), 4326))");
		}
		catch(Exception e) {
		}
		
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO node_tags(node_id, k, v) VALUES (?, ?, ?)");
		
		
		
		long before = SQLUtil.single(conn.createStatement().executeQuery("SELECT COUNT(*) FROM node_tags"), Long.class);
		System.out.println("Before: " + before);
		
		StopWatch sw = new StopWatch();
		sw.start();
		
		conn.setAutoCommit(false);
		
		long offset = 1000000000;
		int n = 10000;
		for(int i = 0; i < n; ++i) {
			if(i % 1000 == 0) {
				System.out.println("Ratio = " + (i / (sw.getTime() / 1000.0)));				
			}
			
			SQLUtil.execute(stmt, Void.class, 0, "a", "b");
		}
		
		conn.commit();
		conn.setAutoCommit(true);
		sw.stop();
		long after = SQLUtil.single(conn.createStatement().executeQuery("SELECT COUNT(*) FROM node_tags"), Long.class);
		
		System.out.println("Time taken: " + (sw.getTime() / 1000.0));
		System.out.println("After: " + after);
	
		sw = new StopWatch();
		sw.start();
		
		//conn.createStatement().execute("DELETE FROM node_tags WHERE node_id >= " + offset);
		conn.createStatement().execute("DELETE FROM node_tags WHERE node_id = 0");
		
		sw.stop();
		System.out.println("Time taken: " + (sw.getTime() / 1000.0));
		long fin = SQLUtil.single(conn.createStatement().executeQuery("SELECT COUNT(*) FROM node_tags"), Long.class);
		System.out.println("Final: " + fin);
	}
	
	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/	
	private static void initCLIOptions()
	{
		cliOptions = new Options();
		
		cliOptions.addOption("t", "type", true, "Database type (posgres, mysql,...)");
		cliOptions.addOption("d", "database", true, "Database name");
		cliOptions.addOption("u", "user", true, "");
		cliOptions.addOption("w", "password", true, "");
		cliOptions.addOption("h", "host", true, "");
	}
}
