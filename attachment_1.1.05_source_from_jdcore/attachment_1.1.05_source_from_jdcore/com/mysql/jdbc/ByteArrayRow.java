package com.mysql.jdbc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

























public class ByteArrayRow
  extends ResultSetRow
{
  byte[][] internalRowData;
  
  public ByteArrayRow(byte[][] internalRowData, ExceptionInterceptor exceptionInterceptor)
  {
    super(exceptionInterceptor);
    
    this.internalRowData = internalRowData;
  }
  
  public byte[] getColumnValue(int index) throws SQLException
  {
    return internalRowData[index];
  }
  
  public void setColumnValue(int index, byte[] value) throws SQLException
  {
    internalRowData[index] = value;
  }
  
  public String getString(int index, String encoding, MySQLConnection conn) throws SQLException
  {
    byte[] columnData = internalRowData[index];
    
    if (columnData == null) {
      return null;
    }
    
    return getString(encoding, conn, columnData, 0, columnData.length);
  }
  
  public boolean isNull(int index) throws SQLException
  {
    return internalRowData[index] == null;
  }
  
  public boolean isFloatingPointNumber(int index) throws SQLException
  {
    byte[] numAsBytes = internalRowData[index];
    
    if ((internalRowData[index] == null) || (internalRowData[index].length == 0)) {
      return false;
    }
    
    for (int i = 0; i < numAsBytes.length; i++) {
      if (((char)numAsBytes[i] == 'e') || ((char)numAsBytes[i] == 'E')) {
        return true;
      }
    }
    
    return false;
  }
  
  public long length(int index) throws SQLException
  {
    if (internalRowData[index] == null) {
      return 0L;
    }
    
    return internalRowData[index].length;
  }
  
  public int getInt(int columnIndex)
  {
    if (internalRowData[columnIndex] == null) {
      return 0;
    }
    
    return StringUtils.getInt(internalRowData[columnIndex]);
  }
  
  public long getLong(int columnIndex)
  {
    if (internalRowData[columnIndex] == null) {
      return 0L;
    }
    
    return StringUtils.getLong(internalRowData[columnIndex]);
  }
  
  public Timestamp getTimestampFast(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    byte[] columnValue = internalRowData[columnIndex];
    
    if (columnValue == null) {
      return null;
    }
    
    return getTimestampFast(columnIndex, internalRowData[columnIndex], 0, columnValue.length, targetCalendar, tz, rollForward, conn, rs);
  }
  
  public double getNativeDouble(int columnIndex) throws SQLException
  {
    if (internalRowData[columnIndex] == null) {
      return 0.0D;
    }
    
    return getNativeDouble(internalRowData[columnIndex], 0);
  }
  
  public float getNativeFloat(int columnIndex) throws SQLException
  {
    if (internalRowData[columnIndex] == null) {
      return 0.0F;
    }
    
    return getNativeFloat(internalRowData[columnIndex], 0);
  }
  
  public int getNativeInt(int columnIndex) throws SQLException
  {
    if (internalRowData[columnIndex] == null) {
      return 0;
    }
    
    return getNativeInt(internalRowData[columnIndex], 0);
  }
  
  public long getNativeLong(int columnIndex) throws SQLException
  {
    if (internalRowData[columnIndex] == null) {
      return 0L;
    }
    
    return getNativeLong(internalRowData[columnIndex], 0);
  }
  
  public short getNativeShort(int columnIndex) throws SQLException
  {
    if (internalRowData[columnIndex] == null) {
      return 0;
    }
    
    return getNativeShort(internalRowData[columnIndex], 0);
  }
  
  public Timestamp getNativeTimestamp(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    byte[] bits = internalRowData[columnIndex];
    
    if (bits == null) {
      return null;
    }
    
    return getNativeTimestamp(bits, 0, bits.length, targetCalendar, tz, rollForward, conn, rs);
  }
  

  public void closeOpenStreams() {}
  

  public InputStream getBinaryInputStream(int columnIndex)
    throws SQLException
  {
    if (internalRowData[columnIndex] == null) {
      return null;
    }
    
    return new ByteArrayInputStream(internalRowData[columnIndex]);
  }
  
  public Reader getReader(int columnIndex) throws SQLException
  {
    InputStream stream = getBinaryInputStream(columnIndex);
    
    if (stream == null) {
      return null;
    }
    try
    {
      return new InputStreamReader(stream, metadata[columnIndex].getEncoding());
    } catch (UnsupportedEncodingException e) {
      SQLException sqlEx = SQLError.createSQLException("", exceptionInterceptor);
      
      sqlEx.initCause(e);
      
      throw sqlEx;
    }
  }
  
  public Time getTimeFast(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    byte[] columnValue = internalRowData[columnIndex];
    
    if (columnValue == null) {
      return null;
    }
    
    return getTimeFast(columnIndex, internalRowData[columnIndex], 0, columnValue.length, targetCalendar, tz, rollForward, conn, rs);
  }
  
  public Date getDateFast(int columnIndex, MySQLConnection conn, ResultSetImpl rs, Calendar targetCalendar) throws SQLException
  {
    byte[] columnValue = internalRowData[columnIndex];
    
    if (columnValue == null) {
      return null;
    }
    
    return getDateFast(columnIndex, internalRowData[columnIndex], 0, columnValue.length, conn, rs, targetCalendar);
  }
  
  public Object getNativeDateTimeValue(int columnIndex, Calendar targetCalendar, int jdbcType, int mysqlType, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    byte[] columnValue = internalRowData[columnIndex];
    
    if (columnValue == null) {
      return null;
    }
    
    return getNativeDateTimeValue(columnIndex, columnValue, 0, columnValue.length, targetCalendar, jdbcType, mysqlType, tz, rollForward, conn, rs);
  }
  
  public Date getNativeDate(int columnIndex, MySQLConnection conn, ResultSetImpl rs, Calendar cal) throws SQLException
  {
    byte[] columnValue = internalRowData[columnIndex];
    
    if (columnValue == null) {
      return null;
    }
    
    return getNativeDate(columnIndex, columnValue, 0, columnValue.length, conn, rs, cal);
  }
  
  public Time getNativeTime(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    byte[] columnValue = internalRowData[columnIndex];
    
    if (columnValue == null) {
      return null;
    }
    
    return getNativeTime(columnIndex, columnValue, 0, columnValue.length, targetCalendar, tz, rollForward, conn, rs);
  }
  
  public int getBytesSize()
  {
    if (internalRowData == null) {
      return 0;
    }
    
    int bytesSize = 0;
    
    for (int i = 0; i < internalRowData.length; i++) {
      if (internalRowData[i] != null) {
        bytesSize += internalRowData[i].length;
      }
    }
    
    return bytesSize;
  }
}
