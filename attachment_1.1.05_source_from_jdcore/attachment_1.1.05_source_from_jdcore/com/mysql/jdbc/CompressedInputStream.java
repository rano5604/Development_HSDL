package com.mysql.jdbc;

import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.log.NullLogger;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;







































class CompressedInputStream
  extends InputStream
{
  private byte[] buffer;
  private InputStream in;
  private Inflater inflater;
  private ConnectionPropertiesImpl.BooleanConnectionProperty traceProtocol;
  private Log log;
  private byte[] packetHeaderBuffer = new byte[7];
  

  private int pos = 0;
  






  public CompressedInputStream(Connection conn, InputStream streamFromServer)
  {
    traceProtocol = traceProtocol;
    try {
      log = conn.getLog();
    } catch (SQLException e) {
      log = new NullLogger(null);
    }
    
    in = streamFromServer;
    inflater = new Inflater();
  }
  


  public int available()
    throws IOException
  {
    if (buffer == null) {
      return in.available();
    }
    
    return buffer.length - pos + in.available();
  }
  


  public void close()
    throws IOException
  {
    in.close();
    buffer = null;
    inflater.end();
    inflater = null;
    traceProtocol = null;
    log = null;
  }
  





  private void getNextPacketFromServer()
    throws IOException
  {
    byte[] uncompressedData = null;
    
    int lengthRead = readFully(packetHeaderBuffer, 0, 7);
    
    if (lengthRead < 7) {
      throw new IOException("Unexpected end of input stream");
    }
    
    int compressedPacketLength = (packetHeaderBuffer[0] & 0xFF) + ((packetHeaderBuffer[1] & 0xFF) << 8) + ((packetHeaderBuffer[2] & 0xFF) << 16);
    

    int uncompressedLength = (packetHeaderBuffer[4] & 0xFF) + ((packetHeaderBuffer[5] & 0xFF) << 8) + ((packetHeaderBuffer[6] & 0xFF) << 16);
    

    boolean doTrace = traceProtocol.getValueAsBoolean();
    
    if (doTrace) {
      log.logTrace("Reading compressed packet of length " + compressedPacketLength + " uncompressed to " + uncompressedLength);
    }
    
    if (uncompressedLength > 0) {
      uncompressedData = new byte[uncompressedLength];
      
      byte[] compressedBuffer = new byte[compressedPacketLength];
      
      readFully(compressedBuffer, 0, compressedPacketLength);
      
      inflater.reset();
      
      inflater.setInput(compressedBuffer);
      try
      {
        inflater.inflate(uncompressedData);
      } catch (DataFormatException dfe) {
        throw new IOException("Error while uncompressing packet from server.");
      }
    }
    else {
      if (doTrace) {
        log.logTrace("Packet didn't meet compression threshold, not uncompressing...");
      }
      



      uncompressedLength = compressedPacketLength;
      uncompressedData = new byte[uncompressedLength];
      readFully(uncompressedData, 0, uncompressedLength);
    }
    
    if (doTrace) {
      if (uncompressedLength > 1024) {
        log.logTrace("Uncompressed packet: \n" + StringUtils.dumpAsHex(uncompressedData, 256));
        byte[] tempData = new byte['Ā'];
        System.arraycopy(uncompressedData, uncompressedLength - 256, tempData, 0, 256);
        log.logTrace("Uncompressed packet: \n" + StringUtils.dumpAsHex(tempData, 256));
        log.logTrace("Large packet dump truncated. Showing first and last 256 bytes.");
      } else {
        log.logTrace("Uncompressed packet: \n" + StringUtils.dumpAsHex(uncompressedData, uncompressedLength));
      }
    }
    
    if ((buffer != null) && (pos < buffer.length)) {
      if (doTrace) {
        log.logTrace("Combining remaining packet with new: ");
      }
      
      int remaining = buffer.length - pos;
      byte[] newBuffer = new byte[remaining + uncompressedData.length];
      
      System.arraycopy(buffer, pos, newBuffer, 0, remaining);
      System.arraycopy(uncompressedData, 0, newBuffer, remaining, uncompressedData.length);
      
      uncompressedData = newBuffer;
    }
    
    pos = 0;
    buffer = uncompressedData;
  }
  










  private void getNextPacketIfRequired(int numBytes)
    throws IOException
  {
    if ((buffer == null) || (pos + numBytes > buffer.length)) {
      getNextPacketFromServer();
    }
  }
  

  public int read()
    throws IOException
  {
    try
    {
      getNextPacketIfRequired(1);
    } catch (IOException ioEx) {
      return -1;
    }
    
    return buffer[(pos++)] & 0xFF;
  }
  


  public int read(byte[] b)
    throws IOException
  {
    return read(b, 0, b.length);
  }
  


  public int read(byte[] b, int off, int len)
    throws IOException
  {
    if (b == null)
      throw new NullPointerException();
    if ((off < 0) || (off > b.length) || (len < 0) || (off + len > b.length) || (off + len < 0)) {
      throw new IndexOutOfBoundsException();
    }
    
    if (len <= 0) {
      return 0;
    }
    try
    {
      getNextPacketIfRequired(len);
    } catch (IOException ioEx) {
      return -1;
    }
    
    int remainingBufferLength = buffer.length - pos;
    int consummedBytesLength = Math.min(remainingBufferLength, len);
    
    System.arraycopy(buffer, pos, b, off, consummedBytesLength);
    pos += consummedBytesLength;
    
    return consummedBytesLength;
  }
  
  private final int readFully(byte[] b, int off, int len) throws IOException {
    if (len < 0) {
      throw new IndexOutOfBoundsException();
    }
    
    int n = 0;
    
    while (n < len) {
      int count = in.read(b, off + n, len - n);
      
      if (count < 0) {
        throw new EOFException();
      }
      
      n += count;
    }
    
    return n;
  }
  


  public long skip(long n)
    throws IOException
  {
    long count = 0L;
    
    for (long i = 0L; i < n; i += 1L) {
      int bytesRead = read();
      
      if (bytesRead == -1) {
        break;
      }
      
      count += 1L;
    }
    
    return count;
  }
}
