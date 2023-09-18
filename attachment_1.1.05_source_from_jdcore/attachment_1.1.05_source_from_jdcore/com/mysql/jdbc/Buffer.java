package com.mysql.jdbc;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.SQLException;




























public class Buffer
{
  static final int MAX_BYTES_TO_DUMP = 512;
  static final int NO_LENGTH_LIMIT = -1;
  static final long NULL_LENGTH = -1L;
  private int bufLength = 0;
  
  private byte[] byteBuffer;
  
  private int position = 0;
  
  protected boolean wasMultiPacket = false;
  
  public static final short TYPE_ID_ERROR = 255;
  
  public static final short TYPE_ID_EOF = 254;
  public static final short TYPE_ID_AUTH_SWITCH = 254;
  public static final short TYPE_ID_LOCAL_INFILE = 251;
  public static final short TYPE_ID_OK = 0;
  
  public Buffer(byte[] buf)
  {
    byteBuffer = buf;
    setBufLength(buf.length);
  }
  
  Buffer(int size) {
    byteBuffer = new byte[size];
    setBufLength(byteBuffer.length);
    position = 4;
  }
  
  final void clear() {
    position = 4;
  }
  
  final void dump() {
    dump(getBufLength());
  }
  
  final String dump(int numBytes) {
    return StringUtils.dumpAsHex(getBytes(0, numBytes > getBufLength() ? getBufLength() : numBytes), numBytes > getBufLength() ? getBufLength() : numBytes);
  }
  
  final String dumpClampedBytes(int numBytes) {
    int numBytesToDump = numBytes < 512 ? numBytes : 512;
    
    String dumped = StringUtils.dumpAsHex(getBytes(0, numBytesToDump > getBufLength() ? getBufLength() : numBytesToDump), numBytesToDump > getBufLength() ? getBufLength() : numBytesToDump);
    

    if (numBytesToDump < numBytes) {
      return dumped + " ....(packet exceeds max. dump length)";
    }
    
    return dumped;
  }
  
  final void dumpHeader() {
    for (int i = 0; i < 4; i++) {
      String hexVal = Integer.toHexString(readByte(i) & 0xFF);
      
      if (hexVal.length() == 1) {
        hexVal = "0" + hexVal;
      }
      
      System.out.print(hexVal + " ");
    }
  }
  
  final void dumpNBytes(int start, int nBytes) {
    StringBuilder asciiBuf = new StringBuilder();
    
    for (int i = start; (i < start + nBytes) && (i < getBufLength()); i++) {
      String hexVal = Integer.toHexString(readByte(i) & 0xFF);
      
      if (hexVal.length() == 1) {
        hexVal = "0" + hexVal;
      }
      
      System.out.print(hexVal + " ");
      
      if ((readByte(i) > 32) && (readByte(i) < Byte.MAX_VALUE)) {
        asciiBuf.append((char)readByte(i));
      } else {
        asciiBuf.append(".");
      }
      
      asciiBuf.append(" ");
    }
    
    System.out.println("    " + asciiBuf.toString());
  }
  
  final void ensureCapacity(int additionalData) throws SQLException {
    if (position + additionalData > getBufLength()) {
      if (position + additionalData < byteBuffer.length)
      {


        setBufLength(byteBuffer.length);

      }
      else
      {
        int newLength = (int)(byteBuffer.length * 1.25D);
        
        if (newLength < byteBuffer.length + additionalData) {
          newLength = byteBuffer.length + (int)(additionalData * 1.25D);
        }
        
        if (newLength < byteBuffer.length) {
          newLength = byteBuffer.length + additionalData;
        }
        
        byte[] newBytes = new byte[newLength];
        
        System.arraycopy(byteBuffer, 0, newBytes, 0, byteBuffer.length);
        byteBuffer = newBytes;
        setBufLength(byteBuffer.length);
      }
    }
  }
  




  public int fastSkipLenString()
  {
    long len = readFieldLength();
    
    position = ((int)(position + len));
    
    return (int)len;
  }
  
  public void fastSkipLenByteArray() {
    long len = readFieldLength();
    
    if ((len == -1L) || (len == 0L)) {
      return;
    }
    
    position = ((int)(position + len));
  }
  
  protected final byte[] getBufferSource() {
    return byteBuffer;
  }
  
  public int getBufLength() {
    return bufLength;
  }
  




  public byte[] getByteBuffer()
  {
    return byteBuffer;
  }
  
  final byte[] getBytes(int len) {
    byte[] b = new byte[len];
    System.arraycopy(byteBuffer, position, b, 0, len);
    position += len;
    
    return b;
  }
  




  byte[] getBytes(int offset, int len)
  {
    byte[] dest = new byte[len];
    System.arraycopy(byteBuffer, offset, dest, 0, len);
    
    return dest;
  }
  
  int getCapacity() {
    return byteBuffer.length;
  }
  
  public ByteBuffer getNioBuffer() {
    throw new IllegalArgumentException(Messages.getString("ByteArrayBuffer.0"));
  }
  




  public int getPosition()
  {
    return position;
  }
  
  final boolean isEOFPacket() {
    return ((byteBuffer[0] & 0xFF) == 254) && (getBufLength() <= 5);
  }
  
  final boolean isAuthMethodSwitchRequestPacket() {
    return (byteBuffer[0] & 0xFF) == 254;
  }
  
  final boolean isOKPacket() {
    return (byteBuffer[0] & 0xFF) == 0;
  }
  
  final boolean isResultSetOKPacket() {
    return ((byteBuffer[0] & 0xFF) == 254) && (getBufLength() < 16777215);
  }
  
  final boolean isRawPacket() {
    return (byteBuffer[0] & 0xFF) == 1;
  }
  
  final long newReadLength() {
    int sw = byteBuffer[(position++)] & 0xFF;
    
    switch (sw) {
    case 251: 
      return 0L;
    
    case 252: 
      return readInt();
    
    case 253: 
      return readLongInt();
    
    case 254: 
      return readLongLong();
    }
    
    return sw;
  }
  
  final byte readByte()
  {
    return byteBuffer[(position++)];
  }
  
  final byte readByte(int readAt) {
    return byteBuffer[readAt];
  }
  
  final long readFieldLength() {
    int sw = byteBuffer[(position++)] & 0xFF;
    
    switch (sw) {
    case 251: 
      return -1L;
    
    case 252: 
      return readInt();
    
    case 253: 
      return readLongInt();
    
    case 254: 
      return readLongLong();
    }
    
    return sw;
  }
  
  final int readInt()
  {
    byte[] b = byteBuffer;
    
    return b[(position++)] & 0xFF | (b[(position++)] & 0xFF) << 8;
  }
  
  final int readIntAsLong() {
    byte[] b = byteBuffer;
    
    return b[(position++)] & 0xFF | (b[(position++)] & 0xFF) << 8 | (b[(position++)] & 0xFF) << 16 | (b[(position++)] & 0xFF) << 24;
  }
  
  final byte[] readLenByteArray(int offset) {
    long len = readFieldLength();
    
    if (len == -1L) {
      return null;
    }
    
    if (len == 0L) {
      return Constants.EMPTY_BYTE_ARRAY;
    }
    
    position += offset;
    
    return getBytes((int)len);
  }
  
  final long readLength() {
    int sw = byteBuffer[(position++)] & 0xFF;
    
    switch (sw) {
    case 251: 
      return 0L;
    
    case 252: 
      return readInt();
    
    case 253: 
      return readLongInt();
    
    case 254: 
      return readLong();
    }
    
    return sw;
  }
  
  final long readLong()
  {
    byte[] b = byteBuffer;
    
    return b[(position++)] & 0xFF | (b[(position++)] & 0xFF) << 8 | (b[(position++)] & 0xFF) << 16 | (b[(position++)] & 0xFF) << 24;
  }
  
  final int readLongInt()
  {
    byte[] b = byteBuffer;
    
    return b[(position++)] & 0xFF | (b[(position++)] & 0xFF) << 8 | (b[(position++)] & 0xFF) << 16;
  }
  
  final long readLongLong() {
    byte[] b = byteBuffer;
    
    return b[(position++)] & 0xFF | (b[(position++)] & 0xFF) << 8 | (b[(position++)] & 0xFF) << 16 | (b[(position++)] & 0xFF) << 24 | (b[(position++)] & 0xFF) << 32 | (b[(position++)] & 0xFF) << 40 | (b[(position++)] & 0xFF) << 48 | (b[(position++)] & 0xFF) << 56;
  }
  

  final int readnBytes()
  {
    int sw = byteBuffer[(position++)] & 0xFF;
    
    switch (sw) {
    case 1: 
      return byteBuffer[(position++)] & 0xFF;
    
    case 2: 
      return readInt();
    
    case 3: 
      return readLongInt();
    
    case 4: 
      return (int)readLong();
    }
    
    return 255;
  }
  





  public final String readString()
  {
    int i = position;
    int len = 0;
    int maxLen = getBufLength();
    
    while ((i < maxLen) && (byteBuffer[i] != 0)) {
      len++;
      i++;
    }
    
    String s = StringUtils.toString(byteBuffer, position, len);
    position += len + 1;
    
    return s;
  }
  





  final String readString(String encoding, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    int i = position;
    int len = 0;
    int maxLen = getBufLength();
    
    while ((i < maxLen) && (byteBuffer[i] != 0)) {
      len++;
      i++;
    }
    try
    {
      return StringUtils.toString(byteBuffer, position, len, encoding);
    } catch (UnsupportedEncodingException uEE) {
      throw SQLError.createSQLException(Messages.getString("ByteArrayBuffer.1") + encoding + "'", "S1009", exceptionInterceptor);
    }
    finally {
      position += len + 1;
    }
  }
  

  final String readString(String encoding, ExceptionInterceptor exceptionInterceptor, int expectedLength)
    throws SQLException
  {
    if (position + expectedLength > getBufLength()) {
      throw SQLError.createSQLException(Messages.getString("ByteArrayBuffer.2"), "S1009", exceptionInterceptor);
    }
    try
    {
      return StringUtils.toString(byteBuffer, position, expectedLength, encoding);
    } catch (UnsupportedEncodingException uEE) {
      throw SQLError.createSQLException(Messages.getString("ByteArrayBuffer.1") + encoding + "'", "S1009", exceptionInterceptor);
    }
    finally {
      position += expectedLength;
    }
  }
  
  public void setBufLength(int bufLengthToSet) {
    bufLength = bufLengthToSet;
  }
  





  public void setByteBuffer(byte[] byteBufferToSet)
  {
    byteBuffer = byteBufferToSet;
  }
  





  public void setPosition(int positionToSet)
  {
    position = positionToSet;
  }
  





  public void setWasMultiPacket(boolean flag)
  {
    wasMultiPacket = flag;
  }
  
  public String toString()
  {
    return dumpClampedBytes(getPosition());
  }
  
  public String toSuperString() {
    return super.toString();
  }
  




  public boolean wasMultiPacket()
  {
    return wasMultiPacket;
  }
  
  public final void writeByte(byte b) throws SQLException {
    ensureCapacity(1);
    
    byteBuffer[(position++)] = b;
  }
  
  public final void writeBytesNoNull(byte[] bytes) throws SQLException
  {
    int len = bytes.length;
    ensureCapacity(len);
    System.arraycopy(bytes, 0, byteBuffer, position, len);
    position += len;
  }
  
  final void writeBytesNoNull(byte[] bytes, int offset, int length) throws SQLException
  {
    ensureCapacity(length);
    System.arraycopy(bytes, offset, byteBuffer, position, length);
    position += length;
  }
  
  final void writeDouble(double d) throws SQLException {
    long l = Double.doubleToLongBits(d);
    writeLongLong(l);
  }
  
  final void writeFieldLength(long length) throws SQLException {
    if (length < 251L) {
      writeByte((byte)(int)length);
    } else if (length < 65536L) {
      ensureCapacity(3);
      writeByte((byte)-4);
      writeInt((int)length);
    } else if (length < 16777216L) {
      ensureCapacity(4);
      writeByte((byte)-3);
      writeLongInt((int)length);
    } else {
      ensureCapacity(9);
      writeByte((byte)-2);
      writeLongLong(length);
    }
  }
  
  final void writeFloat(float f) throws SQLException {
    ensureCapacity(4);
    
    int i = Float.floatToIntBits(f);
    byte[] b = byteBuffer;
    b[(position++)] = ((byte)(i & 0xFF));
    b[(position++)] = ((byte)(i >>> 8));
    b[(position++)] = ((byte)(i >>> 16));
    b[(position++)] = ((byte)(i >>> 24));
  }
  
  final void writeInt(int i) throws SQLException {
    ensureCapacity(2);
    
    byte[] b = byteBuffer;
    b[(position++)] = ((byte)(i & 0xFF));
    b[(position++)] = ((byte)(i >>> 8));
  }
  
  final void writeLenBytes(byte[] b) throws SQLException
  {
    int len = b.length;
    ensureCapacity(len + 9);
    writeFieldLength(len);
    System.arraycopy(b, 0, byteBuffer, position, len);
    position += len;
  }
  
  final void writeLenString(String s, String encoding, String serverEncoding, SingleByteCharsetConverter converter, boolean parserKnowsUnicode, MySQLConnection conn)
    throws UnsupportedEncodingException, SQLException
  {
    byte[] b = null;
    
    if (converter != null) {
      b = converter.toBytes(s);
    } else {
      b = StringUtils.getBytes(s, encoding, serverEncoding, parserKnowsUnicode, conn, conn.getExceptionInterceptor());
    }
    
    int len = b.length;
    ensureCapacity(len + 9);
    writeFieldLength(len);
    System.arraycopy(b, 0, byteBuffer, position, len);
    position += len;
  }
  
  final void writeLong(long i) throws SQLException {
    ensureCapacity(4);
    
    byte[] b = byteBuffer;
    b[(position++)] = ((byte)(int)(i & 0xFF));
    b[(position++)] = ((byte)(int)(i >>> 8));
    b[(position++)] = ((byte)(int)(i >>> 16));
    b[(position++)] = ((byte)(int)(i >>> 24));
  }
  
  final void writeLongInt(int i) throws SQLException {
    ensureCapacity(3);
    byte[] b = byteBuffer;
    b[(position++)] = ((byte)(i & 0xFF));
    b[(position++)] = ((byte)(i >>> 8));
    b[(position++)] = ((byte)(i >>> 16));
  }
  
  final void writeLongLong(long i) throws SQLException {
    ensureCapacity(8);
    byte[] b = byteBuffer;
    b[(position++)] = ((byte)(int)(i & 0xFF));
    b[(position++)] = ((byte)(int)(i >>> 8));
    b[(position++)] = ((byte)(int)(i >>> 16));
    b[(position++)] = ((byte)(int)(i >>> 24));
    b[(position++)] = ((byte)(int)(i >>> 32));
    b[(position++)] = ((byte)(int)(i >>> 40));
    b[(position++)] = ((byte)(int)(i >>> 48));
    b[(position++)] = ((byte)(int)(i >>> 56));
  }
  
  final void writeString(String s) throws SQLException
  {
    ensureCapacity(s.length() * 3 + 1);
    writeStringNoNull(s);
    byteBuffer[(position++)] = 0;
  }
  
  final void writeString(String s, String encoding, MySQLConnection conn) throws SQLException
  {
    ensureCapacity(s.length() * 3 + 1);
    try {
      writeStringNoNull(s, encoding, encoding, false, conn);
    } catch (UnsupportedEncodingException ue) {
      throw new SQLException(ue.toString(), "S1000");
    }
    
    byteBuffer[(position++)] = 0;
  }
  
  final void writeStringNoNull(String s) throws SQLException
  {
    int len = s.length();
    ensureCapacity(len * 3);
    System.arraycopy(StringUtils.getBytes(s), 0, byteBuffer, position, len);
    position += len;
  }
  





  final void writeStringNoNull(String s, String encoding, String serverEncoding, boolean parserKnowsUnicode, MySQLConnection conn)
    throws UnsupportedEncodingException, SQLException
  {
    byte[] b = StringUtils.getBytes(s, encoding, serverEncoding, parserKnowsUnicode, conn, conn.getExceptionInterceptor());
    
    int len = b.length;
    ensureCapacity(len);
    System.arraycopy(b, 0, byteBuffer, position, len);
    position += len;
  }
}
