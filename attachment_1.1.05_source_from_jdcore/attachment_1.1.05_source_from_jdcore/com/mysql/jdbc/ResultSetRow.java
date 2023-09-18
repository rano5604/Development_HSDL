package com.mysql.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;



























public abstract class ResultSetRow
{
  protected ExceptionInterceptor exceptionInterceptor;
  protected Field[] metadata;
  
  protected ResultSetRow(ExceptionInterceptor exceptionInterceptor)
  {
    this.exceptionInterceptor = exceptionInterceptor;
  }
  








  public abstract void closeOpenStreams();
  







  public abstract InputStream getBinaryInputStream(int paramInt)
    throws SQLException;
  







  public abstract byte[] getColumnValue(int paramInt)
    throws SQLException;
  







  protected final Date getDateFast(int columnIndex, byte[] dateAsBytes, int offset, int length, MySQLConnection conn, ResultSetImpl rs, Calendar targetCalendar)
    throws SQLException
  {
    int year = 0;
    int month = 0;
    int day = 0;
    try
    {
      if (dateAsBytes == null) {
        return null;
      }
      
      boolean allZeroDate = true;
      
      boolean onlyTimePresent = false;
      
      for (int i = 0; i < length; i++) {
        if (dateAsBytes[(offset + i)] == 58) {
          onlyTimePresent = true;
          break;
        }
      }
      
      for (int i = 0; i < length; i++) {
        byte b = dateAsBytes[(offset + i)];
        
        if ((b == 32) || (b == 45) || (b == 47)) {
          onlyTimePresent = false;
        }
        
        if ((b != 48) && (b != 32) && (b != 58) && (b != 45) && (b != 47) && (b != 46)) {
          allZeroDate = false;
          
          break;
        }
      }
      

      int decimalIndex = -1;
      for (int i = 0; i < length; i++) {
        if (dateAsBytes[(offset + i)] == 46) {
          decimalIndex = i;
          break;
        }
      }
      

      if (decimalIndex > -1) {
        length = decimalIndex;
      }
      
      if ((!onlyTimePresent) && (allZeroDate))
      {
        if ("convertToNull".equals(conn.getZeroDateTimeBehavior()))
        {
          return null; }
        if ("exception".equals(conn.getZeroDateTimeBehavior())) {
          throw SQLError.createSQLException("Value '" + StringUtils.toString(dateAsBytes) + "' can not be represented as java.sql.Date", "S1009", exceptionInterceptor);
        }
        


        return rs.fastDateCreate(targetCalendar, 1, 1, 1);
      }
      if (metadata[columnIndex].getMysqlType() == 7)
      {
        switch (length) {
        case 19: 
        case 21: 
        case 29: 
          year = StringUtils.getInt(dateAsBytes, offset + 0, offset + 4);
          month = StringUtils.getInt(dateAsBytes, offset + 5, offset + 7);
          day = StringUtils.getInt(dateAsBytes, offset + 8, offset + 10);
          
          return rs.fastDateCreate(targetCalendar, year, month, day);
        

        case 8: 
        case 14: 
          year = StringUtils.getInt(dateAsBytes, offset + 0, offset + 4);
          month = StringUtils.getInt(dateAsBytes, offset + 4, offset + 6);
          day = StringUtils.getInt(dateAsBytes, offset + 6, offset + 8);
          
          return rs.fastDateCreate(targetCalendar, year, month, day);
        

        case 6: 
        case 10: 
        case 12: 
          year = StringUtils.getInt(dateAsBytes, offset + 0, offset + 2);
          
          if (year <= 69) {
            year += 100;
          }
          
          month = StringUtils.getInt(dateAsBytes, offset + 2, offset + 4);
          day = StringUtils.getInt(dateAsBytes, offset + 4, offset + 6);
          
          return rs.fastDateCreate(targetCalendar, year + 1900, month, day);
        

        case 4: 
          year = StringUtils.getInt(dateAsBytes, offset + 0, offset + 4);
          
          if (year <= 69) {
            year += 100;
          }
          
          month = StringUtils.getInt(dateAsBytes, offset + 2, offset + 4);
          
          return rs.fastDateCreate(targetCalendar, year + 1900, month, 1);
        

        case 2: 
          year = StringUtils.getInt(dateAsBytes, offset + 0, offset + 2);
          
          if (year <= 69) {
            year += 100;
          }
          
          return rs.fastDateCreate(targetCalendar, year + 1900, 1, 1);
        }
        
        
        throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_Date", new Object[] { StringUtils.toString(dateAsBytes), Integer.valueOf(columnIndex + 1) }), "S1009", exceptionInterceptor);
      }
      


      if (metadata[columnIndex].getMysqlType() == 13)
      {
        if ((length == 2) || (length == 1)) {
          year = StringUtils.getInt(dateAsBytes, offset, offset + length);
          
          if (year <= 69) {
            year += 100;
          }
          
          year += 1900;
        } else {
          year = StringUtils.getInt(dateAsBytes, offset + 0, offset + 4);
        }
        
        return rs.fastDateCreate(targetCalendar, year, 1, 1); }
      if (metadata[columnIndex].getMysqlType() == 11) {
        return rs.fastDateCreate(targetCalendar, 1970, 1, 1);
      }
      if (length < 10) {
        if (length == 8) {
          return rs.fastDateCreate(targetCalendar, 1970, 1, 1);
        }
        

        throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_Date", new Object[] { StringUtils.toString(dateAsBytes), Integer.valueOf(columnIndex + 1) }), "S1009", exceptionInterceptor);
      }
      



      if (length != 18) {
        year = StringUtils.getInt(dateAsBytes, offset + 0, offset + 4);
        month = StringUtils.getInt(dateAsBytes, offset + 5, offset + 7);
        day = StringUtils.getInt(dateAsBytes, offset + 8, offset + 10);
      }
      else {
        StringTokenizer st = new StringTokenizer(StringUtils.toString(dateAsBytes, offset, length, "ISO8859_1"), "- ");
        
        year = Integer.parseInt(st.nextToken());
        month = Integer.parseInt(st.nextToken());
        day = Integer.parseInt(st.nextToken());
      }
      

      return rs.fastDateCreate(targetCalendar, year, month, day);
    } catch (SQLException sqlEx) {
      throw sqlEx;
    } catch (Exception e) {
      SQLException sqlEx = SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_Date", new Object[] { StringUtils.toString(dateAsBytes), Integer.valueOf(columnIndex + 1) }), "S1009", exceptionInterceptor);
      

      sqlEx.initCause(e);
      
      throw sqlEx;
    }
  }
  







  public abstract Date getDateFast(int paramInt, MySQLConnection paramMySQLConnection, ResultSetImpl paramResultSetImpl, Calendar paramCalendar)
    throws SQLException;
  







  public abstract int getInt(int paramInt)
    throws SQLException;
  







  public abstract long getLong(int paramInt)
    throws SQLException;
  






  protected Date getNativeDate(int columnIndex, byte[] bits, int offset, int length, MySQLConnection conn, ResultSetImpl rs, Calendar cal)
    throws SQLException
  {
    int year = 0;
    int month = 0;
    int day = 0;
    
    if (length != 0) {
      year = bits[(offset + 0)] & 0xFF | (bits[(offset + 1)] & 0xFF) << 8;
      
      month = bits[(offset + 2)];
      day = bits[(offset + 3)];
    }
    
    if ((length == 0) || ((year == 0) && (month == 0) && (day == 0))) {
      if ("convertToNull".equals(conn.getZeroDateTimeBehavior()))
        return null;
      if ("exception".equals(conn.getZeroDateTimeBehavior())) {
        throw SQLError.createSQLException("Value '0000-00-00' can not be represented as java.sql.Date", "S1009", exceptionInterceptor);
      }
      

      year = 1;
      month = 1;
      day = 1;
    }
    
    if (!useLegacyDatetimeCode) {
      return TimeUtil.fastDateCreate(year, month, day, cal);
    }
    
    return rs.fastDateCreate(cal == null ? rs.getCalendarInstanceForSessionOrNew() : cal, year, month, day);
  }
  
  public abstract Date getNativeDate(int paramInt, MySQLConnection paramMySQLConnection, ResultSetImpl paramResultSetImpl, Calendar paramCalendar)
    throws SQLException;
  
  protected Object getNativeDateTimeValue(int columnIndex, byte[] bits, int offset, int length, Calendar targetCalendar, int jdbcType, int mysqlType, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs) throws SQLException
  {
    int year = 0;
    int month = 0;
    int day = 0;
    
    int hour = 0;
    int minute = 0;
    int seconds = 0;
    
    int nanos = 0;
    
    if (bits == null)
    {
      return null;
    }
    
    Calendar sessionCalendar = conn.getUseJDBCCompliantTimezoneShift() ? conn.getUtcCalendar() : rs.getCalendarInstanceForSessionOrNew();
    
    boolean populatedFromDateTimeValue = false;
    
    switch (mysqlType) {
    case 7: 
    case 12: 
      populatedFromDateTimeValue = true;
      
      if (length != 0) {
        year = bits[(offset + 0)] & 0xFF | (bits[(offset + 1)] & 0xFF) << 8;
        month = bits[(offset + 2)];
        day = bits[(offset + 3)];
        
        if (length > 4) {
          hour = bits[(offset + 4)];
          minute = bits[(offset + 5)];
          seconds = bits[(offset + 6)];
        }
        
        if (length > 7)
        {
          nanos = (bits[(offset + 7)] & 0xFF | (bits[(offset + 8)] & 0xFF) << 8 | (bits[(offset + 9)] & 0xFF) << 16 | (bits[(offset + 10)] & 0xFF) << 24) * 1000;
        }
      }
      

      break;
    case 10: 
      populatedFromDateTimeValue = true;
      
      if (length != 0) {
        year = bits[(offset + 0)] & 0xFF | (bits[(offset + 1)] & 0xFF) << 8;
        month = bits[(offset + 2)];
        day = bits[(offset + 3)];
      }
      
      break;
    case 11: 
      populatedFromDateTimeValue = true;
      
      if (length != 0)
      {

        hour = bits[(offset + 5)];
        minute = bits[(offset + 6)];
        seconds = bits[(offset + 7)];
      }
      
      year = 1970;
      month = 1;
      day = 1;
      
      break;
    case 8: case 9: default: 
      populatedFromDateTimeValue = false;
    }
    
    switch (jdbcType) {
    case 92: 
      if (populatedFromDateTimeValue) {
        if (!useLegacyDatetimeCode) {
          return TimeUtil.fastTimeCreate(hour, minute, seconds, targetCalendar, exceptionInterceptor);
        }
        
        Time time = TimeUtil.fastTimeCreate(rs.getCalendarInstanceForSessionOrNew(), hour, minute, seconds, exceptionInterceptor);
        
        Time adjustedTime = TimeUtil.changeTimezone(conn, sessionCalendar, targetCalendar, time, conn.getServerTimezoneTZ(), tz, rollForward);
        
        return adjustedTime;
      }
      
      return rs.getNativeTimeViaParseConversion(columnIndex + 1, targetCalendar, tz, rollForward);
    
    case 91: 
      if (populatedFromDateTimeValue) {
        if ((year == 0) && (month == 0) && (day == 0)) {
          if ("convertToNull".equals(conn.getZeroDateTimeBehavior()))
          {
            return null; }
          if ("exception".equals(conn.getZeroDateTimeBehavior())) {
            throw new SQLException("Value '0000-00-00' can not be represented as java.sql.Date", "S1009");
          }
          
          year = 1;
          month = 1;
          day = 1;
        }
        
        if (!useLegacyDatetimeCode) {
          return TimeUtil.fastDateCreate(year, month, day, targetCalendar);
        }
        
        return rs.fastDateCreate(rs.getCalendarInstanceForSessionOrNew(), year, month, day);
      }
      
      return rs.getNativeDateViaParseConversion(columnIndex + 1);
    case 93: 
      if (populatedFromDateTimeValue) {
        if ((year == 0) && (month == 0) && (day == 0)) {
          if ("convertToNull".equals(conn.getZeroDateTimeBehavior()))
          {
            return null; }
          if ("exception".equals(conn.getZeroDateTimeBehavior())) {
            throw new SQLException("Value '0000-00-00' can not be represented as java.sql.Timestamp", "S1009");
          }
          
          year = 1;
          month = 1;
          day = 1;
        }
        
        if (!useLegacyDatetimeCode) {
          return TimeUtil.fastTimestampCreate(tz, year, month, day, hour, minute, seconds, nanos);
        }
        
        Timestamp ts = rs.fastTimestampCreate(rs.getCalendarInstanceForSessionOrNew(), year, month, day, hour, minute, seconds, nanos);
        
        Timestamp adjustedTs = TimeUtil.changeTimezone(conn, sessionCalendar, targetCalendar, ts, conn.getServerTimezoneTZ(), tz, rollForward);
        
        return adjustedTs;
      }
      
      return rs.getNativeTimestampViaParseConversion(columnIndex + 1, targetCalendar, tz, rollForward);
    }
    
    throw new SQLException("Internal error - conversion method doesn't support this type", "S1000");
  }
  
  public abstract Object getNativeDateTimeValue(int paramInt1, Calendar paramCalendar, int paramInt2, int paramInt3, TimeZone paramTimeZone, boolean paramBoolean, MySQLConnection paramMySQLConnection, ResultSetImpl paramResultSetImpl)
    throws SQLException;
  
  protected double getNativeDouble(byte[] bits, int offset)
  {
    long valueAsLong = bits[(offset + 0)] & 0xFF | (bits[(offset + 1)] & 0xFF) << 8 | (bits[(offset + 2)] & 0xFF) << 16 | (bits[(offset + 3)] & 0xFF) << 24 | (bits[(offset + 4)] & 0xFF) << 32 | (bits[(offset + 5)] & 0xFF) << 40 | (bits[(offset + 6)] & 0xFF) << 48 | (bits[(offset + 7)] & 0xFF) << 56;
    


    return Double.longBitsToDouble(valueAsLong);
  }
  
  public abstract double getNativeDouble(int paramInt) throws SQLException;
  
  protected float getNativeFloat(byte[] bits, int offset) {
    int asInt = bits[(offset + 0)] & 0xFF | (bits[(offset + 1)] & 0xFF) << 8 | (bits[(offset + 2)] & 0xFF) << 16 | (bits[(offset + 3)] & 0xFF) << 24;
    
    return Float.intBitsToFloat(asInt);
  }
  
  public abstract float getNativeFloat(int paramInt) throws SQLException;
  
  protected int getNativeInt(byte[] bits, int offset)
  {
    int valueAsInt = bits[(offset + 0)] & 0xFF | (bits[(offset + 1)] & 0xFF) << 8 | (bits[(offset + 2)] & 0xFF) << 16 | (bits[(offset + 3)] & 0xFF) << 24;
    
    return valueAsInt;
  }
  
  public abstract int getNativeInt(int paramInt) throws SQLException;
  
  protected long getNativeLong(byte[] bits, int offset) {
    long valueAsLong = bits[(offset + 0)] & 0xFF | (bits[(offset + 1)] & 0xFF) << 8 | (bits[(offset + 2)] & 0xFF) << 16 | (bits[(offset + 3)] & 0xFF) << 24 | (bits[(offset + 4)] & 0xFF) << 32 | (bits[(offset + 5)] & 0xFF) << 40 | (bits[(offset + 6)] & 0xFF) << 48 | (bits[(offset + 7)] & 0xFF) << 56;
    


    return valueAsLong;
  }
  
  public abstract long getNativeLong(int paramInt) throws SQLException;
  
  protected short getNativeShort(byte[] bits, int offset) {
    short asShort = (short)(bits[(offset + 0)] & 0xFF | (bits[(offset + 1)] & 0xFF) << 8);
    
    return asShort;
  }
  






  public abstract short getNativeShort(int paramInt)
    throws SQLException;
  





  protected Time getNativeTime(int columnIndex, byte[] bits, int offset, int length, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    int hour = 0;
    int minute = 0;
    int seconds = 0;
    
    if (length != 0)
    {

      hour = bits[(offset + 5)];
      minute = bits[(offset + 6)];
      seconds = bits[(offset + 7)];
    }
    
    if (!useLegacyDatetimeCode) {
      return TimeUtil.fastTimeCreate(hour, minute, seconds, targetCalendar, exceptionInterceptor);
    }
    
    Calendar sessionCalendar = rs.getCalendarInstanceForSessionOrNew();
    
    Time time = TimeUtil.fastTimeCreate(sessionCalendar, hour, minute, seconds, exceptionInterceptor);
    
    Time adjustedTime = TimeUtil.changeTimezone(conn, sessionCalendar, targetCalendar, time, conn.getServerTimezoneTZ(), tz, rollForward);
    
    return adjustedTime;
  }
  
  public abstract Time getNativeTime(int paramInt, Calendar paramCalendar, TimeZone paramTimeZone, boolean paramBoolean, MySQLConnection paramMySQLConnection, ResultSetImpl paramResultSetImpl)
    throws SQLException;
  
  protected Timestamp getNativeTimestamp(byte[] bits, int offset, int length, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs) throws SQLException
  {
    int year = 0;
    int month = 0;
    int day = 0;
    
    int hour = 0;
    int minute = 0;
    int seconds = 0;
    
    int nanos = 0;
    
    if (length != 0) {
      year = bits[(offset + 0)] & 0xFF | (bits[(offset + 1)] & 0xFF) << 8;
      month = bits[(offset + 2)];
      day = bits[(offset + 3)];
      
      if (length > 4) {
        hour = bits[(offset + 4)];
        minute = bits[(offset + 5)];
        seconds = bits[(offset + 6)];
      }
      
      if (length > 7)
      {
        nanos = (bits[(offset + 7)] & 0xFF | (bits[(offset + 8)] & 0xFF) << 8 | (bits[(offset + 9)] & 0xFF) << 16 | (bits[(offset + 10)] & 0xFF) << 24) * 1000;
      }
    }
    

    if ((length == 0) || ((year == 0) && (month == 0) && (day == 0))) {
      if ("convertToNull".equals(conn.getZeroDateTimeBehavior()))
      {
        return null; }
      if ("exception".equals(conn.getZeroDateTimeBehavior())) {
        throw SQLError.createSQLException("Value '0000-00-00' can not be represented as java.sql.Timestamp", "S1009", exceptionInterceptor);
      }
      

      year = 1;
      month = 1;
      day = 1;
    }
    
    if (!useLegacyDatetimeCode) {
      return TimeUtil.fastTimestampCreate(tz, year, month, day, hour, minute, seconds, nanos);
    }
    
    Calendar sessionCalendar = conn.getUseJDBCCompliantTimezoneShift() ? conn.getUtcCalendar() : rs.getCalendarInstanceForSessionOrNew();
    
    Timestamp ts = rs.fastTimestampCreate(sessionCalendar, year, month, day, hour, minute, seconds, nanos);
    
    Timestamp adjustedTs = TimeUtil.changeTimezone(conn, sessionCalendar, targetCalendar, ts, conn.getServerTimezoneTZ(), tz, rollForward);
    
    return adjustedTs;
  }
  









  public abstract Timestamp getNativeTimestamp(int paramInt, Calendar paramCalendar, TimeZone paramTimeZone, boolean paramBoolean, MySQLConnection paramMySQLConnection, ResultSetImpl paramResultSetImpl)
    throws SQLException;
  









  public abstract Reader getReader(int paramInt)
    throws SQLException;
  









  public abstract String getString(int paramInt, String paramString, MySQLConnection paramMySQLConnection)
    throws SQLException;
  








  protected String getString(String encoding, MySQLConnection conn, byte[] value, int offset, int length)
    throws SQLException
  {
    String stringVal = null;
    
    if ((conn != null) && (conn.getUseUnicode())) {
      try {
        if (encoding == null) {
          stringVal = StringUtils.toString(value);
        } else {
          SingleByteCharsetConverter converter = conn.getCharsetConverter(encoding);
          
          if (converter != null) {
            stringVal = converter.toString(value, offset, length);
          } else {
            stringVal = StringUtils.toString(value, offset, length, encoding);
          }
        }
      } catch (UnsupportedEncodingException E) {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Unsupported_character_encoding____101") + encoding + "'.", "0S100", exceptionInterceptor);
      }
      
    } else {
      stringVal = StringUtils.toAsciiString(value, offset, length);
    }
    
    return stringVal;
  }
  
  protected Time getTimeFast(int columnIndex, byte[] timeAsBytes, int offset, int fullLength, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    int hr = 0;
    int min = 0;
    int sec = 0;
    int nanos = 0;
    
    int decimalIndex = -1;
    
    try
    {
      if (timeAsBytes == null) {
        return null;
      }
      
      boolean allZeroTime = true;
      boolean onlyTimePresent = false;
      
      for (int i = 0; i < fullLength; i++) {
        if (timeAsBytes[(offset + i)] == 58) {
          onlyTimePresent = true;
          break;
        }
      }
      
      for (int i = 0; i < fullLength; i++) {
        if (timeAsBytes[(offset + i)] == 46) {
          decimalIndex = i;
          break;
        }
      }
      
      for (int i = 0; i < fullLength; i++) {
        byte b = timeAsBytes[(offset + i)];
        
        if ((b == 32) || (b == 45) || (b == 47)) {
          onlyTimePresent = false;
        }
        
        if ((b != 48) && (b != 32) && (b != 58) && (b != 45) && (b != 47) && (b != 46)) {
          allZeroTime = false;
          
          break;
        }
      }
      
      if ((!onlyTimePresent) && (allZeroTime)) {
        if ("convertToNull".equals(conn.getZeroDateTimeBehavior()))
          return null;
        if ("exception".equals(conn.getZeroDateTimeBehavior())) {
          throw SQLError.createSQLException("Value '" + StringUtils.toString(timeAsBytes) + "' can not be represented as java.sql.Time", "S1009", exceptionInterceptor);
        }
        


        return rs.fastTimeCreate(targetCalendar, 0, 0, 0);
      }
      
      Field timeColField = metadata[columnIndex];
      
      int length = fullLength;
      
      if (decimalIndex != -1)
      {
        length = decimalIndex;
        
        if (decimalIndex + 2 <= fullLength) {
          nanos = StringUtils.getInt(timeAsBytes, offset + decimalIndex + 1, offset + fullLength);
          
          int numDigits = fullLength - (decimalIndex + 1);
          
          if (numDigits < 9) {
            int factor = (int)Math.pow(10.0D, 9 - numDigits);
            nanos *= factor;
          }
        } else {
          throw new IllegalArgumentException();
        }
      }
      


      SQLWarning precisionLost;
      

      if (timeColField.getMysqlType() == 7)
      {
        switch (length)
        {
        case 19: 
          hr = StringUtils.getInt(timeAsBytes, offset + length - 8, offset + length - 6);
          min = StringUtils.getInt(timeAsBytes, offset + length - 5, offset + length - 3);
          sec = StringUtils.getInt(timeAsBytes, offset + length - 2, offset + length);
          

          break;
        case 12: 
        case 14: 
          hr = StringUtils.getInt(timeAsBytes, offset + length - 6, offset + length - 4);
          min = StringUtils.getInt(timeAsBytes, offset + length - 4, offset + length - 2);
          sec = StringUtils.getInt(timeAsBytes, offset + length - 2, offset + length);
          

          break;
        
        case 10: 
          hr = StringUtils.getInt(timeAsBytes, offset + 6, offset + 8);
          min = StringUtils.getInt(timeAsBytes, offset + 8, offset + 10);
          sec = 0;
          

          break;
        case 11: case 13: case 15: case 16: 
        case 17: case 18: default: 
          throw SQLError.createSQLException(Messages.getString("ResultSet.Timestamp_too_small_to_convert_to_Time_value_in_column__257") + (columnIndex + 1) + "(" + timeColField + ").", "S1009", exceptionInterceptor);
        }
        
        

        precisionLost = new SQLWarning(Messages.getString("ResultSet.Precision_lost_converting_TIMESTAMP_to_Time_with_getTime()_on_column__261") + columnIndex + "(" + timeColField + ").");
      }
      else
      {
        SQLWarning precisionLost;
        


        if (timeColField.getMysqlType() == 12) {
          hr = StringUtils.getInt(timeAsBytes, offset + 11, offset + 13);
          min = StringUtils.getInt(timeAsBytes, offset + 14, offset + 16);
          sec = StringUtils.getInt(timeAsBytes, offset + 17, offset + 19);
          

          precisionLost = new SQLWarning(Messages.getString("ResultSet.Precision_lost_converting_DATETIME_to_Time_with_getTime()_on_column__264") + (columnIndex + 1) + "(" + timeColField + ").");



        }
        else
        {


          if (timeColField.getMysqlType() == 10) {
            return rs.fastTimeCreate(null, 0, 0, 0);
          }
          

          if ((length != 5) && (length != 8)) {
            throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_Time____267") + StringUtils.toString(timeAsBytes) + Messages.getString("ResultSet.___in_column__268") + (columnIndex + 1), "S1009", exceptionInterceptor);
          }
          



          hr = StringUtils.getInt(timeAsBytes, offset + 0, offset + 2);
          min = StringUtils.getInt(timeAsBytes, offset + 3, offset + 5);
          sec = length == 5 ? 0 : StringUtils.getInt(timeAsBytes, offset + 6, offset + 8);
        }
      }
      Calendar sessionCalendar = rs.getCalendarInstanceForSessionOrNew();
      
      if (!useLegacyDatetimeCode)
      {


        return rs.fastTimeCreate(targetCalendar, hr, min, sec);
      }
      
      return TimeUtil.changeTimezone(conn, sessionCalendar, targetCalendar, rs.fastTimeCreate(sessionCalendar, hr, min, sec), conn.getServerTimezoneTZ(), tz, rollForward);

    }
    catch (RuntimeException ex)
    {

      SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", exceptionInterceptor);
      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
  }
  
  public abstract Time getTimeFast(int paramInt, Calendar paramCalendar, TimeZone paramTimeZone, boolean paramBoolean, MySQLConnection paramMySQLConnection, ResultSetImpl paramResultSetImpl)
    throws SQLException;
  
  protected Timestamp getTimestampFast(int columnIndex, byte[] timestampAsBytes, int offset, int length, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs) throws SQLException
  {
    try
    {
      Calendar sessionCalendar = conn.getUseJDBCCompliantTimezoneShift() ? conn.getUtcCalendar() : rs.getCalendarInstanceForSessionOrNew();
      
      boolean allZeroTimestamp = true;
      
      boolean onlyTimePresent = false;
      
      for (int i = 0; i < length; i++) {
        if (timestampAsBytes[(offset + i)] == 58) {
          onlyTimePresent = true;
          break;
        }
      }
      
      for (int i = 0; i < length; i++) {
        byte b = timestampAsBytes[(offset + i)];
        
        if ((b == 32) || (b == 45) || (b == 47)) {
          onlyTimePresent = false;
        }
        
        if ((b != 48) && (b != 32) && (b != 58) && (b != 45) && (b != 47) && (b != 46)) {
          allZeroTimestamp = false;
          
          break;
        }
      }
      
      if ((!onlyTimePresent) && (allZeroTimestamp))
      {
        if ("convertToNull".equals(conn.getZeroDateTimeBehavior()))
        {
          return null; }
        if ("exception".equals(conn.getZeroDateTimeBehavior())) {
          throw SQLError.createSQLException("Value '" + StringUtils.toString(timestampAsBytes) + "' can not be represented as java.sql.Timestamp", "S1009", exceptionInterceptor);
        }
        

        if (!useLegacyDatetimeCode) {
          return TimeUtil.fastTimestampCreate(tz, 1, 1, 1, 0, 0, 0, 0);
        }
        
        return rs.fastTimestampCreate(null, 1, 1, 1, 0, 0, 0, 0);
      }
      if (metadata[columnIndex].getMysqlType() == 13)
      {
        if (!useLegacyDatetimeCode) {
          return TimeUtil.fastTimestampCreate(tz, StringUtils.getInt(timestampAsBytes, offset, 4), 1, 1, 0, 0, 0, 0);
        }
        
        return TimeUtil.changeTimezone(conn, sessionCalendar, targetCalendar, rs.fastTimestampCreate(sessionCalendar, StringUtils.getInt(timestampAsBytes, offset, 4), 1, 1, 0, 0, 0, 0), conn.getServerTimezoneTZ(), tz, rollForward);
      }
      



      int year = 0;
      int month = 0;
      int day = 0;
      int hour = 0;
      int minutes = 0;
      int seconds = 0;
      int nanos = 0;
      

      int decimalIndex = -1;
      for (int i = 0; i < length; i++) {
        if (timestampAsBytes[(offset + i)] == 46) {
          decimalIndex = i;
          break;
        }
      }
      
      if (decimalIndex == offset + length - 1)
      {
        length--;
      }
      else if (decimalIndex != -1) {
        if (decimalIndex + 2 <= length) {
          nanos = StringUtils.getInt(timestampAsBytes, offset + decimalIndex + 1, offset + length);
          
          int numDigits = length - (decimalIndex + 1);
          
          if (numDigits < 9) {
            int factor = (int)Math.pow(10.0D, 9 - numDigits);
            nanos *= factor;
          }
        } else {
          throw new IllegalArgumentException();
        }
        

        length = decimalIndex;
      }
      
      switch (length) {
      case 19: 
      case 20: 
      case 21: 
      case 22: 
      case 23: 
      case 24: 
      case 25: 
      case 26: 
      case 29: 
        year = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 4);
        month = StringUtils.getInt(timestampAsBytes, offset + 5, offset + 7);
        day = StringUtils.getInt(timestampAsBytes, offset + 8, offset + 10);
        hour = StringUtils.getInt(timestampAsBytes, offset + 11, offset + 13);
        minutes = StringUtils.getInt(timestampAsBytes, offset + 14, offset + 16);
        seconds = StringUtils.getInt(timestampAsBytes, offset + 17, offset + 19);
        
        break;
      

      case 14: 
        year = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 4);
        month = StringUtils.getInt(timestampAsBytes, offset + 4, offset + 6);
        day = StringUtils.getInt(timestampAsBytes, offset + 6, offset + 8);
        hour = StringUtils.getInt(timestampAsBytes, offset + 8, offset + 10);
        minutes = StringUtils.getInt(timestampAsBytes, offset + 10, offset + 12);
        seconds = StringUtils.getInt(timestampAsBytes, offset + 12, offset + 14);
        
        break;
      

      case 12: 
        year = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 2);
        
        if (year <= 69) {
          year += 100;
        }
        
        year += 1900;
        
        month = StringUtils.getInt(timestampAsBytes, offset + 2, offset + 4);
        day = StringUtils.getInt(timestampAsBytes, offset + 4, offset + 6);
        hour = StringUtils.getInt(timestampAsBytes, offset + 6, offset + 8);
        minutes = StringUtils.getInt(timestampAsBytes, offset + 8, offset + 10);
        seconds = StringUtils.getInt(timestampAsBytes, offset + 10, offset + 12);
        
        break;
      

      case 10: 
        boolean hasDash = false;
        
        for (int i = 0; i < length; i++) {
          if (timestampAsBytes[(offset + i)] == 45) {
            hasDash = true;
            break;
          }
        }
        
        if ((metadata[columnIndex].getMysqlType() == 10) || (hasDash)) {
          year = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 4);
          month = StringUtils.getInt(timestampAsBytes, offset + 5, offset + 7);
          day = StringUtils.getInt(timestampAsBytes, offset + 8, offset + 10);
          hour = 0;
          minutes = 0;
        } else {
          year = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 2);
          
          if (year <= 69) {
            year += 100;
          }
          
          month = StringUtils.getInt(timestampAsBytes, offset + 2, offset + 4);
          day = StringUtils.getInt(timestampAsBytes, offset + 4, offset + 6);
          hour = StringUtils.getInt(timestampAsBytes, offset + 6, offset + 8);
          minutes = StringUtils.getInt(timestampAsBytes, offset + 8, offset + 10);
          
          year += 1900;
        }
        
        break;
      

      case 8: 
        boolean hasColon = false;
        
        for (int i = 0; i < length; i++) {
          if (timestampAsBytes[(offset + i)] == 58) {
            hasColon = true;
            break;
          }
        }
        
        if (hasColon) {
          hour = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 2);
          minutes = StringUtils.getInt(timestampAsBytes, offset + 3, offset + 5);
          seconds = StringUtils.getInt(timestampAsBytes, offset + 6, offset + 8);
          
          year = 1970;
          month = 1;
          day = 1;

        }
        else
        {
          year = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 4);
          month = StringUtils.getInt(timestampAsBytes, offset + 4, offset + 6);
          day = StringUtils.getInt(timestampAsBytes, offset + 6, offset + 8);
          
          year -= 1900;
          month--;
        }
        break;
      

      case 6: 
        year = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 2);
        
        if (year <= 69) {
          year += 100;
        }
        
        year += 1900;
        
        month = StringUtils.getInt(timestampAsBytes, offset + 2, offset + 4);
        day = StringUtils.getInt(timestampAsBytes, offset + 4, offset + 6);
        
        break;
      

      case 4: 
        year = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 2);
        
        if (year <= 69) {
          year += 100;
        }
        
        month = StringUtils.getInt(timestampAsBytes, offset + 2, offset + 4);
        
        day = 1;
        
        break;
      

      case 2: 
        year = StringUtils.getInt(timestampAsBytes, offset + 0, offset + 2);
        
        if (year <= 69) {
          year += 100;
        }
        
        year += 1900;
        month = 1;
        day = 1;
        
        break;
      case 3: case 5: case 7: case 9: case 11: 
      case 13: case 15: case 16: case 17: 
      case 18: case 27: case 28: default: 
        throw new SQLException("Bad format for Timestamp '" + StringUtils.toString(timestampAsBytes) + "' in column " + (columnIndex + 1) + ".", "S1009");
      }
      
      

      if (!useLegacyDatetimeCode) {
        return TimeUtil.fastTimestampCreate(tz, year, month, day, hour, minutes, seconds, nanos);
      }
      
      return TimeUtil.changeTimezone(conn, sessionCalendar, targetCalendar, rs.fastTimestampCreate(sessionCalendar, year, month, day, hour, minutes, seconds, nanos), conn.getServerTimezoneTZ(), tz, rollForward);
    }
    catch (RuntimeException e)
    {
      SQLException sqlEx = SQLError.createSQLException("Cannot convert value '" + getString(columnIndex, "ISO8859_1", conn) + "' from column " + (columnIndex + 1) + " to TIMESTAMP.", "S1009", exceptionInterceptor);
      

      sqlEx.initCause(e);
      
      throw sqlEx;
    }
  }
  








  public abstract Timestamp getTimestampFast(int paramInt, Calendar paramCalendar, TimeZone paramTimeZone, boolean paramBoolean, MySQLConnection paramMySQLConnection, ResultSetImpl paramResultSetImpl)
    throws SQLException;
  







  public abstract boolean isFloatingPointNumber(int paramInt)
    throws SQLException;
  







  public abstract boolean isNull(int paramInt)
    throws SQLException;
  







  public abstract long length(int paramInt)
    throws SQLException;
  







  public abstract void setColumnValue(int paramInt, byte[] paramArrayOfByte)
    throws SQLException;
  







  public ResultSetRow setMetadata(Field[] f)
    throws SQLException
  {
    metadata = f;
    
    return this;
  }
  
  public abstract int getBytesSize();
}
