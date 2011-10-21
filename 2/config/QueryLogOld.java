package org.linkedgeodata.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.aksw.commons.util.strings.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.linkedgeodata.util.SinglePrefetchIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.PatternFilenameFilter;

// TODO properly acknowlegde these two sources:
// http://www.java2s.com/Code/Java/Development-Class/ParseanApachelogfilewithRegularExpressions.htm
// http://www.java2s.com/Code/Java/Regular-Expressions/ParseanApachelogfilewithStringTokenizer.htm


class WindowedSorterIterator<T>
	extends SinglePrefetchIterator<T>
{
	private NavigableSet<T> buffer;
	private int maxBufferSize;
	private Iterator<T> it;
	
	public WindowedSorterIterator(Iterator<T> it, int maxBufferSize, Comparator<T> comparator) 
	{
		this.buffer = new TreeSet<T>(comparator);
		this.it = it;
		this.maxBufferSize = maxBufferSize;
	}
	
	@Override
	protected T prefetch() throws Exception
	{
		while(buffer.size() < maxBufferSize && it.hasNext()) {
			buffer.add(it.next());
		}

		return buffer.isEmpty() ? finish() : buffer.pollFirst();
	}
}


class Request 
{
	private static final Pattern requestPattern = Pattern.compile("^([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)"); 
	private String method;
	private String url;
	private String protocol;
	
	Request(String str)
		throws ParseException
	{
	    Matcher matcher = requestPattern.matcher(str);
	    if (!matcher.matches()) {
	    	throw new ParseException("No matches found when parsing request: " + str, 0);
	    }
	    
	    if (3 != matcher.groupCount()) {
	    	throw new ParseException("Error parsing request: " + str + " ; groupCount = " + matcher.groupCount(), 0);
	    }
	    
	    this.method = matcher.group(1);
	    this.url = matcher.group(2);		
	    this.protocol = matcher.group(3);		
	}
	
	public static Request parse(String str)
		throws ParseException
	{
		return new Request(str);
	}
	
	public Request(String method, String url, String protocol)
	{
		super();
		this.method = method;
		this.url = url;
		this.protocol = protocol;
	}
	
	public String getMethod()
	{
		return method;
	}
	public String getUrl()
	{
		return url;
	}
	public String getProtocol()
	{
		return protocol;
	}
}

class ApacheLogEntry {
	
	private static final int NUM_FIELDS = 9;
	private static final Pattern logEntryPattern = Pattern.compile("^([^\\s]+) (\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(.+?)\" (\\d{3}) (\\d+) \"([^\"]+)\" \"([^\"]+)\"");
    
	// 17/Apr/2011:06:47:47 +0200
	private static final DateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");

	
	private String hostname;
	private Date date;
	private Request request;
	private String response;
	private long byteCount;
	private String referer;
	private String userAgent;
		
	/*
	public ApacheLogEntry(String hostname, Date date, String request,
			String response, String byteCount, String referer, String userAgent)
	{
		super();
		this.hostname = hostname;
		this.date = date;
		this.request = request;
		this.response = response;
		this.byteCount = byteCount;
		this.referer = referer;
		this.userAgent = userAgent;
	}*/
	
	public String getHostname()
	{
		return hostname;
	}

	public Date getDate()
	{
		return date;
	}

	public Request getRequest()
	{
		return request;
	}
	
	
	public String getResponse()
	{
		return response;
	}

	public long getByteCount()
	{
		return byteCount;
	}

	public String getReferer()
	{
		return referer;
	}

	public String getUserAgent()
	{
		return userAgent;
	}
	

	ApacheLogEntry(String logEntryLine) throws ParseException
	{
	    Matcher matcher = logEntryPattern.matcher(logEntryLine);
	    if (!matcher.matches()) {
	    	throw new ParseException("No matches found when parsing line: " + logEntryLine, 0);
	    }
	    
	    if (NUM_FIELDS != matcher.groupCount()) {
	    	throw new ParseException("Error parsing line: " + logEntryLine + " ; groupCount = " + matcher.groupCount(), 0);
	    }
	    
	    this.hostname = matcher.group(1);
	    this.date = dateFormat.parse(matcher.group(4));
	    this.request = Request.parse(matcher.group(5));
	    this.response = matcher.group(6);
	    this.byteCount = Long.parseLong(matcher.group(7));

	    this.referer = "";
	    if (!matcher.group(8).equals("-")) {
	    	this.referer =  matcher.group(8);
	    }
	    this.userAgent = matcher.group(9);
	}

	public static ApacheLogEntry parse(String logEntryLine)
		throws ParseException
	{
		return new ApacheLogEntry(logEntryLine);
	}
}


class ApacheLogEntryIterator
	extends SinglePrefetchIterator<ApacheLogEntry>
{
	private BufferedReader reader;
	private boolean closeWhenDone;

	private NavigableMap<Date, ApacheLogEntry> sortedBuffer = new TreeMap<Date, ApacheLogEntry>();
	int maxSortedBufferSize = 1000;

	
	public ApacheLogEntryIterator(InputStream in, boolean closeWhenDone)
	{
		this.reader = new BufferedReader(new InputStreamReader(in));
	}
	
	public ApacheLogEntryIterator(BufferedReader reader)
	{
		this.reader = reader;
	}
	
	
	@Override
	protected ApacheLogEntry prefetch() throws Exception
	{
		String line;
		while(sortedBuffer.size() < maxSortedBufferSize && ((line = reader.readLine()) != null)) {
			try {
				ApacheLogEntry entry = ApacheLogEntry.parse(line);
				sortedBuffer.put(entry.getDate(), entry);
			} catch(ParseException e) {
				// TODO Shouldn't happen, but the parser is sucky, so it happens
				e.printStackTrace();
			}
		}

		
		if(!sortedBuffer.isEmpty()) {
			return sortedBuffer.pollFirstEntry().getValue();
		}

		
		return finish();
	}
	
	@Override
	public void close()
	{
		if(closeWhenDone) {
			try {
				reader.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}


class RangeApacheLogEntryIterator
	extends SinglePrefetchIterator<ApacheLogEntry>
{
	//private NavigableMap<Date, File> dateToFile = new TreeMap<Date, File>();
	private Iterator<Map.Entry<Date, File>> itFile;
	
	private ApacheLogEntryIterator itEntry;
	//private Iterator<ApacheLogEntry> itEntry;
	
	
	private NavigableMap<Date, ApacheLogEntry> sortedBuffer = new TreeMap<Date, ApacheLogEntry>();
	int maxSortedBufferSize = 1000;
	
	private Date low;
	private Date high;
	private boolean lowInclusive;
	private boolean highInclusive;
	
	private Date sanityCheckMonotonictyDate = null;
	
	public RangeApacheLogEntryIterator(Iterator<Map.Entry<Date, File>> itFile, Date low, boolean lowInclusive, Date high, boolean highInclusive)
	{
		this.itFile = itFile;
		this.low = low;
		this.high = high;
		this.lowInclusive = lowInclusive;
		this.highInclusive = highInclusive;
	}

	@Override
	protected ApacheLogEntry prefetch() throws Exception
	{
		while(sortedBuffer.size() < maxSortedBufferSize) {
			if(itEntry == null) {
				if(itFile.hasNext()) {
					itEntry = new ApacheLogEntryIterator(LogDirectory.open(itFile.next().getValue()), true);
				} else {
					break;
				}
			}
			
			if(!itEntry.hasNext()) {
				itEntry = null;
				continue;
			}

			ApacheLogEntry entry = itEntry.next();
			
			if(low != null) {
				int d = entry.getDate().compareTo(low);
				if(d < 0 || (d == 0 && !lowInclusive)) {
					continue;
				}
			}

			sortedBuffer.put(entry.getDate(), entry);
		}
			
			
		if(!sortedBuffer.isEmpty()) {
			ApacheLogEntry entry = sortedBuffer.pollFirstEntry().getValue();

			if(sanityCheckMonotonictyDate != null) {
				if(sanityCheckMonotonictyDate.compareTo(entry.getDate()) > 0) {
					throw new RuntimeException("Dates are not monoton");
				}
			}
			sanityCheckMonotonictyDate = entry.getDate();
			
			if(high != null) {
				int d = high.compareTo(entry.getDate());
				if(d < 0 || (d == 0 && !highInclusive)) {
					return finish();
				}				
			}
			

			return entry;
		}
		
		return finish();
	}
	
	@Override
	public void close()
	{
		if(itEntry != null) {
			itEntry.close();
		}
	}
}

class LogDirectory
{
	private static final Logger logger = LoggerFactory.getLogger(LogDirectory.class);
	
	private NavigableMap<Date, File> dateToFile = new TreeMap<Date, File>();

	public static InputStream open(File file)
		throws IOException
	{
		InputStream in = new FileInputStream(file);
		if(file.getName().endsWith(".gz")) {
			in = new GZIPInputStream(in);
		}
		
		return in;
	}
	
	public LogDirectory(File dir, Pattern pattern) throws IOException
	{
		if(!dir.isDirectory()) {
			throw new IllegalArgumentException("Argument must be a directory, got: " + dir);
		}
		
		// Read the date from the first line of each file, and sort the files
		// accordingly
		for(File file : dir.listFiles(new PatternFilenameFilter(pattern))) {
			ApacheLogEntryIterator it = new ApacheLogEntryIterator(open(file), true);
			
			while(it.hasNext()) {
				Date date = it.next().getDate();
				//logger.debug("Found log file " + file.getAbsolutePath() + " with date " + date);
				dateToFile.put(date, file);
				it.close();
				break;
			}
		}
		logger.debug("Found log files: " + dateToFile);
	}
	
	/**
	 * 
	 * lowInclusive and highInclusive are ignored if the respective bound in null (unbounded).
	 * 
	 * @param low
	 * @param lowInclusive
	 * @param high
	 * @param highInclusive
	 * @return
	 */
	public RangeApacheLogEntryIterator getIterator(Date low, Date high, boolean lowInclusive, boolean highInclusive)
	{
		NavigableMap<Date, File> subMap = dateToFile; 
		
		Date adjustedLow = low == null ? null : dateToFile.floorKey(low);
		Date adjustedHigh = high == null ? null : dateToFile.ceilingKey(high);
		
		// lower bound
		if(adjustedLow != null) {
			subMap = subMap.tailMap(adjustedLow, true);
		}
		
		// upperbound
		if(adjustedHigh != null) {
			subMap = subMap.headMap(adjustedHigh, true);
		}

		logger.debug("Adjust: Creating an iterator from " + adjustedLow + " until " + adjustedHigh + "; spanning " + subMap.size() + " files.");
		logger.debug("Creating an iterator from " + low + " until " + high + "; spanning " + subMap.size() + " files.");
		
		return new RangeApacheLogEntryIterator(subMap.entrySet().iterator(), low, lowInclusive, high, highInclusive);
	}
}

public class QueryLogOld
{
	public static void main(String[] args)
		throws Exception
	{
		PropertyConfigurator.configure("log4j.properties");
		
		File dir = new File("/home/raven/Desktop/lgd/QueryLogs/all/");
		
		LogDirectory logDir = new LogDirectory(dir, Pattern.compile("access.*"));
		
		Date low = new GregorianCalendar(2011, 3, 10, 0, 0, 0).getTime();
		Date high = new GregorianCalendar(2011, 3, 17, 12, 0, 0).getTime();
		
		//low = new GregorianCalendar(2011, 3, 17, 0, 0, 0).getTime();
		//high = new GregorianCalendar(2011, 3, 19, 12, 0, 0).getTime();
		
		low = null;
		high = null;
		
		
		File outFile = new File("/home/raven/Desktop/LGDSparql.txt");
		PrintWriter writer = new PrintWriter(outFile);
		
		RangeApacheLogEntryIterator it = logDir.getIterator(low, high, true, true);
		
		
		
		DateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
		
		int i = 0;
		while(it.hasNext()) {
			ApacheLogEntry entry = it.next();
			
			String uri = entry.getRequest().getUrl();
			//*
			try {
				StringUtils.decodeUtf8(uri);
			} catch(Exception e) {
				e.printStackTrace();
				continue;
			}//*/
			
			if(!(uri.contains("sparql") && uri.contains("query=") && uri.contains("linkedgeodata")))
				continue;

			
			writer.println(dateFormat.format(entry.getDate()) + "\t" + entry.getHostname() + "\t" + uri);
			//++i;
			
			//System.out.println(i + " --- " + entry.getDate() + " --- ");
		}
		
		
		//processFile(new File("/home/raven/Desktop/lgd/QueryLogs/access.log"));
	}
	
	public static void processFile(File file) throws IOException, ParseException {
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

		String line;
		while((line = reader.readLine()) != null) {
			
			ApacheLogEntry entry = ApacheLogEntry.parse(line);
			
			String uri = entry.getRequest().getUrl();
			
			if(!(uri.contains("sparql") && uri.contains("query=") && uri.contains("linkedgeodata")))
				continue;

			System.out.println(StringUtils.decodeUtf8(uri));
		
			
			//String uri = parts[]
		}
	}
}
