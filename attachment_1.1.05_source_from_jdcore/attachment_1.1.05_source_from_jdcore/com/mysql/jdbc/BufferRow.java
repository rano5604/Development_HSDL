package com.mysql.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

































public class BufferRow
  extends ResultSetRow
{
  private Buffer rowFromServer;
  private int homePosition = 0;
  



  private int preNullBitmaskHomePosition = 0;
  




  private int lastRequestedIndex = -1;
  


  private int lastRequestedPos;
  


  private Field[] metadata;
  


  private boolean isBinaryEncoded;
  


  private boolean[] isNull;
  

  private List<InputStream> openStreams;
  


  public BufferRow(Buffer buf, Field[] fields, boolean isBinaryEncoded, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    super(exceptionInterceptor);
    
    rowFromServer = buf;
    metadata = fields;
    this.isBinaryEncoded = isBinaryEncoded;
    homePosition = rowFromServer.getPosition();
    preNullBitmaskHomePosition = homePosition;
    
    if (fields != null) {
      setMetadata(fields);
    }
  }
  
  public synchronized void closeOpenStreams()
  {
    if (openStreams != null)
    {


      Iterator<InputStream> iter = openStreams.iterator();
      
      while (iter.hasNext()) {
        try
        {
          ((InputStream)iter.next()).close();
        }
        catch (IOException e) {}
      }
      

      openStreams.clear();
    }
  }
  
  private int findAndSeekToOffset(int index) throws SQLException {
    if (!isBinaryEncoded)
    {
      if (index == 0) {
        lastRequestedIndex = 0;
        lastRequestedPos = homePosition;
        rowFromServer.setPosition(homePosition);
        
        return 0;
      }
      
      if (index == lastRequestedIndex) {
        rowFromServer.setPosition(lastRequestedPos);
        
        return lastRequestedPos;
      }
      
      int startingIndex = 0;
      
      if (index > lastRequestedIndex) {
        if (lastRequestedIndex >= 0) {
          startingIndex = lastRequestedIndex;
        } else {
          startingIndex = 0;
        }
        
        rowFromServer.setPosition(lastRequestedPos);
      } else {
        rowFromServer.setPosition(homePosition);
      }
      
      for (int i = startingIndex; i < index; i++) {
        rowFromServer.fastSkipLenByteArray();
      }
      
      lastRequestedIndex = index;
      lastRequestedPos = rowFromServer.getPosition();
      
      return lastRequestedPos;
    }
    
    return findAndSeekToOffsetForBinaryEncoding(index);
  }
  
  private int findAndSeekToOffsetForBinaryEncoding(int index) throws SQLException {
    if (index == 0) {
      lastRequestedIndex = 0;
      lastRequestedPos = homePosition;
      rowFromServer.setPosition(homePosition);
      
      return 0;
    }
    
    if (index == lastRequestedIndex) {
      rowFromServer.setPosition(lastRequestedPos);
      
      return lastRequestedPos;
    }
    
    int startingIndex = 0;
    
    if (index > lastRequestedIndex) {
      if (lastRequestedIndex >= 0) {
        startingIndex = lastRequestedIndex;
      }
      else {
        startingIndex = 0;
        lastRequestedPos = homePosition;
      }
      
      rowFromServer.setPosition(lastRequestedPos);
    } else {
      rowFromServer.setPosition(homePosition);
    }
    
    for (int i = startingIndex; i < index; i++) {
      if (isNull[i] == 0)
      {


        int curPosition = rowFromServer.getPosition();
        
        switch (metadata[i].getMysqlType())
        {
        case 6: 
          break;
        
        case 1: 
          rowFromServer.setPosition(curPosition + 1);
          break;
        
        case 2: 
        case 13: 
          rowFromServer.setPosition(curPosition + 2);
          
          break;
        case 3: 
        case 9: 
          rowFromServer.setPosition(curPosition + 4);
          
          break;
        case 8: 
          rowFromServer.setPosition(curPosition + 8);
          
          break;
        case 4: 
          rowFromServer.setPosition(curPosition + 4);
          
          break;
        case 5: 
          rowFromServer.setPosition(curPosition + 8);
          
          break;
        case 11: 
          rowFromServer.fastSkipLenByteArray();
          
          break;
        
        case 10: 
          rowFromServer.fastSkipLenByteArray();
          
          break;
        case 7: 
        case 12: 
          rowFromServer.fastSkipLenByteArray();
          
          break;
        case 0: 
        case 15: 
        case 16: 
        case 245: 
        case 246: 
        case 249: 
        case 250: 
        case 251: 
        case 252: 
        case 253: 
        case 254: 
        case 255: 
          rowFromServer.fastSkipLenByteArray();
          
          break;
        
        default: 
          throw SQLError.createSQLException(Messages.getString("MysqlIO.97") + metadata[i].getMysqlType() + Messages.getString("MysqlIO.98") + (i + 1) + Messages.getString("MysqlIO.99") + metadata.length + Messages.getString("MysqlIO.100"), "S1000", exceptionInterceptor);
        }
        
      }
    }
    

    lastRequestedIndex = index;
    lastRequestedPos = rowFromServer.getPosition();
    
    return lastRequestedPos;
  }
  
  public synchronized InputStream getBinaryInputStream(int columnIndex) throws SQLException
  {
    if ((isBinaryEncoded) && 
      (isNull(columnIndex))) {
      return null;
    }
    

    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    if (length == -1L) {
      return null;
    }
    
    InputStream stream = new ByteArrayInputStream(rowFromServer.getByteBuffer(), offset, (int)length);
    
    if (openStreams == null) {
      openStreams = new LinkedList();
    }
    
    return stream;
  }
  
  public byte[] getColumnValue(int index) throws SQLException
  {
    findAndSeekToOffset(index);
    
    if (!isBinaryEncoded) {
      return rowFromServer.readLenByteArray(0);
    }
    
    if (isNull[index] != 0) {
      return null;
    }
    
    switch (metadata[index].getMysqlType()) {
    case 6: 
      return null;
    
    case 1: 
      return new byte[] { rowFromServer.readByte() };
    
    case 2: 
    case 13: 
      return rowFromServer.getBytes(2);
    
    case 3: 
    case 9: 
      return rowFromServer.getBytes(4);
    
    case 8: 
      return rowFromServer.getBytes(8);
    
    case 4: 
      return rowFromServer.getBytes(4);
    
    case 5: 
      return rowFromServer.getBytes(8);
    
    case 0: 
    case 7: 
    case 10: 
    case 11: 
    case 12: 
    case 15: 
    case 16: 
    case 245: 
    case 246: 
    case 249: 
    case 250: 
    case 251: 
    case 252: 
    case 253: 
    case 254: 
    case 255: 
      return rowFromServer.readLenByteArray(0);
    }
    
    throw SQLError.createSQLException(Messages.getString("MysqlIO.97") + metadata[index].getMysqlType() + Messages.getString("MysqlIO.98") + (index + 1) + Messages.getString("MysqlIO.99") + metadata.length + Messages.getString("MysqlIO.100"), "S1000", exceptionInterceptor);
  }
  




  public int getInt(int columnIndex)
    throws SQLException
  {
    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    if (length == -1L) {
      return 0;
    }
    
    return StringUtils.getInt(rowFromServer.getByteBuffer(), offset, offset + (int)length);
  }
  
  public long getLong(int columnIndex) throws SQLException
  {
    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    if (length == -1L) {
      return 0L;
    }
    
    return StringUtils.getLong(rowFromServer.getByteBuffer(), offset, offset + (int)length);
  }
  
  public double getNativeDouble(int columnIndex) throws SQLException
  {
    if (isNull(columnIndex)) {
      return 0.0D;
    }
    
    findAndSeekToOffset(columnIndex);
    
    int offset = rowFromServer.getPosition();
    
    return getNativeDouble(rowFromServer.getByteBuffer(), offset);
  }
  
  public float getNativeFloat(int columnIndex) throws SQLException
  {
    if (isNull(columnIndex)) {
      return 0.0F;
    }
    
    findAndSeekToOffset(columnIndex);
    
    int offset = rowFromServer.getPosition();
    
    return getNativeFloat(rowFromServer.getByteBuffer(), offset);
  }
  
  public int getNativeInt(int columnIndex) throws SQLException
  {
    if (isNull(columnIndex)) {
      return 0;
    }
    
    findAndSeekToOffset(columnIndex);
    
    int offset = rowFromServer.getPosition();
    
    return getNativeInt(rowFromServer.getByteBuffer(), offset);
  }
  
  public long getNativeLong(int columnIndex) throws SQLException
  {
    if (isNull(columnIndex)) {
      return 0L;
    }
    
    findAndSeekToOffset(columnIndex);
    
    int offset = rowFromServer.getPosition();
    
    return getNativeLong(rowFromServer.getByteBuffer(), offset);
  }
  
  public short getNativeShort(int columnIndex) throws SQLException
  {
    if (isNull(columnIndex)) {
      return 0;
    }
    
    findAndSeekToOffset(columnIndex);
    
    int offset = rowFromServer.getPosition();
    
    return getNativeShort(rowFromServer.getByteBuffer(), offset);
  }
  
  public Timestamp getNativeTimestamp(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    if (isNull(columnIndex)) {
      return null;
    }
    
    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    return getNativeTimestamp(rowFromServer.getByteBuffer(), offset, (int)length, targetCalendar, tz, rollForward, conn, rs);
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
  
  public String getString(int columnIndex, String encoding, MySQLConnection conn) throws SQLException
  {
    if ((isBinaryEncoded) && 
      (isNull(columnIndex))) {
      return null;
    }
    

    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    if (length == -1L) {
      return null;
    }
    
    if (length == 0L) {
      return "";
    }
    


    int offset = rowFromServer.getPosition();
    
    return getString(encoding, conn, rowFromServer.getByteBuffer(), offset, (int)length);
  }
  
  public Time getTimeFast(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    if (isNull(columnIndex)) {
      return null;
    }
    
    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    return getTimeFast(columnIndex, rowFromServer.getByteBuffer(), offset, (int)length, targetCalendar, tz, rollForward, conn, rs);
  }
  
  public Timestamp getTimestampFast(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    if (isNull(columnIndex)) {
      return null;
    }
    
    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    return getTimestampFast(columnIndex, rowFromServer.getByteBuffer(), offset, (int)length, targetCalendar, tz, rollForward, conn, rs);
  }
  
  public boolean isFloatingPointNumber(int index) throws SQLException
  {
    if (isBinaryEncoded) {
      switch (metadata[index].getSQLType()) {
      case 2: 
      case 3: 
      case 6: 
      case 8: 
        return true;
      }
      return false;
    }
    

    findAndSeekToOffset(index);
    
    long length = rowFromServer.readFieldLength();
    
    if (length == -1L) {
      return false;
    }
    
    if (length == 0L) {
      return false;
    }
    
    int offset = rowFromServer.getPosition();
    byte[] buffer = rowFromServer.getByteBuffer();
    
    for (int i = 0; i < (int)length; i++) {
      char c = (char)buffer[(offset + i)];
      
      if ((c == 'e') || (c == 'E')) {
        return true;
      }
    }
    
    return false;
  }
  
  public boolean isNull(int index) throws SQLException
  {
    if (!isBinaryEncoded) {
      findAndSeekToOffset(index);
      
      return rowFromServer.readFieldLength() == -1L;
    }
    
    return isNull[index];
  }
  
  public long length(int index) throws SQLException
  {
    findAndSeekToOffset(index);
    
    long length = rowFromServer.readFieldLength();
    
    if (length == -1L) {
      return 0L;
    }
    
    return length;
  }
  
  public void setColumnValue(int index, byte[] value) throws SQLException
  {
    throw new OperationNotSupportedException();
  }
  
  public ResultSetRow setMetadata(Field[] f) throws SQLException
  {
    super.setMetadata(f);
    
    if (isBinaryEncoded) {
      setupIsNullBitmask();
    }
    
    return this;
  }
  



  private void setupIsNullBitmask()
    throws SQLException
  {
    if (isNull != null) {
      return;
    }
    
    rowFromServer.setPosition(preNullBitmaskHomePosition);
    
    int nullCount = (metadata.length + 9) / 8;
    
    byte[] nullBitMask = new byte[nullCount];
    
    for (int i = 0; i < nullCount; i++) {
      nullBitMask[i] = rowFromServer.readByte();
    }
    
    homePosition = rowFromServer.getPosition();
    
    isNull = new boolean[metadata.length];
    
    int nullMaskPos = 0;
    int bit = 4;
    
    for (int i = 0; i < metadata.length; i++)
    {
      isNull[i] = ((nullBitMask[nullMaskPos] & bit) != 0 ? 1 : false);
      
      if ((bit <<= 1 & 0xFF) == 0) {
        bit = 1;
        
        nullMaskPos++;
      }
    }
  }
  
  public Date getDateFast(int columnIndex, MySQLConnection conn, ResultSetImpl rs, Calendar targetCalendar) throws SQLException
  {
    if (isNull(columnIndex)) {
      return null;
    }
    
    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    return getDateFast(columnIndex, rowFromServer.getByteBuffer(), offset, (int)length, conn, rs, targetCalendar);
  }
  
  public Date getNativeDate(int columnIndex, MySQLConnection conn, ResultSetImpl rs, Calendar cal) throws SQLException
  {
    if (isNull(columnIndex)) {
      return null;
    }
    
    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    return getNativeDate(columnIndex, rowFromServer.getByteBuffer(), offset, (int)length, conn, rs, cal);
  }
  
  public Object getNativeDateTimeValue(int columnIndex, Calendar targetCalendar, int jdbcType, int mysqlType, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    if (isNull(columnIndex)) {
      return null;
    }
    
    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    return getNativeDateTimeValue(columnIndex, rowFromServer.getByteBuffer(), offset, (int)length, targetCalendar, jdbcType, mysqlType, tz, rollForward, conn, rs);
  }
  

  public Time getNativeTime(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward, MySQLConnection conn, ResultSetImpl rs)
    throws SQLException
  {
    if (isNull(columnIndex)) {
      return null;
    }
    
    findAndSeekToOffset(columnIndex);
    
    long length = rowFromServer.readFieldLength();
    
    int offset = rowFromServer.getPosition();
    
    return getNativeTime(columnIndex, rowFromServer.getByteBuffer(), offset, (int)length, targetCalendar, tz, rollForward, conn, rs);
  }
  
  public int getBytesSize()
  {
    return rowFromServer.getBufLength();
  }
}
