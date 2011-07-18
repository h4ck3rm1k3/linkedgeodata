package org.linkedgeodata.i18n.gettext;

import java.io.BufferedReader;

import org.linkedgeodata.util.SinglePrefetchIterator;

class GetTextIterator
		extends SinglePrefetchIterator<GetTextRecord>
{
	private BufferedReader	reader;

	public GetTextIterator(BufferedReader reader)
	{
		this.reader = reader;
	}

	private static String stripDoubleQuotes(String str)
	{
		int offset = str.startsWith("\"") ? 1 : 0;
		int deltaLen = str.endsWith("\"") ? 1 : 0;

		String result = str.substring(offset, str.length() - deltaLen);
		return result;
	}

	@Override
	protected GetTextRecord prefetch() throws Exception
	{
		GetTextRecord record = new GetTextRecord();

		String line = "";
		while ((line = reader.readLine()) != null) {
			if (line.trim().isEmpty()) {
				if (record.isEmpty() && record.getPlainValues().isEmpty())
					continue;

				return record;
			}

			if (line.startsWith(GetTextRecord.Msg.COMMENT.getValue()))
				continue;

			for (GetTextRecord.Msg msg : GetTextRecord.Msg.values()) {
				if (line.startsWith(msg.getValue())) {
					String sub = line.substring(msg.getValue().length()).trim();

					sub = stripDoubleQuotes(sub);

					record.put(msg, sub);
					continue;
				}
			}

			record.getPlainValues().add(stripDoubleQuotes(line));
		}

		return finish();
	}

}