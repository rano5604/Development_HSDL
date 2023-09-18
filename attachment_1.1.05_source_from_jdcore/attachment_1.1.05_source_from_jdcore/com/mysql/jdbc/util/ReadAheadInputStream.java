package com.mysql.jdbc.util;

import com.mysql.jdbc.log.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;































public class ReadAheadInputStream
  extends InputStream
{
  private static final int DEFAULT_BUFFER_SIZE = 4096;
  private InputStream underlyingStream;
  private byte[] buf;
  protected int endOfCurrentData;
  protected int currentPosition;
  protected boolean doDebug = false;
  protected Log log;
  
  private void fill(int readAtLeastTheseManyBytes) throws IOException
  {
    checkClosed();
    
    currentPosition = 0;
    
    endOfCurrentData = currentPosition;
    


    int bytesToRead = Math.min(buf.length - currentPosition, readAtLeastTheseManyBytes);
    
    int bytesAvailable = underlyingStream.available();
    
    if (bytesAvailable > bytesToRead)
    {


      bytesToRead = Math.min(buf.length - currentPosition, bytesAvailable);
    }
    
    if (doDebug) {
      StringBuilder debugBuf = new StringBuilder();
      debugBuf.append("  ReadAheadInputStream.fill(");
      debugBuf.append(readAtLeastTheseManyBytes);
      debugBuf.append("), buffer_size=");
      debugBuf.append(buf.length);
      debugBuf.append(", current_position=");
      debugBuf.append(currentPosition);
      debugBuf.append(", need to read ");
      debugBuf.append(Math.min(buf.length - currentPosition, readAtLeastTheseManyBytes));
      debugBuf.append(" bytes to fill request,");
      
      if (bytesAvailable > 0) {
        debugBuf.append(" underlying InputStream reports ");
        debugBuf.append(bytesAvailable);
        
        debugBuf.append(" total bytes available,");
      }
      
      debugBuf.append(" attempting to read ");
      debugBuf.append(bytesToRead);
      debugBuf.append(" bytes.");
      
      if (log != null) {
        log.logTrace(debugBuf.toString());
      } else {
        System.err.println(debugBuf.toString());
      }
    }
    
    int n = underlyingStream.read(buf, currentPosition, bytesToRead);
    
    if (n > 0) {
      endOfCurrentData = (n + currentPosition);
    }
  }
  
  private int readFromUnderlyingStreamIfNecessary(byte[] b, int off, int len) throws IOException {
    checkClosed();
    
    int avail = endOfCurrentData - currentPosition;
    
    if (doDebug) {
      StringBuilder debugBuf = new StringBuilder();
      debugBuf.append("ReadAheadInputStream.readIfNecessary(");
      debugBuf.append(Arrays.toString(b));
      debugBuf.append(",");
      debugBuf.append(off);
      debugBuf.append(",");
      debugBuf.append(len);
      debugBuf.append(")");
      
      if (avail <= 0) {
        debugBuf.append(" not all data available in buffer, must read from stream");
        
        if (len >= buf.length) {
          debugBuf.append(", amount requested > buffer, returning direct read() from stream");
        }
      }
      
      if (log != null) {
        log.logTrace(debugBuf.toString());
      } else {
        System.err.println(debugBuf.toString());
      }
    }
    
    if (avail <= 0)
    {
      if (len >= buf.length) {
        return underlyingStream.read(b, off, len);
      }
      
      fill(len);
      
      avail = endOfCurrentData - currentPosition;
      
      if (avail <= 0) {
        return -1;
      }
    }
    
    int bytesActuallyRead = avail < len ? avail : len;
    
    System.arraycopy(buf, currentPosition, b, off, bytesActuallyRead);
    
    currentPosition += bytesActuallyRead;
    
    return bytesActuallyRead;
  }
  
  public synchronized int read(byte[] b, int off, int len) throws IOException
  {
    checkClosed();
    if ((off | len | off + len | b.length - (off + len)) < 0)
      throw new IndexOutOfBoundsException();
    if (len == 0) {
      return 0;
    }
    
    int totalBytesRead = 0;
    for (;;)
    {
      int bytesReadThisRound = readFromUnderlyingStreamIfNecessary(b, off + totalBytesRead, len - totalBytesRead);
      

      if (bytesReadThisRound <= 0) {
        if (totalBytesRead == 0) {
          totalBytesRead = bytesReadThisRound;
        }
        
      }
      else
      {
        totalBytesRead += bytesReadThisRound;
        

        if (totalBytesRead < len)
        {



          if (underlyingStream.available() <= 0)
            break;
        }
      }
    }
    return totalBytesRead;
  }
  
  public int read() throws IOException
  {
    checkClosed();
    
    if (currentPosition >= endOfCurrentData) {
      fill(1);
      if (currentPosition >= endOfCurrentData) {
        return -1;
      }
    }
    
    return buf[(currentPosition++)] & 0xFF;
  }
  
  public int available() throws IOException
  {
    checkClosed();
    
    return underlyingStream.available() + (endOfCurrentData - currentPosition);
  }
  
  private void checkClosed() throws IOException
  {
    if (buf == null) {
      throw new IOException("Stream closed");
    }
  }
  
  public ReadAheadInputStream(InputStream toBuffer, boolean debug, Log logTo) {
    this(toBuffer, 4096, debug, logTo);
  }
  
  public ReadAheadInputStream(InputStream toBuffer, int bufferSize, boolean debug, Log logTo) {
    underlyingStream = toBuffer;
    buf = new byte[bufferSize];
    doDebug = debug;
    log = logTo;
  }
  




  public void close()
    throws IOException
  {
    if (underlyingStream != null) {
      try {
        underlyingStream.close();
      } finally {
        underlyingStream = null;
        buf = null;
        log = null;
      }
    }
  }
  





  public boolean markSupported()
  {
    return false;
  }
  




  public long skip(long n)
    throws IOException
  {
    checkClosed();
    if (n <= 0L) {
      return 0L;
    }
    
    long bytesAvailInBuffer = endOfCurrentData - currentPosition;
    
    if (bytesAvailInBuffer <= 0L)
    {
      fill((int)n);
      bytesAvailInBuffer = endOfCurrentData - currentPosition;
      if (bytesAvailInBuffer <= 0L) {
        return 0L;
      }
    }
    
    long bytesSkipped = bytesAvailInBuffer < n ? bytesAvailInBuffer : n;
    currentPosition = ((int)(currentPosition + bytesSkipped));
    return bytesSkipped;
  }
}
