package com.novoda.wallpaper;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Utils {
	
	// Sunrise: 5am-9am 
	// Day: 9am-5pm
	// Sunset: 5pm-7pm
	// Night: 7pm-5am
	public static int currentPeriodOfDay(long time){
    	Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    	cal.setTime(new Date(time));
    	boolean sunrise = cal.get(Calendar.HOUR_OF_DAY) >= 5 && cal.get(Calendar.HOUR_OF_DAY) <= 8;
		boolean sunset = cal.get(Calendar.HOUR_OF_DAY) >= 17  && cal.get(Calendar.HOUR_OF_DAY) < 19;

		if (sunrise || sunset){
    		return Outrun.TIME_PERIOD_SUNSET;
    	}
    	
		if (cal.get(Calendar.HOUR_OF_DAY) >= 9  && cal.get(Calendar.HOUR_OF_DAY) <= 16){
    		return Outrun.TIME_PERIOD_DAY;
    	}
    	
		if ((cal.get(Calendar.HOUR_OF_DAY) >= 19 && cal.get(Calendar.HOUR_OF_DAY) <=24) || cal.get(Calendar.HOUR_OF_DAY) < 5){
    		return Outrun.TIME_PERIOD_NIGHT;
    	}
    	
    	return -1;
	}
	
	public static int currentPeriodOfDay(){
		return currentPeriodOfDay(System.currentTimeMillis());
	}
}
