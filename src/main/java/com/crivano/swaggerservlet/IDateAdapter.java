package com.crivano.swaggerservlet;

import java.util.Date;

public interface IDateAdapter {

	String format(Date date);

	Date parse(String date);

}