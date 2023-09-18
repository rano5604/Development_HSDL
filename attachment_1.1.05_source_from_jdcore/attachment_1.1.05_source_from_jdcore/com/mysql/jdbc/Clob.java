package com.mysql.jdbc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.SQLException;
























public class Clob
  implements java.sql.Clob, OutputStreamWatcher, WriterWatcher
{
  private String charData;
  private ExceptionInterceptor exceptionInterceptor;
  
  Clob(ExceptionInterceptor exceptionInterceptor)
  {
    charData = "";
    this.exceptionInterceptor = exceptionInterceptor;
  }
  
  Clob(String charDataInit, ExceptionInterceptor exceptionInterceptor) {
    charData = charDataInit;
    this.exceptionInterceptor = exceptionInterceptor;
  }
  

  public InputStream getAsciiStream()
    throws SQLException
  {
    if (charData != null) {
      return new ByteArrayInputStream(StringUtils.getBytes(charData));
    }
    
    return null;
  }
  

  public Reader getCharacterStream()
    throws SQLException
  {
    if (charData != null) {
      return new StringReader(charData);
    }
    
    return null;
  }
  

  public String getSubString(long startPos, int length)
    throws SQLException
  {
    if (startPos < 1L) {
      throw SQLError.createSQLException(Messages.getString("Clob.6"), "S1009", exceptionInterceptor);
    }
    
    int adjustedStartPos = (int)startPos - 1;
    int adjustedEndIndex = adjustedStartPos + length;
    
    if (charData != null) {
      if (adjustedEndIndex > charData.length()) {
        throw SQLError.createSQLException(Messages.getString("Clob.7"), "S1009", exceptionInterceptor);
      }
      
      return charData.substring(adjustedStartPos, adjustedEndIndex);
    }
    
    return null;
  }
  

  public long length()
    throws SQLException
  {
    if (charData != null) {
      return charData.length();
    }
    
    return 0L;
  }
  

  public long position(java.sql.Clob arg0, long arg1)
    throws SQLException
  {
    return position(arg0.getSubString(1L, (int)arg0.length()), arg1);
  }
  

  public long position(String stringToFind, long startPos)
    throws SQLException
  {
    if (startPos < 1L) {
      throw SQLError.createSQLException(Messages.getString("Clob.8") + startPos + Messages.getString("Clob.9"), "S1009", exceptionInterceptor);
    }
    

    if (charData != null) {
      if (startPos - 1L > charData.length()) {
        throw SQLError.createSQLException(Messages.getString("Clob.10"), "S1009", exceptionInterceptor);
      }
      
      int pos = charData.indexOf(stringToFind, (int)(startPos - 1L));
      
      return pos == -1 ? -1L : pos + 1;
    }
    
    return -1L;
  }
  

  public OutputStream setAsciiStream(long indexToWriteAt)
    throws SQLException
  {
    if (indexToWriteAt < 1L) {
      throw SQLError.createSQLException(Messages.getString("Clob.0"), "S1009", exceptionInterceptor);
    }
    
    WatchableOutputStream bytesOut = new WatchableOutputStream();
    bytesOut.setWatcher(this);
    
    if (indexToWriteAt > 0L) {
      bytesOut.write(StringUtils.getBytes(charData), 0, (int)(indexToWriteAt - 1L));
    }
    
    return bytesOut;
  }
  

  public Writer setCharacterStream(long indexToWriteAt)
    throws SQLException
  {
    if (indexToWriteAt < 1L) {
      throw SQLError.createSQLException(Messages.getString("Clob.1"), "S1009", exceptionInterceptor);
    }
    
    WatchableWriter writer = new WatchableWriter();
    writer.setWatcher(this);
    



    if (indexToWriteAt > 1L) {
      writer.write(charData, 0, (int)(indexToWriteAt - 1L));
    }
    
    return writer;
  }
  

  public int setString(long pos, String str)
    throws SQLException
  {
    if (pos < 1L) {
      throw SQLError.createSQLException(Messages.getString("Clob.2"), "S1009", exceptionInterceptor);
    }
    
    if (str == null) {
      throw SQLError.createSQLException(Messages.getString("Clob.3"), "S1009", exceptionInterceptor);
    }
    
    StringBuilder charBuf = new StringBuilder(charData);
    
    pos -= 1L;
    
    int strLength = str.length();
    
    charBuf.replace((int)pos, (int)(pos + strLength), str);
    
    charData = charBuf.toString();
    
    return strLength;
  }
  

  public int setString(long pos, String str, int offset, int len)
    throws SQLException
  {
    if (pos < 1L) {
      throw SQLError.createSQLException(Messages.getString("Clob.4"), "S1009", exceptionInterceptor);
    }
    
    if (str == null) {
      throw SQLError.createSQLException(Messages.getString("Clob.5"), "S1009", exceptionInterceptor);
    }
    
    StringBuilder charBuf = new StringBuilder(charData);
    
    pos -= 1L;
    try
    {
      String replaceString = str.substring(offset, offset + len);
      
      charBuf.replace((int)pos, (int)(pos + replaceString.length()), replaceString);
    } catch (StringIndexOutOfBoundsException e) {
      throw SQLError.createSQLException(e.getMessage(), "S1009", e, exceptionInterceptor);
    }
    
    charData = charBuf.toString();
    
    return len;
  }
  


  public void streamClosed(WatchableOutputStream out)
  {
    int streamSize = out.size();
    
    if (streamSize < charData.length()) {
      try {
        out.write(StringUtils.getBytes(charData, null, null, false, null, exceptionInterceptor), streamSize, charData.length() - streamSize);
      }
      catch (SQLException ex) {}
    }
    


    charData = StringUtils.toAsciiString(out.toByteArray());
  }
  

  public void truncate(long length)
    throws SQLException
  {
    if (length > charData.length()) {
      throw SQLError.createSQLException(Messages.getString("Clob.11") + charData.length() + Messages.getString("Clob.12") + length + Messages.getString("Clob.13"), exceptionInterceptor);
    }
    


    charData = charData.substring(0, (int)length);
  }
  


  public void writerClosed(char[] charDataBeingWritten)
  {
    charData = new String(charDataBeingWritten);
  }
  


  public void writerClosed(WatchableWriter out)
  {
    int dataLength = out.size();
    
    if (dataLength < charData.length()) {
      out.write(charData, dataLength, charData.length() - dataLength);
    }
    
    charData = out.toString();
  }
  
  public void free() throws SQLException {
    charData = null;
  }
  
  public Reader getCharacterStream(long pos, long length) throws SQLException {
    return new StringReader(getSubString(pos, (int)length));
  }
}
