package com.mysql.jdbc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;



























public class TimeUtil
{
  static final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");
  

  private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();
  

  private static final String TIME_ZONE_MAPPINGS_RESOURCE = "/com/mysql/jdbc/TimeZoneMapping.properties";
  
  private static Properties timeZoneMappings = null;
  protected static final Method systemNanoTimeMethod;
  
  static
  {
    Method aMethod;
    try
    {
      aMethod = System.class.getMethod("nanoTime", (Class[])null);
    } catch (SecurityException e) {
      aMethod = null;
    } catch (NoSuchMethodException e) {
      aMethod = null;
    }
    
    systemNanoTimeMethod = aMethod;
  }
  
  public static boolean nanoTimeAvailable() {
    return systemNanoTimeMethod != null;
  }
  
  public static final TimeZone getDefaultTimeZone(boolean useCache) {
    return (TimeZone)(useCache ? DEFAULT_TIMEZONE.clone() : TimeZone.getDefault().clone());
  }
  
  public static long getCurrentTimeNanosOrMillis() {
    if (systemNanoTimeMethod != null) {
      try {
        return ((Long)systemNanoTimeMethod.invoke(null, (Object[])null)).longValue();
      }
      catch (IllegalArgumentException e) {}catch (IllegalAccessException e) {}catch (InvocationTargetException e) {}
    }
    





    return System.currentTimeMillis();
  }
  














  public static Time changeTimezone(MySQLConnection conn, Calendar sessionCalendar, Calendar targetCalendar, Time t, TimeZone fromTz, TimeZone toTz, boolean rollForward)
  {
    if (conn != null) {
      if ((conn.getUseTimezone()) && (!conn.getNoTimezoneConversionForTimeType()))
      {
        Calendar fromCal = Calendar.getInstance(fromTz);
        fromCal.setTime(t);
        
        int fromOffset = fromCal.get(15) + fromCal.get(16);
        Calendar toCal = Calendar.getInstance(toTz);
        toCal.setTime(t);
        
        int toOffset = toCal.get(15) + toCal.get(16);
        int offsetDiff = fromOffset - toOffset;
        long toTime = toCal.getTime().getTime();
        
        if (rollForward) {
          toTime += offsetDiff;
        } else {
          toTime -= offsetDiff;
        }
        
        Time changedTime = new Time(toTime);
        
        return changedTime; }
      if ((conn.getUseJDBCCompliantTimezoneShift()) && 
        (targetCalendar != null))
      {
        Time adjustedTime = new Time(jdbcCompliantZoneShift(sessionCalendar, targetCalendar, t));
        
        return adjustedTime;
      }
    }
    

    return t;
  }
  














  public static Timestamp changeTimezone(MySQLConnection conn, Calendar sessionCalendar, Calendar targetCalendar, Timestamp tstamp, TimeZone fromTz, TimeZone toTz, boolean rollForward)
  {
    if (conn != null) {
      if (conn.getUseTimezone())
      {
        Calendar fromCal = Calendar.getInstance(fromTz);
        fromCal.setTime(tstamp);
        
        int fromOffset = fromCal.get(15) + fromCal.get(16);
        Calendar toCal = Calendar.getInstance(toTz);
        toCal.setTime(tstamp);
        
        int toOffset = toCal.get(15) + toCal.get(16);
        int offsetDiff = fromOffset - toOffset;
        long toTime = toCal.getTime().getTime();
        
        if (rollForward) {
          toTime += offsetDiff;
        } else {
          toTime -= offsetDiff;
        }
        
        Timestamp changedTimestamp = new Timestamp(toTime);
        
        return changedTimestamp; }
      if ((conn.getUseJDBCCompliantTimezoneShift()) && 
        (targetCalendar != null))
      {
        Timestamp adjustedTimestamp = new Timestamp(jdbcCompliantZoneShift(sessionCalendar, targetCalendar, tstamp));
        
        adjustedTimestamp.setNanos(tstamp.getNanos());
        
        return adjustedTimestamp;
      }
    }
    

    return tstamp;
  }
  
  private static long jdbcCompliantZoneShift(Calendar sessionCalendar, Calendar targetCalendar, java.util.Date dt) {
    if (sessionCalendar == null) {
      sessionCalendar = new GregorianCalendar();
    }
    
    synchronized (sessionCalendar)
    {

      java.util.Date origCalDate = targetCalendar.getTime();
      java.util.Date origSessionDate = sessionCalendar.getTime();
      try
      {
        sessionCalendar.setTime(dt);
        
        targetCalendar.set(1, sessionCalendar.get(1));
        targetCalendar.set(2, sessionCalendar.get(2));
        targetCalendar.set(5, sessionCalendar.get(5));
        
        targetCalendar.set(11, sessionCalendar.get(11));
        targetCalendar.set(12, sessionCalendar.get(12));
        targetCalendar.set(13, sessionCalendar.get(13));
        targetCalendar.set(14, sessionCalendar.get(14));
        
        long l = targetCalendar.getTime().getTime();jsr 16;return l;
      }
      finally {
        jsr 6; } localObject2 = returnAddress;sessionCalendar.setTime(origSessionDate);
      targetCalendar.setTime(origCalDate);ret;
    }
  }
  

  static final java.sql.Date fastDateCreate(boolean useGmtConversion, Calendar gmtCalIfNeeded, Calendar cal, int year, int month, int day)
  {
    Calendar dateCal = cal;
    
    if (useGmtConversion)
    {
      if (gmtCalIfNeeded == null) {
        gmtCalIfNeeded = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
      }
      
      dateCal = gmtCalIfNeeded;
    }
    
    synchronized (dateCal) {
      java.util.Date origCalDate = dateCal.getTime();
      try {
        dateCal.clear();
        dateCal.set(14, 0);
        

        dateCal.set(year, month - 1, day, 0, 0, 0);
        
        long dateAsMillis = dateCal.getTimeInMillis();
        
        java.sql.Date localDate = new java.sql.Date(dateAsMillis);jsr 17;return localDate;
      } finally {
        jsr 6; } localObject2 = returnAddress;dateCal.setTime(origCalDate);ret;
    }
  }
  


  static final java.sql.Date fastDateCreate(int year, int month, int day, Calendar targetCalendar)
  {
    Calendar dateCal = targetCalendar == null ? new GregorianCalendar() : targetCalendar;
    
    synchronized (dateCal) {
      java.util.Date origCalDate = dateCal.getTime();
      try {
        dateCal.clear();
        

        dateCal.set(year, month - 1, day, 0, 0, 0);
        dateCal.set(14, 0);
        
        long dateAsMillis = dateCal.getTimeInMillis();
        
        java.sql.Date localDate = new java.sql.Date(dateAsMillis);jsr 17;return localDate;
      } finally {
        jsr 6; } localObject2 = returnAddress;dateCal.setTime(origCalDate);ret;
    }
  }
  
  static final Time fastTimeCreate(Calendar cal, int hour, int minute, int second, ExceptionInterceptor exceptionInterceptor) throws SQLException
  {
    if ((hour < 0) || (hour > 24)) {
      throw SQLError.createSQLException("Illegal hour value '" + hour + "' for java.sql.Time type in value '" + timeFormattedString(hour, minute, second) + ".", "S1009", exceptionInterceptor);
    }
    


    if ((minute < 0) || (minute > 59)) {
      throw SQLError.createSQLException("Illegal minute value '" + minute + "' for java.sql.Time type in value '" + timeFormattedString(hour, minute, second) + ".", "S1009", exceptionInterceptor);
    }
    


    if ((second < 0) || (second > 59)) {
      throw SQLError.createSQLException("Illegal minute value '" + second + "' for java.sql.Time type in value '" + timeFormattedString(hour, minute, second) + ".", "S1009", exceptionInterceptor);
    }
    


    synchronized (cal) {
      java.util.Date origCalDate = cal.getTime();
      try {
        cal.clear();
        

        cal.set(1970, 0, 1, hour, minute, second);
        
        long timeAsMillis = cal.getTimeInMillis();
        
        Time localTime = new Time(timeAsMillis);jsr 17;return localTime;
      } finally {
        jsr 6; } localObject2 = returnAddress;cal.setTime(origCalDate);ret;
    }
  }
  
  static final Time fastTimeCreate(int hour, int minute, int second, Calendar targetCalendar, ExceptionInterceptor exceptionInterceptor) throws SQLException
  {
    if ((hour < 0) || (hour > 23)) {
      throw SQLError.createSQLException("Illegal hour value '" + hour + "' for java.sql.Time type in value '" + timeFormattedString(hour, minute, second) + ".", "S1009", exceptionInterceptor);
    }
    


    if ((minute < 0) || (minute > 59)) {
      throw SQLError.createSQLException("Illegal minute value '" + minute + "' for java.sql.Time type in value '" + timeFormattedString(hour, minute, second) + ".", "S1009", exceptionInterceptor);
    }
    


    if ((second < 0) || (second > 59)) {
      throw SQLError.createSQLException("Illegal minute value '" + second + "' for java.sql.Time type in value '" + timeFormattedString(hour, minute, second) + ".", "S1009", exceptionInterceptor);
    }
    


    Calendar cal = targetCalendar == null ? new GregorianCalendar() : targetCalendar;
    
    synchronized (cal) {
      java.util.Date origCalDate = cal.getTime();
      try {
        cal.clear();
        

        cal.set(1970, 0, 1, hour, minute, second);
        
        long timeAsMillis = cal.getTimeInMillis();
        
        Time localTime = new Time(timeAsMillis);jsr 17;return localTime;
      } finally {
        jsr 6; } localObject2 = returnAddress;cal.setTime(origCalDate);ret;
    }
  }
  


  static final Timestamp fastTimestampCreate(boolean useGmtConversion, Calendar gmtCalIfNeeded, Calendar cal, int year, int month, int day, int hour, int minute, int seconds, int secondsPart)
  {
    synchronized (cal) {
      java.util.Date origCalDate = cal.getTime();
      try {
        cal.clear();
        

        cal.set(year, month - 1, day, hour, minute, seconds);
        
        int offsetDiff = 0;
        
        if (useGmtConversion) {
          int fromOffset = cal.get(15) + cal.get(16);
          
          if (gmtCalIfNeeded == null) {
            gmtCalIfNeeded = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
          }
          gmtCalIfNeeded.clear();
          
          gmtCalIfNeeded.setTimeInMillis(cal.getTimeInMillis());
          
          int toOffset = gmtCalIfNeeded.get(15) + gmtCalIfNeeded.get(16);
          offsetDiff = fromOffset - toOffset;
        }
        
        if (secondsPart != 0) {
          cal.set(14, secondsPart / 1000000);
        }
        
        long tsAsMillis = cal.getTimeInMillis();
        
        Timestamp ts = new Timestamp(tsAsMillis + offsetDiff);
        
        ts.setNanos(secondsPart);
        
        Timestamp localTimestamp1 = ts;jsr 17;return localTimestamp1;
      } finally {
        jsr 6; } localObject2 = returnAddress;cal.setTime(origCalDate);ret;
    }
  }
  
  static final Timestamp fastTimestampCreate(TimeZone tz, int year, int month, int day, int hour, int minute, int seconds, int secondsPart)
  {
    Calendar cal = tz == null ? new GregorianCalendar() : new GregorianCalendar(tz);
    cal.clear();
    

    cal.set(year, month - 1, day, hour, minute, seconds);
    
    long tsAsMillis = cal.getTimeInMillis();
    
    Timestamp ts = new Timestamp(tsAsMillis);
    ts.setNanos(secondsPart);
    
    return ts;
  }
  









  public static String getCanonicalTimezone(String timezoneStr, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    if (timezoneStr == null) {
      return null;
    }
    
    timezoneStr = timezoneStr.trim();
    

    if ((timezoneStr.length() > 2) && 
      ((timezoneStr.charAt(0) == '+') || (timezoneStr.charAt(0) == '-')) && (Character.isDigit(timezoneStr.charAt(1)))) {
      return "GMT" + timezoneStr;
    }
    

    synchronized (TimeUtil.class) {
      if (timeZoneMappings == null) {
        loadTimeZoneMappings(exceptionInterceptor);
      }
    }
    
    String canonicalTz;
    if ((canonicalTz = timeZoneMappings.getProperty(timezoneStr)) != null) {
      return canonicalTz;
    }
    
    throw SQLError.createSQLException(Messages.getString("TimeUtil.UnrecognizedTimezoneId", new Object[] { timezoneStr }), "01S00", exceptionInterceptor);
  }
  


  private static String timeFormattedString(int hours, int minutes, int seconds)
  {
    StringBuilder buf = new StringBuilder(8);
    if (hours < 10) {
      buf.append("0");
    }
    
    buf.append(hours);
    buf.append(":");
    
    if (minutes < 10) {
      buf.append("0");
    }
    
    buf.append(minutes);
    buf.append(":");
    
    if (seconds < 10) {
      buf.append("0");
    }
    
    buf.append(seconds);
    
    return buf.toString();
  }
  

  public static String formatNanos(int nanos, boolean serverSupportsFracSecs, boolean usingMicros)
  {
    if (nanos > 999999999) {
      nanos %= 100000000;
    }
    
    if (usingMicros) {
      nanos /= 1000;
    }
    
    if ((!serverSupportsFracSecs) || (nanos == 0)) {
      return "0";
    }
    
    int digitCount = usingMicros ? 6 : 9;
    
    String nanosString = Integer.toString(nanos);
    String zeroPadding = usingMicros ? "000000" : "000000000";
    
    nanosString = zeroPadding.substring(0, digitCount - nanosString.length()) + nanosString;
    
    int pos = digitCount - 1;
    
    while (nanosString.charAt(pos) == '0') {
      pos--;
    }
    
    nanosString = nanosString.substring(0, pos + 1);
    
    return nanosString;
  }
  




  private static void loadTimeZoneMappings(ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    timeZoneMappings = new Properties();
    try {
      timeZoneMappings.load(TimeUtil.class.getResourceAsStream("/com/mysql/jdbc/TimeZoneMapping.properties"));
    } catch (IOException e) {
      throw SQLError.createSQLException(Messages.getString("TimeUtil.LoadTimeZoneMappingError"), "01S00", exceptionInterceptor);
    }
    

    for (String tz : TimeZone.getAvailableIDs()) {
      if (!timeZoneMappings.containsKey(tz)) {
        timeZoneMappings.put(tz, tz);
      }
    }
  }
  
  public static Timestamp truncateFractionalSeconds(Timestamp timestamp) {
    Timestamp truncatedTimestamp = new Timestamp(timestamp.getTime());
    truncatedTimestamp.setNanos(0);
    return truncatedTimestamp;
  }
  
  public TimeUtil() {}
}
