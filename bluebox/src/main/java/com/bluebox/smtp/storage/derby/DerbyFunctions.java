package com.bluebox.smtp.storage.derby;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class DerbyFunctions {

	public static int dayOfWeek(java.sql.Timestamp date ) throws Exception {
		GregorianCalendar    calendar = new GregorianCalendar();

		calendar.setTime( date );

		int weekday = calendar.get( 
				Calendar.DAY_OF_WEEK );

//		switch( weekday ) {
//		case 1: return "Sunday";
//		case 2: return "Monday";
//		case 3: return "Tuesday";
//		case 4: return "Wednesday";
//		case 5: return "Thursday";
//		case 6: return "Friday";
//		case 7: return "Saturday";
//		default: throw new SQLException( "Unknown weekday: " + weekday );
//		}
		return weekday;
	}
}
