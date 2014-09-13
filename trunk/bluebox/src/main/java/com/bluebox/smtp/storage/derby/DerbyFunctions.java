package com.bluebox.smtp.storage.derby;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DerbyFunctions {

	public static String replace(String str,String subStr1,String subStr2){
		return str.replace(subStr1, subStr2);
	}

	public static String dayOfWeek(java.sql.Date date ) throws Exception {
		GregorianCalendar    calendar = new GregorianCalendar();

		calendar.setTime( date );

		int weekday = calendar.get( 
				Calendar.DAY_OF_WEEK );

		switch( weekday ) {
		case 1: return "Sunday";
		case 2: return "Monday";
		case 3: return "Tuesday";
		case 4: return "Wednesday";
		case 5: return "Thursday";
		case 6: return "Friday";
		case 7: return "Saturday";
		default: throw new SQLException( "Unknown weekday: " + weekday );
		}
	}
}
