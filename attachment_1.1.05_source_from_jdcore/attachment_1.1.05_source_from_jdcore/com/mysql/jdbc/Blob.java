package com.mysql.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;





































public class Blob
  implements java.sql.Blob, OutputStreamWatcher
{
  private byte[] binaryData = null;
  private boolean isClosed = false;
  
  private ExceptionInterceptor exceptionInterceptor;
  

  Blob(ExceptionInterceptor exceptionInterceptor)
  {
    setBinaryData(Constants.EMPTY_BYTE_ARRAY);
    this.exceptionInterceptor = exceptionInterceptor;
  }
  




  Blob(byte[] data, ExceptionInterceptor exceptionInterceptor)
  {
    setBinaryData(data);
    this.exceptionInterceptor = exceptionInterceptor;
  }
  






  Blob(byte[] data, ResultSetInternalMethods creatorResultSetToSet, int columnIndexToSet)
  {
    setBinaryData(data);
  }
  
  private synchronized byte[] getBinaryData() {
    return binaryData;
  }
  






  public synchronized InputStream getBinaryStream()
    throws SQLException
  {
    checkClosed();
    
    return new ByteArrayInputStream(getBinaryData());
  }
  












  public synchronized byte[] getBytes(long pos, int length)
    throws SQLException
  {
    checkClosed();
    
    if (pos < 1L) {
      throw SQLError.createSQLException(Messages.getString("Blob.2"), "S1009", exceptionInterceptor);
    }
    
    pos -= 1L;
    
    if (pos > binaryData.length) {
      throw SQLError.createSQLException("\"pos\" argument can not be larger than the BLOB's length.", "S1009", exceptionInterceptor);
    }
    

    if (pos + length > binaryData.length) {
      throw SQLError.createSQLException("\"pos\" + \"length\" arguments can not be larger than the BLOB's length.", "S1009", exceptionInterceptor);
    }
    

    byte[] newData = new byte[length];
    System.arraycopy(getBinaryData(), (int)pos, newData, 0, length);
    
    return newData;
  }
  







  public synchronized long length()
    throws SQLException
  {
    checkClosed();
    
    return getBinaryData().length;
  }
  

  public synchronized long position(byte[] pattern, long start)
    throws SQLException
  {
    throw SQLError.createSQLException("Not implemented", exceptionInterceptor);
  }
  












  public synchronized long position(java.sql.Blob pattern, long start)
    throws SQLException
  {
    checkClosed();
    
    return position(pattern.getBytes(0L, (int)pattern.length()), start);
  }
  
  private synchronized void setBinaryData(byte[] newBinaryData) {
    binaryData = newBinaryData;
  }
  

  public synchronized OutputStream setBinaryStream(long indexToWriteAt)
    throws SQLException
  {
    checkClosed();
    
    if (indexToWriteAt < 1L) {
      throw SQLError.createSQLException(Messages.getString("Blob.0"), "S1009", exceptionInterceptor);
    }
    
    WatchableOutputStream bytesOut = new WatchableOutputStream();
    bytesOut.setWatcher(this);
    
    if (indexToWriteAt > 0L) {
      bytesOut.write(binaryData, 0, (int)(indexToWriteAt - 1L));
    }
    
    return bytesOut;
  }
  

  public synchronized int setBytes(long writeAt, byte[] bytes)
    throws SQLException
  {
    checkClosed();
    
    return setBytes(writeAt, bytes, 0, bytes.length);
  }
  

  public synchronized int setBytes(long writeAt, byte[] bytes, int offset, int length)
    throws SQLException
  {
    checkClosed();
    
    OutputStream bytesOut = setBinaryStream(writeAt);
    try
    {
      bytesOut.write(bytes, offset, length);
    } catch (IOException ioEx) {
      SQLException sqlEx = SQLError.createSQLException(Messages.getString("Blob.1"), "S1000", exceptionInterceptor);
      sqlEx.initCause(ioEx);
      
      throw sqlEx;
    } finally {
      try {
        bytesOut.close();
      }
      catch (IOException doNothing) {}
    }
    

    return length;
  }
  


  public synchronized void streamClosed(byte[] byteData)
  {
    binaryData = byteData;
  }
  


  public synchronized void streamClosed(WatchableOutputStream out)
  {
    int streamSize = out.size();
    
    if (streamSize < binaryData.length) {
      out.write(binaryData, streamSize, binaryData.length - streamSize);
    }
    
    binaryData = out.toByteArray();
  }
  














  public synchronized void truncate(long len)
    throws SQLException
  {
    checkClosed();
    
    if (len < 0L) {
      throw SQLError.createSQLException("\"len\" argument can not be < 1.", "S1009", exceptionInterceptor);
    }
    
    if (len > binaryData.length) {
      throw SQLError.createSQLException("\"len\" argument can not be larger than the BLOB's length.", "S1009", exceptionInterceptor);
    }
    



    byte[] newData = new byte[(int)len];
    System.arraycopy(getBinaryData(), 0, newData, 0, (int)len);
    binaryData = newData;
  }
  















  public synchronized void free()
    throws SQLException
  {
    binaryData = null;
    isClosed = true;
  }
  


















  public synchronized InputStream getBinaryStream(long pos, long length)
    throws SQLException
  {
    checkClosed();
    
    if (pos < 1L) {
      throw SQLError.createSQLException("\"pos\" argument can not be < 1.", "S1009", exceptionInterceptor);
    }
    
    pos -= 1L;
    
    if (pos > binaryData.length) {
      throw SQLError.createSQLException("\"pos\" argument can not be larger than the BLOB's length.", "S1009", exceptionInterceptor);
    }
    

    if (pos + length > binaryData.length) {
      throw SQLError.createSQLException("\"pos\" + \"length\" arguments can not be larger than the BLOB's length.", "S1009", exceptionInterceptor);
    }
    

    return new ByteArrayInputStream(getBinaryData(), (int)pos, (int)length);
  }
  
  private synchronized void checkClosed() throws SQLException {
    if (isClosed) {
      throw SQLError.createSQLException("Invalid operation on closed BLOB", "S1009", exceptionInterceptor);
    }
  }
}
