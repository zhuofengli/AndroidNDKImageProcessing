package com.zhuofengli.blackwhitephotos;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.util.Log;

public class TimeUtils {
	private static long minute = 1;
	private static long hour =     60 ;
	private static long day =      60 *24 ;
	private static long week =     60 *24 * 7 ;
	private static long month =    60 *24 * 30  ;
	private static long year =     60 *24 * 30 *12 ;
	
	public static String getDisplayTime(String datetime, Context con) {
	
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			df.setTimeZone(TimeZone.getDefault());
			if(datetime == null || datetime.length() <=0 )
				return null;
			Date input = df.parse(datetime);
			Date now = new Date();
			long sb = now.getTime() - input.getTime();
			
			return getTimeStr(sb,con);
			
		} catch (Exception e) {
			Log.e("TimeHelper", e.toString());
		}
		return null;
	}
	
	
	
	private static String getTimeStr(long ti, Context con) {
		
		  String result ="";
		  int t = (int )  (ti/(long) 60000);
          if(t< minute)
          {
        	  result =  "0"+con.getString(R.string.minutes);
          }
          else if(t< hour)
          {
        	  result =   (int)( t/minute)+con.getString(R.string.minutes); 
          }
          else if(t<day)
          {
        	  result =  (int)( t/hour)+con.getString(R.string.hours); 
          }
          else if(t<week)
          {
        	  result = (int)( t/day)+con.getString(R.string.days); 
          }
          else if(t<month)
          {
        	  result =  (int)( t/week)+con.getString(R.string.weeks); 
          }
          else if(t<year)
          {
        	  result =  (int)( t/month)+con.getString(R.string.months); 
          }
          else 
          {
        	  result =  (int)( t/year)+con.getString(R.string.years); 
          }
          return con.getString(R.string.ago_prefix) + result + con.getString(R.string.ago);
		
	}
	
	public static String getCurrentDateTime(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateandTime = sdf.format(new Date());
		
		return currentDateandTime;
	}
}
