package com.mysql.jdbc;

import com.mysql.jdbc.exceptions.MySQLStatementCancelledException;
import com.mysql.jdbc.exceptions.MySQLTimeoutException;
import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;





























public class PreparedStatement
  extends StatementImpl
  implements java.sql.PreparedStatement
{
  private static final Constructor<?> JDBC_4_PSTMT_2_ARG_CTOR;
  private static final Constructor<?> JDBC_4_PSTMT_3_ARG_CTOR;
  private static final Constructor<?> JDBC_4_PSTMT_4_ARG_CTOR;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        String jdbc4ClassName = Util.isJdbc42() ? "com.mysql.jdbc.JDBC42PreparedStatement" : "com.mysql.jdbc.JDBC4PreparedStatement";
        JDBC_4_PSTMT_2_ARG_CTOR = Class.forName(jdbc4ClassName).getConstructor(new Class[] { MySQLConnection.class, String.class });
        JDBC_4_PSTMT_3_ARG_CTOR = Class.forName(jdbc4ClassName).getConstructor(new Class[] { MySQLConnection.class, String.class, String.class });
        JDBC_4_PSTMT_4_ARG_CTOR = Class.forName(jdbc4ClassName).getConstructor(new Class[] { MySQLConnection.class, String.class, String.class, ParseInfo.class });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDBC_4_PSTMT_2_ARG_CTOR = null;
      JDBC_4_PSTMT_3_ARG_CTOR = null;
      JDBC_4_PSTMT_4_ARG_CTOR = null;
    }
  }
  
  public class BatchParams {
    public boolean[] isNull = null;
    
    public boolean[] isStream = null;
    
    public InputStream[] parameterStreams = null;
    
    public byte[][] parameterStrings = (byte[][])null;
    
    public int[] streamLengths = null;
    


    BatchParams(byte[][] strings, InputStream[] streams, boolean[] isStreamFlags, int[] lengths, boolean[] isNullFlags)
    {
      parameterStrings = new byte[strings.length][];
      parameterStreams = new InputStream[streams.length];
      isStream = new boolean[isStreamFlags.length];
      streamLengths = new int[lengths.length];
      isNull = new boolean[isNullFlags.length];
      System.arraycopy(strings, 0, parameterStrings, 0, strings.length);
      System.arraycopy(streams, 0, parameterStreams, 0, streams.length);
      System.arraycopy(isStreamFlags, 0, isStream, 0, isStreamFlags.length);
      System.arraycopy(lengths, 0, streamLengths, 0, lengths.length);
      System.arraycopy(isNullFlags, 0, isNull, 0, isNullFlags.length);
    }
  }
  
  class EndPoint
  {
    int begin;
    int end;
    
    EndPoint(int b, int e) {
      begin = b;
      end = e;
    }
  }
  
  public static final class ParseInfo {
    char firstStmtChar = '\000';
    
    boolean foundLoadData = false;
    
    long lastUsed = 0L;
    
    int statementLength = 0;
    
    int statementStartPos = 0;
    
    boolean canRewriteAsMultiValueInsert = false;
    
    byte[][] staticSql = (byte[][])null;
    
    boolean isOnDuplicateKeyUpdate = false;
    
    int locationOfOnDuplicateKeyUpdate = -1;
    
    String valuesClause;
    
    boolean parametersInDuplicateKeyClause = false;
    String charEncoding;
    private ParseInfo batchHead;
    private ParseInfo batchValues;
    private ParseInfo batchODKUClause;
    
    ParseInfo(String sql, MySQLConnection conn, DatabaseMetaData dbmd, String encoding, SingleByteCharsetConverter converter)
      throws SQLException
    {
      this(sql, conn, dbmd, encoding, converter, true);
    }
    
    public ParseInfo(String sql, MySQLConnection conn, DatabaseMetaData dbmd, String encoding, SingleByteCharsetConverter converter, boolean buildRewriteInfo) throws SQLException
    {
      try {
        if (sql == null) {
          throw SQLError.createSQLException(Messages.getString("PreparedStatement.61"), "S1009", conn.getExceptionInterceptor());
        }
        

        charEncoding = encoding;
        lastUsed = System.currentTimeMillis();
        
        String quotedIdentifierString = dbmd.getIdentifierQuoteString();
        
        char quotedIdentifierChar = '\000';
        
        if ((quotedIdentifierString != null) && (!quotedIdentifierString.equals(" ")) && (quotedIdentifierString.length() > 0)) {
          quotedIdentifierChar = quotedIdentifierString.charAt(0);
        }
        
        statementLength = sql.length();
        
        ArrayList<int[]> endpointList = new ArrayList();
        boolean inQuotes = false;
        char quoteChar = '\000';
        boolean inQuotedId = false;
        int lastParmEnd = 0;
        

        boolean noBackslashEscapes = conn.isNoBackslashEscapesSet();
        



        statementStartPos = StatementImpl.findStartOfStatement(sql);
        
        for (int i = statementStartPos; i < statementLength; i++) {
          char c = sql.charAt(i);
          
          if ((firstStmtChar == 0) && (Character.isLetter(c)))
          {
            firstStmtChar = Character.toUpperCase(c);
            

            if (firstStmtChar == 'I') {
              locationOfOnDuplicateKeyUpdate = StatementImpl.getOnDuplicateKeyLocation(sql, conn.getDontCheckOnDuplicateKeyUpdateInSQL(), conn.getRewriteBatchedStatements(), conn.isNoBackslashEscapesSet());
              
              isOnDuplicateKeyUpdate = (locationOfOnDuplicateKeyUpdate != -1);
            }
          }
          
          if ((!noBackslashEscapes) && (c == '\\') && (i < statementLength - 1)) {
            i++;

          }
          else
          {
            if ((!inQuotes) && (quotedIdentifierChar != 0) && (c == quotedIdentifierChar)) {
              inQuotedId = !inQuotedId;
            } else if (!inQuotedId)
            {

              if (inQuotes) {
                if (((c == '\'') || (c == '"')) && (c == quoteChar)) {
                  if ((i < statementLength - 1) && (sql.charAt(i + 1) == quoteChar)) {
                    i++;
                    continue;
                  }
                  
                  inQuotes = !inQuotes;
                  quoteChar = '\000';
                } else if (((c == '\'') || (c == '"')) && (c == quoteChar)) {
                  inQuotes = !inQuotes;
                  quoteChar = '\000';
                }
              } else {
                if ((c == '#') || ((c == '-') && (i + 1 < statementLength) && (sql.charAt(i + 1) == '-')))
                {
                  int endOfStmt = statementLength - 1;
                  for (; 
                      i < endOfStmt; i++) {
                    c = sql.charAt(i);
                    
                    if ((c == '\r') || (c == '\n')) {
                      break;
                    }
                  }
                }
                
                if ((c == '/') && (i + 1 < statementLength))
                {
                  char cNext = sql.charAt(i + 1);
                  
                  if (cNext == '*') {
                    i += 2;
                    
                    for (int j = i; j < statementLength; j++) {
                      i++;
                      cNext = sql.charAt(j);
                      
                      if ((cNext == '*') && (j + 1 < statementLength) && 
                        (sql.charAt(j + 1) == '/')) {
                        i++;
                        
                        if (i >= statementLength) break;
                        c = sql.charAt(i); break;
                      }
                      
                    }
                    
                  }
                  
                }
                else if ((c == '\'') || (c == '"')) {
                  inQuotes = true;
                  quoteChar = c;
                }
              }
            }
            
            if ((c == '?') && (!inQuotes) && (!inQuotedId)) {
              endpointList.add(new int[] { lastParmEnd, i });
              lastParmEnd = i + 1;
              
              if ((isOnDuplicateKeyUpdate) && (i > locationOfOnDuplicateKeyUpdate)) {
                parametersInDuplicateKeyClause = true;
              }
            }
          }
        }
        if (firstStmtChar == 'L') {
          if (StringUtils.startsWithIgnoreCaseAndWs(sql, "LOAD DATA")) {
            foundLoadData = true;
          } else {
            foundLoadData = false;
          }
        } else {
          foundLoadData = false;
        }
        
        endpointList.add(new int[] { lastParmEnd, statementLength });
        staticSql = new byte[endpointList.size()][];
        
        for (i = 0; i < staticSql.length; i++) {
          int[] ep = (int[])endpointList.get(i);
          int end = ep[1];
          int begin = ep[0];
          int len = end - begin;
          
          if (foundLoadData) {
            staticSql[i] = StringUtils.getBytes(sql, begin, len);
          } else if (encoding == null) {
            byte[] buf = new byte[len];
            
            for (int j = 0; j < len; j++) {
              buf[j] = ((byte)sql.charAt(begin + j));
            }
            
            staticSql[i] = buf;
          }
          else if (converter != null) {
            staticSql[i] = StringUtils.getBytes(sql, converter, encoding, conn.getServerCharset(), begin, len, conn.parserKnowsUnicode(), conn.getExceptionInterceptor());
          }
          else {
            staticSql[i] = StringUtils.getBytes(sql, encoding, conn.getServerCharset(), begin, len, conn.parserKnowsUnicode(), conn, conn.getExceptionInterceptor());
          }
        }
      }
      catch (StringIndexOutOfBoundsException oobEx)
      {
        SQLException sqlEx = new SQLException("Parse error for " + sql);
        sqlEx.initCause(oobEx);
        
        throw sqlEx;
      }
      
      if (buildRewriteInfo) {
        canRewriteAsMultiValueInsert = ((PreparedStatement.canRewrite(sql, isOnDuplicateKeyUpdate, locationOfOnDuplicateKeyUpdate, statementStartPos)) && (!parametersInDuplicateKeyClause));
        

        if ((canRewriteAsMultiValueInsert) && (conn.getRewriteBatchedStatements())) {
          buildRewriteBatchedParams(sql, conn, dbmd, encoding, converter);
        }
      }
    }
    





    private void buildRewriteBatchedParams(String sql, MySQLConnection conn, DatabaseMetaData metadata, String encoding, SingleByteCharsetConverter converter)
      throws SQLException
    {
      valuesClause = extractValuesClause(sql, conn.getMetaData().getIdentifierQuoteString());
      String odkuClause = isOnDuplicateKeyUpdate ? sql.substring(locationOfOnDuplicateKeyUpdate) : null;
      
      String headSql = null;
      
      if (isOnDuplicateKeyUpdate) {
        headSql = sql.substring(0, locationOfOnDuplicateKeyUpdate);
      } else {
        headSql = sql;
      }
      
      batchHead = new ParseInfo(headSql, conn, metadata, encoding, converter, false);
      batchValues = new ParseInfo("," + valuesClause, conn, metadata, encoding, converter, false);
      batchODKUClause = null;
      
      if ((odkuClause != null) && (odkuClause.length() > 0)) {
        batchODKUClause = new ParseInfo("," + valuesClause + " " + odkuClause, conn, metadata, encoding, converter, false);
      }
    }
    
    private String extractValuesClause(String sql, String quoteCharStr) throws SQLException {
      int indexOfValues = -1;
      int valuesSearchStart = statementStartPos;
      
      while (indexOfValues == -1) {
        if (quoteCharStr.length() > 0) {
          indexOfValues = StringUtils.indexOfIgnoreCase(valuesSearchStart, sql, "VALUES", quoteCharStr, quoteCharStr, StringUtils.SEARCH_MODE__MRK_COM_WS);
        }
        else {
          indexOfValues = StringUtils.indexOfIgnoreCase(valuesSearchStart, sql, "VALUES");
        }
        
        if (indexOfValues <= 0)
          break;
        char c = sql.charAt(indexOfValues - 1);
        if ((!Character.isWhitespace(c)) && (c != ')') && (c != '`')) {
          valuesSearchStart = indexOfValues + 6;
          indexOfValues = -1;
        }
        else {
          c = sql.charAt(indexOfValues + 6);
          if ((!Character.isWhitespace(c)) && (c != '(')) {
            valuesSearchStart = indexOfValues + 6;
            indexOfValues = -1;
          }
        }
      }
      



      if (indexOfValues == -1) {
        return null;
      }
      
      int indexOfFirstParen = sql.indexOf('(', indexOfValues + 6);
      
      if (indexOfFirstParen == -1) {
        return null;
      }
      
      int endOfValuesClause = sql.lastIndexOf(')');
      
      if (endOfValuesClause == -1) {
        return null;
      }
      
      if (isOnDuplicateKeyUpdate) {
        endOfValuesClause = locationOfOnDuplicateKeyUpdate - 1;
      }
      
      return sql.substring(indexOfFirstParen, endOfValuesClause + 1);
    }
    


    synchronized ParseInfo getParseInfoForBatch(int numBatch)
    {
      PreparedStatement.AppendingBatchVisitor apv = new PreparedStatement.AppendingBatchVisitor();
      buildInfoForBatch(numBatch, apv);
      
      ParseInfo batchParseInfo = new ParseInfo(apv.getStaticSqlStrings(), firstStmtChar, foundLoadData, isOnDuplicateKeyUpdate, locationOfOnDuplicateKeyUpdate, statementLength, statementStartPos);
      

      return batchParseInfo;
    }
    



    String getSqlForBatch(int numBatch)
      throws UnsupportedEncodingException
    {
      ParseInfo batchInfo = getParseInfoForBatch(numBatch);
      
      return getSqlForBatch(batchInfo);
    }
    

    String getSqlForBatch(ParseInfo batchInfo)
      throws UnsupportedEncodingException
    {
      int size = 0;
      byte[][] sqlStrings = staticSql;
      int sqlStringsLength = sqlStrings.length;
      
      for (int i = 0; i < sqlStringsLength; i++) {
        size += sqlStrings[i].length;
        size++;
      }
      
      StringBuilder buf = new StringBuilder(size);
      
      for (int i = 0; i < sqlStringsLength - 1; i++) {
        buf.append(StringUtils.toString(sqlStrings[i], charEncoding));
        buf.append("?");
      }
      
      buf.append(StringUtils.toString(sqlStrings[(sqlStringsLength - 1)]));
      
      return buf.toString();
    }
    







    private void buildInfoForBatch(int numBatch, PreparedStatement.BatchVisitor visitor)
    {
      byte[][] headStaticSql = batchHead.staticSql;
      int headStaticSqlLength = headStaticSql.length;
      
      if (headStaticSqlLength > 1) {
        for (int i = 0; i < headStaticSqlLength - 1; i++) {
          visitor.append(headStaticSql[i]).increment();
        }
      }
      

      byte[] endOfHead = headStaticSql[(headStaticSqlLength - 1)];
      byte[][] valuesStaticSql = batchValues.staticSql;
      byte[] beginOfValues = valuesStaticSql[0];
      
      visitor.merge(endOfHead, beginOfValues).increment();
      
      int numValueRepeats = numBatch - 1;
      
      if (batchODKUClause != null) {
        numValueRepeats--;
      }
      
      int valuesStaticSqlLength = valuesStaticSql.length;
      byte[] endOfValues = valuesStaticSql[(valuesStaticSqlLength - 1)];
      
      for (int i = 0; i < numValueRepeats; i++) {
        for (int j = 1; j < valuesStaticSqlLength - 1; j++) {
          visitor.append(valuesStaticSql[j]).increment();
        }
        visitor.merge(endOfValues, beginOfValues).increment();
      }
      
      if (batchODKUClause != null) {
        byte[][] batchOdkuStaticSql = batchODKUClause.staticSql;
        byte[] beginOfOdku = batchOdkuStaticSql[0];
        visitor.decrement().merge(endOfValues, beginOfOdku).increment();
        
        int batchOdkuStaticSqlLength = batchOdkuStaticSql.length;
        
        if (numBatch > 1) {
          for (int i = 1; i < batchOdkuStaticSqlLength; i++) {
            visitor.append(batchOdkuStaticSql[i]).increment();
          }
        } else {
          visitor.decrement().append(batchOdkuStaticSql[(batchOdkuStaticSqlLength - 1)]);
        }
      }
      else {
        visitor.decrement().append(staticSql[(staticSql.length - 1)]);
      }
    }
    
    private ParseInfo(byte[][] staticSql, char firstStmtChar, boolean foundLoadData, boolean isOnDuplicateKeyUpdate, int locationOfOnDuplicateKeyUpdate, int statementLength, int statementStartPos)
    {
      this.firstStmtChar = firstStmtChar;
      this.foundLoadData = foundLoadData;
      this.isOnDuplicateKeyUpdate = isOnDuplicateKeyUpdate;
      this.locationOfOnDuplicateKeyUpdate = locationOfOnDuplicateKeyUpdate;
      this.statementLength = statementLength;
      this.statementStartPos = statementStartPos;
      this.staticSql = staticSql;
    }
  }
  
  static abstract interface BatchVisitor { public abstract BatchVisitor increment();
    
    public abstract BatchVisitor decrement();
    
    public abstract BatchVisitor append(byte[] paramArrayOfByte);
    
    public abstract BatchVisitor merge(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2);
  }
  
  static class AppendingBatchVisitor implements PreparedStatement.BatchVisitor { AppendingBatchVisitor() {}
    
    LinkedList<byte[]> statementComponents = new LinkedList();
    
    public PreparedStatement.BatchVisitor append(byte[] values) {
      statementComponents.addLast(values);
      
      return this;
    }
    
    public PreparedStatement.BatchVisitor increment()
    {
      return this;
    }
    
    public PreparedStatement.BatchVisitor decrement() {
      statementComponents.removeLast();
      
      return this;
    }
    
    public PreparedStatement.BatchVisitor merge(byte[] front, byte[] back) {
      int mergedLength = front.length + back.length;
      byte[] merged = new byte[mergedLength];
      System.arraycopy(front, 0, merged, 0, front.length);
      System.arraycopy(back, 0, merged, front.length, back.length);
      statementComponents.addLast(merged);
      return this;
    }
    
    public byte[][] getStaticSqlStrings() {
      byte[][] asBytes = new byte[statementComponents.size()][];
      statementComponents.toArray(asBytes);
      
      return asBytes;
    }
    
    public String toString()
    {
      StringBuilder buf = new StringBuilder();
      Iterator<byte[]> iter = statementComponents.iterator();
      while (iter.hasNext()) {
        buf.append(StringUtils.toString((byte[])iter.next()));
      }
      
      return buf.toString();
    }
  }
  

  private static final byte[] HEX_DIGITS = { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70 };
  









  protected static int readFully(Reader reader, char[] buf, int length)
    throws IOException
  {
    int numCharsRead = 0;
    
    while (numCharsRead < length) {
      int count = reader.read(buf, numCharsRead, length - numCharsRead);
      
      if (count < 0) {
        break;
      }
      
      numCharsRead += count;
    }
    
    return numCharsRead;
  }
  






  protected boolean batchHasPlainStatements = false;
  
  private DatabaseMetaData dbmd = null;
  




  protected char firstCharOfStmt = '\000';
  

  protected boolean isLoadDataQuery = false;
  
  protected boolean[] isNull = null;
  
  private boolean[] isStream = null;
  
  protected int numberOfExecutions = 0;
  

  protected String originalSql = null;
  

  protected int parameterCount;
  
  protected MysqlParameterMetadata parameterMetaData;
  
  private InputStream[] parameterStreams = null;
  
  private byte[][] parameterValues = (byte[][])null;
  




  protected int[] parameterTypes = null;
  
  protected ParseInfo parseInfo;
  
  private java.sql.ResultSetMetaData pstmtResultMetaData;
  
  private byte[][] staticSqlStrings = (byte[][])null;
  
  private byte[] streamConvertBuf = null;
  
  private int[] streamLengths = null;
  
  private SimpleDateFormat tsdf = null;
  

  private SimpleDateFormat ddf;
  

  private SimpleDateFormat tdf;
  

  protected boolean useTrueBoolean = false;
  
  protected boolean usingAnsiMode;
  
  protected String batchedValuesClause;
  
  private boolean doPingInstead;
  
  private boolean compensateForOnDuplicateKeyUpdate = false;
  

  private CharsetEncoder charsetEncoder;
  

  protected int batchCommandIndex = -1;
  


  protected boolean serverSupportsFracSecs;
  



  protected static PreparedStatement getInstance(MySQLConnection conn, String catalog)
    throws SQLException
  {
    if (!Util.isJdbc4()) {
      return new PreparedStatement(conn, catalog);
    }
    
    return (PreparedStatement)Util.handleNewInstance(JDBC_4_PSTMT_2_ARG_CTOR, new Object[] { conn, catalog }, conn.getExceptionInterceptor());
  }
  





  protected static PreparedStatement getInstance(MySQLConnection conn, String sql, String catalog)
    throws SQLException
  {
    if (!Util.isJdbc4()) {
      return new PreparedStatement(conn, sql, catalog);
    }
    
    return (PreparedStatement)Util.handleNewInstance(JDBC_4_PSTMT_3_ARG_CTOR, new Object[] { conn, sql, catalog }, conn.getExceptionInterceptor());
  }
  





  protected static PreparedStatement getInstance(MySQLConnection conn, String sql, String catalog, ParseInfo cachedParseInfo)
    throws SQLException
  {
    if (!Util.isJdbc4()) {
      return new PreparedStatement(conn, sql, catalog, cachedParseInfo);
    }
    
    return (PreparedStatement)Util.handleNewInstance(JDBC_4_PSTMT_4_ARG_CTOR, new Object[] { conn, sql, catalog, cachedParseInfo }, conn.getExceptionInterceptor());
  }
  










  public PreparedStatement(MySQLConnection conn, String catalog)
    throws SQLException
  {
    super(conn, catalog);
    
    detectFractionalSecondsSupport();
    compensateForOnDuplicateKeyUpdate = connection.getCompensateOnDuplicateKeyUpdateCounts();
  }
  
  protected void detectFractionalSecondsSupport() throws SQLException {
    serverSupportsFracSecs = ((connection != null) && (connection.versionMeetsMinimum(5, 6, 4)));
  }
  











  public PreparedStatement(MySQLConnection conn, String sql, String catalog)
    throws SQLException
  {
    super(conn, catalog);
    
    if (sql == null) {
      throw SQLError.createSQLException(Messages.getString("PreparedStatement.0"), "S1009", getExceptionInterceptor());
    }
    
    detectFractionalSecondsSupport();
    originalSql = sql;
    
    doPingInstead = originalSql.startsWith("/* ping */");
    
    dbmd = connection.getMetaData();
    
    useTrueBoolean = connection.versionMeetsMinimum(3, 21, 23);
    
    parseInfo = new ParseInfo(sql, connection, dbmd, charEncoding, charConverter);
    
    initializeFromParseInfo();
    
    compensateForOnDuplicateKeyUpdate = connection.getCompensateOnDuplicateKeyUpdateCounts();
    
    if (conn.getRequiresEscapingEncoder()) {
      charsetEncoder = Charset.forName(conn.getEncoding()).newEncoder();
    }
  }
  












  public PreparedStatement(MySQLConnection conn, String sql, String catalog, ParseInfo cachedParseInfo)
    throws SQLException
  {
    super(conn, catalog);
    
    if (sql == null) {
      throw SQLError.createSQLException(Messages.getString("PreparedStatement.1"), "S1009", getExceptionInterceptor());
    }
    
    detectFractionalSecondsSupport();
    originalSql = sql;
    
    dbmd = connection.getMetaData();
    
    useTrueBoolean = connection.versionMeetsMinimum(3, 21, 23);
    
    parseInfo = cachedParseInfo;
    
    usingAnsiMode = (!connection.useAnsiQuotedIdentifiers());
    
    initializeFromParseInfo();
    
    compensateForOnDuplicateKeyUpdate = connection.getCompensateOnDuplicateKeyUpdateCounts();
    
    if (conn.getRequiresEscapingEncoder()) {
      charsetEncoder = Charset.forName(conn.getEncoding()).newEncoder();
    }
  }
  






  public void addBatch()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (batchedArgs == null) {
        batchedArgs = new ArrayList();
      }
      
      for (int i = 0; i < parameterValues.length; i++) {
        checkAllParametersSet(parameterValues[i], parameterStreams[i], i);
      }
      
      batchedArgs.add(new BatchParams(parameterValues, parameterStreams, isStream, streamLengths, isNull));
    }
  }
  
  public void addBatch(String sql) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      batchHasPlainStatements = true;
      
      super.addBatch(sql);
    }
  }
  
  public String asSql() throws SQLException {
    return asSql(false);
  }
  
  public String asSql(boolean quoteStreamsAndUnknowns) throws SQLException {
    synchronized (checkClosed().getConnectionMutex())
    {
      StringBuilder buf = new StringBuilder();
      try
      {
        int realParameterCount = parameterCount + getParameterIndexOffset();
        Object batchArg = null;
        if (batchCommandIndex != -1) {
          batchArg = batchedArgs.get(batchCommandIndex);
        }
        
        for (int i = 0; i < realParameterCount; i++) {
          if (charEncoding != null) {
            buf.append(StringUtils.toString(staticSqlStrings[i], charEncoding));
          } else {
            buf.append(StringUtils.toString(staticSqlStrings[i]));
          }
          
          byte[] val = null;
          if ((batchArg != null) && ((batchArg instanceof String))) {
            buf.append((String)batchArg);
          }
          else {
            if (batchCommandIndex == -1) {
              val = parameterValues[i];
            } else {
              val = parameterStrings[i];
            }
            
            boolean isStreamParam = false;
            if (batchCommandIndex == -1) {
              isStreamParam = isStream[i];
            } else {
              isStreamParam = isStream[i];
            }
            
            if ((val == null) && (!isStreamParam)) {
              if (quoteStreamsAndUnknowns) {
                buf.append("'");
              }
              
              buf.append("** NOT SPECIFIED **");
              
              if (quoteStreamsAndUnknowns) {
                buf.append("'");
              }
            } else if (isStreamParam) {
              if (quoteStreamsAndUnknowns) {
                buf.append("'");
              }
              
              buf.append("** STREAM DATA **");
              
              if (quoteStreamsAndUnknowns) {
                buf.append("'");
              }
            }
            else if (charConverter != null) {
              buf.append(charConverter.toString(val));
            }
            else if (charEncoding != null) {
              buf.append(new String(val, charEncoding));
            } else {
              buf.append(StringUtils.toAsciiString(val));
            }
          }
        }
        

        if (charEncoding != null) {
          buf.append(StringUtils.toString(staticSqlStrings[(parameterCount + getParameterIndexOffset())], charEncoding));
        } else {
          buf.append(StringUtils.toAsciiString(staticSqlStrings[(parameterCount + getParameterIndexOffset())]));
        }
      } catch (UnsupportedEncodingException uue) {
        throw new RuntimeException(Messages.getString("PreparedStatement.32") + charEncoding + Messages.getString("PreparedStatement.33"));
      }
      
      return buf.toString();
    }
  }
  
  public void clearBatch() throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      batchHasPlainStatements = false;
      
      super.clearBatch();
    }
  }
  








  public void clearParameters()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      for (int i = 0; i < parameterValues.length; i++) {
        parameterValues[i] = null;
        parameterStreams[i] = null;
        isStream[i] = false;
        isNull[i] = false;
        parameterTypes[i] = 0;
      }
    }
  }
  
  private final void escapeblockFast(byte[] buf, Buffer packet, int size) throws SQLException {
    int lastwritten = 0;
    
    for (int i = 0; i < size; i++) {
      byte b = buf[i];
      
      if (b == 0)
      {
        if (i > lastwritten) {
          packet.writeBytesNoNull(buf, lastwritten, i - lastwritten);
        }
        

        packet.writeByte((byte)92);
        packet.writeByte((byte)48);
        lastwritten = i + 1;
      }
      else if ((b == 92) || (b == 39) || ((!usingAnsiMode) && (b == 34)))
      {
        if (i > lastwritten) {
          packet.writeBytesNoNull(buf, lastwritten, i - lastwritten);
        }
        

        packet.writeByte((byte)92);
        lastwritten = i;
      }
    }
    


    if (lastwritten < size) {
      packet.writeBytesNoNull(buf, lastwritten, size - lastwritten);
    }
  }
  
  private final void escapeblockFast(byte[] buf, ByteArrayOutputStream bytesOut, int size) {
    int lastwritten = 0;
    
    for (int i = 0; i < size; i++) {
      byte b = buf[i];
      
      if (b == 0)
      {
        if (i > lastwritten) {
          bytesOut.write(buf, lastwritten, i - lastwritten);
        }
        

        bytesOut.write(92);
        bytesOut.write(48);
        lastwritten = i + 1;
      }
      else if ((b == 92) || (b == 39) || ((!usingAnsiMode) && (b == 34)))
      {
        if (i > lastwritten) {
          bytesOut.write(buf, lastwritten, i - lastwritten);
        }
        

        bytesOut.write(92);
        lastwritten = i;
      }
    }
    


    if (lastwritten < size) {
      bytesOut.write(buf, lastwritten, size - lastwritten);
    }
  }
  




  protected boolean checkReadOnlySafeStatement()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return (firstCharOfStmt == 'S') || (!connection.isReadOnly());
    }
  }
  









  public boolean execute()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      MySQLConnection locallyScopedConn = connection;
      
      if ((!doPingInstead) && (!checkReadOnlySafeStatement())) {
        throw SQLError.createSQLException(Messages.getString("PreparedStatement.20") + Messages.getString("PreparedStatement.21"), "S1009", getExceptionInterceptor());
      }
      

      ResultSetInternalMethods rs = null;
      
      lastQueryIsOnDupKeyUpdate = false;
      
      if (retrieveGeneratedKeys) {
        lastQueryIsOnDupKeyUpdate = containsOnDuplicateKeyUpdateInSQL();
      }
      
      batchedGeneratedKeys = null;
      
      resetCancelledState();
      
      implicitlyCloseAllOpenResults();
      
      clearWarnings();
      
      if (doPingInstead) {
        doPingInstead();
        
        return true;
      }
      
      setupStreamingTimeout(locallyScopedConn);
      
      Buffer sendPacket = fillSendPacket();
      
      String oldCatalog = null;
      
      if (!locallyScopedConn.getCatalog().equals(currentCatalog)) {
        oldCatalog = locallyScopedConn.getCatalog();
        locallyScopedConn.setCatalog(currentCatalog);
      }
      



      CachedResultSetMetaData cachedMetadata = null;
      if (locallyScopedConn.getCacheResultSetMetadata()) {
        cachedMetadata = locallyScopedConn.getCachedMetaData(originalSql);
      }
      
      Field[] metadataFromCache = null;
      
      if (cachedMetadata != null) {
        metadataFromCache = fields;
      }
      
      boolean oldInfoMsgState = false;
      
      if (retrieveGeneratedKeys) {
        oldInfoMsgState = locallyScopedConn.isReadInfoMsgEnabled();
        locallyScopedConn.setReadInfoMsgEnabled(true);
      }
      



      locallyScopedConn.setSessionMaxRows(firstCharOfStmt == 'S' ? maxRows : -1);
      
      rs = executeInternal(maxRows, sendPacket, createStreamingResultSet(), firstCharOfStmt == 'S', metadataFromCache, false);
      
      if (cachedMetadata != null) {
        locallyScopedConn.initializeResultsMetadataFromCache(originalSql, cachedMetadata, rs);
      }
      else if ((rs.reallyResult()) && (locallyScopedConn.getCacheResultSetMetadata())) {
        locallyScopedConn.initializeResultsMetadataFromCache(originalSql, null, rs);
      }
      

      if (retrieveGeneratedKeys) {
        locallyScopedConn.setReadInfoMsgEnabled(oldInfoMsgState);
        rs.setFirstCharOfQuery(firstCharOfStmt);
      }
      
      if (oldCatalog != null) {
        locallyScopedConn.setCatalog(oldCatalog);
      }
      
      if (rs != null) {
        lastInsertId = rs.getUpdateID();
        
        results = rs;
      }
      
      return (rs != null) && (rs.reallyResult());
    }
  }
  
  protected long[] executeBatchInternal() throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (connection.isReadOnly()) {
        throw new SQLException(Messages.getString("PreparedStatement.25") + Messages.getString("PreparedStatement.26"), "S1009");
      }
      

      if ((batchedArgs == null) || (batchedArgs.size() == 0)) {
        return new long[0];
      }
      

      int batchTimeout = timeoutInMillis;
      timeoutInMillis = 0;
      
      resetCancelledState();
      try
      {
        statementBegins();
        
        clearWarnings();
        
        if ((!batchHasPlainStatements) && (connection.getRewriteBatchedStatements()))
        {
          if (canRewriteAsMultiValueInsertAtSqlLevel()) {
            arrayOfLong = executeBatchedInserts(batchTimeout);jsr 83;return arrayOfLong;
          }
          
          if ((connection.versionMeetsMinimum(4, 1, 0)) && (!batchHasPlainStatements) && (batchedArgs != null) && (batchedArgs.size() > 3))
          {
            arrayOfLong = executePreparedBatchAsMultiStatement(batchTimeout);jsr 28;return arrayOfLong;
          }
        }
        
        long[] arrayOfLong = executeBatchSerially(batchTimeout);jsr 15;return arrayOfLong;
      } finally {
        jsr 6; } localObject2 = returnAddress;statementExecuting.set(false);
      
      clearBatch();ret;
    }
  }
  
  public boolean canRewriteAsMultiValueInsertAtSqlLevel() throws SQLException
  {
    return parseInfo.canRewriteAsMultiValueInsert;
  }
  
  protected int getLocationOfOnDuplicateKeyUpdate() throws SQLException {
    return parseInfo.locationOfOnDuplicateKeyUpdate;
  }
  














































































































































































  private String generateMultiStatementForBatch(int numBatches)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      StringBuilder newStatementSql = new StringBuilder((originalSql.length() + 1) * numBatches);
      
      newStatementSql.append(originalSql);
      
      for (int i = 0; i < numBatches - 1; i++) {
        newStatementSql.append(';');
        newStatementSql.append(originalSql);
      }
      
      return newStatementSql.toString();
    }
  }
  















































































































































  protected String getValuesClause()
    throws SQLException
  {
    return parseInfo.valuesClause;
  }
  





  protected int computeBatchSize(int numBatchedArgs)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      long[] combinedValues = computeMaxParameterSetSizeAndBatchSize(numBatchedArgs);
      
      long maxSizeOfParameterSet = combinedValues[0];
      long sizeOfEntireBatch = combinedValues[1];
      
      int maxAllowedPacket = connection.getMaxAllowedPacket();
      
      if (sizeOfEntireBatch < maxAllowedPacket - originalSql.length()) {
        return numBatchedArgs;
      }
      
      return (int)Math.max(1L, (maxAllowedPacket - originalSql.length()) / maxSizeOfParameterSet);
    }
  }
  




  protected long[] computeMaxParameterSetSizeAndBatchSize(int numBatchedArgs)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      long sizeOfEntireBatch = 0L;
      long maxSizeOfParameterSet = 0L;
      
      for (int i = 0; i < numBatchedArgs; i++) {
        BatchParams paramArg = (BatchParams)batchedArgs.get(i);
        
        boolean[] isNullBatch = isNull;
        boolean[] isStreamBatch = isStream;
        
        long sizeOfParameterSet = 0L;
        
        for (int j = 0; j < isNullBatch.length; j++) {
          if (isNullBatch[j] == 0)
          {
            if (isStreamBatch[j] != 0) {
              int streamLength = streamLengths[j];
              
              if (streamLength != -1) {
                sizeOfParameterSet += streamLength * 2;
              } else {
                int paramLength = parameterStrings[j].length;
                sizeOfParameterSet += paramLength;
              }
            } else {
              sizeOfParameterSet += parameterStrings[j].length;
            }
          } else {
            sizeOfParameterSet += 4L;
          }
        }
        






        if (getValuesClause() != null) {
          sizeOfParameterSet += getValuesClause().length() + 1;
        } else {
          sizeOfParameterSet += originalSql.length() + 1;
        }
        
        sizeOfEntireBatch += sizeOfParameterSet;
        
        if (sizeOfParameterSet > maxSizeOfParameterSet) {
          maxSizeOfParameterSet = sizeOfParameterSet;
        }
      }
      
      return new long[] { maxSizeOfParameterSet, sizeOfEntireBatch };
    }
  }
  






  protected long[] executeBatchSerially(int batchTimeout)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      MySQLConnection locallyScopedConn = connection;
      
      if (locallyScopedConn == null) {
        checkClosed();
      }
      
      long[] updateCounts = null;
      
      if (batchedArgs != null) {
        int nbrCommands = batchedArgs.size();
        updateCounts = new long[nbrCommands];
        
        for (int i = 0; i < nbrCommands; i++) {
          updateCounts[i] = -3L;
        }
        
        SQLException sqlEx = null;
        
        StatementImpl.CancelTask timeoutTask = null;
        try
        {
          if ((locallyScopedConn.getEnableQueryTimeouts()) && (batchTimeout != 0) && (locallyScopedConn.versionMeetsMinimum(5, 0, 0))) {
            timeoutTask = new StatementImpl.CancelTask(this, this);
            locallyScopedConn.getCancelTimer().schedule(timeoutTask, batchTimeout);
          }
          
          if (retrieveGeneratedKeys) {
            batchedGeneratedKeys = new ArrayList(nbrCommands);
          }
          
          for (batchCommandIndex = 0; batchCommandIndex < nbrCommands; batchCommandIndex += 1) {
            Object arg = batchedArgs.get(batchCommandIndex);
            try
            {
              if ((arg instanceof String)) {
                updateCounts[batchCommandIndex] = executeUpdateInternal((String)arg, true, retrieveGeneratedKeys);
                

                getBatchedGeneratedKeys((results.getFirstCharOfQuery() == 'I') && (containsOnDuplicateKeyInString((String)arg)) ? 1 : 0);
              } else {
                BatchParams paramArg = (BatchParams)arg;
                updateCounts[batchCommandIndex] = executeUpdateInternal(parameterStrings, parameterStreams, isStream, streamLengths, isNull, true);
                


                getBatchedGeneratedKeys(containsOnDuplicateKeyUpdateInSQL() ? 1 : 0);
              }
            } catch (SQLException ex) {
              updateCounts[batchCommandIndex] = -3L;
              
              if ((continueBatchOnError) && (!(ex instanceof MySQLTimeoutException)) && (!(ex instanceof MySQLStatementCancelledException)) && (!hasDeadlockOrTimeoutRolledBackTx(ex)))
              {
                sqlEx = ex;
              } else {
                long[] newUpdateCounts = new long[batchCommandIndex];
                System.arraycopy(updateCounts, 0, newUpdateCounts, 0, batchCommandIndex);
                
                throw SQLError.createBatchUpdateException(ex, newUpdateCounts, getExceptionInterceptor());
              }
            }
          }
          
          if (sqlEx != null) {
            throw SQLError.createBatchUpdateException(sqlEx, updateCounts, getExceptionInterceptor());
          }
        } catch (NullPointerException npe) {
          try {
            checkClosed();
          } catch (SQLException connectionClosedEx) {
            updateCounts[batchCommandIndex] = -3L;
            
            long[] newUpdateCounts = new long[batchCommandIndex];
            
            System.arraycopy(updateCounts, 0, newUpdateCounts, 0, batchCommandIndex);
            
            throw SQLError.createBatchUpdateException(connectionClosedEx, newUpdateCounts, getExceptionInterceptor());
          }
          
          throw npe;
        } finally {
          batchCommandIndex = -1;
          
          if (timeoutTask != null) {
            timeoutTask.cancel();
            locallyScopedConn.getCancelTimer().purge();
          }
          
          resetCancelledState();
        }
      }
      
      return updateCounts != null ? updateCounts : new long[0];
    }
  }
  
  public String getDateTime(String pattern)
  {
    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
    return sdf.format(new java.util.Date());
  }
  


















  protected ResultSetInternalMethods executeInternal(int maxRowsToRetrieve, Buffer sendPacket, boolean createStreamingResultSet, boolean queryIsSelectOnly, Field[] metadataFromCache, boolean isBatch)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      try {
        MySQLConnection locallyScopedConnection = connection;
        
        numberOfExecutions += 1;
        


        StatementImpl.CancelTask timeoutTask = null;
        ResultSetInternalMethods rs;
        try {
          if ((locallyScopedConnection.getEnableQueryTimeouts()) && (timeoutInMillis != 0) && (locallyScopedConnection.versionMeetsMinimum(5, 0, 0))) {
            timeoutTask = new StatementImpl.CancelTask(this, this);
            locallyScopedConnection.getCancelTimer().schedule(timeoutTask, timeoutInMillis);
          }
          
          if (!isBatch) {
            statementBegins();
          }
          
          rs = locallyScopedConnection.execSQL(this, null, maxRowsToRetrieve, sendPacket, resultSetType, resultSetConcurrency, createStreamingResultSet, currentCatalog, metadataFromCache, isBatch);
          

          if (timeoutTask != null) {
            timeoutTask.cancel();
            
            locallyScopedConnection.getCancelTimer().purge();
            
            if (caughtWhileCancelling != null) {
              throw caughtWhileCancelling;
            }
            
            timeoutTask = null;
          }
          
          synchronized (cancelTimeoutMutex) {
            if (wasCancelled) {
              SQLException cause = null;
              
              if (wasCancelledByTimeout) {
                cause = new MySQLTimeoutException();
              } else {
                cause = new MySQLStatementCancelledException();
              }
              
              resetCancelledState();
              
              throw cause;
            }
          }
        } finally {
          if (!isBatch) {
            statementExecuting.set(false);
          }
          
          if (timeoutTask != null) {
            timeoutTask.cancel();
            locallyScopedConnection.getCancelTimer().purge();
          }
        }
        
        return rs;
      } catch (NullPointerException npe) {
        checkClosed();
        

        throw npe;
      }
    }
  }
  







  public ResultSet executeQuery()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      MySQLConnection locallyScopedConn = connection;
      
      checkForDml(originalSql, firstCharOfStmt);
      
      batchedGeneratedKeys = null;
      
      resetCancelledState();
      
      implicitlyCloseAllOpenResults();
      
      clearWarnings();
      
      if (doPingInstead) {
        doPingInstead();
        
        return results;
      }
      
      setupStreamingTimeout(locallyScopedConn);
      
      Buffer sendPacket = fillSendPacket();
      
      String oldCatalog = null;
      
      if (!locallyScopedConn.getCatalog().equals(currentCatalog)) {
        oldCatalog = locallyScopedConn.getCatalog();
        locallyScopedConn.setCatalog(currentCatalog);
      }
      



      CachedResultSetMetaData cachedMetadata = null;
      if (locallyScopedConn.getCacheResultSetMetadata()) {
        cachedMetadata = locallyScopedConn.getCachedMetaData(originalSql);
      }
      
      Field[] metadataFromCache = null;
      
      if (cachedMetadata != null) {
        metadataFromCache = fields;
      }
      
      locallyScopedConn.setSessionMaxRows(maxRows);
      
      results = executeInternal(maxRows, sendPacket, createStreamingResultSet(), true, metadataFromCache, false);
      
      if (oldCatalog != null) {
        locallyScopedConn.setCatalog(oldCatalog);
      }
      
      if (cachedMetadata != null) {
        locallyScopedConn.initializeResultsMetadataFromCache(originalSql, cachedMetadata, results);
      }
      else if (locallyScopedConn.getCacheResultSetMetadata()) {
        locallyScopedConn.initializeResultsMetadataFromCache(originalSql, null, results);
      }
      

      lastInsertId = results.getUpdateID();
      
      return results;
    }
  }
  









  public int executeUpdate()
    throws SQLException
  {
    return Util.truncateAndConvertToInt(executeLargeUpdate());
  }
  



  protected long executeUpdateInternal(boolean clearBatchedGeneratedKeysAndWarnings, boolean isBatch)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (clearBatchedGeneratedKeysAndWarnings) {
        clearWarnings();
        batchedGeneratedKeys = null;
      }
      
      return executeUpdateInternal(parameterValues, parameterStreams, isStream, streamLengths, isNull, isBatch);
    }
  }
  



















  protected long executeUpdateInternal(byte[][] batchedParameterStrings, InputStream[] batchedParameterStreams, boolean[] batchedIsStream, int[] batchedStreamLengths, boolean[] batchedIsNull, boolean isReallyBatch)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      MySQLConnection locallyScopedConn = connection;
      
      if (locallyScopedConn.isReadOnly(false)) {
        throw SQLError.createSQLException(Messages.getString("PreparedStatement.34") + Messages.getString("PreparedStatement.35"), "S1009", getExceptionInterceptor());
      }
      

      if ((firstCharOfStmt == 'S') && (isSelectQuery())) {
        throw SQLError.createSQLException(Messages.getString("PreparedStatement.37"), "01S03", getExceptionInterceptor());
      }
      
      resetCancelledState();
      
      implicitlyCloseAllOpenResults();
      
      ResultSetInternalMethods rs = null;
      
      Buffer sendPacket = fillSendPacket(batchedParameterStrings, batchedParameterStreams, batchedIsStream, batchedStreamLengths);
      
      String oldCatalog = null;
      
      if (!locallyScopedConn.getCatalog().equals(currentCatalog)) {
        oldCatalog = locallyScopedConn.getCatalog();
        locallyScopedConn.setCatalog(currentCatalog);
      }
      



      locallyScopedConn.setSessionMaxRows(-1);
      
      boolean oldInfoMsgState = false;
      
      if (retrieveGeneratedKeys) {
        oldInfoMsgState = locallyScopedConn.isReadInfoMsgEnabled();
        locallyScopedConn.setReadInfoMsgEnabled(true);
      }
      
      rs = executeInternal(-1, sendPacket, false, false, null, isReallyBatch);
      
      if (retrieveGeneratedKeys) {
        locallyScopedConn.setReadInfoMsgEnabled(oldInfoMsgState);
        rs.setFirstCharOfQuery(firstCharOfStmt);
      }
      
      if (oldCatalog != null) {
        locallyScopedConn.setCatalog(oldCatalog);
      }
      
      results = rs;
      
      updateCount = rs.getUpdateCount();
      
      if ((containsOnDuplicateKeyUpdateInSQL()) && (compensateForOnDuplicateKeyUpdate) && (
        (updateCount == 2L) || (updateCount == 0L))) {
        updateCount = 1L;
      }
      

      lastInsertId = rs.getUpdateID();
      
      return updateCount;
    }
  }
  
  protected boolean containsOnDuplicateKeyUpdateInSQL() {
    return parseInfo.isOnDuplicateKeyUpdate;
  }
  







  protected Buffer fillSendPacket()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return fillSendPacket(parameterValues, parameterStreams, isStream, streamLengths);
    }
  }
  
















  protected Buffer fillSendPacket(byte[][] batchedParameterStrings, InputStream[] batchedParameterStreams, boolean[] batchedIsStream, int[] batchedStreamLengths)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      Buffer sendPacket = connection.getIO().getSharedSendPacket();
      
      sendPacket.clear();
      
      sendPacket.writeByte((byte)3);
      
      boolean useStreamLengths = connection.getUseStreamLengthsInPrepStmts();
      



      int ensurePacketSize = 0;
      
      String statementComment = connection.getStatementComment();
      
      byte[] commentAsBytes = null;
      
      if (statementComment != null) {
        if (charConverter != null) {
          commentAsBytes = charConverter.toBytes(statementComment);
        } else {
          commentAsBytes = StringUtils.getBytes(statementComment, charConverter, charEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor());
        }
        

        ensurePacketSize += commentAsBytes.length;
        ensurePacketSize += 6;
      }
      
      for (int i = 0; i < batchedParameterStrings.length; i++) {
        if ((batchedIsStream[i] != 0) && (useStreamLengths)) {
          ensurePacketSize += batchedStreamLengths[i];
        }
      }
      
      if (ensurePacketSize != 0) {
        sendPacket.ensureCapacity(ensurePacketSize);
      }
      
      if (commentAsBytes != null) {
        sendPacket.writeBytesNoNull(Constants.SLASH_STAR_SPACE_AS_BYTES);
        sendPacket.writeBytesNoNull(commentAsBytes);
        sendPacket.writeBytesNoNull(Constants.SPACE_STAR_SLASH_SPACE_AS_BYTES);
      }
      
      for (int i = 0; i < batchedParameterStrings.length; i++) {
        checkAllParametersSet(batchedParameterStrings[i], batchedParameterStreams[i], i);
        
        sendPacket.writeBytesNoNull(staticSqlStrings[i]);
        
        if (batchedIsStream[i] != 0) {
          streamToBytes(sendPacket, batchedParameterStreams[i], true, batchedStreamLengths[i], useStreamLengths);
        } else {
          sendPacket.writeBytesNoNull(batchedParameterStrings[i]);
        }
      }
      
      sendPacket.writeBytesNoNull(staticSqlStrings[batchedParameterStrings.length]);
      
      return sendPacket;
    }
  }
  
  private void checkAllParametersSet(byte[] parameterString, InputStream parameterStream, int columnIndex) throws SQLException {
    if ((parameterString == null) && (parameterStream == null))
    {
      throw SQLError.createSQLException(Messages.getString("PreparedStatement.40") + (columnIndex + 1), "07001", getExceptionInterceptor());
    }
  }
  


  protected PreparedStatement prepareBatchedInsertSQL(MySQLConnection localConn, int numBatches)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      PreparedStatement pstmt = new PreparedStatement(localConn, "Rewritten batch of: " + originalSql, currentCatalog, parseInfo.getParseInfoForBatch(numBatches));
      
      pstmt.setRetrieveGeneratedKeys(retrieveGeneratedKeys);
      rewrittenBatchSize = numBatches;
      
      return pstmt;
    }
  }
  
  protected void setRetrieveGeneratedKeys(boolean flag) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      retrieveGeneratedKeys = flag;
    }
  }
  
  protected int rewrittenBatchSize = 0;
  
  public int getRewrittenBatchSize() {
    return rewrittenBatchSize;
  }
  
  public String getNonRewrittenSql() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      int indexOfBatch = originalSql.indexOf(" of: ");
      
      if (indexOfBatch != -1) {
        return originalSql.substring(indexOfBatch + 5);
      }
      
      return originalSql;
    }
  }
  



  public byte[] getBytesRepresentation(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (isStream[parameterIndex] != 0) {
        return streamToBytes(parameterStreams[parameterIndex], false, streamLengths[parameterIndex], connection.getUseStreamLengthsInPrepStmts());
      }
      

      byte[] parameterVal = parameterValues[parameterIndex];
      
      if (parameterVal == null) {
        return null;
      }
      
      if ((parameterVal[0] == 39) && (parameterVal[(parameterVal.length - 1)] == 39)) {
        byte[] valNoQuotes = new byte[parameterVal.length - 2];
        System.arraycopy(parameterVal, 1, valNoQuotes, 0, parameterVal.length - 2);
        
        return valNoQuotes;
      }
      
      return parameterVal;
    }
  }
  





  protected byte[] getBytesRepresentationForBatch(int parameterIndex, int commandIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      Object batchedArg = batchedArgs.get(commandIndex);
      if ((batchedArg instanceof String)) {
        try {
          return StringUtils.getBytes((String)batchedArg, charEncoding);
        }
        catch (UnsupportedEncodingException uue) {
          throw new RuntimeException(Messages.getString("PreparedStatement.32") + charEncoding + Messages.getString("PreparedStatement.33"));
        }
      }
      
      BatchParams params = (BatchParams)batchedArg;
      if (isStream[parameterIndex] != 0) {
        return streamToBytes(parameterStreams[parameterIndex], false, streamLengths[parameterIndex], connection.getUseStreamLengthsInPrepStmts());
      }
      
      byte[] parameterVal = parameterStrings[parameterIndex];
      if (parameterVal == null) {
        return null;
      }
      
      if ((parameterVal[0] == 39) && (parameterVal[(parameterVal.length - 1)] == 39)) {
        byte[] valNoQuotes = new byte[parameterVal.length - 2];
        System.arraycopy(parameterVal, 1, valNoQuotes, 0, parameterVal.length - 2);
        
        return valNoQuotes;
      }
      
      return parameterVal;
    }
  }
  



  private final String getDateTimePattern(String dt, boolean toTime)
    throws Exception
  {
    int dtLength = dt != null ? dt.length() : 0;
    
    if ((dtLength >= 8) && (dtLength <= 10)) {
      int dashCount = 0;
      boolean isDateOnly = true;
      
      for (int i = 0; i < dtLength; i++) {
        char c = dt.charAt(i);
        
        if ((!Character.isDigit(c)) && (c != '-')) {
          isDateOnly = false;
          
          break;
        }
        
        if (c == '-') {
          dashCount++;
        }
      }
      
      if ((isDateOnly) && (dashCount == 2)) {
        return "yyyy-MM-dd";
      }
    }
    



    boolean colonsOnly = true;
    
    for (int i = 0; i < dtLength; i++) {
      char c = dt.charAt(i);
      
      if ((!Character.isDigit(c)) && (c != ':')) {
        colonsOnly = false;
        
        break;
      }
    }
    
    if (colonsOnly) {
      return "HH:mm:ss";
    }
    






    StringReader reader = new StringReader(dt + " ");
    ArrayList<Object[]> vec = new ArrayList();
    ArrayList<Object[]> vecRemovelist = new ArrayList();
    Object[] nv = new Object[3];
    
    nv[0] = Character.valueOf('y');
    nv[1] = new StringBuilder();
    nv[2] = Integer.valueOf(0);
    vec.add(nv);
    
    if (toTime) {
      nv = new Object[3];
      nv[0] = Character.valueOf('h');
      nv[1] = new StringBuilder();
      nv[2] = Integer.valueOf(0);
      vec.add(nv);
    }
    int z;
    while ((z = reader.read()) != -1) {
      char separator = (char)z;
      int maxvecs = vec.size();
      
      for (int count = 0; count < maxvecs; count++) {
        Object[] v = (Object[])vec.get(count);
        int n = ((Integer)v[2]).intValue();
        char c = getSuccessor(((Character)v[0]).charValue(), n);
        
        if (!Character.isLetterOrDigit(separator)) {
          if ((c == ((Character)v[0]).charValue()) && (c != 'S')) {
            vecRemovelist.add(v);
          } else {
            ((StringBuilder)v[1]).append(separator);
            
            if ((c == 'X') || (c == 'Y')) {
              v[2] = Integer.valueOf(4);
            }
          }
        } else {
          if (c == 'X') {
            c = 'y';
            nv = new Object[3];
            nv[1] = new StringBuilder(((StringBuilder)v[1]).toString()).append('M');
            nv[0] = Character.valueOf('M');
            nv[2] = Integer.valueOf(1);
            vec.add(nv);
          } else if (c == 'Y') {
            c = 'M';
            nv = new Object[3];
            nv[1] = new StringBuilder(((StringBuilder)v[1]).toString()).append('d');
            nv[0] = Character.valueOf('d');
            nv[2] = Integer.valueOf(1);
            vec.add(nv);
          }
          
          ((StringBuilder)v[1]).append(c);
          
          if (c == ((Character)v[0]).charValue()) {
            v[2] = Integer.valueOf(n + 1);
          } else {
            v[0] = Character.valueOf(c);
            v[2] = Integer.valueOf(1);
          }
        }
      }
      
      int size = vecRemovelist.size();
      
      for (int i = 0; i < size; i++) {
        Object[] v = (Object[])vecRemovelist.get(i);
        vec.remove(v);
      }
      
      vecRemovelist.clear();
    }
    
    int size = vec.size();
    
    for (int i = 0; i < size; i++) {
      Object[] v = (Object[])vec.get(i);
      char c = ((Character)v[0]).charValue();
      int n = ((Integer)v[2]).intValue();
      
      boolean bk = getSuccessor(c, n) != c;
      boolean atEnd = ((c == 's') || (c == 'm') || ((c == 'h') && (toTime))) && (bk);
      boolean finishesAtDate = (bk) && (c == 'd') && (!toTime);
      boolean containsEnd = ((StringBuilder)v[1]).toString().indexOf('W') != -1;
      
      if (((!atEnd) && (!finishesAtDate)) || (containsEnd)) {
        vecRemovelist.add(v);
      }
    }
    
    size = vecRemovelist.size();
    
    for (int i = 0; i < size; i++) {
      vec.remove(vecRemovelist.get(i));
    }
    
    vecRemovelist.clear();
    Object[] v = (Object[])vec.get(0);
    
    StringBuilder format = (StringBuilder)v[1];
    format.setLength(format.length() - 1);
    
    return format.toString();
  }
  








  public java.sql.ResultSetMetaData getMetaData()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {






      if (!isSelectQuery()) {
        return null;
      }
      
      PreparedStatement mdStmt = null;
      ResultSet mdRs = null;
      
      if (pstmtResultMetaData == null) {
        try {
          mdStmt = new PreparedStatement(connection, originalSql, currentCatalog, parseInfo);
          
          mdStmt.setMaxRows(1);
          
          int paramCount = parameterValues.length;
          
          for (int i = 1; i <= paramCount; i++) {
            mdStmt.setString(i, "");
          }
          
          boolean hadResults = mdStmt.execute();
          
          if (hadResults) {
            mdRs = mdStmt.getResultSet();
            
            pstmtResultMetaData = mdRs.getMetaData();
          } else {
            pstmtResultMetaData = new ResultSetMetaData(new Field[0], connection.getUseOldAliasMetadataBehavior(), connection.getYearIsDateType(), getExceptionInterceptor());
          }
        }
        finally {
          SQLException sqlExRethrow = null;
          
          if (mdRs != null) {
            try {
              mdRs.close();
            } catch (SQLException sqlEx) {
              sqlExRethrow = sqlEx;
            }
            
            mdRs = null;
          }
          
          if (mdStmt != null) {
            try {
              mdStmt.close();
            } catch (SQLException sqlEx) {
              sqlExRethrow = sqlEx;
            }
            
            mdStmt = null;
          }
          
          if (sqlExRethrow != null) {
            throw sqlExRethrow;
          }
        }
      }
      
      return pstmtResultMetaData;
    }
  }
  
  protected boolean isSelectQuery() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      return StringUtils.startsWithIgnoreCaseAndWs(StringUtils.stripComments(originalSql, "'\"", "'\"", true, false, true, true), "SELECT");
    }
  }
  

  public ParameterMetaData getParameterMetaData()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (parameterMetaData == null) {
        if (connection.getGenerateSimpleParameterMetadata()) {
          parameterMetaData = new MysqlParameterMetadata(parameterCount);
        } else {
          parameterMetaData = new MysqlParameterMetadata(null, parameterCount, getExceptionInterceptor());
        }
      }
      
      return parameterMetaData;
    }
  }
  
  ParseInfo getParseInfo() {
    return parseInfo;
  }
  
  private final char getSuccessor(char c, int n) {
    return (c == 's') && (n < 2) ? 's' : c == 'm' ? 's' : (c == 'm') && (n < 2) ? 'm' : c == 'H' ? 'm' : (c == 'H') && (n < 2) ? 'H' : c == 'd' ? 'H' : (c == 'd') && (n < 2) ? 'd' : c == 'M' ? 'd' : (c == 'M') && (n < 3) ? 'M' : (c == 'M') && (n == 2) ? 'Y' : c == 'y' ? 'M' : (c == 'y') && (n < 4) ? 'y' : (c == 'y') && (n == 2) ? 'X' : 'W';
  }
  










  private final void hexEscapeBlock(byte[] buf, Buffer packet, int size)
    throws SQLException
  {
    for (int i = 0; i < size; i++) {
      byte b = buf[i];
      int lowBits = (b & 0xFF) / 16;
      int highBits = (b & 0xFF) % 16;
      
      packet.writeByte(HEX_DIGITS[lowBits]);
      packet.writeByte(HEX_DIGITS[highBits]);
    }
  }
  
  private void initializeFromParseInfo() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      staticSqlStrings = parseInfo.staticSql;
      isLoadDataQuery = parseInfo.foundLoadData;
      firstCharOfStmt = parseInfo.firstStmtChar;
      
      parameterCount = (staticSqlStrings.length - 1);
      
      parameterValues = new byte[parameterCount][];
      parameterStreams = new InputStream[parameterCount];
      isStream = new boolean[parameterCount];
      streamLengths = new int[parameterCount];
      isNull = new boolean[parameterCount];
      parameterTypes = new int[parameterCount];
      
      clearParameters();
      
      for (int j = 0; j < parameterCount; j++) {
        isStream[j] = false;
      }
    }
  }
  
  boolean isNull(int paramIndex) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      return isNull[paramIndex];
    }
  }
  
  private final int readblock(InputStream i, byte[] b) throws SQLException {
    try {
      return i.read(b);
    } catch (Throwable ex) {
      SQLException sqlEx = SQLError.createSQLException(Messages.getString("PreparedStatement.56") + ex.getClass().getName(), "S1000", getExceptionInterceptor());
      
      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
  }
  
  private final int readblock(InputStream i, byte[] b, int length) throws SQLException {
    try {
      int lengthToRead = length;
      
      if (lengthToRead > b.length) {
        lengthToRead = b.length;
      }
      
      return i.read(b, 0, lengthToRead);
    } catch (Throwable ex) {
      SQLException sqlEx = SQLError.createSQLException(Messages.getString("PreparedStatement.56") + ex.getClass().getName(), "S1000", getExceptionInterceptor());
      
      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
  }
  








  protected void realClose(boolean calledExplicitly, boolean closeOpenResults)
    throws SQLException
  {
    MySQLConnection locallyScopedConn = connection;
    
    if (locallyScopedConn == null) {
      return;
    }
    
    synchronized (locallyScopedConn.getConnectionMutex())
    {


      if (isClosed) {
        return;
      }
      
      if ((useUsageAdvisor) && 
        (numberOfExecutions <= 1)) {
        String message = Messages.getString("PreparedStatement.43");
        
        eventSink.consumeEvent(new ProfilerEvent((byte)0, "", currentCatalog, connectionId, getId(), -1, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, message));
      }
      


      super.realClose(calledExplicitly, closeOpenResults);
      
      dbmd = null;
      originalSql = null;
      staticSqlStrings = ((byte[][])null);
      parameterValues = ((byte[][])null);
      parameterStreams = null;
      isStream = null;
      streamLengths = null;
      isNull = null;
      streamConvertBuf = null;
      parameterTypes = null;
    }
  }
  










  public void setArray(int i, Array x)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  



















  public void setAsciiStream(int parameterIndex, InputStream x, int length)
    throws SQLException
  {
    if (x == null) {
      setNull(parameterIndex, 12);
    } else {
      setBinaryStream(parameterIndex, x, length);
    }
  }
  










  public void setBigDecimal(int parameterIndex, BigDecimal x)
    throws SQLException
  {
    if (x == null) {
      setNull(parameterIndex, 3);
    } else {
      setInternal(parameterIndex, StringUtils.fixDecimalExponent(StringUtils.consistentToString(x)));
      
      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 3;
    }
  }
  

















  public void setBinaryStream(int parameterIndex, InputStream x, int length)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (x == null) {
        setNull(parameterIndex, -2);
      } else {
        int parameterIndexOffset = getParameterIndexOffset();
        
        if ((parameterIndex < 1) || (parameterIndex > staticSqlStrings.length)) {
          throw SQLError.createSQLException(Messages.getString("PreparedStatement.2") + parameterIndex + Messages.getString("PreparedStatement.3") + staticSqlStrings.length + Messages.getString("PreparedStatement.4"), "S1009", getExceptionInterceptor());
        }
        

        if ((parameterIndexOffset == -1) && (parameterIndex == 1)) {
          throw SQLError.createSQLException("Can't set IN parameter for return value of stored function call.", "S1009", getExceptionInterceptor());
        }
        

        parameterStreams[(parameterIndex - 1 + parameterIndexOffset)] = x;
        isStream[(parameterIndex - 1 + parameterIndexOffset)] = true;
        streamLengths[(parameterIndex - 1 + parameterIndexOffset)] = length;
        isNull[(parameterIndex - 1 + parameterIndexOffset)] = false;
        parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 2004;
      }
    }
  }
  
  public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
    setBinaryStream(parameterIndex, inputStream, (int)length);
  }
  









  public void setBlob(int i, Blob x)
    throws SQLException
  {
    if (x == null) {
      setNull(i, 2004);
    } else {
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      
      bytesOut.write(39);
      escapeblockFast(x.getBytes(1L, (int)x.length()), bytesOut, (int)x.length());
      bytesOut.write(39);
      
      setInternal(i, bytesOut.toByteArray());
      
      parameterTypes[(i - 1 + getParameterIndexOffset())] = 2004;
    }
  }
  










  public void setBoolean(int parameterIndex, boolean x)
    throws SQLException
  {
    if (useTrueBoolean) {
      setInternal(parameterIndex, x ? "1" : "0");
    } else {
      setInternal(parameterIndex, x ? "'t'" : "'f'");
      
      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 16;
    }
  }
  










  public void setByte(int parameterIndex, byte x)
    throws SQLException
  {
    setInternal(parameterIndex, String.valueOf(x));
    
    parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = -6;
  }
  











  public void setBytes(int parameterIndex, byte[] x)
    throws SQLException
  {
    setBytes(parameterIndex, x, true, true);
    
    if (x != null) {
      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = -2;
    }
  }
  
  protected void setBytes(int parameterIndex, byte[] x, boolean checkForIntroducer, boolean escapeForMBChars) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (x == null) {
        setNull(parameterIndex, -2);
      } else {
        String connectionEncoding = connection.getEncoding();
        try
        {
          if ((connection.isNoBackslashEscapesSet()) || ((escapeForMBChars) && (connection.getUseUnicode()) && (connectionEncoding != null) && (CharsetMapping.isMultibyteCharset(connectionEncoding))))
          {



            ByteArrayOutputStream bOut = new ByteArrayOutputStream(x.length * 2 + 3);
            bOut.write(120);
            bOut.write(39);
            
            for (int i = 0; i < x.length; i++) {
              int lowBits = (x[i] & 0xFF) / 16;
              int highBits = (x[i] & 0xFF) % 16;
              
              bOut.write(HEX_DIGITS[lowBits]);
              bOut.write(HEX_DIGITS[highBits]);
            }
            
            bOut.write(39);
            
            setInternal(parameterIndex, bOut.toByteArray());
            
            return;
          }
        } catch (SQLException ex) {
          throw ex;
        } catch (RuntimeException ex) {
          SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
          sqlEx.initCause(ex);
          throw sqlEx;
        }
        

        int numBytes = x.length;
        
        int pad = 2;
        
        boolean needsIntroducer = (checkForIntroducer) && (connection.versionMeetsMinimum(4, 1, 0));
        
        if (needsIntroducer) {
          pad += 7;
        }
        
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(numBytes + pad);
        
        if (needsIntroducer) {
          bOut.write(95);
          bOut.write(98);
          bOut.write(105);
          bOut.write(110);
          bOut.write(97);
          bOut.write(114);
          bOut.write(121);
        }
        bOut.write(39);
        
        for (int i = 0; i < numBytes; i++) {
          byte b = x[i];
          
          switch (b) {
          case 0: 
            bOut.write(92);
            bOut.write(48);
            
            break;
          
          case 10: 
            bOut.write(92);
            bOut.write(110);
            
            break;
          
          case 13: 
            bOut.write(92);
            bOut.write(114);
            
            break;
          
          case 92: 
            bOut.write(92);
            bOut.write(92);
            
            break;
          
          case 39: 
            bOut.write(92);
            bOut.write(39);
            
            break;
          
          case 34: 
            bOut.write(92);
            bOut.write(34);
            
            break;
          
          case 26: 
            bOut.write(92);
            bOut.write(90);
            
            break;
          
          default: 
            bOut.write(b);
          }
          
        }
        bOut.write(39);
        
        setInternal(parameterIndex, bOut.toByteArray());
      }
    }
  }
  










  protected void setBytesNoEscape(int parameterIndex, byte[] parameterAsBytes)
    throws SQLException
  {
    byte[] parameterWithQuotes = new byte[parameterAsBytes.length + 2];
    parameterWithQuotes[0] = 39;
    System.arraycopy(parameterAsBytes, 0, parameterWithQuotes, 1, parameterAsBytes.length);
    parameterWithQuotes[(parameterAsBytes.length + 1)] = 39;
    
    setInternal(parameterIndex, parameterWithQuotes);
  }
  
  protected void setBytesNoEscapeNoQuotes(int parameterIndex, byte[] parameterAsBytes) throws SQLException {
    setInternal(parameterIndex, parameterAsBytes);
  }
  



















  public void setCharacterStream(int parameterIndex, Reader reader, int length)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      try {
        if (reader == null) {
          setNull(parameterIndex, -1);
        } else {
          char[] c = null;
          int len = 0;
          
          boolean useLength = connection.getUseStreamLengthsInPrepStmts();
          
          String forcedEncoding = connection.getClobCharacterEncoding();
          
          if ((useLength) && (length != -1)) {
            c = new char[length];
            
            int numCharsRead = readFully(reader, c, length);
            
            if (forcedEncoding == null) {
              setString(parameterIndex, new String(c, 0, numCharsRead));
            } else {
              try {
                setBytes(parameterIndex, StringUtils.getBytes(new String(c, 0, numCharsRead), forcedEncoding));
              } catch (UnsupportedEncodingException uee) {
                throw SQLError.createSQLException("Unsupported character encoding " + forcedEncoding, "S1009", getExceptionInterceptor());
              }
            }
          }
          else {
            c = new char['က'];
            
            StringBuilder buf = new StringBuilder();
            
            while ((len = reader.read(c)) != -1) {
              buf.append(c, 0, len);
            }
            
            if (forcedEncoding == null) {
              setString(parameterIndex, buf.toString());
            } else {
              try {
                setBytes(parameterIndex, StringUtils.getBytes(buf.toString(), forcedEncoding));
              } catch (UnsupportedEncodingException uee) {
                throw SQLError.createSQLException("Unsupported character encoding " + forcedEncoding, "S1009", getExceptionInterceptor());
              }
            }
          }
          

          parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 2005;
        }
      } catch (IOException ioEx) {
        throw SQLError.createSQLException(ioEx.toString(), "S1000", getExceptionInterceptor());
      }
    }
  }
  









  public void setClob(int i, Clob x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (x == null) {
        setNull(i, 2005);
      }
      else {
        String forcedEncoding = connection.getClobCharacterEncoding();
        
        if (forcedEncoding == null) {
          setString(i, x.getSubString(1L, (int)x.length()));
        } else {
          try {
            setBytes(i, StringUtils.getBytes(x.getSubString(1L, (int)x.length()), forcedEncoding));
          } catch (UnsupportedEncodingException uee) {
            throw SQLError.createSQLException("Unsupported character encoding " + forcedEncoding, "S1009", getExceptionInterceptor());
          }
        }
        

        parameterTypes[(i - 1 + getParameterIndexOffset())] = 2005;
      }
    }
  }
  










  public void setDate(int parameterIndex, java.sql.Date x)
    throws SQLException
  {
    setDate(parameterIndex, x, null);
  }
  












  public void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
    throws SQLException
  {
    if (x == null) {
      setNull(parameterIndex, 91);
    }
    else if (!useLegacyDatetimeCode) {
      newSetDateInternal(parameterIndex, x, cal);
    } else {
      synchronized (checkClosed().getConnectionMutex()) {
        if (ddf == null) {
          ddf = new SimpleDateFormat("''yyyy-MM-dd''", Locale.US);
        }
        if (cal != null) {
          ddf.setTimeZone(cal.getTimeZone());
        }
        
        setInternal(parameterIndex, ddf.format(x));
        
        parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 91;
      }
    }
  }
  











  public void setDouble(int parameterIndex, double x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if ((!connection.getAllowNanAndInf()) && ((x == Double.POSITIVE_INFINITY) || (x == Double.NEGATIVE_INFINITY) || (Double.isNaN(x)))) {
        throw SQLError.createSQLException("'" + x + "' is not a valid numeric or approximate numeric value", "S1009", getExceptionInterceptor());
      }
      


      setInternal(parameterIndex, StringUtils.fixDecimalExponent(String.valueOf(x)));
      
      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 8;
    }
  }
  










  public void setFloat(int parameterIndex, float x)
    throws SQLException
  {
    setInternal(parameterIndex, StringUtils.fixDecimalExponent(String.valueOf(x)));
    
    parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 6;
  }
  










  public void setInt(int parameterIndex, int x)
    throws SQLException
  {
    setInternal(parameterIndex, String.valueOf(x));
    
    parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 4;
  }
  
  protected final void setInternal(int paramIndex, byte[] val) throws SQLException {
    synchronized (checkClosed().getConnectionMutex())
    {
      int parameterIndexOffset = getParameterIndexOffset();
      
      checkBounds(paramIndex, parameterIndexOffset);
      
      isStream[(paramIndex - 1 + parameterIndexOffset)] = false;
      isNull[(paramIndex - 1 + parameterIndexOffset)] = false;
      parameterStreams[(paramIndex - 1 + parameterIndexOffset)] = null;
      parameterValues[(paramIndex - 1 + parameterIndexOffset)] = val;
    }
  }
  
  protected void checkBounds(int paramIndex, int parameterIndexOffset) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (paramIndex < 1) {
        throw SQLError.createSQLException(Messages.getString("PreparedStatement.49") + paramIndex + Messages.getString("PreparedStatement.50"), "S1009", getExceptionInterceptor());
      }
      if (paramIndex > parameterCount) {
        throw SQLError.createSQLException(Messages.getString("PreparedStatement.51") + paramIndex + Messages.getString("PreparedStatement.52") + parameterValues.length + Messages.getString("PreparedStatement.53"), "S1009", getExceptionInterceptor());
      }
      


      if ((parameterIndexOffset == -1) && (paramIndex == 1)) {
        throw SQLError.createSQLException("Can't set IN parameter for return value of stored function call.", "S1009", getExceptionInterceptor());
      }
    }
  }
  
  protected final void setInternal(int paramIndex, String val) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      byte[] parameterAsBytes = null;
      
      if (charConverter != null) {
        parameterAsBytes = charConverter.toBytes(val);
      } else {
        parameterAsBytes = StringUtils.getBytes(val, charConverter, charEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor());
      }
      

      setInternal(paramIndex, parameterAsBytes);
    }
  }
  










  public void setLong(int parameterIndex, long x)
    throws SQLException
  {
    setInternal(parameterIndex, String.valueOf(x));
    
    parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = -5;
  }
  













  public void setNull(int parameterIndex, int sqlType)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      setInternal(parameterIndex, "null");
      isNull[(parameterIndex - 1 + getParameterIndexOffset())] = true;
      
      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 0;
    }
  }
  















  public void setNull(int parameterIndex, int sqlType, String arg)
    throws SQLException
  {
    setNull(parameterIndex, sqlType);
    
    parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 0;
  }
  
  private void setNumericObject(int parameterIndex, Object parameterObj, int targetSqlType, int scale) throws SQLException {
    Number parameterAsNum;
    Number parameterAsNum;
    if ((parameterObj instanceof Boolean)) {
      parameterAsNum = ((Boolean)parameterObj).booleanValue() ? Integer.valueOf(1) : Integer.valueOf(0);
    } else if ((parameterObj instanceof String)) { Number parameterAsNum;
      switch (targetSqlType) {
      case -7:  Number parameterAsNum;
        if (("1".equals(parameterObj)) || ("0".equals(parameterObj))) {
          parameterAsNum = Integer.valueOf((String)parameterObj);
        } else {
          boolean parameterAsBoolean = "true".equalsIgnoreCase((String)parameterObj);
          
          parameterAsNum = parameterAsBoolean ? Integer.valueOf(1) : Integer.valueOf(0);
        }
        
        break;
      
      case -6: 
      case 4: 
      case 5: 
        parameterAsNum = Integer.valueOf((String)parameterObj);
        
        break;
      
      case -5: 
        parameterAsNum = Long.valueOf((String)parameterObj);
        
        break;
      
      case 7: 
        parameterAsNum = Float.valueOf((String)parameterObj);
        
        break;
      
      case 6: 
      case 8: 
        parameterAsNum = Double.valueOf((String)parameterObj);
        
        break;
      case -4: case -3: case -2: 
      case -1: case 0: 
      case 1: case 2: 
      case 3: default: 
        parameterAsNum = new BigDecimal((String)parameterObj);break;
      }
    } else {
      parameterAsNum = (Number)parameterObj;
    }
    
    switch (targetSqlType) {
    case -7: 
    case -6: 
    case 4: 
    case 5: 
      setInt(parameterIndex, parameterAsNum.intValue());
      
      break;
    
    case -5: 
      setLong(parameterIndex, parameterAsNum.longValue());
      
      break;
    
    case 7: 
      setFloat(parameterIndex, parameterAsNum.floatValue());
      
      break;
    
    case 6: 
    case 8: 
      setDouble(parameterIndex, parameterAsNum.doubleValue());
      
      break;
    

    case 2: 
    case 3: 
      if ((parameterAsNum instanceof BigDecimal)) {
        BigDecimal scaledBigDecimal = null;
        try
        {
          scaledBigDecimal = ((BigDecimal)parameterAsNum).setScale(scale);
        } catch (ArithmeticException ex) {
          try {
            scaledBigDecimal = ((BigDecimal)parameterAsNum).setScale(scale, 4);
          } catch (ArithmeticException arEx) {
            throw SQLError.createSQLException("Can't set scale of '" + scale + "' for DECIMAL argument '" + parameterAsNum + "'", "S1009", getExceptionInterceptor());
          }
        }
        

        setBigDecimal(parameterIndex, scaledBigDecimal);
      } else if ((parameterAsNum instanceof BigInteger)) {
        setBigDecimal(parameterIndex, new BigDecimal((BigInteger)parameterAsNum, scale));
      } else {
        setBigDecimal(parameterIndex, new BigDecimal(parameterAsNum.doubleValue()));
      }
      break;
    }
  }
  
  public void setObject(int parameterIndex, Object parameterObj) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (parameterObj == null) {
        setNull(parameterIndex, 1111);
      }
      else if ((parameterObj instanceof Byte)) {
        setInt(parameterIndex, ((Byte)parameterObj).intValue());
      } else if ((parameterObj instanceof String)) {
        setString(parameterIndex, (String)parameterObj);
      } else if ((parameterObj instanceof BigDecimal)) {
        setBigDecimal(parameterIndex, (BigDecimal)parameterObj);
      } else if ((parameterObj instanceof Short)) {
        setShort(parameterIndex, ((Short)parameterObj).shortValue());
      } else if ((parameterObj instanceof Integer)) {
        setInt(parameterIndex, ((Integer)parameterObj).intValue());
      } else if ((parameterObj instanceof Long)) {
        setLong(parameterIndex, ((Long)parameterObj).longValue());
      } else if ((parameterObj instanceof Float)) {
        setFloat(parameterIndex, ((Float)parameterObj).floatValue());
      } else if ((parameterObj instanceof Double)) {
        setDouble(parameterIndex, ((Double)parameterObj).doubleValue());
      } else if ((parameterObj instanceof byte[])) {
        setBytes(parameterIndex, (byte[])parameterObj);
      } else if ((parameterObj instanceof java.sql.Date)) {
        setDate(parameterIndex, (java.sql.Date)parameterObj);
      } else if ((parameterObj instanceof Time)) {
        setTime(parameterIndex, (Time)parameterObj);
      } else if ((parameterObj instanceof Timestamp)) {
        setTimestamp(parameterIndex, (Timestamp)parameterObj);
      } else if ((parameterObj instanceof Boolean)) {
        setBoolean(parameterIndex, ((Boolean)parameterObj).booleanValue());
      } else if ((parameterObj instanceof InputStream)) {
        setBinaryStream(parameterIndex, (InputStream)parameterObj, -1);
      } else if ((parameterObj instanceof Blob)) {
        setBlob(parameterIndex, (Blob)parameterObj);
      } else if ((parameterObj instanceof Clob)) {
        setClob(parameterIndex, (Clob)parameterObj);
      } else if ((connection.getTreatUtilDateAsTimestamp()) && ((parameterObj instanceof java.util.Date))) {
        setTimestamp(parameterIndex, new Timestamp(((java.util.Date)parameterObj).getTime()));
      } else if ((parameterObj instanceof BigInteger)) {
        setString(parameterIndex, parameterObj.toString());
      } else {
        setSerializableObject(parameterIndex, parameterObj);
      }
    }
  }
  






  public void setObject(int parameterIndex, Object parameterObj, int targetSqlType)
    throws SQLException
  {
    if (!(parameterObj instanceof BigDecimal)) {
      setObject(parameterIndex, parameterObj, targetSqlType, 0);
    } else {
      setObject(parameterIndex, parameterObj, targetSqlType, ((BigDecimal)parameterObj).scale());
    }
  }
  

























  public void setObject(int parameterIndex, Object parameterObj, int targetSqlType, int scale)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (parameterObj == null) {
        setNull(parameterIndex, 1111);
      }
      else
      {
        try
        {
          switch (targetSqlType)
          {
          case 16: 
            if ((parameterObj instanceof Boolean)) {
              setBoolean(parameterIndex, ((Boolean)parameterObj).booleanValue());

            }
            else if ((parameterObj instanceof String)) {
              setBoolean(parameterIndex, ("true".equalsIgnoreCase((String)parameterObj)) || (!"0".equalsIgnoreCase((String)parameterObj)));

            }
            else if ((parameterObj instanceof Number)) {
              int intValue = ((Number)parameterObj).intValue();
              
              setBoolean(parameterIndex, intValue != 0);
            }
            else
            {
              throw SQLError.createSQLException("No conversion from " + parameterObj.getClass().getName() + " to Types.BOOLEAN possible.", "S1009", getExceptionInterceptor());
            }
            

            break;
          case -7: 
          case -6: 
          case -5: 
          case 2: 
          case 3: 
          case 4: 
          case 5: 
          case 6: 
          case 7: 
          case 8: 
            setNumericObject(parameterIndex, parameterObj, targetSqlType, scale);
            
            break;
          
          case -1: 
          case 1: 
          case 12: 
            if ((parameterObj instanceof BigDecimal)) {
              setString(parameterIndex, StringUtils.fixDecimalExponent(StringUtils.consistentToString((BigDecimal)parameterObj)));
            } else {
              setString(parameterIndex, parameterObj.toString());
            }
            
            break;
          

          case 2005: 
            if ((parameterObj instanceof Clob)) {
              setClob(parameterIndex, (Clob)parameterObj);
            } else {
              setString(parameterIndex, parameterObj.toString());
            }
            
            break;
          

          case -4: 
          case -3: 
          case -2: 
          case 2004: 
            if ((parameterObj instanceof byte[])) {
              setBytes(parameterIndex, (byte[])parameterObj);
            } else if ((parameterObj instanceof Blob)) {
              setBlob(parameterIndex, (Blob)parameterObj);
            } else {
              setBytes(parameterIndex, StringUtils.getBytes(parameterObj.toString(), charConverter, charEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor()));
            }
            

            break;
          case 91: 
          case 93: 
            java.util.Date parameterAsDate;
            
            java.util.Date parameterAsDate;
            
            if ((parameterObj instanceof String)) {
              ParsePosition pp = new ParsePosition(0);
              DateFormat sdf = new SimpleDateFormat(getDateTimePattern((String)parameterObj, false), Locale.US);
              parameterAsDate = sdf.parse((String)parameterObj, pp);
            } else {
              parameterAsDate = (java.util.Date)parameterObj;
            }
            
            switch (targetSqlType)
            {
            case 91: 
              if ((parameterAsDate instanceof java.sql.Date)) {
                setDate(parameterIndex, (java.sql.Date)parameterAsDate);
              } else {
                setDate(parameterIndex, new java.sql.Date(parameterAsDate.getTime()));
              }
              
              break;
            

            case 93: 
              if ((parameterAsDate instanceof Timestamp)) {
                setTimestamp(parameterIndex, (Timestamp)parameterAsDate);
              } else {
                setTimestamp(parameterIndex, new Timestamp(parameterAsDate.getTime()));
              }
              
              break;
            }
            
            break;
          

          case 92: 
            if ((parameterObj instanceof String)) {
              DateFormat sdf = new SimpleDateFormat(getDateTimePattern((String)parameterObj, true), Locale.US);
              setTime(parameterIndex, new Time(sdf.parse((String)parameterObj).getTime()));
            } else if ((parameterObj instanceof Timestamp)) {
              Timestamp xT = (Timestamp)parameterObj;
              setTime(parameterIndex, new Time(xT.getTime()));
            } else {
              setTime(parameterIndex, (Time)parameterObj);
            }
            
            break;
          
          case 1111: 
            setSerializableObject(parameterIndex, parameterObj);
            
            break;
          
          default: 
            throw SQLError.createSQLException(Messages.getString("PreparedStatement.16"), "S1000", getExceptionInterceptor());
          }
        }
        catch (Exception ex) {
          if ((ex instanceof SQLException)) {
            throw ((SQLException)ex);
          }
          
          SQLException sqlEx = SQLError.createSQLException(Messages.getString("PreparedStatement.17") + parameterObj.getClass().toString() + Messages.getString("PreparedStatement.18") + ex.getClass().getName() + Messages.getString("PreparedStatement.19") + ex.getMessage(), "S1000", getExceptionInterceptor());
          



          sqlEx.initCause(ex);
          
          throw sqlEx;
        }
      }
    }
  }
  
  protected int setOneBatchedParameterSet(java.sql.PreparedStatement batchedStatement, int batchedParamIndex, Object paramSet) throws SQLException {
    BatchParams paramArg = (BatchParams)paramSet;
    
    boolean[] isNullBatch = isNull;
    boolean[] isStreamBatch = isStream;
    
    for (int j = 0; j < isNullBatch.length; j++) {
      if (isNullBatch[j] != 0) {
        batchedStatement.setNull(batchedParamIndex++, 0);
      }
      else if (isStreamBatch[j] != 0) {
        batchedStatement.setBinaryStream(batchedParamIndex++, parameterStreams[j], streamLengths[j]);
      } else {
        ((PreparedStatement)batchedStatement).setBytesNoEscapeNoQuotes(batchedParamIndex++, parameterStrings[j]);
      }
    }
    

    return batchedParamIndex;
  }
  










  public void setRef(int i, Ref x)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  






  private final void setSerializableObject(int parameterIndex, Object parameterObj)
    throws SQLException
  {
    try
    {
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      ObjectOutputStream objectOut = new ObjectOutputStream(bytesOut);
      objectOut.writeObject(parameterObj);
      objectOut.flush();
      objectOut.close();
      bytesOut.flush();
      bytesOut.close();
      
      byte[] buf = bytesOut.toByteArray();
      ByteArrayInputStream bytesIn = new ByteArrayInputStream(buf);
      setBinaryStream(parameterIndex, bytesIn, buf.length);
      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = -2;
    } catch (Exception ex) {
      SQLException sqlEx = SQLError.createSQLException(Messages.getString("PreparedStatement.54") + ex.getClass().getName(), "S1009", getExceptionInterceptor());
      
      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
  }
  










  public void setShort(int parameterIndex, short x)
    throws SQLException
  {
    setInternal(parameterIndex, String.valueOf(x));
    
    parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 5;
  }
  











  public void setString(int parameterIndex, String x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (x == null) {
        setNull(parameterIndex, 1);
      } else {
        checkClosed();
        
        int stringLength = x.length();
        
        if (connection.isNoBackslashEscapesSet())
        {

          boolean needsHexEscape = isEscapeNeededForString(x, stringLength);
          
          if (!needsHexEscape) {
            byte[] parameterAsBytes = null;
            
            StringBuilder quotedString = new StringBuilder(x.length() + 2);
            quotedString.append('\'');
            quotedString.append(x);
            quotedString.append('\'');
            
            if (!isLoadDataQuery) {
              parameterAsBytes = StringUtils.getBytes(quotedString.toString(), charConverter, charEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor());
            }
            else
            {
              parameterAsBytes = StringUtils.getBytes(quotedString.toString());
            }
            
            setInternal(parameterIndex, parameterAsBytes);
          } else {
            byte[] parameterAsBytes = null;
            
            if (!isLoadDataQuery) {
              parameterAsBytes = StringUtils.getBytes(x, charConverter, charEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor());
            }
            else
            {
              parameterAsBytes = StringUtils.getBytes(x);
            }
            
            setBytes(parameterIndex, parameterAsBytes);
          }
          
          return;
        }
        
        String parameterAsString = x;
        boolean needsQuoted = true;
        
        if ((isLoadDataQuery) || (isEscapeNeededForString(x, stringLength))) {
          needsQuoted = false;
          
          StringBuilder buf = new StringBuilder((int)(x.length() * 1.1D));
          
          buf.append('\'');
          




          for (int i = 0; i < stringLength; i++) {
            char c = x.charAt(i);
            
            switch (c) {
            case '\000': 
              buf.append('\\');
              buf.append('0');
              
              break;
            
            case '\n': 
              buf.append('\\');
              buf.append('n');
              
              break;
            
            case '\r': 
              buf.append('\\');
              buf.append('r');
              
              break;
            
            case '\\': 
              buf.append('\\');
              buf.append('\\');
              
              break;
            
            case '\'': 
              buf.append('\\');
              buf.append('\'');
              
              break;
            
            case '"': 
              if (usingAnsiMode) {
                buf.append('\\');
              }
              
              buf.append('"');
              
              break;
            
            case '\032': 
              buf.append('\\');
              buf.append('Z');
              
              break;
            

            case '¥': 
            case '₩': 
              if (charsetEncoder != null) {
                CharBuffer cbuf = CharBuffer.allocate(1);
                ByteBuffer bbuf = ByteBuffer.allocate(1);
                cbuf.put(c);
                cbuf.position(0);
                charsetEncoder.encode(cbuf, bbuf, true);
                if (bbuf.get(0) == 92) {
                  buf.append('\\');
                }
              }
              buf.append(c);
              break;
            
            default: 
              buf.append(c);
            }
            
          }
          buf.append('\'');
          
          parameterAsString = buf.toString();
        }
        
        byte[] parameterAsBytes = null;
        
        if (!isLoadDataQuery) {
          if (needsQuoted) {
            parameterAsBytes = StringUtils.getBytesWrapped(parameterAsString, '\'', '\'', charConverter, charEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor());
          }
          else {
            parameterAsBytes = StringUtils.getBytes(parameterAsString, charConverter, charEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor());
          }
          
        }
        else {
          parameterAsBytes = StringUtils.getBytes(parameterAsString);
        }
        
        setInternal(parameterIndex, parameterAsBytes);
        
        parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 12;
      }
    }
  }
  
  private boolean isEscapeNeededForString(String x, int stringLength) {
    boolean needsHexEscape = false;
    
    for (int i = 0; i < stringLength; i++) {
      char c = x.charAt(i);
      
      switch (c)
      {
      case '\000': 
        needsHexEscape = true;
        break;
      
      case '\n': 
        needsHexEscape = true;
        
        break;
      
      case '\r': 
        needsHexEscape = true;
        break;
      
      case '\\': 
        needsHexEscape = true;
        
        break;
      
      case '\'': 
        needsHexEscape = true;
        
        break;
      
      case '"': 
        needsHexEscape = true;
        
        break;
      
      case '\032': 
        needsHexEscape = true;
      }
      
      
      if (needsHexEscape) {
        break;
      }
    }
    return needsHexEscape;
  }
  












  public void setTime(int parameterIndex, Time x, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      setTimeInternal(parameterIndex, x, cal, cal.getTimeZone(), true);
    }
  }
  










  public void setTime(int parameterIndex, Time x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      setTimeInternal(parameterIndex, x, null, connection.getDefaultTimeZone(), false);
    }
  }
  













  private void setTimeInternal(int parameterIndex, Time x, Calendar targetCalendar, TimeZone tz, boolean rollForward)
    throws SQLException
  {
    if (x == null) {
      setNull(parameterIndex, 92);
    } else {
      checkClosed();
      
      if (!useLegacyDatetimeCode) {
        newSetTimeInternal(parameterIndex, x, targetCalendar);
      } else {
        Calendar sessionCalendar = getCalendarInstanceForSessionOrNew();
        
        x = TimeUtil.changeTimezone(connection, sessionCalendar, targetCalendar, x, tz, connection.getServerTimezoneTZ(), rollForward);
        
        setInternal(parameterIndex, "'" + x.toString() + "'");
      }
      
      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 92;
    }
  }
  












  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      setTimestampInternal(parameterIndex, x, cal, cal.getTimeZone(), true);
    }
  }
  










  public void setTimestamp(int parameterIndex, Timestamp x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      setTimestampInternal(parameterIndex, x, null, connection.getDefaultTimeZone(), false);
    }
  }
  












  private void setTimestampInternal(int parameterIndex, Timestamp x, Calendar targetCalendar, TimeZone tz, boolean rollForward)
    throws SQLException
  {
    if (x == null) {
      setNull(parameterIndex, 93);
    } else {
      checkClosed();
      
      if (!sendFractionalSeconds) {
        x = TimeUtil.truncateFractionalSeconds(x);
      }
      
      if (!useLegacyDatetimeCode) {
        newSetTimestampInternal(parameterIndex, x, targetCalendar);
      } else {
        Calendar sessionCalendar = connection.getUseJDBCCompliantTimezoneShift() ? connection.getUtcCalendar() : getCalendarInstanceForSessionOrNew();
        

        x = TimeUtil.changeTimezone(connection, sessionCalendar, targetCalendar, x, tz, connection.getServerTimezoneTZ(), rollForward);
        
        if (connection.getUseSSPSCompatibleTimezoneShift()) {
          doSSPSCompatibleTimezoneShift(parameterIndex, x);
        } else {
          synchronized (this) {
            if (tsdf == null) {
              tsdf = new SimpleDateFormat("''yyyy-MM-dd HH:mm:ss", Locale.US);
            }
            
            StringBuffer buf = new StringBuffer();
            buf.append(tsdf.format(x));
            
            if (serverSupportsFracSecs) {
              int nanos = x.getNanos();
              
              if (nanos != 0) {
                buf.append('.');
                buf.append(TimeUtil.formatNanos(nanos, serverSupportsFracSecs, true));
              }
            }
            
            buf.append('\'');
            
            setInternal(parameterIndex, buf.toString());
          }
        }
      }
      

      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 93;
    }
  }
  
  private void newSetTimestampInternal(int parameterIndex, Timestamp x, Calendar targetCalendar) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (tsdf == null) {
        tsdf = new SimpleDateFormat("''yyyy-MM-dd HH:mm:ss", Locale.US);
      }
      
      if (targetCalendar != null) {
        tsdf.setTimeZone(targetCalendar.getTimeZone());
      } else {
        tsdf.setTimeZone(connection.getServerTimezoneTZ());
      }
      
      StringBuffer buf = new StringBuffer();
      buf.append(tsdf.format(x));
      buf.append('.');
      buf.append(TimeUtil.formatNanos(x.getNanos(), serverSupportsFracSecs, true));
      buf.append('\'');
      
      setInternal(parameterIndex, buf.toString());
    }
  }
  
  private void newSetTimeInternal(int parameterIndex, Time x, Calendar targetCalendar) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (tdf == null) {
        tdf = new SimpleDateFormat("''HH:mm:ss''", Locale.US);
      }
      
      if (targetCalendar != null) {
        tdf.setTimeZone(targetCalendar.getTimeZone());
      } else {
        tdf.setTimeZone(connection.getServerTimezoneTZ());
      }
      
      setInternal(parameterIndex, tdf.format(x));
    }
  }
  
  private void newSetDateInternal(int parameterIndex, java.sql.Date x, Calendar targetCalendar) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (ddf == null) {
        ddf = new SimpleDateFormat("''yyyy-MM-dd''", Locale.US);
      }
      
      if (targetCalendar != null) {
        ddf.setTimeZone(targetCalendar.getTimeZone());
      } else if (connection.getNoTimezoneConversionForDateType()) {
        ddf.setTimeZone(connection.getDefaultTimeZone());
      } else {
        ddf.setTimeZone(connection.getServerTimezoneTZ());
      }
      
      setInternal(parameterIndex, ddf.format(x));
    }
  }
  
  private void doSSPSCompatibleTimezoneShift(int parameterIndex, Timestamp x) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      Calendar sessionCalendar2 = connection.getUseJDBCCompliantTimezoneShift() ? connection.getUtcCalendar() : getCalendarInstanceForSessionOrNew();
      

      synchronized (sessionCalendar2) {
        java.util.Date oldTime = sessionCalendar2.getTime();
        try
        {
          sessionCalendar2.setTime(x);
          
          int year = sessionCalendar2.get(1);
          int month = sessionCalendar2.get(2) + 1;
          int date = sessionCalendar2.get(5);
          
          int hour = sessionCalendar2.get(11);
          int minute = sessionCalendar2.get(12);
          int seconds = sessionCalendar2.get(13);
          
          StringBuilder tsBuf = new StringBuilder();
          
          tsBuf.append('\'');
          tsBuf.append(year);
          
          tsBuf.append("-");
          
          if (month < 10) {
            tsBuf.append('0');
          }
          
          tsBuf.append(month);
          
          tsBuf.append('-');
          
          if (date < 10) {
            tsBuf.append('0');
          }
          
          tsBuf.append(date);
          
          tsBuf.append(' ');
          
          if (hour < 10) {
            tsBuf.append('0');
          }
          
          tsBuf.append(hour);
          
          tsBuf.append(':');
          
          if (minute < 10) {
            tsBuf.append('0');
          }
          
          tsBuf.append(minute);
          
          tsBuf.append(':');
          
          if (seconds < 10) {
            tsBuf.append('0');
          }
          
          tsBuf.append(seconds);
          
          tsBuf.append('.');
          tsBuf.append(TimeUtil.formatNanos(x.getNanos(), serverSupportsFracSecs, true));
          tsBuf.append('\'');
          
          setInternal(parameterIndex, tsBuf.toString());
        }
        finally {
          sessionCalendar2.setTime(oldTime);
        }
      }
    }
  }
  





















  @Deprecated
  public void setUnicodeStream(int parameterIndex, InputStream x, int length)
    throws SQLException
  {
    if (x == null) {
      setNull(parameterIndex, 12);
    } else {
      setBinaryStream(parameterIndex, x, length);
      
      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 2005;
    }
  }
  

  public void setURL(int parameterIndex, URL arg)
    throws SQLException
  {
    if (arg != null) {
      setString(parameterIndex, arg.toString());
      
      parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 70;
    } else {
      setNull(parameterIndex, 1);
    }
  }
  
  private final void streamToBytes(Buffer packet, InputStream in, boolean escape, int streamLength, boolean useLength) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      try {
        if (streamConvertBuf == null) {
          streamConvertBuf = new byte['က'];
        }
        
        String connectionEncoding = connection.getEncoding();
        
        boolean hexEscape = false;
        try
        {
          if ((connection.isNoBackslashEscapesSet()) || ((connection.getUseUnicode()) && (connectionEncoding != null) && (CharsetMapping.isMultibyteCharset(connectionEncoding)) && (!connection.parserKnowsUnicode())))
          {
            hexEscape = true;
          }
        } catch (RuntimeException ex) {
          SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
          sqlEx.initCause(ex);
          throw sqlEx;
        }
        
        if (streamLength == -1) {
          useLength = false;
        }
        
        int bc = -1;
        
        if (useLength) {
          bc = readblock(in, streamConvertBuf, streamLength);
        } else {
          bc = readblock(in, streamConvertBuf);
        }
        
        int lengthLeftToRead = streamLength - bc;
        
        if (hexEscape) {
          packet.writeStringNoNull("x");
        } else if (connection.getIO().versionMeetsMinimum(4, 1, 0)) {
          packet.writeStringNoNull("_binary");
        }
        
        if (escape) {
          packet.writeByte((byte)39);
        }
        
        while (bc > 0) {
          if (hexEscape) {
            hexEscapeBlock(streamConvertBuf, packet, bc);
          } else if (escape) {
            escapeblockFast(streamConvertBuf, packet, bc);
          } else {
            packet.writeBytesNoNull(streamConvertBuf, 0, bc);
          }
          
          if (useLength) {
            bc = readblock(in, streamConvertBuf, lengthLeftToRead);
            
            if (bc > 0) {
              lengthLeftToRead -= bc;
            }
          } else {
            bc = readblock(in, streamConvertBuf);
          }
        }
        
        if (escape) {
          packet.writeByte((byte)39);
        }
      } finally {
        if (connection.getAutoClosePStmtStreams()) {
          try {
            in.close();
          }
          catch (IOException ioEx) {}
          
          in = null;
        }
      }
    }
  }
  
  private final byte[] streamToBytes(InputStream in, boolean escape, int streamLength, boolean useLength) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      in.mark(Integer.MAX_VALUE);
      try {
        if (streamConvertBuf == null) {
          streamConvertBuf = new byte['က'];
        }
        if (streamLength == -1) {
          useLength = false;
        }
        
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        
        int bc = -1;
        
        if (useLength) {
          bc = readblock(in, streamConvertBuf, streamLength);
        } else {
          bc = readblock(in, streamConvertBuf);
        }
        
        int lengthLeftToRead = streamLength - bc;
        
        if (escape) {
          if (connection.versionMeetsMinimum(4, 1, 0)) {
            bytesOut.write(95);
            bytesOut.write(98);
            bytesOut.write(105);
            bytesOut.write(110);
            bytesOut.write(97);
            bytesOut.write(114);
            bytesOut.write(121);
          }
          
          bytesOut.write(39);
        }
        
        while (bc > 0) {
          if (escape) {
            escapeblockFast(streamConvertBuf, bytesOut, bc);
          } else {
            bytesOut.write(streamConvertBuf, 0, bc);
          }
          
          if (useLength) {
            bc = readblock(in, streamConvertBuf, lengthLeftToRead);
            
            if (bc > 0) {
              lengthLeftToRead -= bc;
            }
          } else {
            bc = readblock(in, streamConvertBuf);
          }
        }
        
        if (escape) {
          bytesOut.write(39);
        }
        
        byte[] arrayOfByte = bytesOut.toByteArray();jsr 17;return arrayOfByte;
      } finally {
        jsr 6; } localObject2 = returnAddress;
      try { in.reset();
      }
      catch (IOException e) {}
      if (connection.getAutoClosePStmtStreams()) {
        try {
          in.close();
        }
        catch (IOException ioEx) {}
        
        in = null; } ret;
    }
  }
  







  public String toString()
  {
    StringBuilder buf = new StringBuilder();
    buf.append(super.toString());
    buf.append(": ");
    try
    {
      buf.append(asSql());
    } catch (SQLException sqlEx) {
      buf.append("EXCEPTION: " + sqlEx.toString());
    }
    
    return buf.toString();
  }
  





  protected int getParameterIndexOffset()
  {
    return 0;
  }
  
  public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
    setAsciiStream(parameterIndex, x, -1);
  }
  
  public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
    setAsciiStream(parameterIndex, x, (int)length);
    parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 2005;
  }
  
  public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
    setBinaryStream(parameterIndex, x, -1);
  }
  
  public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    setBinaryStream(parameterIndex, x, (int)length);
  }
  
  public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    setBinaryStream(parameterIndex, inputStream);
  }
  
  public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    setCharacterStream(parameterIndex, reader, -1);
  }
  
  public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
    setCharacterStream(parameterIndex, reader, (int)length);
  }
  
  public void setClob(int parameterIndex, Reader reader) throws SQLException
  {
    setCharacterStream(parameterIndex, reader);
  }
  
  public void setClob(int parameterIndex, Reader reader, long length) throws SQLException
  {
    setCharacterStream(parameterIndex, reader, length);
  }
  
  public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    setNCharacterStream(parameterIndex, value, -1L);
  }
  












  public void setNString(int parameterIndex, String x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if ((charEncoding.equalsIgnoreCase("UTF-8")) || (charEncoding.equalsIgnoreCase("utf8"))) {
        setString(parameterIndex, x);
        return;
      }
      

      if (x == null) {
        setNull(parameterIndex, 1);
      } else {
        int stringLength = x.length();
        


        StringBuilder buf = new StringBuilder((int)(x.length() * 1.1D + 4.0D));
        buf.append("_utf8");
        buf.append('\'');
        




        for (int i = 0; i < stringLength; i++) {
          char c = x.charAt(i);
          
          switch (c) {
          case '\000': 
            buf.append('\\');
            buf.append('0');
            
            break;
          
          case '\n': 
            buf.append('\\');
            buf.append('n');
            
            break;
          
          case '\r': 
            buf.append('\\');
            buf.append('r');
            
            break;
          
          case '\\': 
            buf.append('\\');
            buf.append('\\');
            
            break;
          
          case '\'': 
            buf.append('\\');
            buf.append('\'');
            
            break;
          
          case '"': 
            if (usingAnsiMode) {
              buf.append('\\');
            }
            
            buf.append('"');
            
            break;
          
          case '\032': 
            buf.append('\\');
            buf.append('Z');
            
            break;
          
          default: 
            buf.append(c);
          }
          
        }
        buf.append('\'');
        
        String parameterAsString = buf.toString();
        
        byte[] parameterAsBytes = null;
        
        if (!isLoadDataQuery) {
          parameterAsBytes = StringUtils.getBytes(parameterAsString, connection.getCharsetConverter("UTF-8"), "UTF-8", connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor());
        }
        else
        {
          parameterAsBytes = StringUtils.getBytes(parameterAsString);
        }
        
        setInternal(parameterIndex, parameterAsBytes);
        
        parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = -9;
      }
    }
  }
  



















  public void setNCharacterStream(int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      try {
        if (reader == null) {
          setNull(parameterIndex, -1);
        }
        else {
          char[] c = null;
          int len = 0;
          
          boolean useLength = connection.getUseStreamLengthsInPrepStmts();
          


          if ((useLength) && (length != -1L)) {
            c = new char[(int)length];
            
            int numCharsRead = readFully(reader, c, (int)length);
            setNString(parameterIndex, new String(c, 0, numCharsRead));
          }
          else {
            c = new char['က'];
            
            StringBuilder buf = new StringBuilder();
            
            while ((len = reader.read(c)) != -1) {
              buf.append(c, 0, len);
            }
            
            setNString(parameterIndex, buf.toString());
          }
          
          parameterTypes[(parameterIndex - 1 + getParameterIndexOffset())] = 2011;
        }
      } catch (IOException ioEx) {
        throw SQLError.createSQLException(ioEx.toString(), "S1000", getExceptionInterceptor());
      }
    }
  }
  
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    setNCharacterStream(parameterIndex, reader);
  }
  











  public void setNClob(int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    if (reader == null) {
      setNull(parameterIndex, -1);
    } else {
      setNCharacterStream(parameterIndex, reader, length);
    }
  }
  
  public ParameterBindings getParameterBindings() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      return new EmulatedPreparedStatementBindings();
    }
  }
  
  class EmulatedPreparedStatementBindings implements ParameterBindings
  {
    private ResultSetImpl bindingsAsRs;
    private boolean[] parameterIsNull;
    
    EmulatedPreparedStatementBindings() throws SQLException {
      List<ResultSetRow> rows = new ArrayList();
      parameterIsNull = new boolean[parameterCount];
      System.arraycopy(isNull, 0, parameterIsNull, 0, parameterCount);
      byte[][] rowData = new byte[parameterCount][];
      Field[] typeMetadata = new Field[parameterCount];
      
      for (int i = 0; i < parameterCount; i++) {
        if (batchCommandIndex == -1) {
          rowData[i] = getBytesRepresentation(i);
        } else {
          rowData[i] = getBytesRepresentationForBatch(i, batchCommandIndex);
        }
        
        int charsetIndex = 0;
        
        if ((parameterTypes[i] == -2) || (parameterTypes[i] == 2004)) {
          charsetIndex = 63;
        } else {
          try {
            charsetIndex = CharsetMapping.getCollationIndexForJavaEncoding(connection.getEncoding(), connection);
          }
          catch (SQLException ex) {
            throw ex;
          } catch (RuntimeException ex) {
            SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
            sqlEx.initCause(ex);
            throw sqlEx;
          }
        }
        
        Field parameterMetadata = new Field(null, "parameter_" + (i + 1), charsetIndex, parameterTypes[i], rowData[i].length);
        parameterMetadata.setConnection(connection);
        typeMetadata[i] = parameterMetadata;
      }
      
      rows.add(new ByteArrayRow(rowData, getExceptionInterceptor()));
      
      bindingsAsRs = new ResultSetImpl(connection.getCatalog(), typeMetadata, new RowDataStatic(rows), connection, null);
      
      bindingsAsRs.next();
    }
    
    public Array getArray(int parameterIndex) throws SQLException {
      return bindingsAsRs.getArray(parameterIndex);
    }
    
    public InputStream getAsciiStream(int parameterIndex) throws SQLException {
      return bindingsAsRs.getAsciiStream(parameterIndex);
    }
    
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
      return bindingsAsRs.getBigDecimal(parameterIndex);
    }
    
    public InputStream getBinaryStream(int parameterIndex) throws SQLException {
      return bindingsAsRs.getBinaryStream(parameterIndex);
    }
    
    public Blob getBlob(int parameterIndex) throws SQLException {
      return bindingsAsRs.getBlob(parameterIndex);
    }
    
    public boolean getBoolean(int parameterIndex) throws SQLException {
      return bindingsAsRs.getBoolean(parameterIndex);
    }
    
    public byte getByte(int parameterIndex) throws SQLException {
      return bindingsAsRs.getByte(parameterIndex);
    }
    
    public byte[] getBytes(int parameterIndex) throws SQLException {
      return bindingsAsRs.getBytes(parameterIndex);
    }
    
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
      return bindingsAsRs.getCharacterStream(parameterIndex);
    }
    
    public Clob getClob(int parameterIndex) throws SQLException {
      return bindingsAsRs.getClob(parameterIndex);
    }
    
    public java.sql.Date getDate(int parameterIndex) throws SQLException {
      return bindingsAsRs.getDate(parameterIndex);
    }
    
    public double getDouble(int parameterIndex) throws SQLException {
      return bindingsAsRs.getDouble(parameterIndex);
    }
    
    public float getFloat(int parameterIndex) throws SQLException {
      return bindingsAsRs.getFloat(parameterIndex);
    }
    
    public int getInt(int parameterIndex) throws SQLException {
      return bindingsAsRs.getInt(parameterIndex);
    }
    
    public long getLong(int parameterIndex) throws SQLException {
      return bindingsAsRs.getLong(parameterIndex);
    }
    
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
      return bindingsAsRs.getCharacterStream(parameterIndex);
    }
    
    public Reader getNClob(int parameterIndex) throws SQLException {
      return bindingsAsRs.getCharacterStream(parameterIndex);
    }
    
    public Object getObject(int parameterIndex) throws SQLException {
      checkBounds(parameterIndex, 0);
      
      if (parameterIsNull[(parameterIndex - 1)] != 0) {
        return null;
      }
      


      switch (parameterTypes[(parameterIndex - 1)]) {
      case -6: 
        return Byte.valueOf(getByte(parameterIndex));
      case 5: 
        return Short.valueOf(getShort(parameterIndex));
      case 4: 
        return Integer.valueOf(getInt(parameterIndex));
      case -5: 
        return Long.valueOf(getLong(parameterIndex));
      case 6: 
        return Float.valueOf(getFloat(parameterIndex));
      case 8: 
        return Double.valueOf(getDouble(parameterIndex));
      }
      return bindingsAsRs.getObject(parameterIndex);
    }
    
    public Ref getRef(int parameterIndex) throws SQLException
    {
      return bindingsAsRs.getRef(parameterIndex);
    }
    
    public short getShort(int parameterIndex) throws SQLException {
      return bindingsAsRs.getShort(parameterIndex);
    }
    
    public String getString(int parameterIndex) throws SQLException {
      return bindingsAsRs.getString(parameterIndex);
    }
    
    public Time getTime(int parameterIndex) throws SQLException {
      return bindingsAsRs.getTime(parameterIndex);
    }
    
    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
      return bindingsAsRs.getTimestamp(parameterIndex);
    }
    
    public URL getURL(int parameterIndex) throws SQLException {
      return bindingsAsRs.getURL(parameterIndex);
    }
    
    public boolean isNull(int parameterIndex) throws SQLException {
      checkBounds(parameterIndex, 0);
      
      return parameterIsNull[(parameterIndex - 1)];
    }
  }
  

















  public int getUpdateCount()
    throws SQLException
  {
    int count = super.getUpdateCount();
    
    if ((containsOnDuplicateKeyUpdateInSQL()) && (compensateForOnDuplicateKeyUpdate) && (
      (count == 2) || (count == 0))) {
      count = 1;
    }
    

    return count;
  }
  


  protected static boolean canRewrite(String sql, boolean isOnDuplicateKeyUpdate, int locationOfOnDuplicateKeyUpdate, int statementStartPos)
  {
    if (StringUtils.startsWithIgnoreCaseAndWs(sql, "INSERT", statementStartPos)) {
      if (StringUtils.indexOfIgnoreCase(statementStartPos, sql, "SELECT", "\"'`", "\"'`", StringUtils.SEARCH_MODE__MRK_COM_WS) != -1) {
        return false;
      }
      if (isOnDuplicateKeyUpdate) {
        int updateClausePos = StringUtils.indexOfIgnoreCase(locationOfOnDuplicateKeyUpdate, sql, " UPDATE ");
        if (updateClausePos != -1) {
          return StringUtils.indexOfIgnoreCase(updateClausePos, sql, "LAST_INSERT_ID", "\"'`", "\"'`", StringUtils.SEARCH_MODE__MRK_COM_WS) == -1;
        }
      }
      return true;
    }
    
    return (StringUtils.startsWithIgnoreCaseAndWs(sql, "REPLACE", statementStartPos)) && (StringUtils.indexOfIgnoreCase(statementStartPos, sql, "SELECT", "\"'`", "\"'`", StringUtils.SEARCH_MODE__MRK_COM_WS) == -1);
  }
  



  public long executeLargeUpdate()
    throws SQLException
  {
    return executeUpdateInternal(true, false);
  }
  
  /* Error */
  protected long[] executePreparedBatchAsMultiStatement(int batchTimeout)
    throws SQLException
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 62	com/mysql/jdbc/PreparedStatement:checkClosed	()Lcom/mysql/jdbc/MySQLConnection;
    //   4: invokeinterface 63 1 0
    //   9: dup
    //   10: astore_2
    //   11: monitorenter
    //   12: aload_0
    //   13: getfield 151	com/mysql/jdbc/PreparedStatement:batchedValuesClause	Ljava/lang/String;
    //   16: ifnonnull +29 -> 45
    //   19: aload_0
    //   20: new 73	java/lang/StringBuilder
    //   23: dup
    //   24: invokespecial 74	java/lang/StringBuilder:<init>	()V
    //   27: aload_0
    //   28: getfield 21	com/mysql/jdbc/PreparedStatement:originalSql	Ljava/lang/String;
    //   31: invokevirtual 79	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   34: ldc -104
    //   36: invokevirtual 79	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   39: invokevirtual 94	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   42: putfield 151	com/mysql/jdbc/PreparedStatement:batchedValuesClause	Ljava/lang/String;
    //   45: aload_0
    //   46: getfield 35	com/mysql/jdbc/PreparedStatement:connection	Lcom/mysql/jdbc/MySQLConnection;
    //   49: astore_3
    //   50: aload_3
    //   51: invokeinterface 153 1 0
    //   56: istore 4
    //   58: aconst_null
    //   59: astore 5
    //   61: aload_0
    //   62: invokevirtual 111	com/mysql/jdbc/PreparedStatement:clearWarnings	()V
    //   65: aload_0
    //   66: getfield 64	com/mysql/jdbc/PreparedStatement:batchedArgs	Ljava/util/List;
    //   69: invokeinterface 138 1 0
    //   74: istore 6
    //   76: aload_0
    //   77: getfield 106	com/mysql/jdbc/PreparedStatement:retrieveGeneratedKeys	Z
    //   80: ifeq +16 -> 96
    //   83: aload_0
    //   84: new 65	java/util/ArrayList
    //   87: dup
    //   88: iload 6
    //   90: invokespecial 154	java/util/ArrayList:<init>	(I)V
    //   93: putfield 108	com/mysql/jdbc/PreparedStatement:batchedGeneratedKeys	Ljava/util/ArrayList;
    //   96: aload_0
    //   97: iload 6
    //   99: invokevirtual 155	com/mysql/jdbc/PreparedStatement:computeBatchSize	(I)I
    //   102: istore 7
    //   104: iload 6
    //   106: iload 7
    //   108: if_icmpge +7 -> 115
    //   111: iload 6
    //   113: istore 7
    //   115: aconst_null
    //   116: astore 8
    //   118: iconst_1
    //   119: istore 9
    //   121: iconst_0
    //   122: istore 10
    //   124: iconst_0
    //   125: istore 11
    //   127: iconst_0
    //   128: istore 12
    //   130: iload 6
    //   132: newarray long
    //   134: astore 13
    //   136: aconst_null
    //   137: astore 14
    //   139: iload 4
    //   141: ifne +12 -> 153
    //   144: aload_3
    //   145: invokeinterface 156 1 0
    //   150: invokevirtual 157	com/mysql/jdbc/MysqlIO:enableMultiQueries	()V
    //   153: aload_0
    //   154: getfield 106	com/mysql/jdbc/PreparedStatement:retrieveGeneratedKeys	Z
    //   157: ifeq +35 -> 192
    //   160: aload_3
    //   161: aload_0
    //   162: iload 7
    //   164: invokespecial 158	com/mysql/jdbc/PreparedStatement:generateMultiStatementForBatch	(I)Ljava/lang/String;
    //   167: iconst_1
    //   168: invokeinterface 159 3 0
    //   173: checkcast 160	com/mysql/jdbc/Wrapper
    //   176: ldc_w 161
    //   179: invokeinterface 162 2 0
    //   184: checkcast 161	java/sql/PreparedStatement
    //   187: astore 8
    //   189: goto +31 -> 220
    //   192: aload_3
    //   193: aload_0
    //   194: iload 7
    //   196: invokespecial 158	com/mysql/jdbc/PreparedStatement:generateMultiStatementForBatch	(I)Ljava/lang/String;
    //   199: invokeinterface 163 2 0
    //   204: checkcast 160	com/mysql/jdbc/Wrapper
    //   207: ldc_w 161
    //   210: invokeinterface 162 2 0
    //   215: checkcast 161	java/sql/PreparedStatement
    //   218: astore 8
    //   220: aload_3
    //   221: invokeinterface 164 1 0
    //   226: ifeq +47 -> 273
    //   229: iload_1
    //   230: ifeq +43 -> 273
    //   233: aload_3
    //   234: iconst_5
    //   235: iconst_0
    //   236: iconst_0
    //   237: invokeinterface 37 4 0
    //   242: ifeq +31 -> 273
    //   245: new 165	com/mysql/jdbc/StatementImpl$CancelTask
    //   248: dup
    //   249: aload_0
    //   250: aload 8
    //   252: checkcast 166	com/mysql/jdbc/StatementImpl
    //   255: invokespecial 167	com/mysql/jdbc/StatementImpl$CancelTask:<init>	(Lcom/mysql/jdbc/StatementImpl;Lcom/mysql/jdbc/StatementImpl;)V
    //   258: astore 5
    //   260: aload_3
    //   261: invokeinterface 168 1 0
    //   266: aload 5
    //   268: iload_1
    //   269: i2l
    //   270: invokevirtual 169	java/util/Timer:schedule	(Ljava/util/TimerTask;J)V
    //   273: iload 6
    //   275: iload 7
    //   277: if_icmpge +10 -> 287
    //   280: iload 6
    //   282: istore 10
    //   284: goto +10 -> 294
    //   287: iload 6
    //   289: iload 7
    //   291: idiv
    //   292: istore 10
    //   294: iload 10
    //   296: iload 7
    //   298: imul
    //   299: istore 15
    //   301: iconst_0
    //   302: istore 16
    //   304: iload 16
    //   306: iload 15
    //   308: if_icmpge +98 -> 406
    //   311: iload 16
    //   313: ifeq +63 -> 376
    //   316: iload 16
    //   318: iload 7
    //   320: irem
    //   321: ifne +55 -> 376
    //   324: aload 8
    //   326: invokeinterface 170 1 0
    //   331: pop
    //   332: goto +19 -> 351
    //   335: astore 17
    //   337: aload_0
    //   338: iload 11
    //   340: iload 7
    //   342: aload 13
    //   344: aload 17
    //   346: invokevirtual 171	com/mysql/jdbc/PreparedStatement:handleExceptionForBatch	(II[JLjava/sql/SQLException;)Ljava/sql/SQLException;
    //   349: astore 14
    //   351: aload_0
    //   352: aload 8
    //   354: checkcast 166	com/mysql/jdbc/StatementImpl
    //   357: iload 12
    //   359: aload 13
    //   361: invokevirtual 172	com/mysql/jdbc/PreparedStatement:processMultiCountsAndKeys	(Lcom/mysql/jdbc/StatementImpl;I[J)I
    //   364: istore 12
    //   366: aload 8
    //   368: invokeinterface 173 1 0
    //   373: iconst_1
    //   374: istore 9
    //   376: aload_0
    //   377: aload 8
    //   379: iload 9
    //   381: aload_0
    //   382: getfield 64	com/mysql/jdbc/PreparedStatement:batchedArgs	Ljava/util/List;
    //   385: iload 11
    //   387: iinc 11 1
    //   390: invokeinterface 77 2 0
    //   395: invokevirtual 174	com/mysql/jdbc/PreparedStatement:setOneBatchedParameterSet	(Ljava/sql/PreparedStatement;ILjava/lang/Object;)I
    //   398: istore 9
    //   400: iinc 16 1
    //   403: goto -99 -> 304
    //   406: aload 8
    //   408: invokeinterface 170 1 0
    //   413: pop
    //   414: goto +21 -> 435
    //   417: astore 16
    //   419: aload_0
    //   420: iload 11
    //   422: iconst_1
    //   423: isub
    //   424: iload 7
    //   426: aload 13
    //   428: aload 16
    //   430: invokevirtual 171	com/mysql/jdbc/PreparedStatement:handleExceptionForBatch	(II[JLjava/sql/SQLException;)Ljava/sql/SQLException;
    //   433: astore 14
    //   435: aload_0
    //   436: aload 8
    //   438: checkcast 166	com/mysql/jdbc/StatementImpl
    //   441: iload 12
    //   443: aload 13
    //   445: invokevirtual 172	com/mysql/jdbc/PreparedStatement:processMultiCountsAndKeys	(Lcom/mysql/jdbc/StatementImpl;I[J)I
    //   448: istore 12
    //   450: aload 8
    //   452: invokeinterface 173 1 0
    //   457: iload 6
    //   459: iload 11
    //   461: isub
    //   462: istore 7
    //   464: jsr +14 -> 478
    //   467: goto +30 -> 497
    //   470: astore 18
    //   472: jsr +6 -> 478
    //   475: aload 18
    //   477: athrow
    //   478: astore 19
    //   480: aload 8
    //   482: ifnull +13 -> 495
    //   485: aload 8
    //   487: invokeinterface 175 1 0
    //   492: aconst_null
    //   493: astore 8
    //   495: ret 19
    //   497: iload 7
    //   499: ifle +145 -> 644
    //   502: aload_0
    //   503: getfield 106	com/mysql/jdbc/PreparedStatement:retrieveGeneratedKeys	Z
    //   506: ifeq +21 -> 527
    //   509: aload_3
    //   510: aload_0
    //   511: iload 7
    //   513: invokespecial 158	com/mysql/jdbc/PreparedStatement:generateMultiStatementForBatch	(I)Ljava/lang/String;
    //   516: iconst_1
    //   517: invokeinterface 159 3 0
    //   522: astore 8
    //   524: goto +17 -> 541
    //   527: aload_3
    //   528: aload_0
    //   529: iload 7
    //   531: invokespecial 158	com/mysql/jdbc/PreparedStatement:generateMultiStatementForBatch	(I)Ljava/lang/String;
    //   534: invokeinterface 163 2 0
    //   539: astore 8
    //   541: aload 5
    //   543: ifnull +13 -> 556
    //   546: aload 5
    //   548: aload 8
    //   550: checkcast 166	com/mysql/jdbc/StatementImpl
    //   553: putfield 176	com/mysql/jdbc/StatementImpl$CancelTask:toCancel	Lcom/mysql/jdbc/StatementImpl;
    //   556: iconst_1
    //   557: istore 9
    //   559: iload 11
    //   561: iload 6
    //   563: if_icmpge +30 -> 593
    //   566: aload_0
    //   567: aload 8
    //   569: iload 9
    //   571: aload_0
    //   572: getfield 64	com/mysql/jdbc/PreparedStatement:batchedArgs	Ljava/util/List;
    //   575: iload 11
    //   577: iinc 11 1
    //   580: invokeinterface 77 2 0
    //   585: invokevirtual 174	com/mysql/jdbc/PreparedStatement:setOneBatchedParameterSet	(Ljava/sql/PreparedStatement;ILjava/lang/Object;)I
    //   588: istore 9
    //   590: goto -31 -> 559
    //   593: aload 8
    //   595: invokeinterface 170 1 0
    //   600: pop
    //   601: goto +21 -> 622
    //   604: astore 15
    //   606: aload_0
    //   607: iload 11
    //   609: iconst_1
    //   610: isub
    //   611: iload 7
    //   613: aload 13
    //   615: aload 15
    //   617: invokevirtual 171	com/mysql/jdbc/PreparedStatement:handleExceptionForBatch	(II[JLjava/sql/SQLException;)Ljava/sql/SQLException;
    //   620: astore 14
    //   622: aload_0
    //   623: aload 8
    //   625: checkcast 166	com/mysql/jdbc/StatementImpl
    //   628: iload 12
    //   630: aload 13
    //   632: invokevirtual 172	com/mysql/jdbc/PreparedStatement:processMultiCountsAndKeys	(Lcom/mysql/jdbc/StatementImpl;I[J)I
    //   635: istore 12
    //   637: aload 8
    //   639: invokeinterface 173 1 0
    //   644: aload 5
    //   646: ifnull +36 -> 682
    //   649: aload 5
    //   651: getfield 177	com/mysql/jdbc/StatementImpl$CancelTask:caughtWhileCancelling	Ljava/sql/SQLException;
    //   654: ifnull +9 -> 663
    //   657: aload 5
    //   659: getfield 177	com/mysql/jdbc/StatementImpl$CancelTask:caughtWhileCancelling	Ljava/sql/SQLException;
    //   662: athrow
    //   663: aload 5
    //   665: invokevirtual 178	com/mysql/jdbc/StatementImpl$CancelTask:cancel	()Z
    //   668: pop
    //   669: aload_3
    //   670: invokeinterface 168 1 0
    //   675: invokevirtual 179	java/util/Timer:purge	()I
    //   678: pop
    //   679: aconst_null
    //   680: astore 5
    //   682: aload 14
    //   684: ifnull +15 -> 699
    //   687: aload 14
    //   689: aload 13
    //   691: aload_0
    //   692: invokevirtual 42	com/mysql/jdbc/PreparedStatement:getExceptionInterceptor	()Lcom/mysql/jdbc/ExceptionInterceptor;
    //   695: invokestatic 180	com/mysql/jdbc/SQLError:createBatchUpdateException	(Ljava/sql/SQLException;[JLcom/mysql/jdbc/ExceptionInterceptor;)Ljava/sql/SQLException;
    //   698: athrow
    //   699: aload 13
    //   701: astore 15
    //   703: jsr +19 -> 722
    //   706: jsr +40 -> 746
    //   709: aload_2
    //   710: monitorexit
    //   711: aload 15
    //   713: areturn
    //   714: astore 20
    //   716: jsr +6 -> 722
    //   719: aload 20
    //   721: athrow
    //   722: astore 21
    //   724: aload 8
    //   726: ifnull +10 -> 736
    //   729: aload 8
    //   731: invokeinterface 175 1 0
    //   736: ret 21
    //   738: astore 22
    //   740: jsr +6 -> 746
    //   743: aload 22
    //   745: athrow
    //   746: astore 23
    //   748: aload 5
    //   750: ifnull +19 -> 769
    //   753: aload 5
    //   755: invokevirtual 178	com/mysql/jdbc/StatementImpl$CancelTask:cancel	()Z
    //   758: pop
    //   759: aload_3
    //   760: invokeinterface 168 1 0
    //   765: invokevirtual 179	java/util/Timer:purge	()I
    //   768: pop
    //   769: aload_0
    //   770: invokevirtual 109	com/mysql/jdbc/PreparedStatement:resetCancelledState	()V
    //   773: iload 4
    //   775: ifne +12 -> 787
    //   778: aload_3
    //   779: invokeinterface 156 1 0
    //   784: invokevirtual 181	com/mysql/jdbc/MysqlIO:disableMultiQueries	()V
    //   787: aload_0
    //   788: invokevirtual 148	com/mysql/jdbc/PreparedStatement:clearBatch	()V
    //   791: ret 23
    //   793: astore 24
    //   795: aload_2
    //   796: monitorexit
    //   797: aload 24
    //   799: athrow
    // Line number table:
    //   Java source line #1290	-> byte code offset #0
    //   Java source line #1292	-> byte code offset #12
    //   Java source line #1293	-> byte code offset #19
    //   Java source line #1296	-> byte code offset #45
    //   Java source line #1298	-> byte code offset #50
    //   Java source line #1299	-> byte code offset #58
    //   Java source line #1302	-> byte code offset #61
    //   Java source line #1304	-> byte code offset #65
    //   Java source line #1306	-> byte code offset #76
    //   Java source line #1307	-> byte code offset #83
    //   Java source line #1310	-> byte code offset #96
    //   Java source line #1312	-> byte code offset #104
    //   Java source line #1313	-> byte code offset #111
    //   Java source line #1316	-> byte code offset #115
    //   Java source line #1318	-> byte code offset #118
    //   Java source line #1319	-> byte code offset #121
    //   Java source line #1320	-> byte code offset #124
    //   Java source line #1321	-> byte code offset #127
    //   Java source line #1322	-> byte code offset #130
    //   Java source line #1323	-> byte code offset #136
    //   Java source line #1326	-> byte code offset #139
    //   Java source line #1327	-> byte code offset #144
    //   Java source line #1330	-> byte code offset #153
    //   Java source line #1331	-> byte code offset #160
    //   Java source line #1334	-> byte code offset #192
    //   Java source line #1338	-> byte code offset #220
    //   Java source line #1339	-> byte code offset #245
    //   Java source line #1340	-> byte code offset #260
    //   Java source line #1343	-> byte code offset #273
    //   Java source line #1344	-> byte code offset #280
    //   Java source line #1346	-> byte code offset #287
    //   Java source line #1349	-> byte code offset #294
    //   Java source line #1351	-> byte code offset #301
    //   Java source line #1352	-> byte code offset #311
    //   Java source line #1354	-> byte code offset #324
    //   Java source line #1357	-> byte code offset #332
    //   Java source line #1355	-> byte code offset #335
    //   Java source line #1356	-> byte code offset #337
    //   Java source line #1359	-> byte code offset #351
    //   Java source line #1361	-> byte code offset #366
    //   Java source line #1362	-> byte code offset #373
    //   Java source line #1365	-> byte code offset #376
    //   Java source line #1351	-> byte code offset #400
    //   Java source line #1369	-> byte code offset #406
    //   Java source line #1372	-> byte code offset #414
    //   Java source line #1370	-> byte code offset #417
    //   Java source line #1371	-> byte code offset #419
    //   Java source line #1374	-> byte code offset #435
    //   Java source line #1376	-> byte code offset #450
    //   Java source line #1378	-> byte code offset #457
    //   Java source line #1379	-> byte code offset #464
    //   Java source line #1384	-> byte code offset #467
    //   Java source line #1380	-> byte code offset #470
    //   Java source line #1381	-> byte code offset #485
    //   Java source line #1382	-> byte code offset #492
    //   Java source line #1387	-> byte code offset #497
    //   Java source line #1389	-> byte code offset #502
    //   Java source line #1390	-> byte code offset #509
    //   Java source line #1392	-> byte code offset #527
    //   Java source line #1395	-> byte code offset #541
    //   Java source line #1396	-> byte code offset #546
    //   Java source line #1399	-> byte code offset #556
    //   Java source line #1401	-> byte code offset #559
    //   Java source line #1402	-> byte code offset #566
    //   Java source line #1406	-> byte code offset #593
    //   Java source line #1409	-> byte code offset #601
    //   Java source line #1407	-> byte code offset #604
    //   Java source line #1408	-> byte code offset #606
    //   Java source line #1411	-> byte code offset #622
    //   Java source line #1413	-> byte code offset #637
    //   Java source line #1416	-> byte code offset #644
    //   Java source line #1417	-> byte code offset #649
    //   Java source line #1418	-> byte code offset #657
    //   Java source line #1421	-> byte code offset #663
    //   Java source line #1423	-> byte code offset #669
    //   Java source line #1425	-> byte code offset #679
    //   Java source line #1428	-> byte code offset #682
    //   Java source line #1429	-> byte code offset #687
    //   Java source line #1432	-> byte code offset #699
    //   Java source line #1434	-> byte code offset #714
    //   Java source line #1435	-> byte code offset #729
    //   Java source line #1439	-> byte code offset #738
    //   Java source line #1440	-> byte code offset #753
    //   Java source line #1441	-> byte code offset #759
    //   Java source line #1444	-> byte code offset #769
    //   Java source line #1446	-> byte code offset #773
    //   Java source line #1447	-> byte code offset #778
    //   Java source line #1450	-> byte code offset #787
    //   Java source line #1452	-> byte code offset #793
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	800	0	this	PreparedStatement
    //   0	800	1	batchTimeout	int
    //   10	786	2	Ljava/lang/Object;	Object
    //   49	730	3	locallyScopedConn	MySQLConnection
    //   56	718	4	multiQueriesEnabled	boolean
    //   59	695	5	timeoutTask	StatementImpl.CancelTask
    //   74	488	6	numBatchedArgs	int
    //   102	510	7	numValuesPerBatch	int
    //   116	614	8	batchedStatement	java.sql.PreparedStatement
    //   119	470	9	batchedParamIndex	int
    //   122	173	10	numberToExecuteAsMultiValue	int
    //   125	483	11	batchCounter	int
    //   128	508	12	updateCountCounter	int
    //   134	566	13	updateCounts	long[]
    //   137	551	14	sqlEx	SQLException
    //   299	8	15	numberArgsToExecute	int
    //   604	108	15	ex	SQLException
    //   302	99	16	i	int
    //   417	12	16	ex	SQLException
    //   335	10	17	ex	SQLException
    //   470	6	18	localObject1	Object
    //   478	1	19	localObject2	Object
    //   714	6	20	localObject3	Object
    //   722	1	21	localObject4	Object
    //   738	6	22	localObject5	Object
    //   746	1	23	localObject6	Object
    //   793	5	24	localObject7	Object
    // Exception table:
    //   from	to	target	type
    //   324	332	335	java/sql/SQLException
    //   406	414	417	java/sql/SQLException
    //   139	467	470	finally
    //   470	475	470	finally
    //   593	601	604	java/sql/SQLException
    //   497	706	714	finally
    //   714	719	714	finally
    //   61	709	738	finally
    //   714	743	738	finally
    //   12	711	793	finally
    //   714	797	793	finally
  }
  
  /* Error */
  protected long[] executeBatchedInserts(int batchTimeout)
    throws SQLException
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 62	com/mysql/jdbc/PreparedStatement:checkClosed	()Lcom/mysql/jdbc/MySQLConnection;
    //   4: invokeinterface 63 1 0
    //   9: dup
    //   10: astore_2
    //   11: monitorenter
    //   12: aload_0
    //   13: invokevirtual 185	com/mysql/jdbc/PreparedStatement:getValuesClause	()Ljava/lang/String;
    //   16: astore_3
    //   17: aload_0
    //   18: getfield 35	com/mysql/jdbc/PreparedStatement:connection	Lcom/mysql/jdbc/MySQLConnection;
    //   21: astore 4
    //   23: aload_3
    //   24: ifnonnull +11 -> 35
    //   27: aload_0
    //   28: iload_1
    //   29: invokevirtual 145	com/mysql/jdbc/PreparedStatement:executeBatchSerially	(I)[J
    //   32: aload_2
    //   33: monitorexit
    //   34: areturn
    //   35: aload_0
    //   36: getfield 64	com/mysql/jdbc/PreparedStatement:batchedArgs	Ljava/util/List;
    //   39: invokeinterface 138 1 0
    //   44: istore 5
    //   46: aload_0
    //   47: getfield 106	com/mysql/jdbc/PreparedStatement:retrieveGeneratedKeys	Z
    //   50: ifeq +16 -> 66
    //   53: aload_0
    //   54: new 65	java/util/ArrayList
    //   57: dup
    //   58: iload 5
    //   60: invokespecial 154	java/util/ArrayList:<init>	(I)V
    //   63: putfield 108	com/mysql/jdbc/PreparedStatement:batchedGeneratedKeys	Ljava/util/ArrayList;
    //   66: aload_0
    //   67: iload 5
    //   69: invokevirtual 155	com/mysql/jdbc/PreparedStatement:computeBatchSize	(I)I
    //   72: istore 6
    //   74: iload 5
    //   76: iload 6
    //   78: if_icmpge +7 -> 85
    //   81: iload 5
    //   83: istore 6
    //   85: aconst_null
    //   86: astore 7
    //   88: iconst_1
    //   89: istore 8
    //   91: lconst_0
    //   92: lstore 9
    //   94: iconst_0
    //   95: istore 11
    //   97: iconst_0
    //   98: istore 12
    //   100: aconst_null
    //   101: astore 13
    //   103: aconst_null
    //   104: astore 14
    //   106: iload 5
    //   108: newarray long
    //   110: astore 15
    //   112: aload_0
    //   113: aload 4
    //   115: iload 6
    //   117: invokevirtual 186	com/mysql/jdbc/PreparedStatement:prepareBatchedInsertSQL	(Lcom/mysql/jdbc/MySQLConnection;I)Lcom/mysql/jdbc/PreparedStatement;
    //   120: astore 7
    //   122: aload 4
    //   124: invokeinterface 164 1 0
    //   129: ifeq +46 -> 175
    //   132: iload_1
    //   133: ifeq +42 -> 175
    //   136: aload 4
    //   138: iconst_5
    //   139: iconst_0
    //   140: iconst_0
    //   141: invokeinterface 37 4 0
    //   146: ifeq +29 -> 175
    //   149: new 165	com/mysql/jdbc/StatementImpl$CancelTask
    //   152: dup
    //   153: aload_0
    //   154: aload 7
    //   156: invokespecial 167	com/mysql/jdbc/StatementImpl$CancelTask:<init>	(Lcom/mysql/jdbc/StatementImpl;Lcom/mysql/jdbc/StatementImpl;)V
    //   159: astore 13
    //   161: aload 4
    //   163: invokeinterface 168 1 0
    //   168: aload 13
    //   170: iload_1
    //   171: i2l
    //   172: invokevirtual 169	java/util/Timer:schedule	(Ljava/util/TimerTask;J)V
    //   175: iload 5
    //   177: iload 6
    //   179: if_icmpge +10 -> 189
    //   182: iload 5
    //   184: istore 11
    //   186: goto +10 -> 196
    //   189: iload 5
    //   191: iload 6
    //   193: idiv
    //   194: istore 11
    //   196: iload 11
    //   198: iload 6
    //   200: imul
    //   201: istore 16
    //   203: iconst_0
    //   204: istore 17
    //   206: iload 17
    //   208: iload 16
    //   210: if_icmpge +91 -> 301
    //   213: iload 17
    //   215: ifeq +56 -> 271
    //   218: iload 17
    //   220: iload 6
    //   222: irem
    //   223: ifne +48 -> 271
    //   226: lload 9
    //   228: aload 7
    //   230: invokevirtual 187	com/mysql/jdbc/PreparedStatement:executeLargeUpdate	()J
    //   233: ladd
    //   234: lstore 9
    //   236: goto +21 -> 257
    //   239: astore 18
    //   241: aload_0
    //   242: iload 12
    //   244: iconst_1
    //   245: isub
    //   246: iload 6
    //   248: aload 15
    //   250: aload 18
    //   252: invokevirtual 171	com/mysql/jdbc/PreparedStatement:handleExceptionForBatch	(II[JLjava/sql/SQLException;)Ljava/sql/SQLException;
    //   255: astore 14
    //   257: aload_0
    //   258: aload 7
    //   260: invokevirtual 188	com/mysql/jdbc/PreparedStatement:getBatchedGeneratedKeys	(Ljava/sql/Statement;)V
    //   263: aload 7
    //   265: invokevirtual 189	com/mysql/jdbc/PreparedStatement:clearParameters	()V
    //   268: iconst_1
    //   269: istore 8
    //   271: aload_0
    //   272: aload 7
    //   274: iload 8
    //   276: aload_0
    //   277: getfield 64	com/mysql/jdbc/PreparedStatement:batchedArgs	Ljava/util/List;
    //   280: iload 12
    //   282: iinc 12 1
    //   285: invokeinterface 77 2 0
    //   290: invokevirtual 174	com/mysql/jdbc/PreparedStatement:setOneBatchedParameterSet	(Ljava/sql/PreparedStatement;ILjava/lang/Object;)I
    //   293: istore 8
    //   295: iinc 17 1
    //   298: goto -92 -> 206
    //   301: lload 9
    //   303: aload 7
    //   305: invokevirtual 187	com/mysql/jdbc/PreparedStatement:executeLargeUpdate	()J
    //   308: ladd
    //   309: lstore 9
    //   311: goto +21 -> 332
    //   314: astore 17
    //   316: aload_0
    //   317: iload 12
    //   319: iconst_1
    //   320: isub
    //   321: iload 6
    //   323: aload 15
    //   325: aload 17
    //   327: invokevirtual 171	com/mysql/jdbc/PreparedStatement:handleExceptionForBatch	(II[JLjava/sql/SQLException;)Ljava/sql/SQLException;
    //   330: astore 14
    //   332: aload_0
    //   333: aload 7
    //   335: invokevirtual 188	com/mysql/jdbc/PreparedStatement:getBatchedGeneratedKeys	(Ljava/sql/Statement;)V
    //   338: iload 5
    //   340: iload 12
    //   342: isub
    //   343: istore 6
    //   345: jsr +14 -> 359
    //   348: goto +28 -> 376
    //   351: astore 19
    //   353: jsr +6 -> 359
    //   356: aload 19
    //   358: athrow
    //   359: astore 20
    //   361: aload 7
    //   363: ifnull +11 -> 374
    //   366: aload 7
    //   368: invokevirtual 190	com/mysql/jdbc/PreparedStatement:close	()V
    //   371: aconst_null
    //   372: astore 7
    //   374: ret 20
    //   376: iload 6
    //   378: ifle +99 -> 477
    //   381: aload_0
    //   382: aload 4
    //   384: iload 6
    //   386: invokevirtual 186	com/mysql/jdbc/PreparedStatement:prepareBatchedInsertSQL	(Lcom/mysql/jdbc/MySQLConnection;I)Lcom/mysql/jdbc/PreparedStatement;
    //   389: astore 7
    //   391: aload 13
    //   393: ifnull +10 -> 403
    //   396: aload 13
    //   398: aload 7
    //   400: putfield 176	com/mysql/jdbc/StatementImpl$CancelTask:toCancel	Lcom/mysql/jdbc/StatementImpl;
    //   403: iconst_1
    //   404: istore 8
    //   406: iload 12
    //   408: iload 5
    //   410: if_icmpge +30 -> 440
    //   413: aload_0
    //   414: aload 7
    //   416: iload 8
    //   418: aload_0
    //   419: getfield 64	com/mysql/jdbc/PreparedStatement:batchedArgs	Ljava/util/List;
    //   422: iload 12
    //   424: iinc 12 1
    //   427: invokeinterface 77 2 0
    //   432: invokevirtual 174	com/mysql/jdbc/PreparedStatement:setOneBatchedParameterSet	(Ljava/sql/PreparedStatement;ILjava/lang/Object;)I
    //   435: istore 8
    //   437: goto -31 -> 406
    //   440: lload 9
    //   442: aload 7
    //   444: invokevirtual 187	com/mysql/jdbc/PreparedStatement:executeLargeUpdate	()J
    //   447: ladd
    //   448: lstore 9
    //   450: goto +21 -> 471
    //   453: astore 16
    //   455: aload_0
    //   456: iload 12
    //   458: iconst_1
    //   459: isub
    //   460: iload 6
    //   462: aload 15
    //   464: aload 16
    //   466: invokevirtual 171	com/mysql/jdbc/PreparedStatement:handleExceptionForBatch	(II[JLjava/sql/SQLException;)Ljava/sql/SQLException;
    //   469: astore 14
    //   471: aload_0
    //   472: aload 7
    //   474: invokevirtual 188	com/mysql/jdbc/PreparedStatement:getBatchedGeneratedKeys	(Ljava/sql/Statement;)V
    //   477: aload 14
    //   479: ifnull +15 -> 494
    //   482: aload 14
    //   484: aload 15
    //   486: aload_0
    //   487: invokevirtual 42	com/mysql/jdbc/PreparedStatement:getExceptionInterceptor	()Lcom/mysql/jdbc/ExceptionInterceptor;
    //   490: invokestatic 180	com/mysql/jdbc/SQLError:createBatchUpdateException	(Ljava/sql/SQLException;[JLcom/mysql/jdbc/ExceptionInterceptor;)Ljava/sql/SQLException;
    //   493: athrow
    //   494: iload 5
    //   496: iconst_1
    //   497: if_icmple +45 -> 542
    //   500: lload 9
    //   502: lconst_0
    //   503: lcmp
    //   504: ifle +9 -> 513
    //   507: ldc2_w 191
    //   510: goto +4 -> 514
    //   513: lconst_0
    //   514: lstore 16
    //   516: iconst_0
    //   517: istore 18
    //   519: iload 18
    //   521: iload 5
    //   523: if_icmpge +16 -> 539
    //   526: aload 15
    //   528: iload 18
    //   530: lload 16
    //   532: lastore
    //   533: iinc 18 1
    //   536: goto -17 -> 519
    //   539: goto +9 -> 548
    //   542: aload 15
    //   544: iconst_0
    //   545: lload 9
    //   547: lastore
    //   548: aload 15
    //   550: astore 16
    //   552: jsr +19 -> 571
    //   555: jsr +38 -> 593
    //   558: aload_2
    //   559: monitorexit
    //   560: aload 16
    //   562: areturn
    //   563: astore 21
    //   565: jsr +6 -> 571
    //   568: aload 21
    //   570: athrow
    //   571: astore 22
    //   573: aload 7
    //   575: ifnull +8 -> 583
    //   578: aload 7
    //   580: invokevirtual 190	com/mysql/jdbc/PreparedStatement:close	()V
    //   583: ret 22
    //   585: astore 23
    //   587: jsr +6 -> 593
    //   590: aload 23
    //   592: athrow
    //   593: astore 24
    //   595: aload 13
    //   597: ifnull +20 -> 617
    //   600: aload 13
    //   602: invokevirtual 178	com/mysql/jdbc/StatementImpl$CancelTask:cancel	()Z
    //   605: pop
    //   606: aload 4
    //   608: invokeinterface 168 1 0
    //   613: invokevirtual 179	java/util/Timer:purge	()I
    //   616: pop
    //   617: aload_0
    //   618: invokevirtual 109	com/mysql/jdbc/PreparedStatement:resetCancelledState	()V
    //   621: ret 24
    //   623: astore 25
    //   625: aload_2
    //   626: monitorexit
    //   627: aload 25
    //   629: athrow
    // Line number table:
    //   Java source line #1480	-> byte code offset #0
    //   Java source line #1481	-> byte code offset #12
    //   Java source line #1483	-> byte code offset #17
    //   Java source line #1485	-> byte code offset #23
    //   Java source line #1486	-> byte code offset #27
    //   Java source line #1489	-> byte code offset #35
    //   Java source line #1491	-> byte code offset #46
    //   Java source line #1492	-> byte code offset #53
    //   Java source line #1495	-> byte code offset #66
    //   Java source line #1497	-> byte code offset #74
    //   Java source line #1498	-> byte code offset #81
    //   Java source line #1501	-> byte code offset #85
    //   Java source line #1503	-> byte code offset #88
    //   Java source line #1504	-> byte code offset #91
    //   Java source line #1505	-> byte code offset #94
    //   Java source line #1506	-> byte code offset #97
    //   Java source line #1507	-> byte code offset #100
    //   Java source line #1508	-> byte code offset #103
    //   Java source line #1510	-> byte code offset #106
    //   Java source line #1514	-> byte code offset #112
    //   Java source line #1517	-> byte code offset #122
    //   Java source line #1518	-> byte code offset #149
    //   Java source line #1519	-> byte code offset #161
    //   Java source line #1522	-> byte code offset #175
    //   Java source line #1523	-> byte code offset #182
    //   Java source line #1525	-> byte code offset #189
    //   Java source line #1528	-> byte code offset #196
    //   Java source line #1530	-> byte code offset #203
    //   Java source line #1531	-> byte code offset #213
    //   Java source line #1533	-> byte code offset #226
    //   Java source line #1536	-> byte code offset #236
    //   Java source line #1534	-> byte code offset #239
    //   Java source line #1535	-> byte code offset #241
    //   Java source line #1538	-> byte code offset #257
    //   Java source line #1539	-> byte code offset #263
    //   Java source line #1540	-> byte code offset #268
    //   Java source line #1544	-> byte code offset #271
    //   Java source line #1530	-> byte code offset #295
    //   Java source line #1548	-> byte code offset #301
    //   Java source line #1551	-> byte code offset #311
    //   Java source line #1549	-> byte code offset #314
    //   Java source line #1550	-> byte code offset #316
    //   Java source line #1553	-> byte code offset #332
    //   Java source line #1555	-> byte code offset #338
    //   Java source line #1556	-> byte code offset #345
    //   Java source line #1561	-> byte code offset #348
    //   Java source line #1557	-> byte code offset #351
    //   Java source line #1558	-> byte code offset #366
    //   Java source line #1559	-> byte code offset #371
    //   Java source line #1564	-> byte code offset #376
    //   Java source line #1565	-> byte code offset #381
    //   Java source line #1567	-> byte code offset #391
    //   Java source line #1568	-> byte code offset #396
    //   Java source line #1571	-> byte code offset #403
    //   Java source line #1573	-> byte code offset #406
    //   Java source line #1574	-> byte code offset #413
    //   Java source line #1578	-> byte code offset #440
    //   Java source line #1581	-> byte code offset #450
    //   Java source line #1579	-> byte code offset #453
    //   Java source line #1580	-> byte code offset #455
    //   Java source line #1583	-> byte code offset #471
    //   Java source line #1586	-> byte code offset #477
    //   Java source line #1587	-> byte code offset #482
    //   Java source line #1590	-> byte code offset #494
    //   Java source line #1591	-> byte code offset #500
    //   Java source line #1592	-> byte code offset #516
    //   Java source line #1593	-> byte code offset #526
    //   Java source line #1592	-> byte code offset #533
    //   Java source line #1595	-> byte code offset #539
    //   Java source line #1596	-> byte code offset #542
    //   Java source line #1598	-> byte code offset #548
    //   Java source line #1600	-> byte code offset #563
    //   Java source line #1601	-> byte code offset #578
    //   Java source line #1605	-> byte code offset #585
    //   Java source line #1606	-> byte code offset #600
    //   Java source line #1607	-> byte code offset #606
    //   Java source line #1610	-> byte code offset #617
    //   Java source line #1612	-> byte code offset #623
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	630	0	this	PreparedStatement
    //   0	630	1	batchTimeout	int
    //   10	616	2	Ljava/lang/Object;	Object
    //   16	8	3	valuesClause	String
    //   21	586	4	locallyScopedConn	MySQLConnection
    //   44	478	5	numBatchedArgs	int
    //   72	389	6	numValuesPerBatch	int
    //   86	493	7	batchedStatement	PreparedStatement
    //   89	347	8	batchedParamIndex	int
    //   92	454	9	updateCountRunningTotal	long
    //   95	102	11	numberToExecuteAsMultiValue	int
    //   98	359	12	batchCounter	int
    //   101	500	13	timeoutTask	StatementImpl.CancelTask
    //   104	379	14	sqlEx	SQLException
    //   110	439	15	updateCounts	long[]
    //   201	8	16	numberArgsToExecute	int
    //   453	12	16	ex	SQLException
    //   514	47	16	updCount	long
    //   204	92	17	i	int
    //   314	12	17	ex	SQLException
    //   239	12	18	ex	SQLException
    //   517	17	18	j	int
    //   351	6	19	localObject1	Object
    //   359	1	20	localObject2	Object
    //   563	6	21	localObject3	Object
    //   571	1	22	localObject4	Object
    //   585	6	23	localObject5	Object
    //   593	1	24	localObject6	Object
    //   623	5	25	localObject7	Object
    // Exception table:
    //   from	to	target	type
    //   226	236	239	java/sql/SQLException
    //   301	311	314	java/sql/SQLException
    //   112	348	351	finally
    //   351	356	351	finally
    //   440	450	453	java/sql/SQLException
    //   376	555	563	finally
    //   563	568	563	finally
    //   112	558	585	finally
    //   563	590	585	finally
    //   12	34	623	finally
    //   35	560	623	finally
    //   563	627	623	finally
  }
  
  /* Error */
  public String getPreparedSql()
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 62	com/mysql/jdbc/PreparedStatement:checkClosed	()Lcom/mysql/jdbc/MySQLConnection;
    //   4: invokeinterface 63 1 0
    //   9: dup
    //   10: astore_1
    //   11: monitorenter
    //   12: aload_0
    //   13: getfield 33	com/mysql/jdbc/PreparedStatement:rewrittenBatchSize	I
    //   16: ifne +10 -> 26
    //   19: aload_0
    //   20: getfield 21	com/mysql/jdbc/PreparedStatement:originalSql	Ljava/lang/String;
    //   23: aload_1
    //   24: monitorexit
    //   25: areturn
    //   26: aload_0
    //   27: getfield 52	com/mysql/jdbc/PreparedStatement:parseInfo	Lcom/mysql/jdbc/PreparedStatement$ParseInfo;
    //   30: aload_0
    //   31: getfield 52	com/mysql/jdbc/PreparedStatement:parseInfo	Lcom/mysql/jdbc/PreparedStatement$ParseInfo;
    //   34: invokevirtual 574	com/mysql/jdbc/PreparedStatement$ParseInfo:getSqlForBatch	(Lcom/mysql/jdbc/PreparedStatement$ParseInfo;)Ljava/lang/String;
    //   37: aload_1
    //   38: monitorexit
    //   39: areturn
    //   40: astore_2
    //   41: new 91	java/lang/RuntimeException
    //   44: dup
    //   45: aload_2
    //   46: invokespecial 575	java/lang/RuntimeException:<init>	(Ljava/lang/Throwable;)V
    //   49: athrow
    //   50: astore_3
    //   51: aload_1
    //   52: monitorexit
    //   53: aload_3
    //   54: athrow
    //   55: astore_1
    //   56: new 91	java/lang/RuntimeException
    //   59: dup
    //   60: aload_1
    //   61: invokespecial 575	java/lang/RuntimeException:<init>	(Ljava/lang/Throwable;)V
    //   64: athrow
    // Line number table:
    //   Java source line #5049	-> byte code offset #0
    //   Java source line #5050	-> byte code offset #12
    //   Java source line #5051	-> byte code offset #19
    //   Java source line #5055	-> byte code offset #26
    //   Java source line #5056	-> byte code offset #40
    //   Java source line #5057	-> byte code offset #41
    //   Java source line #5059	-> byte code offset #50
    //   Java source line #5060	-> byte code offset #55
    //   Java source line #5061	-> byte code offset #56
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	65	0	this	PreparedStatement
    //   55	6	1	e	SQLException
    //   40	6	2	e	UnsupportedEncodingException
    //   50	4	3	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   26	37	40	java/io/UnsupportedEncodingException
    //   12	25	50	finally
    //   26	39	50	finally
    //   40	53	50	finally
    //   0	25	55	java/sql/SQLException
    //   26	39	55	java/sql/SQLException
    //   40	55	55	java/sql/SQLException
  }
}
