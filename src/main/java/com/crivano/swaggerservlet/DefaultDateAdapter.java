package com.crivano.swaggerservlet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DefaultDateAdapter implements IDateAdapter {
	public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final SimpleDateFormat isoFormatter = new SimpleDateFormat(ISO_FORMAT);

	@Override
	public String format(Date date) {
		synchronized (isoFormatter) {
			return isoFormatter.format(date);
		}
	}

	@Override
	public Date parse(String date) {
		if (date == null)
			return null;
		try {
			synchronized (isoFormatter) {
				return isoFormatter.parse(date);
			}
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
