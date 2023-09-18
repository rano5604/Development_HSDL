package com.mysql.jdbc;

import com.mysql.jdbc.log.LogUtils;
import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Date;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;




































































































public class ResultSetImpl
  implements ResultSetInternalMethods
{
  private static final Constructor<?> JDBC_4_RS_4_ARG_CTOR;
  private static final Constructor<?> JDBC_4_RS_5_ARG_CTOR;
  private static final Constructor<?> JDBC_4_UPD_RS_5_ARG_CTOR;
  protected static final double MIN_DIFF_PREC;
  protected static final double MAX_DIFF_PREC;
  static int resultCounter;
  
  protected static BigInteger convertLongToUlong(long longVal)
  {
    byte[] asBytes = new byte[8];
    asBytes[7] = ((byte)(int)(longVal & 0xFF));
    asBytes[6] = ((byte)(int)(longVal >>> 8));
    asBytes[5] = ((byte)(int)(longVal >>> 16));
    asBytes[4] = ((byte)(int)(longVal >>> 24));
    asBytes[3] = ((byte)(int)(longVal >>> 32));
    asBytes[2] = ((byte)(int)(longVal >>> 40));
    asBytes[1] = ((byte)(int)(longVal >>> 48));
    asBytes[0] = ((byte)(int)(longVal >>> 56));
    
    return new BigInteger(1, asBytes);
  }
  

  protected String catalog = null;
  

  protected Map<String, Integer> columnLabelToIndex = null;
  




  protected Map<String, Integer> columnToIndexCache = null;
  

  protected boolean[] columnUsed = null;
  

  protected volatile MySQLConnection connection;
  
  protected long connectionId = 0L;
  

  protected int currentRow = -1;
  

  protected boolean doingUpdates = false;
  
  protected ProfilerEventHandler eventSink = null;
  
  Calendar fastDefaultCal = null;
  Calendar fastClientCal = null;
  

  protected int fetchDirection = 1000;
  

  protected int fetchSize = 0;
  


  protected Field[] fields;
  


  protected char firstCharOfQuery;
  


  protected Map<String, Integer> fullColumnNameToIndex = null;
  
  protected Map<String, Integer> columnNameToIndex = null;
  
  protected boolean hasBuiltIndexMapping = false;
  



  protected boolean isBinaryEncoded = false;
  

  protected boolean isClosed = false;
  
  protected ResultSetInternalMethods nextResultSet = null;
  

  protected boolean onInsertRow = false;
  


  protected StatementImpl owningStatement;
  


  protected String pointOfOrigin;
  

  protected boolean profileSql = false;
  



  protected boolean reallyResult = false;
  

  protected int resultId;
  

  protected int resultSetConcurrency = 0;
  

  protected int resultSetType = 0;
  


  protected RowData rowData;
  


  protected String serverInfo = null;
  

  PreparedStatement statementUsedForFetchingRows;
  
  protected ResultSetRow thisRow = null;
  





  protected long updateCount;
  





  protected long updateId = -1L;
  
  private boolean useStrictFloatingPoint = false;
  
  protected boolean useUsageAdvisor = false;
  

  protected SQLWarning warningChain = null;
  

  protected boolean wasNullFlag = false;
  
  protected Statement wrapperStatement;
  
  protected boolean retainOwningStatement;
  
  protected Calendar gmtCalendar = null;
  
  protected boolean useFastDateParsing = false;
  
  private boolean padCharsWithSpace = false;
  
  private boolean jdbcCompliantTruncationForReads;
  
  private boolean useFastIntParsing = true;
  private boolean useColumnNamesInFindColumn;
  private ExceptionInterceptor exceptionInterceptor;
  static final char[] EMPTY_SPACE;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        String jdbc4ClassName = Util.isJdbc42() ? "com.mysql.jdbc.JDBC42ResultSet" : "com.mysql.jdbc.JDBC4ResultSet";
        JDBC_4_RS_4_ARG_CTOR = Class.forName(jdbc4ClassName).getConstructor(new Class[] { Long.TYPE, Long.TYPE, MySQLConnection.class, StatementImpl.class });
        
        JDBC_4_RS_5_ARG_CTOR = Class.forName(jdbc4ClassName).getConstructor(new Class[] { String.class, [Lcom.mysql.jdbc.Field.class, RowData.class, MySQLConnection.class, StatementImpl.class });
        

        jdbc4ClassName = Util.isJdbc42() ? "com.mysql.jdbc.JDBC42UpdatableResultSet" : "com.mysql.jdbc.JDBC4UpdatableResultSet";
        JDBC_4_UPD_RS_5_ARG_CTOR = Class.forName(jdbc4ClassName).getConstructor(new Class[] { String.class, [Lcom.mysql.jdbc.Field.class, RowData.class, MySQLConnection.class, StatementImpl.class });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDBC_4_RS_4_ARG_CTOR = null;
      JDBC_4_RS_5_ARG_CTOR = null;
      JDBC_4_UPD_RS_5_ARG_CTOR = null;
    }
    




    MIN_DIFF_PREC = Float.parseFloat(Float.toString(Float.MIN_VALUE)) - Double.parseDouble(Float.toString(Float.MIN_VALUE));
    



    MAX_DIFF_PREC = Float.parseFloat(Float.toString(Float.MAX_VALUE)) - Double.parseDouble(Float.toString(Float.MAX_VALUE));
    

    resultCounter = 1;
    


































































































































































    EMPTY_SPACE = new char['ÿ'];
    

    for (int i = 0; i < EMPTY_SPACE.length; i++) {
      EMPTY_SPACE[i] = ' ';
    }
  }
  
  protected static ResultSetImpl getInstance(long updateCount, long updateID, MySQLConnection conn, StatementImpl creatorStmt) throws SQLException {
    if (!Util.isJdbc4()) {
      return new ResultSetImpl(updateCount, updateID, conn, creatorStmt);
    }
    
    return (ResultSetImpl)Util.handleNewInstance(JDBC_4_RS_4_ARG_CTOR, new Object[] { Long.valueOf(updateCount), Long.valueOf(updateID), conn, creatorStmt }, conn.getExceptionInterceptor());
  }
  








  protected static ResultSetImpl getInstance(String catalog, Field[] fields, RowData tuples, MySQLConnection conn, StatementImpl creatorStmt, boolean isUpdatable)
    throws SQLException
  {
    if (!Util.isJdbc4()) {
      if (!isUpdatable) {
        return new ResultSetImpl(catalog, fields, tuples, conn, creatorStmt);
      }
      
      return new UpdatableResultSet(catalog, fields, tuples, conn, creatorStmt);
    }
    
    if (!isUpdatable) {
      return (ResultSetImpl)Util.handleNewInstance(JDBC_4_RS_5_ARG_CTOR, new Object[] { catalog, fields, tuples, conn, creatorStmt }, conn.getExceptionInterceptor());
    }
    

    return (ResultSetImpl)Util.handleNewInstance(JDBC_4_UPD_RS_5_ARG_CTOR, new Object[] { catalog, fields, tuples, conn, creatorStmt }, conn.getExceptionInterceptor());
  }
  










  public ResultSetImpl(long updateCount, long updateID, MySQLConnection conn, StatementImpl creatorStmt)
  {
    this.updateCount = updateCount;
    updateId = updateID;
    reallyResult = false;
    fields = new Field[0];
    
    connection = conn;
    owningStatement = creatorStmt;
    
    retainOwningStatement = false;
    
    if (connection != null) {
      exceptionInterceptor = connection.getExceptionInterceptor();
      
      retainOwningStatement = connection.getRetainStatementAfterResultSetClose();
      
      connectionId = connection.getId();
      serverTimeZoneTz = connection.getServerTimezoneTZ();
      padCharsWithSpace = connection.getPadCharsWithSpace();
      
      useLegacyDatetimeCode = connection.getUseLegacyDatetimeCode();
    }
  }
  














  public ResultSetImpl(String catalog, Field[] fields, RowData tuples, MySQLConnection conn, StatementImpl creatorStmt)
    throws SQLException
  {
    connection = conn;
    
    retainOwningStatement = false;
    
    if (connection != null) {
      exceptionInterceptor = connection.getExceptionInterceptor();
      useStrictFloatingPoint = connection.getStrictFloatingPoint();
      connectionId = connection.getId();
      useFastDateParsing = connection.getUseFastDateParsing();
      profileSql = connection.getProfileSql();
      retainOwningStatement = connection.getRetainStatementAfterResultSetClose();
      jdbcCompliantTruncationForReads = connection.getJdbcCompliantTruncationForReads();
      useFastIntParsing = connection.getUseFastIntParsing();
      serverTimeZoneTz = connection.getServerTimezoneTZ();
      padCharsWithSpace = connection.getPadCharsWithSpace();
    }
    
    owningStatement = creatorStmt;
    
    this.catalog = catalog;
    
    this.fields = fields;
    rowData = tuples;
    updateCount = rowData.size();
    




    reallyResult = true;
    

    if (rowData.size() > 0) {
      if ((updateCount == 1L) && 
        (thisRow == null)) {
        rowData.close();
        updateCount = -1L;
      }
    }
    else {
      thisRow = null;
    }
    
    rowData.setOwner(this);
    
    if (this.fields != null) {
      initializeWithMetadata();
    }
    useLegacyDatetimeCode = connection.getUseLegacyDatetimeCode();
    
    useColumnNamesInFindColumn = connection.getUseColumnNamesInFindColumn();
    
    setRowPositionValidity();
  }
  
  public void initializeWithMetadata() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      rowData.setMetadata(fields);
      
      columnToIndexCache = new HashMap();
      
      if ((profileSql) || (connection.getUseUsageAdvisor())) {
        columnUsed = new boolean[fields.length];
        pointOfOrigin = LogUtils.findCallingClassAndMethod(new Throwable());
        resultId = (resultCounter++);
        useUsageAdvisor = connection.getUseUsageAdvisor();
        eventSink = ProfilerEventHandlerFactory.getInstance(connection);
      }
      
      if (connection.getGatherPerformanceMetrics()) {
        connection.incrementNumberOfResultSetsCreated();
        
        Set<String> tableNamesSet = new HashSet();
        
        for (int i = 0; i < fields.length; i++) {
          Field f = fields[i];
          
          String tableName = f.getOriginalTableName();
          
          if (tableName == null) {
            tableName = f.getTableName();
          }
          
          if (tableName != null) {
            if (connection.lowerCaseTableNames()) {
              tableName = tableName.toLowerCase();
            }
            

            tableNamesSet.add(tableName);
          }
        }
        
        connection.reportNumberOfTablesAccessed(tableNamesSet.size());
      }
    }
  }
  
  private synchronized Calendar getFastDefaultCalendar() {
    if (fastDefaultCal == null) {
      fastDefaultCal = new GregorianCalendar(Locale.US);
      fastDefaultCal.setTimeZone(getDefaultTimeZone());
    }
    return fastDefaultCal;
  }
  
  private synchronized Calendar getFastClientCalendar() {
    if (fastClientCal == null) {
      fastClientCal = new GregorianCalendar(Locale.US);
    }
    return fastClientCal;
  }
  































  public boolean absolute(int row)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      boolean b;
      boolean b;
      if (rowData.size() == 0) {
        b = false;
      } else {
        if (onInsertRow) {
          onInsertRow = false;
        }
        
        if (doingUpdates) {
          doingUpdates = false;
        }
        
        if (thisRow != null) {
          thisRow.closeOpenStreams();
        }
        boolean b;
        if (row == 0) {
          beforeFirst();
          b = false; } else { boolean b;
          if (row == 1) {
            b = first(); } else { boolean b;
            if (row == -1) {
              b = last(); } else { boolean b;
              if (row > rowData.size()) {
                afterLast();
                b = false;
              } else { boolean b;
                if (row < 0)
                {
                  int newRowPosition = rowData.size() + row + 1;
                  boolean b;
                  if (newRowPosition <= 0) {
                    beforeFirst();
                    b = false;
                  } else {
                    b = absolute(newRowPosition);
                  }
                } else {
                  row--;
                  rowData.setCurrentRow(row);
                  thisRow = rowData.getAt(row);
                  b = true;
                }
              }
            }
          } } }
      setRowPositionValidity();
      
      return b;
    }
  }
  









  public void afterLast()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (onInsertRow) {
        onInsertRow = false;
      }
      
      if (doingUpdates) {
        doingUpdates = false;
      }
      
      if (thisRow != null) {
        thisRow.closeOpenStreams();
      }
      
      if (rowData.size() != 0) {
        rowData.afterLast();
        thisRow = null;
      }
      
      setRowPositionValidity();
    }
  }
  









  public void beforeFirst()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (onInsertRow) {
        onInsertRow = false;
      }
      
      if (doingUpdates) {
        doingUpdates = false;
      }
      
      if (rowData.size() == 0) {
        return;
      }
      
      if (thisRow != null) {
        thisRow.closeOpenStreams();
      }
      
      rowData.beforeFirst();
      thisRow = null;
      
      setRowPositionValidity();
    }
  }
  





  public void buildIndexMapping()
    throws SQLException
  {
    int numFields = fields.length;
    columnLabelToIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    fullColumnNameToIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    columnNameToIndex = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    







    for (int i = numFields - 1; i >= 0; i--) {
      Integer index = Integer.valueOf(i);
      String columnName = fields[i].getOriginalName();
      String columnLabel = fields[i].getName();
      String fullColumnName = fields[i].getFullName();
      
      if (columnLabel != null) {
        columnLabelToIndex.put(columnLabel, index);
      }
      
      if (fullColumnName != null) {
        fullColumnNameToIndex.put(fullColumnName, index);
      }
      
      if (columnName != null) {
        columnNameToIndex.put(columnName, index);
      }
    }
    

    hasBuiltIndexMapping = true;
  }
  









  public void cancelRowUpdates()
    throws SQLException
  {
    throw new NotUpdatable();
  }
  




  protected final MySQLConnection checkClosed()
    throws SQLException
  {
    MySQLConnection c = connection;
    
    if (c == null) {
      throw SQLError.createSQLException(Messages.getString("ResultSet.Operation_not_allowed_after_ResultSet_closed_144"), "S1000", getExceptionInterceptor());
    }
    

    return c;
  }
  







  protected final void checkColumnBounds(int columnIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (columnIndex < 1) {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Column_Index_out_of_range_low", new Object[] { Integer.valueOf(columnIndex), Integer.valueOf(fields.length) }), "S1009", getExceptionInterceptor());
      }
      

      if (columnIndex > fields.length) {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Column_Index_out_of_range_high", new Object[] { Integer.valueOf(columnIndex), Integer.valueOf(fields.length) }), "S1009", getExceptionInterceptor());
      }
      



      if ((profileSql) || (useUsageAdvisor)) {
        columnUsed[(columnIndex - 1)] = true;
      }
    }
  }
  





  protected void checkRowPos()
    throws SQLException
  {
    checkClosed();
    
    if (!onValidRow) {
      throw SQLError.createSQLException(invalidRowReason, "S1000", getExceptionInterceptor());
    }
  }
  
  private boolean onValidRow = false;
  private String invalidRowReason = null;
  protected boolean useLegacyDatetimeCode;
  private TimeZone serverTimeZoneTz;
  
  private void setRowPositionValidity() throws SQLException {
    if ((!rowData.isDynamic()) && (rowData.size() == 0)) {
      invalidRowReason = Messages.getString("ResultSet.Illegal_operation_on_empty_result_set");
      onValidRow = false;
    } else if (rowData.isBeforeFirst()) {
      invalidRowReason = Messages.getString("ResultSet.Before_start_of_result_set_146");
      onValidRow = false;
    } else if (rowData.isAfterLast()) {
      invalidRowReason = Messages.getString("ResultSet.After_end_of_result_set_148");
      onValidRow = false;
    } else {
      onValidRow = true;
      invalidRowReason = null;
    }
  }
  



  public synchronized void clearNextResult()
  {
    nextResultSet = null;
  }
  





  public void clearWarnings()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      warningChain = null;
    }
  }
  












  public void close()
    throws SQLException
  {
    realClose(true);
  }
  
  private int convertToZeroWithEmptyCheck() throws SQLException {
    if (connection.getEmptyStringsConvertToZero()) {
      return 0;
    }
    
    throw SQLError.createSQLException("Can't convert empty string ('') to numeric", "22018", getExceptionInterceptor());
  }
  
  private String convertToZeroLiteralStringWithEmptyCheck()
    throws SQLException
  {
    if (connection.getEmptyStringsConvertToZero()) {
      return "0";
    }
    
    throw SQLError.createSQLException("Can't convert empty string ('') to numeric", "22018", getExceptionInterceptor());
  }
  


  public ResultSetInternalMethods copy()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      ResultSetImpl rs = getInstance(catalog, fields, rowData, connection, owningStatement, false);
      if (isBinaryEncoded) {
        rs.setBinaryEncoded();
      }
      
      return rs;
    }
  }
  
  public void redefineFieldsForDBMD(Field[] f) {
    fields = f;
    
    for (int i = 0; i < fields.length; i++) {
      fields[i].setUseOldNameMetadata(true);
      fields[i].setConnection(connection);
    }
  }
  
  public void populateCachedMetaData(CachedResultSetMetaData cachedMetaData) throws SQLException {
    fields = fields;
    columnNameToIndex = columnLabelToIndex;
    fullColumnNameToIndex = fullColumnNameToIndex;
    metadata = getMetaData();
  }
  
  public void initializeFromCachedMetaData(CachedResultSetMetaData cachedMetaData) {
    fields = fields;
    columnLabelToIndex = columnNameToIndex;
    fullColumnNameToIndex = fullColumnNameToIndex;
    hasBuiltIndexMapping = true;
  }
  







  public void deleteRow()
    throws SQLException
  {
    throw new NotUpdatable();
  }
  




  private String extractStringFromNativeColumn(int columnIndex, int mysqlType)
    throws SQLException
  {
    int columnIndexMinusOne = columnIndex - 1;
    
    wasNullFlag = false;
    
    if (thisRow.isNull(columnIndexMinusOne)) {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    
    String encoding = fields[columnIndexMinusOne].getEncoding();
    
    return thisRow.getString(columnIndex - 1, encoding, connection);
  }
  
  protected Date fastDateCreate(Calendar cal, int year, int month, int day) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      Calendar targetCalendar = cal;
      
      if (cal == null) {
        if (connection.getNoTimezoneConversionForDateType()) {
          targetCalendar = getFastClientCalendar();
        } else {
          targetCalendar = getFastDefaultCalendar();
        }
      }
      
      if (!useLegacyDatetimeCode) {
        return TimeUtil.fastDateCreate(year, month, day, targetCalendar);
      }
      
      boolean useGmtMillis = (cal == null) && (!connection.getNoTimezoneConversionForDateType()) && (connection.getUseGmtMillisForDatetimes());
      
      return TimeUtil.fastDateCreate(useGmtMillis, useGmtMillis ? getGmtCalendar() : targetCalendar, targetCalendar, year, month, day);
    }
  }
  
  protected Time fastTimeCreate(Calendar cal, int hour, int minute, int second) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!useLegacyDatetimeCode) {
        return TimeUtil.fastTimeCreate(hour, minute, second, cal, getExceptionInterceptor());
      }
      
      if (cal == null) {
        cal = getFastDefaultCalendar();
      }
      
      return TimeUtil.fastTimeCreate(cal, hour, minute, second, getExceptionInterceptor());
    }
  }
  
  protected Timestamp fastTimestampCreate(Calendar cal, int year, int month, int day, int hour, int minute, int seconds, int secondsPart) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!useLegacyDatetimeCode) {
        return TimeUtil.fastTimestampCreate(cal.getTimeZone(), year, month, day, hour, minute, seconds, secondsPart);
      }
      
      if (cal == null) {
        cal = getFastDefaultCalendar();
      }
      
      boolean useGmtMillis = connection.getUseGmtMillisForDatetimes();
      
      return TimeUtil.fastTimestampCreate(useGmtMillis, useGmtMillis ? getGmtCalendar() : null, cal, year, month, day, hour, minute, seconds, secondsPart);
    }
  }
  






































  public int findColumn(String columnName)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {

      if (!hasBuiltIndexMapping) {
        buildIndexMapping();
      }
      
      Integer index = (Integer)columnToIndexCache.get(columnName);
      
      if (index != null) {
        return index.intValue() + 1;
      }
      
      index = (Integer)columnLabelToIndex.get(columnName);
      
      if ((index == null) && (useColumnNamesInFindColumn)) {
        index = (Integer)columnNameToIndex.get(columnName);
      }
      
      if (index == null) {
        index = (Integer)fullColumnNameToIndex.get(columnName);
      }
      
      if (index != null) {
        columnToIndexCache.put(columnName, index);
        
        return index.intValue() + 1;
      }
      


      for (int i = 0; i < fields.length; i++) {
        if (fields[i].getName().equalsIgnoreCase(columnName))
          return i + 1;
        if (fields[i].getFullName().equalsIgnoreCase(columnName)) {
          return i + 1;
        }
      }
      
      throw SQLError.createSQLException(Messages.getString("ResultSet.Column____112") + columnName + Messages.getString("ResultSet.___not_found._113"), "S0022", getExceptionInterceptor());
    }
  }
  












  public boolean first()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      boolean b = true;
      
      if (rowData.isEmpty()) {
        b = false;
      }
      else {
        if (onInsertRow) {
          onInsertRow = false;
        }
        
        if (doingUpdates) {
          doingUpdates = false;
        }
        
        rowData.beforeFirst();
        thisRow = rowData.next();
      }
      
      setRowPositionValidity();
      
      return b;
    }
  }
  










  public Array getArray(int i)
    throws SQLException
  {
    checkColumnBounds(i);
    
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  










  public Array getArray(String colName)
    throws SQLException
  {
    return getArray(findColumn(colName));
  }
  





















  public InputStream getAsciiStream(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    
    if (!isBinaryEncoded) {
      return getBinaryStream(columnIndex);
    }
    
    return getNativeBinaryStream(columnIndex);
  }
  



  public InputStream getAsciiStream(String columnName)
    throws SQLException
  {
    return getAsciiStream(findColumn(columnName));
  }
  











  public BigDecimal getBigDecimal(int columnIndex)
    throws SQLException
  {
    if (!isBinaryEncoded) {
      String stringVal = getString(columnIndex);
      

      if (stringVal != null) {
        if (stringVal.length() == 0)
        {
          BigDecimal val = new BigDecimal(convertToZeroLiteralStringWithEmptyCheck());
          
          return val;
        }
        try
        {
          return new BigDecimal(stringVal);
        }
        catch (NumberFormatException ex)
        {
          throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009", getExceptionInterceptor());
        }
      }
      


      return null;
    }
    
    return getNativeBigDecimal(columnIndex);
  }
  














  @Deprecated
  public BigDecimal getBigDecimal(int columnIndex, int scale)
    throws SQLException
  {
    if (!isBinaryEncoded) {
      String stringVal = getString(columnIndex);
      

      if (stringVal != null) {
        if (stringVal.length() == 0) {
          BigDecimal val = new BigDecimal(convertToZeroLiteralStringWithEmptyCheck());
          try
          {
            return val.setScale(scale);
          } catch (ArithmeticException ex) {
            try {
              return val.setScale(scale, 4);
            } catch (ArithmeticException arEx) {
              throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009", getExceptionInterceptor());
            }
          }
        }
        

        try
        {
          val = new BigDecimal(stringVal);
        } catch (NumberFormatException ex) { BigDecimal val;
          if (fields[(columnIndex - 1)].getMysqlType() == 16) {
            long valueAsLong = getNumericRepresentationOfSQLBitType(columnIndex);
            
            val = new BigDecimal(valueAsLong);
          } else {
            throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { Integer.valueOf(columnIndex), stringVal }), "S1009", getExceptionInterceptor());
          }
        }
        

        try
        {
          return val.setScale(scale);
        } catch (ArithmeticException ex) {
          try { BigDecimal val;
            return val.setScale(scale, 4);
          } catch (ArithmeticException arithEx) {
            throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { Integer.valueOf(columnIndex), stringVal }), "S1009", getExceptionInterceptor());
          }
        }
      }
      


      return null;
    }
    
    return getNativeBigDecimal(columnIndex, scale);
  }
  










  public BigDecimal getBigDecimal(String columnName)
    throws SQLException
  {
    return getBigDecimal(findColumn(columnName));
  }
  






  @Deprecated
  public BigDecimal getBigDecimal(String columnName, int scale)
    throws SQLException
  {
    return getBigDecimal(findColumn(columnName), scale);
  }
  
  private final BigDecimal getBigDecimalFromString(String stringVal, int columnIndex, int scale)
    throws SQLException
  {
    if (stringVal != null) {
      if (stringVal.length() == 0) {
        BigDecimal bdVal = new BigDecimal(convertToZeroLiteralStringWithEmptyCheck());
        try
        {
          return bdVal.setScale(scale);
        } catch (ArithmeticException ex) {
          try {
            return bdVal.setScale(scale, 4);
          } catch (ArithmeticException arEx) {
            throw new SQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009");
          }
        }
      }
      


      try
      {
        return new BigDecimal(stringVal).setScale(scale);
      } catch (ArithmeticException ex) {
        try {
          return new BigDecimal(stringVal).setScale(scale, 4);
        } catch (ArithmeticException arEx) {
          throw new SQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009");
        }
        
      }
      catch (NumberFormatException ex)
      {
        if (fields[(columnIndex - 1)].getMysqlType() == 16) {
          long valueAsLong = getNumericRepresentationOfSQLBitType(columnIndex);
          try
          {
            return new BigDecimal(valueAsLong).setScale(scale);
          } catch (ArithmeticException arEx1) {
            try {
              return new BigDecimal(valueAsLong).setScale(scale, 4);
            } catch (ArithmeticException arEx2) {
              throw new SQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009");
            }
          }
        }
        


        if ((fields[(columnIndex - 1)].getMysqlType() == 1) && (connection.getTinyInt1isBit()) && (fields[(columnIndex - 1)].getLength() == 1L))
        {
          return new BigDecimal(stringVal.equalsIgnoreCase("true") ? 1 : 0).setScale(scale);
        }
        
        throw new SQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009");
      }
    }
    

    return null;
  }
  















  public InputStream getBinaryStream(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    
    if (!isBinaryEncoded) {
      checkColumnBounds(columnIndex);
      
      int columnIndexMinusOne = columnIndex - 1;
      
      if (thisRow.isNull(columnIndexMinusOne)) {
        wasNullFlag = true;
        
        return null;
      }
      
      wasNullFlag = false;
      
      return thisRow.getBinaryInputStream(columnIndexMinusOne);
    }
    
    return getNativeBinaryStream(columnIndex);
  }
  



  public InputStream getBinaryStream(String columnName)
    throws SQLException
  {
    return getBinaryStream(findColumn(columnName));
  }
  









  public java.sql.Blob getBlob(int columnIndex)
    throws SQLException
  {
    if (!isBinaryEncoded) {
      checkRowPos();
      
      checkColumnBounds(columnIndex);
      
      int columnIndexMinusOne = columnIndex - 1;
      
      if (thisRow.isNull(columnIndexMinusOne)) {
        wasNullFlag = true;
      } else {
        wasNullFlag = false;
      }
      
      if (wasNullFlag) {
        return null;
      }
      
      if (!connection.getEmulateLocators()) {
        return new Blob(thisRow.getColumnValue(columnIndexMinusOne), getExceptionInterceptor());
      }
      
      return new BlobFromLocator(this, columnIndex, getExceptionInterceptor());
    }
    
    return getNativeBlob(columnIndex);
  }
  









  public java.sql.Blob getBlob(String colName)
    throws SQLException
  {
    return getBlob(findColumn(colName));
  }
  










  public boolean getBoolean(int columnIndex)
    throws SQLException
  {
    checkColumnBounds(columnIndex);
    




    int columnIndexMinusOne = columnIndex - 1;
    
    Field field = fields[columnIndexMinusOne];
    
    if (field.getMysqlType() == 16) {
      return byteArrayToBoolean(columnIndexMinusOne);
    }
    
    wasNullFlag = false;
    
    int sqlType = field.getSQLType();
    long boolVal;
    switch (sqlType) {
    case 16: 
      if (field.getMysqlType() == -1) {
        String stringVal = getString(columnIndex);
        
        return getBooleanFromString(stringVal);
      }
      
      boolVal = getLong(columnIndex, false);
      
      return (boolVal == -1L) || (boolVal > 0L);
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
      boolVal = getLong(columnIndex, false);
      
      return (boolVal == -1L) || (boolVal > 0L);
    }
    if (connection.getPedantic())
    {
      switch (sqlType) {
      case -4: 
      case -3: 
      case -2: 
      case 70: 
      case 91: 
      case 92: 
      case 93: 
      case 2000: 
      case 2002: 
      case 2003: 
      case 2004: 
      case 2005: 
      case 2006: 
        throw SQLError.createSQLException("Required type conversion not allowed", "22018", getExceptionInterceptor());
      }
      
    }
    
    if ((sqlType == -2) || (sqlType == -3) || (sqlType == -4) || (sqlType == 2004)) {
      return byteArrayToBoolean(columnIndexMinusOne);
    }
    
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getBoolean()", columnIndex, thisRow.getColumnValue(columnIndexMinusOne), fields[columnIndex], new int[] { 16, 5, 1, 2, 3, 8, 4 });
    }
    


    String stringVal = getString(columnIndex);
    
    return getBooleanFromString(stringVal);
  }
  
  private boolean byteArrayToBoolean(int columnIndexMinusOne) throws SQLException
  {
    Object value = thisRow.getColumnValue(columnIndexMinusOne);
    
    if (value == null) {
      wasNullFlag = true;
      
      return false;
    }
    
    wasNullFlag = false;
    
    if (((byte[])value).length == 0) {
      return false;
    }
    
    byte boolVal = ((byte[])(byte[])value)[0];
    
    if (boolVal == 49)
      return true;
    if (boolVal == 48) {
      return false;
    }
    
    return (boolVal == -1) || (boolVal > 0);
  }
  



  public boolean getBoolean(String columnName)
    throws SQLException
  {
    return getBoolean(findColumn(columnName));
  }
  
  private final boolean getBooleanFromString(String stringVal) throws SQLException {
    if ((stringVal != null) && (stringVal.length() > 0)) {
      int c = Character.toLowerCase(stringVal.charAt(0));
      
      return (c == 116) || (c == 121) || (c == 49) || (stringVal.equals("-1"));
    }
    
    return false;
  }
  









  public byte getByte(int columnIndex)
    throws SQLException
  {
    if (!isBinaryEncoded) {
      String stringVal = getString(columnIndex);
      
      if ((wasNullFlag) || (stringVal == null)) {
        return 0;
      }
      
      return getByteFromString(stringVal, columnIndex);
    }
    
    return getNativeByte(columnIndex);
  }
  



  public byte getByte(String columnName)
    throws SQLException
  {
    return getByte(findColumn(columnName));
  }
  
  private final byte getByteFromString(String stringVal, int columnIndex) throws SQLException
  {
    if ((stringVal != null) && (stringVal.length() == 0)) {
      return (byte)convertToZeroWithEmptyCheck();
    }
    







    if (stringVal == null) {
      return 0;
    }
    
    stringVal = stringVal.trim();
    try
    {
      int decimalIndex = stringVal.indexOf(".");
      
      if (decimalIndex != -1) {
        double valueAsDouble = Double.parseDouble(stringVal);
        
        if ((jdbcCompliantTruncationForReads) && (
          (valueAsDouble < -128.0D) || (valueAsDouble > 127.0D))) {
          throwRangeException(stringVal, columnIndex, -6);
        }
        

        return (byte)(int)valueAsDouble;
      }
      
      long valueAsLong = Long.parseLong(stringVal);
      
      if ((jdbcCompliantTruncationForReads) && (
        (valueAsLong < -128L) || (valueAsLong > 127L))) {
        throwRangeException(String.valueOf(valueAsLong), columnIndex, -6);
      }
      

      return (byte)(int)valueAsLong;
    } catch (NumberFormatException NFE) {
      throw SQLError.createSQLException(Messages.getString("ResultSet.Value____173") + stringVal + Messages.getString("ResultSet.___is_out_of_range_[-127,127]_174"), "S1009", getExceptionInterceptor());
    }
  }
  















  public byte[] getBytes(int columnIndex)
    throws SQLException
  {
    return getBytes(columnIndex, false);
  }
  
  protected byte[] getBytes(int columnIndex, boolean noConversion) throws SQLException {
    if (!isBinaryEncoded) {
      checkRowPos();
      
      checkColumnBounds(columnIndex);
      
      int columnIndexMinusOne = columnIndex - 1;
      
      if (thisRow.isNull(columnIndexMinusOne)) {
        wasNullFlag = true;
      } else {
        wasNullFlag = false;
      }
      
      if (wasNullFlag) {
        return null;
      }
      
      return thisRow.getColumnValue(columnIndexMinusOne);
    }
    
    return getNativeBytes(columnIndex, noConversion);
  }
  



  public byte[] getBytes(String columnName)
    throws SQLException
  {
    return getBytes(findColumn(columnName));
  }
  
  private final byte[] getBytesFromString(String stringVal) throws SQLException {
    if (stringVal != null) {
      return StringUtils.getBytes(stringVal, connection.getEncoding(), connection.getServerCharset(), connection.parserKnowsUnicode(), connection, getExceptionInterceptor());
    }
    

    return null;
  }
  
  public int getBytesSize() throws SQLException {
    RowData localRowData = rowData;
    
    checkClosed();
    
    if ((localRowData instanceof RowDataStatic)) {
      int bytesSize = 0;
      
      int numRows = localRowData.size();
      
      for (int i = 0; i < numRows; i++) {
        bytesSize += localRowData.getAt(i).getBytesSize();
      }
      
      return bytesSize;
    }
    
    return -1;
  }
  


  protected Calendar getCalendarInstanceForSessionOrNew()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (connection != null) {
        return connection.getCalendarInstanceForSessionOrNew();
      }
      

      return new GregorianCalendar();
    }
  }
  













  public Reader getCharacterStream(int columnIndex)
    throws SQLException
  {
    if (!isBinaryEncoded) {
      checkColumnBounds(columnIndex);
      
      int columnIndexMinusOne = columnIndex - 1;
      
      if (thisRow.isNull(columnIndexMinusOne)) {
        wasNullFlag = true;
        
        return null;
      }
      
      wasNullFlag = false;
      
      return thisRow.getReader(columnIndexMinusOne);
    }
    
    return getNativeCharacterStream(columnIndex);
  }
  













  public Reader getCharacterStream(String columnName)
    throws SQLException
  {
    return getCharacterStream(findColumn(columnName));
  }
  
  private final Reader getCharacterStreamFromString(String stringVal) throws SQLException {
    if (stringVal != null) {
      return new StringReader(stringVal);
    }
    
    return null;
  }
  









  public java.sql.Clob getClob(int i)
    throws SQLException
  {
    if (!isBinaryEncoded) {
      String asString = getStringForClob(i);
      
      if (asString == null) {
        return null;
      }
      
      return new Clob(asString, getExceptionInterceptor());
    }
    
    return getNativeClob(i);
  }
  









  public java.sql.Clob getClob(String colName)
    throws SQLException
  {
    return getClob(findColumn(colName));
  }
  
  private final java.sql.Clob getClobFromString(String stringVal) throws SQLException {
    return new Clob(stringVal, getExceptionInterceptor());
  }
  







  public int getConcurrency()
    throws SQLException
  {
    return 1007;
  }
  




















  public String getCursorName()
    throws SQLException
  {
    throw SQLError.createSQLException(Messages.getString("ResultSet.Positioned_Update_not_supported"), "S1C00", getExceptionInterceptor());
  }
  










  public Date getDate(int columnIndex)
    throws SQLException
  {
    return getDate(columnIndex, null);
  }
  














  public Date getDate(int columnIndex, Calendar cal)
    throws SQLException
  {
    if (isBinaryEncoded) {
      return getNativeDate(columnIndex, cal);
    }
    
    if (!useFastDateParsing) {
      String stringVal = getStringInternal(columnIndex, false);
      
      if (stringVal == null) {
        return null;
      }
      
      return getDateFromString(stringVal, columnIndex, cal);
    }
    
    checkColumnBounds(columnIndex);
    
    int columnIndexMinusOne = columnIndex - 1;
    Date tmpDate = thisRow.getDateFast(columnIndexMinusOne, connection, this, cal);
    if ((thisRow.isNull(columnIndexMinusOne)) || (tmpDate == null))
    {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    
    return tmpDate;
  }
  



  public Date getDate(String columnName)
    throws SQLException
  {
    return getDate(findColumn(columnName));
  }
  













  public Date getDate(String columnName, Calendar cal)
    throws SQLException
  {
    return getDate(findColumn(columnName), cal);
  }
  
  private final Date getDateFromString(String stringVal, int columnIndex, Calendar targetCalendar) throws SQLException {
    int year = 0;
    int month = 0;
    int day = 0;
    try
    {
      wasNullFlag = false;
      
      if (stringVal == null) {
        wasNullFlag = true;
        
        return null;
      }
      







      stringVal = stringVal.trim();
      

      int dec = stringVal.indexOf(".");
      if (dec > -1) {
        stringVal = stringVal.substring(0, dec);
      }
      
      if ((stringVal.equals("0")) || (stringVal.equals("0000-00-00")) || (stringVal.equals("0000-00-00 00:00:00")) || (stringVal.equals("00000000000000")) || (stringVal.equals("0")))
      {

        if ("convertToNull".equals(connection.getZeroDateTimeBehavior())) {
          wasNullFlag = true;
          
          return null; }
        if ("exception".equals(connection.getZeroDateTimeBehavior())) {
          throw SQLError.createSQLException("Value '" + stringVal + "' can not be represented as java.sql.Date", "S1009", getExceptionInterceptor());
        }
        


        return fastDateCreate(targetCalendar, 1, 1, 1);
      }
      if (fields[(columnIndex - 1)].getMysqlType() == 7)
      {
        switch (stringVal.length()) {
        case 19: 
        case 21: 
          year = Integer.parseInt(stringVal.substring(0, 4));
          month = Integer.parseInt(stringVal.substring(5, 7));
          day = Integer.parseInt(stringVal.substring(8, 10));
          
          return fastDateCreate(targetCalendar, year, month, day);
        

        case 8: 
        case 14: 
          year = Integer.parseInt(stringVal.substring(0, 4));
          month = Integer.parseInt(stringVal.substring(4, 6));
          day = Integer.parseInt(stringVal.substring(6, 8));
          
          return fastDateCreate(targetCalendar, year, month, day);
        

        case 6: 
        case 10: 
        case 12: 
          year = Integer.parseInt(stringVal.substring(0, 2));
          
          if (year <= 69) {
            year += 100;
          }
          
          month = Integer.parseInt(stringVal.substring(2, 4));
          day = Integer.parseInt(stringVal.substring(4, 6));
          
          return fastDateCreate(targetCalendar, year + 1900, month, day);
        

        case 4: 
          year = Integer.parseInt(stringVal.substring(0, 4));
          
          if (year <= 69) {
            year += 100;
          }
          
          month = Integer.parseInt(stringVal.substring(2, 4));
          
          return fastDateCreate(targetCalendar, year + 1900, month, 1);
        

        case 2: 
          year = Integer.parseInt(stringVal.substring(0, 2));
          
          if (year <= 69) {
            year += 100;
          }
          
          return fastDateCreate(targetCalendar, year + 1900, 1, 1);
        }
        
        
        throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_Date", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009", getExceptionInterceptor());
      }
      

      if (fields[(columnIndex - 1)].getMysqlType() == 13)
      {
        if ((stringVal.length() == 2) || (stringVal.length() == 1)) {
          year = Integer.parseInt(stringVal);
          
          if (year <= 69) {
            year += 100;
          }
          
          year += 1900;
        } else {
          year = Integer.parseInt(stringVal.substring(0, 4));
        }
        
        return fastDateCreate(targetCalendar, year, 1, 1); }
      if (fields[(columnIndex - 1)].getMysqlType() == 11) {
        return fastDateCreate(targetCalendar, 1970, 1, 1);
      }
      if (stringVal.length() < 10) {
        if (stringVal.length() == 8) {
          return fastDateCreate(targetCalendar, 1970, 1, 1);
        }
        
        throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_Date", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009", getExceptionInterceptor());
      }
      


      if (stringVal.length() != 18) {
        year = Integer.parseInt(stringVal.substring(0, 4));
        month = Integer.parseInt(stringVal.substring(5, 7));
        day = Integer.parseInt(stringVal.substring(8, 10));
      }
      else {
        StringTokenizer st = new StringTokenizer(stringVal, "- ");
        
        year = Integer.parseInt(st.nextToken());
        month = Integer.parseInt(st.nextToken());
        day = Integer.parseInt(st.nextToken());
      }
      

      return fastDateCreate(targetCalendar, year, month, day);
    } catch (SQLException sqlEx) {
      throw sqlEx;
    } catch (Exception e) {
      SQLException sqlEx = SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_Date", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009", getExceptionInterceptor());
      


      sqlEx.initCause(e);
      
      throw sqlEx;
    }
  }
  
  private TimeZone getDefaultTimeZone() {
    return useLegacyDatetimeCode ? connection.getDefaultTimeZone() : serverTimeZoneTz;
  }
  









  public double getDouble(int columnIndex)
    throws SQLException
  {
    if (!isBinaryEncoded) {
      return getDoubleInternal(columnIndex);
    }
    
    return getNativeDouble(columnIndex);
  }
  



  public double getDouble(String columnName)
    throws SQLException
  {
    return getDouble(findColumn(columnName));
  }
  
  private final double getDoubleFromString(String stringVal, int columnIndex) throws SQLException {
    return getDoubleInternal(stringVal, columnIndex);
  }
  










  protected double getDoubleInternal(int colIndex)
    throws SQLException
  {
    return getDoubleInternal(getString(colIndex), colIndex);
  }
  




































































  public int getFetchDirection()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return fetchDirection;
    }
  }
  






  public int getFetchSize()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return fetchSize;
    }
  }
  

























  public float getFloat(int columnIndex)
    throws SQLException
  {
    if (!isBinaryEncoded) {
      String val = null;
      
      val = getString(columnIndex);
      
      return getFloatFromString(val, columnIndex);
    }
    
    return getNativeFloat(columnIndex);
  }
  



  public float getFloat(String columnName)
    throws SQLException
  {
    return getFloat(findColumn(columnName));
  }
  
  private final float getFloatFromString(String val, int columnIndex) throws SQLException {
    try {
      if (val != null) {
        if (val.length() == 0) {
          return convertToZeroWithEmptyCheck();
        }
        
        float f = Float.parseFloat(val);
        
        if ((jdbcCompliantTruncationForReads) && (
          (f == Float.MIN_VALUE) || (f == Float.MAX_VALUE))) {
          double valAsDouble = Double.parseDouble(val);
          


          if ((valAsDouble < 1.401298464324817E-45D - MIN_DIFF_PREC) || (valAsDouble > 3.4028234663852886E38D - MAX_DIFF_PREC)) {
            throwRangeException(String.valueOf(valAsDouble), columnIndex, 6);
          }
        }
        

        return f;
      }
      
      return 0.0F;
    } catch (NumberFormatException nfe) {
      try {
        Double valueAsDouble = new Double(val);
        float valueAsFloat = valueAsDouble.floatValue();
        
        if (jdbcCompliantTruncationForReads)
        {
          if (((jdbcCompliantTruncationForReads) && (valueAsFloat == Float.NEGATIVE_INFINITY)) || (valueAsFloat == Float.POSITIVE_INFINITY)) {
            throwRangeException(valueAsDouble.toString(), columnIndex, 6);
          }
        }
        
        return valueAsFloat;

      }
      catch (NumberFormatException newNfe)
      {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getFloat()_-____200") + val + Messages.getString("ResultSet.___in_column__201") + columnIndex, "S1009", getExceptionInterceptor());
      }
    }
  }
  









  public int getInt(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    if (!isBinaryEncoded) {
      int columnIndexMinusOne = columnIndex - 1;
      
      if (thisRow.isNull(columnIndexMinusOne)) {
        wasNullFlag = true;
        return 0;
      }
      wasNullFlag = false;
      
      if (fields[columnIndexMinusOne].getMysqlType() == 16) {
        long valueAsLong = getNumericRepresentationOfSQLBitType(columnIndex);
        
        if ((jdbcCompliantTruncationForReads) && ((valueAsLong < -2147483648L) || (valueAsLong > 2147483647L))) {
          throwRangeException(String.valueOf(valueAsLong), columnIndex, 4);
        }
        
        return (int)valueAsLong;
      }
      
      if (useFastIntParsing) {
        if (thisRow.length(columnIndexMinusOne) == 0L) {
          return convertToZeroWithEmptyCheck();
        }
        
        boolean needsFullParse = thisRow.isFloatingPointNumber(columnIndexMinusOne);
        
        if (!needsFullParse) {
          try {
            return getIntWithOverflowCheck(columnIndexMinusOne);
          } catch (NumberFormatException nfe) {
            try {
              return parseIntAsDouble(columnIndex, thisRow.getString(columnIndexMinusOne, fields[columnIndexMinusOne].getEncoding(), connection));

            }
            catch (NumberFormatException newNfe)
            {

              throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getInt()_-____74") + thisRow.getString(columnIndexMinusOne, fields[columnIndexMinusOne].getEncoding(), connection) + "'", "S1009", getExceptionInterceptor());
            }
          }
        }
      }
      


      String val = null;
      try {
        val = getString(columnIndex);
        if (val == null) {
          return 0;
        }
        
        if (val.length() == 0) {
          return convertToZeroWithEmptyCheck();
        }
        
        if ((val.indexOf("e") == -1) && (val.indexOf("E") == -1) && (val.indexOf(".") == -1)) {
          int intVal = Integer.parseInt(val);
          
          checkForIntegerTruncation(columnIndexMinusOne, null, intVal);
          
          return intVal;
        }
        

        int intVal = parseIntAsDouble(columnIndex, val);
        
        checkForIntegerTruncation(columnIndex, null, intVal);
        
        return intVal;
      }
      catch (NumberFormatException nfe) {
        try {
          return parseIntAsDouble(columnIndex, val);

        }
        catch (NumberFormatException newNfe)
        {
          throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getInt()_-____74") + val + "'", "S1009", getExceptionInterceptor());
        }
      }
    }
    
    return getNativeInt(columnIndex);
  }
  



  public int getInt(String columnName)
    throws SQLException
  {
    return getInt(findColumn(columnName));
  }
  
  private final int getIntFromString(String val, int columnIndex) throws SQLException {
    try {
      if (val != null)
      {
        if (val.length() == 0) {
          return convertToZeroWithEmptyCheck();
        }
        
        if ((val.indexOf("e") == -1) && (val.indexOf("E") == -1) && (val.indexOf(".") == -1))
        {






          val = val.trim();
          
          int valueAsInt = Integer.parseInt(val);
          
          if ((jdbcCompliantTruncationForReads) && (
            (valueAsInt == Integer.MIN_VALUE) || (valueAsInt == Integer.MAX_VALUE))) {
            long valueAsLong = Long.parseLong(val);
            
            if ((valueAsLong < -2147483648L) || (valueAsLong > 2147483647L)) {
              throwRangeException(String.valueOf(valueAsLong), columnIndex, 4);
            }
          }
          

          return valueAsInt;
        }
        


        double valueAsDouble = Double.parseDouble(val);
        
        if ((jdbcCompliantTruncationForReads) && (
          (valueAsDouble < -2.147483648E9D) || (valueAsDouble > 2.147483647E9D))) {
          throwRangeException(String.valueOf(valueAsDouble), columnIndex, 4);
        }
        

        return (int)valueAsDouble;
      }
      
      return 0;
    } catch (NumberFormatException nfe) {
      try {
        double valueAsDouble = Double.parseDouble(val);
        
        if ((jdbcCompliantTruncationForReads) && (
          (valueAsDouble < -2.147483648E9D) || (valueAsDouble > 2.147483647E9D))) {
          throwRangeException(String.valueOf(valueAsDouble), columnIndex, 4);
        }
        

        return (int)valueAsDouble;

      }
      catch (NumberFormatException newNfe)
      {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getInt()_-____206") + val + Messages.getString("ResultSet.___in_column__207") + columnIndex, "S1009", getExceptionInterceptor());
      }
    }
  }
  










  public long getLong(int columnIndex)
    throws SQLException
  {
    return getLong(columnIndex, true);
  }
  
  private long getLong(int columnIndex, boolean overflowCheck) throws SQLException {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    if (!isBinaryEncoded) {
      int columnIndexMinusOne = columnIndex - 1;
      
      if (thisRow.isNull(columnIndexMinusOne)) {
        wasNullFlag = true;
        return 0L;
      }
      wasNullFlag = false;
      
      if (fields[columnIndexMinusOne].getMysqlType() == 16) {
        return getNumericRepresentationOfSQLBitType(columnIndex);
      }
      
      if (useFastIntParsing) {
        if (thisRow.length(columnIndexMinusOne) == 0L) {
          return convertToZeroWithEmptyCheck();
        }
        
        boolean needsFullParse = thisRow.isFloatingPointNumber(columnIndexMinusOne);
        
        if (!needsFullParse) {
          try {
            return getLongWithOverflowCheck(columnIndexMinusOne, overflowCheck);
          } catch (NumberFormatException nfe) {
            try {
              return parseLongAsDouble(columnIndexMinusOne, thisRow.getString(columnIndexMinusOne, fields[columnIndexMinusOne].getEncoding(), connection));

            }
            catch (NumberFormatException newNfe)
            {

              throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getLong()_-____79") + thisRow.getString(columnIndexMinusOne, fields[columnIndexMinusOne].getEncoding(), connection) + "'", "S1009", getExceptionInterceptor());
            }
          }
        }
      }
      


      String val = null;
      try {
        val = getString(columnIndex);
        if (val == null) {
          return 0L;
        }
        
        if (val.length() == 0) {
          return convertToZeroWithEmptyCheck();
        }
        
        if ((val.indexOf("e") == -1) && (val.indexOf("E") == -1)) {
          return parseLongWithOverflowCheck(columnIndexMinusOne, null, val, overflowCheck);
        }
        

        return parseLongAsDouble(columnIndexMinusOne, val);
      }
      catch (NumberFormatException nfe) {
        try {
          return parseLongAsDouble(columnIndexMinusOne, val);

        }
        catch (NumberFormatException newNfe)
        {
          throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getLong()_-____79") + val + "'", "S1009", getExceptionInterceptor());
        }
      }
    }
    
    return getNativeLong(columnIndex, overflowCheck, true);
  }
  



  public long getLong(String columnName)
    throws SQLException
  {
    return getLong(findColumn(columnName));
  }
  
  private final long getLongFromString(String val, int columnIndexZeroBased) throws SQLException {
    try {
      if (val != null)
      {
        if (val.length() == 0) {
          return convertToZeroWithEmptyCheck();
        }
        
        if ((val.indexOf("e") == -1) && (val.indexOf("E") == -1)) {
          return parseLongWithOverflowCheck(columnIndexZeroBased, null, val, true);
        }
        

        return parseLongAsDouble(columnIndexZeroBased, val);
      }
      
      return 0L;
    }
    catch (NumberFormatException nfe) {
      try {
        return parseLongAsDouble(columnIndexZeroBased, val);

      }
      catch (NumberFormatException newNfe)
      {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getLong()_-____211") + val + Messages.getString("ResultSet.___in_column__212") + (columnIndexZeroBased + 1), "S1009", getExceptionInterceptor());
      }
    }
  }
  








  public java.sql.ResultSetMetaData getMetaData()
    throws SQLException
  {
    checkClosed();
    
    return new ResultSetMetaData(fields, connection.getUseOldAliasMetadataBehavior(), connection.getYearIsDateType(), getExceptionInterceptor());
  }
  











  protected Array getNativeArray(int i)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  





















  protected InputStream getNativeAsciiStream(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    
    return getNativeBinaryStream(columnIndex);
  }
  












  protected BigDecimal getNativeBigDecimal(int columnIndex)
    throws SQLException
  {
    checkColumnBounds(columnIndex);
    
    int scale = fields[(columnIndex - 1)].getDecimals();
    
    return getNativeBigDecimal(columnIndex, scale);
  }
  












  protected BigDecimal getNativeBigDecimal(int columnIndex, int scale)
    throws SQLException
  {
    checkColumnBounds(columnIndex);
    
    String stringVal = null;
    
    Field f = fields[(columnIndex - 1)];
    
    Object value = thisRow.getColumnValue(columnIndex - 1);
    
    if (value == null) {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    
    switch (f.getSQLType()) {
    case 2: 
    case 3: 
      stringVal = StringUtils.toAsciiString((byte[])value);
      break;
    default: 
      stringVal = getNativeString(columnIndex);
    }
    
    return getBigDecimalFromString(stringVal, columnIndex, scale);
  }
  















  protected InputStream getNativeBinaryStream(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    
    int columnIndexMinusOne = columnIndex - 1;
    
    if (thisRow.isNull(columnIndexMinusOne)) {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    
    switch (fields[columnIndexMinusOne].getSQLType()) {
    case -7: 
    case -4: 
    case -3: 
    case -2: 
    case 2004: 
      return thisRow.getBinaryInputStream(columnIndexMinusOne);
    }
    
    byte[] b = getNativeBytes(columnIndex, false);
    
    if (b != null) {
      return new ByteArrayInputStream(b);
    }
    
    return null;
  }
  









  protected java.sql.Blob getNativeBlob(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    
    checkColumnBounds(columnIndex);
    
    Object value = thisRow.getColumnValue(columnIndex - 1);
    
    if (value == null) {
      wasNullFlag = true;
    } else {
      wasNullFlag = false;
    }
    
    if (wasNullFlag) {
      return null;
    }
    
    int mysqlType = fields[(columnIndex - 1)].getMysqlType();
    
    byte[] dataAsBytes = null;
    
    switch (mysqlType) {
    case 249: 
    case 250: 
    case 251: 
    case 252: 
      dataAsBytes = (byte[])value;
      break;
    
    default: 
      dataAsBytes = getNativeBytes(columnIndex, false);
    }
    
    if (!connection.getEmulateLocators()) {
      return new Blob(dataAsBytes, getExceptionInterceptor());
    }
    
    return new BlobFromLocator(this, columnIndex, getExceptionInterceptor());
  }
  
  public static boolean arraysEqual(byte[] left, byte[] right) {
    if (left == null) {
      return right == null;
    }
    if (right == null) {
      return false;
    }
    if (left.length != right.length) {
      return false;
    }
    for (int i = 0; i < left.length; i++) {
      if (left[i] != right[i]) {
        return false;
      }
    }
    return true;
  }
  









  protected byte getNativeByte(int columnIndex)
    throws SQLException
  {
    return getNativeByte(columnIndex, true);
  }
  
  protected byte getNativeByte(int columnIndex, boolean overflowCheck) throws SQLException {
    checkRowPos();
    
    checkColumnBounds(columnIndex);
    
    Object value = thisRow.getColumnValue(columnIndex - 1);
    
    if (value == null) {
      wasNullFlag = true;
      
      return 0;
    }
    
    wasNullFlag = false;
    
    columnIndex--;
    
    Field field = fields[columnIndex];
    long valueAsLong;
    short valueAsShort; switch (field.getMysqlType()) {
    case 16: 
      valueAsLong = getNumericRepresentationOfSQLBitType(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && ((valueAsLong < -128L) || (valueAsLong > 127L))) {
        throwRangeException(String.valueOf(valueAsLong), columnIndex + 1, -6);
      }
      
      return (byte)(int)valueAsLong;
    case 1: 
      byte valueAsByte = ((byte[])(byte[])value)[0];
      
      if (!field.isUnsigned()) {
        return valueAsByte;
      }
      
      valueAsShort = valueAsByte >= 0 ? (short)valueAsByte : (short)(valueAsByte + 256);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && 
        (valueAsShort > 127)) {
        throwRangeException(String.valueOf(valueAsShort), columnIndex + 1, -6);
      }
      

      return (byte)valueAsShort;
    
    case 2: 
    case 13: 
      valueAsShort = getNativeShort(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsShort < -128) || (valueAsShort > 127))) {
        throwRangeException(String.valueOf(valueAsShort), columnIndex + 1, -6);
      }
      

      return (byte)valueAsShort;
    case 3: 
    case 9: 
      int valueAsInt = getNativeInt(columnIndex + 1, false);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsInt < -128) || (valueAsInt > 127))) {
        throwRangeException(String.valueOf(valueAsInt), columnIndex + 1, -6);
      }
      

      return (byte)valueAsInt;
    
    case 4: 
      float valueAsFloat = getNativeFloat(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsFloat < -128.0F) || (valueAsFloat > 127.0F)))
      {
        throwRangeException(String.valueOf(valueAsFloat), columnIndex + 1, -6);
      }
      

      return (byte)(int)valueAsFloat;
    
    case 5: 
      double valueAsDouble = getNativeDouble(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsDouble < -128.0D) || (valueAsDouble > 127.0D))) {
        throwRangeException(String.valueOf(valueAsDouble), columnIndex + 1, -6);
      }
      

      return (byte)(int)valueAsDouble;
    
    case 8: 
      valueAsLong = getNativeLong(columnIndex + 1, false, true);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsLong < -128L) || (valueAsLong > 127L))) {
        throwRangeException(String.valueOf(valueAsLong), columnIndex + 1, -6);
      }
      

      return (byte)(int)valueAsLong;
    }
    
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getByte()", columnIndex, thisRow.getColumnValue(columnIndex - 1), fields[columnIndex], new int[] { 5, 1, 2, 3, 8, 4 });
    }
    


    return getByteFromString(getNativeString(columnIndex + 1), columnIndex + 1);
  }
  














  protected byte[] getNativeBytes(int columnIndex, boolean noConversion)
    throws SQLException
  {
    checkRowPos();
    
    checkColumnBounds(columnIndex);
    
    Object value = thisRow.getColumnValue(columnIndex - 1);
    
    if (value == null) {
      wasNullFlag = true;
    } else {
      wasNullFlag = false;
    }
    
    if (wasNullFlag) {
      return null;
    }
    
    Field field = fields[(columnIndex - 1)];
    
    int mysqlType = field.getMysqlType();
    

    if (noConversion) {
      mysqlType = 252;
    }
    
    switch (mysqlType) {
    case 16: 
    case 249: 
    case 250: 
    case 251: 
    case 252: 
      return (byte[])value;
    
    case 15: 
    case 253: 
    case 254: 
      if ((value instanceof byte[])) {
        return (byte[])value;
      }
      
      break;
    }
    
    int sqlType = field.getSQLType();
    
    if ((sqlType == -3) || (sqlType == -2)) {
      return (byte[])value;
    }
    
    return getBytesFromString(getNativeString(columnIndex));
  }
  













  protected Reader getNativeCharacterStream(int columnIndex)
    throws SQLException
  {
    int columnIndexMinusOne = columnIndex - 1;
    
    switch (fields[columnIndexMinusOne].getSQLType()) {
    case -1: 
    case 1: 
    case 12: 
    case 2005: 
      if (thisRow.isNull(columnIndexMinusOne)) {
        wasNullFlag = true;
        
        return null;
      }
      
      wasNullFlag = false;
      
      return thisRow.getReader(columnIndexMinusOne);
    }
    
    String asString = getStringForClob(columnIndex);
    
    if (asString == null) {
      return null;
    }
    
    return getCharacterStreamFromString(asString);
  }
  









  protected java.sql.Clob getNativeClob(int columnIndex)
    throws SQLException
  {
    String stringVal = getStringForClob(columnIndex);
    
    if (stringVal == null) {
      return null;
    }
    
    return getClobFromString(stringVal);
  }
  
  private String getNativeConvertToString(int columnIndex, Field field) throws SQLException {
    synchronized (checkClosed().getConnectionMutex())
    {
      int sqlType = field.getSQLType();
      int mysqlType = field.getMysqlType();
      int intVal;
      long longVal; switch (sqlType) {
      case -7: 
        return String.valueOf(getNumericRepresentationOfSQLBitType(columnIndex));
      case 16: 
        boolean booleanVal = getBoolean(columnIndex);
        
        if (wasNullFlag) {
          return null;
        }
        
        return String.valueOf(booleanVal);
      
      case -6: 
        byte tinyintVal = getNativeByte(columnIndex, false);
        
        if (wasNullFlag) {
          return null;
        }
        
        if ((!field.isUnsigned()) || (tinyintVal >= 0)) {
          return String.valueOf(tinyintVal);
        }
        
        short unsignedTinyVal = (short)(tinyintVal & 0xFF);
        
        return String.valueOf(unsignedTinyVal);
      

      case 5: 
        intVal = getNativeInt(columnIndex, false);
        
        if (wasNullFlag) {
          return null;
        }
        
        if ((!field.isUnsigned()) || (intVal >= 0)) {
          return String.valueOf(intVal);
        }
        
        intVal &= 0xFFFF;
        
        return String.valueOf(intVal);
      
      case 4: 
        intVal = getNativeInt(columnIndex, false);
        
        if (wasNullFlag) {
          return null;
        }
        
        if ((!field.isUnsigned()) || (intVal >= 0) || (field.getMysqlType() == 9))
        {
          return String.valueOf(intVal);
        }
        
        longVal = intVal & 0xFFFFFFFF;
        
        return String.valueOf(longVal);
      

      case -5: 
        if (!field.isUnsigned()) {
          longVal = getNativeLong(columnIndex, false, true);
          
          if (wasNullFlag) {
            return null;
          }
          
          return String.valueOf(longVal);
        }
        
        long longVal = getNativeLong(columnIndex, false, false);
        
        if (wasNullFlag) {
          return null;
        }
        
        return String.valueOf(convertLongToUlong(longVal));
      case 7: 
        float floatVal = getNativeFloat(columnIndex);
        
        if (wasNullFlag) {
          return null;
        }
        
        return String.valueOf(floatVal);
      
      case 6: 
      case 8: 
        double doubleVal = getNativeDouble(columnIndex);
        
        if (wasNullFlag) {
          return null;
        }
        
        return String.valueOf(doubleVal);
      
      case 2: 
      case 3: 
        String stringVal = StringUtils.toAsciiString(thisRow.getColumnValue(columnIndex - 1));
        


        if (stringVal != null) {
          wasNullFlag = false;
          
          if (stringVal.length() == 0) {
            BigDecimal val = new BigDecimal(0);
            
            return val.toString();
          }
          BigDecimal val;
          try {
            val = new BigDecimal(stringVal);
          } catch (NumberFormatException ex) {
            throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009", getExceptionInterceptor());
          }
          


          return val.toString();
        }
        
        wasNullFlag = true;
        
        return null;
      

      case -1: 
      case 1: 
      case 12: 
        return extractStringFromNativeColumn(columnIndex, mysqlType);
      
      case -4: 
      case -3: 
      case -2: 
        if (!field.isBlob())
          return extractStringFromNativeColumn(columnIndex, mysqlType);
        if (!field.isBinary()) {
          return extractStringFromNativeColumn(columnIndex, mysqlType);
        }
        byte[] data = getBytes(columnIndex);
        Object obj = data;
        
        if ((connection.getAutoDeserialize()) && 
          (data != null) && (data.length >= 2)) {
          if ((data[0] == -84) && (data[1] == -19)) {
            try
            {
              ByteArrayInputStream bytesIn = new ByteArrayInputStream(data);
              ObjectInputStream objIn = new ObjectInputStream(bytesIn);
              obj = objIn.readObject();
              objIn.close();
              bytesIn.close();
            } catch (ClassNotFoundException cnfe) {
              throw SQLError.createSQLException(Messages.getString("ResultSet.Class_not_found___91") + cnfe.toString() + Messages.getString("ResultSet._while_reading_serialized_object_92"), getExceptionInterceptor());
            }
            catch (IOException ex) {
              obj = data;
            }
          }
          
          return obj.toString();
        }
        

        return extractStringFromNativeColumn(columnIndex, mysqlType);
      



      case 91: 
        if (mysqlType == 13) {
          short shortVal = getNativeShort(columnIndex);
          
          if (!connection.getYearIsDateType())
          {
            if (wasNullFlag) {
              return null;
            }
            
            return String.valueOf(shortVal);
          }
          
          if (field.getLength() == 2L)
          {
            if (shortVal <= 69) {
              shortVal = (short)(shortVal + 100);
            }
            
            shortVal = (short)(shortVal + 1900);
          }
          
          return fastDateCreate(null, shortVal, 1, 1).toString();
        }
        

        if (connection.getNoDatetimeStringSync()) {
          byte[] asBytes = getNativeBytes(columnIndex, true);
          
          if (asBytes == null) {
            return null;
          }
          
          if (asBytes.length == 0)
          {


            return "0000-00-00";
          }
          
          int year = asBytes[0] & 0xFF | (asBytes[1] & 0xFF) << 8;
          int month = asBytes[2];
          int day = asBytes[3];
          
          if ((year == 0) && (month == 0) && (day == 0)) {
            return "0000-00-00";
          }
        }
        
        Date dt = getNativeDate(columnIndex);
        
        if (dt == null) {
          return null;
        }
        
        return String.valueOf(dt);
      
      case 92: 
        Time tm = getNativeTime(columnIndex, null, connection.getDefaultTimeZone(), false);
        
        if (tm == null) {
          return null;
        }
        
        return String.valueOf(tm);
      
      case 93: 
        if (connection.getNoDatetimeStringSync()) {
          byte[] asBytes = getNativeBytes(columnIndex, true);
          
          if (asBytes == null) {
            return null;
          }
          
          if (asBytes.length == 0)
          {


            return "0000-00-00 00:00:00";
          }
          
          int year = asBytes[0] & 0xFF | (asBytes[1] & 0xFF) << 8;
          int month = asBytes[2];
          int day = asBytes[3];
          
          if ((year == 0) && (month == 0) && (day == 0)) {
            return "0000-00-00 00:00:00";
          }
        }
        
        Timestamp tstamp = getNativeTimestamp(columnIndex, null, connection.getDefaultTimeZone(), false);
        
        if (tstamp == null) {
          return null;
        }
        
        String result = String.valueOf(tstamp);
        
        if (!connection.getNoDatetimeStringSync()) {
          return result;
        }
        
        if (result.endsWith(".0")) {
          return result.substring(0, result.length() - 2);
        }
        return extractStringFromNativeColumn(columnIndex, mysqlType);
      }
      
      return extractStringFromNativeColumn(columnIndex, mysqlType);
    }
  }
  










  protected Date getNativeDate(int columnIndex)
    throws SQLException
  {
    return getNativeDate(columnIndex, null);
  }
  














  protected Date getNativeDate(int columnIndex, Calendar cal)
    throws SQLException
  {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    int columnIndexMinusOne = columnIndex - 1;
    
    int mysqlType = fields[columnIndexMinusOne].getMysqlType();
    
    Date dateToReturn = null;
    
    if (mysqlType == 10)
    {
      dateToReturn = thisRow.getNativeDate(columnIndexMinusOne, connection, this, cal);
    } else {
      TimeZone tz = cal != null ? cal.getTimeZone() : getDefaultTimeZone();
      
      boolean rollForward = (tz != null) && (!tz.equals(getDefaultTimeZone()));
      
      dateToReturn = (Date)thisRow.getNativeDateTimeValue(columnIndexMinusOne, null, 91, mysqlType, tz, rollForward, connection, this);
    }
    





    if (dateToReturn == null)
    {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    
    return dateToReturn;
  }
  
  Date getNativeDateViaParseConversion(int columnIndex) throws SQLException {
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getDate()", columnIndex, thisRow.getColumnValue(columnIndex - 1), fields[(columnIndex - 1)], new int[] { 10 });
    }
    

    String stringVal = getNativeString(columnIndex);
    
    return getDateFromString(stringVal, columnIndex, null);
  }
  









  protected double getNativeDouble(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    columnIndex--;
    
    if (thisRow.isNull(columnIndex)) {
      wasNullFlag = true;
      
      return 0.0D;
    }
    
    wasNullFlag = false;
    
    Field f = fields[columnIndex];
    
    switch (f.getMysqlType()) {
    case 5: 
      return thisRow.getNativeDouble(columnIndex);
    case 1: 
      if (!f.isUnsigned()) {
        return getNativeByte(columnIndex + 1);
      }
      
      return getNativeShort(columnIndex + 1);
    case 2: 
    case 13: 
      if (!f.isUnsigned()) {
        return getNativeShort(columnIndex + 1);
      }
      
      return getNativeInt(columnIndex + 1);
    case 3: 
    case 9: 
      if (!f.isUnsigned()) {
        return getNativeInt(columnIndex + 1);
      }
      
      return getNativeLong(columnIndex + 1);
    case 8: 
      long valueAsLong = getNativeLong(columnIndex + 1);
      
      if (!f.isUnsigned()) {
        return valueAsLong;
      }
      
      BigInteger asBigInt = convertLongToUlong(valueAsLong);
      


      return asBigInt.doubleValue();
    case 4: 
      return getNativeFloat(columnIndex + 1);
    case 16: 
      return getNumericRepresentationOfSQLBitType(columnIndex + 1);
    }
    String stringVal = getNativeString(columnIndex + 1);
    
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getDouble()", columnIndex, stringVal, fields[columnIndex], new int[] { 5, 1, 2, 3, 8, 4 });
    }
    


    return getDoubleFromString(stringVal, columnIndex + 1);
  }
  










  protected float getNativeFloat(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    columnIndex--;
    
    if (thisRow.isNull(columnIndex)) {
      wasNullFlag = true;
      
      return 0.0F;
    }
    
    wasNullFlag = false;
    
    Field f = fields[columnIndex];
    long valueAsLong;
    switch (f.getMysqlType()) {
    case 16: 
      valueAsLong = getNumericRepresentationOfSQLBitType(columnIndex + 1);
      
      return (float)valueAsLong;
    


    case 5: 
      Double valueAsDouble = new Double(getNativeDouble(columnIndex + 1));
      
      float valueAsFloat = valueAsDouble.floatValue();
      
      if (((jdbcCompliantTruncationForReads) && (valueAsFloat == Float.NEGATIVE_INFINITY)) || (valueAsFloat == Float.POSITIVE_INFINITY)) {
        throwRangeException(valueAsDouble.toString(), columnIndex + 1, 6);
      }
      
      return (float)getNativeDouble(columnIndex + 1);
    case 1: 
      if (!f.isUnsigned()) {
        return getNativeByte(columnIndex + 1);
      }
      
      return getNativeShort(columnIndex + 1);
    case 2: 
    case 13: 
      if (!f.isUnsigned()) {
        return getNativeShort(columnIndex + 1);
      }
      
      return getNativeInt(columnIndex + 1);
    case 3: 
    case 9: 
      if (!f.isUnsigned()) {
        return getNativeInt(columnIndex + 1);
      }
      
      return (float)getNativeLong(columnIndex + 1);
    case 8: 
      valueAsLong = getNativeLong(columnIndex + 1);
      
      if (!f.isUnsigned()) {
        return (float)valueAsLong;
      }
      
      BigInteger asBigInt = convertLongToUlong(valueAsLong);
      


      return asBigInt.floatValue();
    
    case 4: 
      return thisRow.getNativeFloat(columnIndex);
    }
    
    String stringVal = getNativeString(columnIndex + 1);
    
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getFloat()", columnIndex, stringVal, fields[columnIndex], new int[] { 5, 1, 2, 3, 8, 4 });
    }
    


    return getFloatFromString(stringVal, columnIndex + 1);
  }
  










  protected int getNativeInt(int columnIndex)
    throws SQLException
  {
    return getNativeInt(columnIndex, true);
  }
  
  protected int getNativeInt(int columnIndex, boolean overflowCheck) throws SQLException {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    columnIndex--;
    
    if (thisRow.isNull(columnIndex)) {
      wasNullFlag = true;
      
      return 0;
    }
    
    wasNullFlag = false;
    
    Field f = fields[columnIndex];
    long valueAsLong;
    double valueAsDouble; switch (f.getMysqlType()) {
    case 16: 
      valueAsLong = getNumericRepresentationOfSQLBitType(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && ((valueAsLong < -2147483648L) || (valueAsLong > 2147483647L))) {
        throwRangeException(String.valueOf(valueAsLong), columnIndex + 1, 4);
      }
      
      return (int)valueAsLong;
    case 1: 
      byte tinyintVal = getNativeByte(columnIndex + 1, false);
      
      if ((!f.isUnsigned()) || (tinyintVal >= 0)) {
        return tinyintVal;
      }
      
      return tinyintVal + 256;
    case 2: 
    case 13: 
      short asShort = getNativeShort(columnIndex + 1, false);
      
      if ((!f.isUnsigned()) || (asShort >= 0)) {
        return asShort;
      }
      
      return asShort + 65536;
    
    case 3: 
    case 9: 
      int valueAsInt = thisRow.getNativeInt(columnIndex);
      
      if (!f.isUnsigned()) {
        return valueAsInt;
      }
      
      valueAsLong = valueAsInt >= 0 ? valueAsInt : valueAsInt + 4294967296L;
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (valueAsLong > 2147483647L)) {
        throwRangeException(String.valueOf(valueAsLong), columnIndex + 1, 4);
      }
      
      return (int)valueAsLong;
    case 8: 
      valueAsLong = getNativeLong(columnIndex + 1, false, true);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsLong < -2147483648L) || (valueAsLong > 2147483647L))) {
        throwRangeException(String.valueOf(valueAsLong), columnIndex + 1, 4);
      }
      

      return (int)valueAsLong;
    case 5: 
      valueAsDouble = getNativeDouble(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsDouble < -2.147483648E9D) || (valueAsDouble > 2.147483647E9D))) {
        throwRangeException(String.valueOf(valueAsDouble), columnIndex + 1, 4);
      }
      

      return (int)valueAsDouble;
    case 4: 
      valueAsDouble = getNativeFloat(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsDouble < -2.147483648E9D) || (valueAsDouble > 2.147483647E9D))) {
        throwRangeException(String.valueOf(valueAsDouble), columnIndex + 1, 4);
      }
      

      return (int)valueAsDouble;
    }
    
    String stringVal = getNativeString(columnIndex + 1);
    
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getInt()", columnIndex, stringVal, fields[columnIndex], new int[] { 5, 1, 2, 3, 8, 4 });
    }
    


    return getIntFromString(stringVal, columnIndex + 1);
  }
  










  protected long getNativeLong(int columnIndex)
    throws SQLException
  {
    return getNativeLong(columnIndex, true, true);
  }
  
  protected long getNativeLong(int columnIndex, boolean overflowCheck, boolean expandUnsignedLong) throws SQLException {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    columnIndex--;
    
    if (thisRow.isNull(columnIndex)) {
      wasNullFlag = true;
      
      return 0L;
    }
    
    wasNullFlag = false;
    
    Field f = fields[columnIndex];
    double valueAsDouble;
    switch (f.getMysqlType()) {
    case 16: 
      return getNumericRepresentationOfSQLBitType(columnIndex + 1);
    case 1: 
      if (!f.isUnsigned()) {
        return getNativeByte(columnIndex + 1);
      }
      
      return getNativeInt(columnIndex + 1);
    case 2: 
      if (!f.isUnsigned()) {
        return getNativeShort(columnIndex + 1);
      }
      
      return getNativeInt(columnIndex + 1, false);
    
    case 13: 
      return getNativeShort(columnIndex + 1);
    case 3: 
    case 9: 
      int asInt = getNativeInt(columnIndex + 1, false);
      
      if ((!f.isUnsigned()) || (asInt >= 0)) {
        return asInt;
      }
      
      return asInt + 4294967296L;
    case 8: 
      long valueAsLong = thisRow.getNativeLong(columnIndex);
      
      if ((!f.isUnsigned()) || (!expandUnsignedLong)) {
        return valueAsLong;
      }
      
      BigInteger asBigInt = convertLongToUlong(valueAsLong);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && ((asBigInt.compareTo(new BigInteger(String.valueOf(Long.MAX_VALUE))) > 0) || (asBigInt.compareTo(new BigInteger(String.valueOf(Long.MIN_VALUE))) < 0)))
      {
        throwRangeException(asBigInt.toString(), columnIndex + 1, -5);
      }
      
      return getLongFromString(asBigInt.toString(), columnIndex);
    
    case 5: 
      valueAsDouble = getNativeDouble(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsDouble < -9.223372036854776E18D) || (valueAsDouble > 9.223372036854776E18D))) {
        throwRangeException(String.valueOf(valueAsDouble), columnIndex + 1, -5);
      }
      

      return valueAsDouble;
    case 4: 
      valueAsDouble = getNativeFloat(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsDouble < -9.223372036854776E18D) || (valueAsDouble > 9.223372036854776E18D))) {
        throwRangeException(String.valueOf(valueAsDouble), columnIndex + 1, -5);
      }
      

      return valueAsDouble;
    }
    String stringVal = getNativeString(columnIndex + 1);
    
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getLong()", columnIndex, stringVal, fields[columnIndex], new int[] { 5, 1, 2, 3, 8, 4 });
    }
    


    return getLongFromString(stringVal, columnIndex + 1);
  }
  











  protected Ref getNativeRef(int i)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  









  protected short getNativeShort(int columnIndex)
    throws SQLException
  {
    return getNativeShort(columnIndex, true);
  }
  
  protected short getNativeShort(int columnIndex, boolean overflowCheck) throws SQLException {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    columnIndex--;
    
    if (thisRow.isNull(columnIndex)) {
      wasNullFlag = true;
      
      return 0;
    }
    
    wasNullFlag = false;
    
    Field f = fields[columnIndex];
    long valueAsLong;
    int valueAsInt; switch (f.getMysqlType()) {
    case 16: 
      valueAsLong = getNumericRepresentationOfSQLBitType(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && ((valueAsLong < -32768L) || (valueAsLong > 32767L))) {
        throwRangeException(String.valueOf(valueAsLong), columnIndex + 1, 5);
      }
      
      return (short)(int)valueAsLong;
    
    case 1: 
      byte tinyintVal = getNativeByte(columnIndex + 1, false);
      
      if ((!f.isUnsigned()) || (tinyintVal >= 0)) {
        return (short)tinyintVal;
      }
      
      return (short)(tinyintVal + 256);
    
    case 2: 
    case 13: 
      short asShort = thisRow.getNativeShort(columnIndex);
      
      if (!f.isUnsigned()) {
        return asShort;
      }
      
      valueAsInt = asShort & 0xFFFF;
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (valueAsInt > 32767)) {
        throwRangeException(String.valueOf(valueAsInt), columnIndex + 1, 5);
      }
      
      return (short)valueAsInt;
    case 3: 
    case 9: 
      if (!f.isUnsigned()) {
        valueAsInt = getNativeInt(columnIndex + 1, false);
        
        if (((overflowCheck) && (jdbcCompliantTruncationForReads) && (valueAsInt > 32767)) || (valueAsInt < 32768)) {
          throwRangeException(String.valueOf(valueAsInt), columnIndex + 1, 5);
        }
        
        return (short)valueAsInt;
      }
      
      valueAsLong = getNativeLong(columnIndex + 1, false, true);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (valueAsLong > 32767L)) {
        throwRangeException(String.valueOf(valueAsLong), columnIndex + 1, 5);
      }
      
      return (short)(int)valueAsLong;
    
    case 8: 
      valueAsLong = getNativeLong(columnIndex + 1, false, false);
      
      if (!f.isUnsigned()) {
        if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
          (valueAsLong < -32768L) || (valueAsLong > 32767L))) {
          throwRangeException(String.valueOf(valueAsLong), columnIndex + 1, 5);
        }
        

        return (short)(int)valueAsLong;
      }
      
      BigInteger asBigInt = convertLongToUlong(valueAsLong);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && ((asBigInt.compareTo(new BigInteger(String.valueOf(32767))) > 0) || (asBigInt.compareTo(new BigInteger(String.valueOf(32768))) < 0)))
      {
        throwRangeException(asBigInt.toString(), columnIndex + 1, 5);
      }
      
      return (short)getIntFromString(asBigInt.toString(), columnIndex + 1);
    
    case 5: 
      double valueAsDouble = getNativeDouble(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsDouble < -32768.0D) || (valueAsDouble > 32767.0D))) {
        throwRangeException(String.valueOf(valueAsDouble), columnIndex + 1, 5);
      }
      

      return (short)(int)valueAsDouble;
    case 4: 
      float valueAsFloat = getNativeFloat(columnIndex + 1);
      
      if ((overflowCheck) && (jdbcCompliantTruncationForReads) && (
        (valueAsFloat < -32768.0F) || (valueAsFloat > 32767.0F))) {
        throwRangeException(String.valueOf(valueAsFloat), columnIndex + 1, 5);
      }
      

      return (short)(int)valueAsFloat;
    }
    String stringVal = getNativeString(columnIndex + 1);
    
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getShort()", columnIndex, stringVal, fields[columnIndex], new int[] { 5, 1, 2, 3, 8, 4 });
    }
    


    return getShortFromString(stringVal, columnIndex + 1);
  }
  










  protected String getNativeString(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    if (fields == null) {
      throw SQLError.createSQLException(Messages.getString("ResultSet.Query_generated_no_fields_for_ResultSet_133"), "S1002", getExceptionInterceptor());
    }
    

    if (thisRow.isNull(columnIndex - 1)) {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    
    String stringVal = null;
    
    Field field = fields[(columnIndex - 1)];
    

    stringVal = getNativeConvertToString(columnIndex, field);
    int mysqlType = field.getMysqlType();
    
    if ((mysqlType != 7) && (mysqlType != 10) && (field.isZeroFill()) && (stringVal != null)) {
      int origLength = stringVal.length();
      
      StringBuilder zeroFillBuf = new StringBuilder(origLength);
      
      long numZeros = field.getLength() - origLength;
      
      for (long i = 0L; i < numZeros; i += 1L) {
        zeroFillBuf.append('0');
      }
      
      zeroFillBuf.append(stringVal);
      
      stringVal = zeroFillBuf.toString();
    }
    
    return stringVal;
  }
  
  private Time getNativeTime(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward) throws SQLException {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    int columnIndexMinusOne = columnIndex - 1;
    
    int mysqlType = fields[columnIndexMinusOne].getMysqlType();
    
    Time timeVal = null;
    
    if (mysqlType == 11) {
      timeVal = thisRow.getNativeTime(columnIndexMinusOne, targetCalendar, tz, rollForward, connection, this);
    }
    else {
      timeVal = (Time)thisRow.getNativeDateTimeValue(columnIndexMinusOne, null, 92, mysqlType, tz, rollForward, connection, this);
    }
    





    if (timeVal == null)
    {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    
    return timeVal;
  }
  
  Time getNativeTimeViaParseConversion(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward) throws SQLException {
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getTime()", columnIndex, thisRow.getColumnValue(columnIndex - 1), fields[(columnIndex - 1)], new int[] { 11 });
    }
    

    String strTime = getNativeString(columnIndex);
    
    return getTimeFromString(strTime, targetCalendar, columnIndex, tz, rollForward);
  }
  
  private Timestamp getNativeTimestamp(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward) throws SQLException {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    int columnIndexMinusOne = columnIndex - 1;
    
    Timestamp tsVal = null;
    
    int mysqlType = fields[columnIndexMinusOne].getMysqlType();
    
    switch (mysqlType) {
    case 7: 
    case 12: 
      tsVal = thisRow.getNativeTimestamp(columnIndexMinusOne, targetCalendar, tz, rollForward, connection, this);
      break;
    

    default: 
      tsVal = (Timestamp)thisRow.getNativeDateTimeValue(columnIndexMinusOne, null, 93, mysqlType, tz, rollForward, connection, this);
    }
    
    





    if (tsVal == null)
    {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    
    return tsVal;
  }
  
  Timestamp getNativeTimestampViaParseConversion(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward) throws SQLException {
    if (useUsageAdvisor) {
      issueConversionViaParsingWarning("getTimestamp()", columnIndex, thisRow.getColumnValue(columnIndex - 1), fields[(columnIndex - 1)], new int[] { 7, 12 });
    }
    

    String strTimestamp = getNativeString(columnIndex);
    
    return getTimestampFromString(columnIndex, targetCalendar, strTimestamp, tz, rollForward);
  }
  



















  protected InputStream getNativeUnicodeStream(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    
    return getBinaryStream(columnIndex);
  }
  

  protected URL getNativeURL(int colIndex)
    throws SQLException
  {
    String val = getString(colIndex);
    
    if (val == null) {
      return null;
    }
    try
    {
      return new URL(val);
    } catch (MalformedURLException mfe) {
      throw SQLError.createSQLException(Messages.getString("ResultSet.Malformed_URL____141") + val + "'", "S1009", getExceptionInterceptor());
    }
  }
  



  public synchronized ResultSetInternalMethods getNextResultSet()
  {
    return nextResultSet;
  }
  


















  public Object getObject(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    int columnIndexMinusOne = columnIndex - 1;
    
    if (thisRow.isNull(columnIndexMinusOne)) {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    

    Field field = fields[columnIndexMinusOne];
    String stringVal;
    switch (field.getSQLType()) {
    case -7: 
      if ((field.getMysqlType() == 16) && (!field.isSingleBit())) {
        return getObjectDeserializingIfNeeded(columnIndex);
      }
      return Boolean.valueOf(getBoolean(columnIndex));
    
    case 16: 
      return Boolean.valueOf(getBoolean(columnIndex));
    
    case -6: 
      if (!field.isUnsigned()) {
        return Integer.valueOf(getByte(columnIndex));
      }
      
      return Integer.valueOf(getInt(columnIndex));
    

    case 5: 
      return Integer.valueOf(getInt(columnIndex));
    

    case 4: 
      if ((!field.isUnsigned()) || (field.getMysqlType() == 9)) {
        return Integer.valueOf(getInt(columnIndex));
      }
      
      return Long.valueOf(getLong(columnIndex));
    

    case -5: 
      if (!field.isUnsigned()) {
        return Long.valueOf(getLong(columnIndex));
      }
      
      stringVal = getString(columnIndex);
      
      if (stringVal == null) {
        return null;
      }
      try
      {
        return new BigInteger(stringVal);
      } catch (NumberFormatException nfe) {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_BigInteger", new Object[] { Integer.valueOf(columnIndex), stringVal }), "S1009", getExceptionInterceptor());
      }
    


    case 2: 
    case 3: 
      stringVal = getString(columnIndex);
      


      if (stringVal != null) {
        if (stringVal.length() == 0) {
          BigDecimal val = new BigDecimal(0);
          
          return val;
        }
        BigDecimal val;
        try {
          val = new BigDecimal(stringVal);
        } catch (NumberFormatException ex) {
          throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009", getExceptionInterceptor());
        }
        


        return val;
      }
      
      return null;
    
    case 7: 
      return new Float(getFloat(columnIndex));
    
    case 6: 
    case 8: 
      return new Double(getDouble(columnIndex));
    
    case 1: 
    case 12: 
      if (!field.isOpaqueBinary()) {
        return getString(columnIndex);
      }
      
      return getBytes(columnIndex);
    case -1: 
      if (!field.isOpaqueBinary()) {
        return getStringForClob(columnIndex);
      }
      
      return getBytes(columnIndex);
    
    case -4: 
    case -3: 
    case -2: 
      if (field.getMysqlType() == 255) {
        return getBytes(columnIndex);
      }
      return getObjectDeserializingIfNeeded(columnIndex);
    
    case 91: 
      if ((field.getMysqlType() == 13) && (!connection.getYearIsDateType())) {
        return Short.valueOf(getShort(columnIndex));
      }
      
      return getDate(columnIndex);
    
    case 92: 
      return getTime(columnIndex);
    
    case 93: 
      return getTimestamp(columnIndex);
    }
    
    return getString(columnIndex);
  }
  
  private Object getObjectDeserializingIfNeeded(int columnIndex) throws SQLException
  {
    Field field = fields[(columnIndex - 1)];
    
    if ((field.isBinary()) || (field.isBlob())) {
      byte[] data = getBytes(columnIndex);
      
      if (connection.getAutoDeserialize()) {
        Object obj = data;
        
        if ((data != null) && (data.length >= 2)) {
          if ((data[0] == -84) && (data[1] == -19)) {
            try
            {
              ByteArrayInputStream bytesIn = new ByteArrayInputStream(data);
              ObjectInputStream objIn = new ObjectInputStream(bytesIn);
              obj = objIn.readObject();
              objIn.close();
              bytesIn.close();
            } catch (ClassNotFoundException cnfe) {
              throw SQLError.createSQLException(Messages.getString("ResultSet.Class_not_found___91") + cnfe.toString() + Messages.getString("ResultSet._while_reading_serialized_object_92"), getExceptionInterceptor());
            }
            catch (IOException ex) {
              obj = data;
            }
          } else {
            return getString(columnIndex);
          }
        }
        
        return obj;
      }
      
      return data;
    }
    
    return getBytes(columnIndex);
  }
  
  public <T> T getObject(int columnIndex, Class<T> type) throws SQLException
  {
    if (type == null) {
      throw SQLError.createSQLException("Type parameter can not be null", "S1009", getExceptionInterceptor());
    }
    
    if (type.equals(String.class))
      return getString(columnIndex);
    if (type.equals(BigDecimal.class))
      return getBigDecimal(columnIndex);
    if ((type.equals(Boolean.class)) || (type.equals(Boolean.TYPE)))
      return Boolean.valueOf(getBoolean(columnIndex));
    if ((type.equals(Integer.class)) || (type.equals(Integer.TYPE)))
      return Integer.valueOf(getInt(columnIndex));
    if ((type.equals(Long.class)) || (type.equals(Long.TYPE)))
      return Long.valueOf(getLong(columnIndex));
    if ((type.equals(Float.class)) || (type.equals(Float.TYPE)))
      return Float.valueOf(getFloat(columnIndex));
    if ((type.equals(Double.class)) || (type.equals(Double.TYPE)))
      return Double.valueOf(getDouble(columnIndex));
    if (type.equals([B.class))
      return getBytes(columnIndex);
    if (type.equals(Date.class))
      return getDate(columnIndex);
    if (type.equals(Time.class))
      return getTime(columnIndex);
    if (type.equals(Timestamp.class))
      return getTimestamp(columnIndex);
    if (type.equals(Clob.class))
      return getClob(columnIndex);
    if (type.equals(Blob.class))
      return getBlob(columnIndex);
    if (type.equals(Array.class))
      return getArray(columnIndex);
    if (type.equals(Ref.class))
      return getRef(columnIndex);
    if (type.equals(URL.class)) {
      return getURL(columnIndex);
    }
    if (connection.getAutoDeserialize()) {
      try {
        return type.cast(getObject(columnIndex));
      } catch (ClassCastException cce) {
        SQLException sqlEx = SQLError.createSQLException("Conversion not supported for type " + type.getName(), "S1009", getExceptionInterceptor());
        
        sqlEx.initCause(cce);
        
        throw sqlEx;
      }
    }
    
    throw SQLError.createSQLException("Conversion not supported for type " + type.getName(), "S1009", getExceptionInterceptor());
  }
  

  public <T> T getObject(String columnLabel, Class<T> type)
    throws SQLException
  {
    return getObject(findColumn(columnLabel), type);
  }
  













  public Object getObject(int i, Map<String, Class<?>> map)
    throws SQLException
  {
    return getObject(i);
  }
  


















  public Object getObject(String columnName)
    throws SQLException
  {
    return getObject(findColumn(columnName));
  }
  













  public Object getObject(String colName, Map<String, Class<?>> map)
    throws SQLException
  {
    return getObject(findColumn(colName), map);
  }
  
  public Object getObjectStoredProc(int columnIndex, int desiredSqlType) throws SQLException {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    Object value = thisRow.getColumnValue(columnIndex - 1);
    
    if (value == null) {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    

    Field field = fields[(columnIndex - 1)];
    
    switch (desiredSqlType)
    {
    case -7: 
    case 16: 
      return Boolean.valueOf(getBoolean(columnIndex));
    
    case -6: 
      return Integer.valueOf(getInt(columnIndex));
    
    case 5: 
      return Integer.valueOf(getInt(columnIndex));
    

    case 4: 
      if ((!field.isUnsigned()) || (field.getMysqlType() == 9)) {
        return Integer.valueOf(getInt(columnIndex));
      }
      
      return Long.valueOf(getLong(columnIndex));
    

    case -5: 
      if (field.isUnsigned()) {
        return getBigDecimal(columnIndex);
      }
      
      return Long.valueOf(getLong(columnIndex));
    

    case 2: 
    case 3: 
      String stringVal = getString(columnIndex);
      

      if (stringVal != null) {
        if (stringVal.length() == 0) {
          BigDecimal val = new BigDecimal(0);
          
          return val;
        }
        BigDecimal val;
        try {
          val = new BigDecimal(stringVal);
        } catch (NumberFormatException ex) {
          throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_BigDecimal", new Object[] { stringVal, Integer.valueOf(columnIndex) }), "S1009", getExceptionInterceptor());
        }
        


        return val;
      }
      
      return null;
    
    case 7: 
      return new Float(getFloat(columnIndex));
    

    case 6: 
      if (!connection.getRunningCTS13()) {
        return new Double(getFloat(columnIndex));
      }
      return new Float(getFloat(columnIndex));
    

    case 8: 
      return new Double(getDouble(columnIndex));
    
    case 1: 
    case 12: 
      return getString(columnIndex);
    case -1: 
      return getStringForClob(columnIndex);
    case -4: 
    case -3: 
    case -2: 
      return getBytes(columnIndex);
    
    case 91: 
      if ((field.getMysqlType() == 13) && (!connection.getYearIsDateType())) {
        return Short.valueOf(getShort(columnIndex));
      }
      
      return getDate(columnIndex);
    
    case 92: 
      return getTime(columnIndex);
    
    case 93: 
      return getTimestamp(columnIndex);
    }
    
    return getString(columnIndex);
  }
  
  public Object getObjectStoredProc(int i, Map<Object, Object> map, int desiredSqlType) throws SQLException
  {
    return getObjectStoredProc(i, desiredSqlType);
  }
  
  public Object getObjectStoredProc(String columnName, int desiredSqlType) throws SQLException {
    return getObjectStoredProc(findColumn(columnName), desiredSqlType);
  }
  
  public Object getObjectStoredProc(String colName, Map<Object, Object> map, int desiredSqlType) throws SQLException {
    return getObjectStoredProc(findColumn(colName), map, desiredSqlType);
  }
  










  public Ref getRef(int i)
    throws SQLException
  {
    checkColumnBounds(i);
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  










  public Ref getRef(String colName)
    throws SQLException
  {
    return getRef(findColumn(colName));
  }
  










  public int getRow()
    throws SQLException
  {
    checkClosed();
    
    int currentRowNumber = rowData.getCurrentRowNumber();
    int row = 0;
    

    if (!rowData.isDynamic()) {
      if ((currentRowNumber < 0) || (rowData.isAfterLast()) || (rowData.isEmpty())) {
        row = 0;
      } else {
        row = currentRowNumber + 1;
      }
    }
    else {
      row = currentRowNumber + 1;
    }
    
    return row;
  }
  














  private long getNumericRepresentationOfSQLBitType(int columnIndex)
    throws SQLException
  {
    Object value = thisRow.getColumnValue(columnIndex - 1);
    
    if ((fields[(columnIndex - 1)].isSingleBit()) || (((byte[])value).length == 1)) {
      return ((byte[])(byte[])value)[0];
    }
    
    byte[] asBytes = (byte[])value;
    
    int shift = 0;
    
    long[] steps = new long[asBytes.length];
    
    for (int i = asBytes.length - 1; i >= 0; i--) {
      steps[i] = ((asBytes[i] & 0xFF) << shift);
      shift += 8;
    }
    
    long valueAsLong = 0L;
    
    for (int i = 0; i < asBytes.length; i++) {
      valueAsLong |= steps[i];
    }
    
    return valueAsLong;
  }
  









  public short getShort(int columnIndex)
    throws SQLException
  {
    checkRowPos();
    checkColumnBounds(columnIndex);
    
    if (!isBinaryEncoded) {
      if (thisRow.isNull(columnIndex - 1)) {
        wasNullFlag = true;
        return 0;
      }
      wasNullFlag = false;
      
      if (fields[(columnIndex - 1)].getMysqlType() == 16) {
        long valueAsLong = getNumericRepresentationOfSQLBitType(columnIndex);
        
        if ((jdbcCompliantTruncationForReads) && ((valueAsLong < -32768L) || (valueAsLong > 32767L))) {
          throwRangeException(String.valueOf(valueAsLong), columnIndex, 5);
        }
        
        return (short)(int)valueAsLong;
      }
      
      if (useFastIntParsing) {
        byte[] shortAsBytes = thisRow.getColumnValue(columnIndex - 1);
        
        if (shortAsBytes.length == 0) {
          return (short)convertToZeroWithEmptyCheck();
        }
        
        boolean needsFullParse = false;
        
        for (int i = 0; i < shortAsBytes.length; i++) {
          if (((char)shortAsBytes[i] == 'e') || ((char)shortAsBytes[i] == 'E')) {
            needsFullParse = true;
            
            break;
          }
        }
        
        if (!needsFullParse) {
          try {
            return parseShortWithOverflowCheck(columnIndex, shortAsBytes, null);
          } catch (NumberFormatException nfe) {
            try {
              return parseShortAsDouble(columnIndex, StringUtils.toString(shortAsBytes));

            }
            catch (NumberFormatException newNfe)
            {
              throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getShort()_-____96") + StringUtils.toString(shortAsBytes) + "'", "S1009", getExceptionInterceptor());
            }
          }
        }
      }
      

      String val = null;
      try {
        val = getString(columnIndex);
        if (val == null) {
          return 0;
        }
        
        if (val.length() == 0) {
          return (short)convertToZeroWithEmptyCheck();
        }
        
        if ((val.indexOf("e") == -1) && (val.indexOf("E") == -1) && (val.indexOf(".") == -1)) {
          return parseShortWithOverflowCheck(columnIndex, null, val);
        }
        

        return parseShortAsDouble(columnIndex, val);
      }
      catch (NumberFormatException nfe) {
        try {
          return parseShortAsDouble(columnIndex, val);

        }
        catch (NumberFormatException newNfe)
        {
          throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getShort()_-____96") + val + "'", "S1009", getExceptionInterceptor());
        }
      }
    }
    
    return getNativeShort(columnIndex);
  }
  



  public short getShort(String columnName)
    throws SQLException
  {
    return getShort(findColumn(columnName));
  }
  
  private final short getShortFromString(String val, int columnIndex) throws SQLException {
    try {
      if (val != null)
      {
        if (val.length() == 0) {
          return (short)convertToZeroWithEmptyCheck();
        }
        
        if ((val.indexOf("e") == -1) && (val.indexOf("E") == -1) && (val.indexOf(".") == -1)) {
          return parseShortWithOverflowCheck(columnIndex, null, val);
        }
        

        return parseShortAsDouble(columnIndex, val);
      }
      
      return 0;
    } catch (NumberFormatException nfe) {
      try {
        return parseShortAsDouble(columnIndex, val);

      }
      catch (NumberFormatException newNfe)
      {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Invalid_value_for_getShort()_-____217") + val + Messages.getString("ResultSet.___in_column__218") + columnIndex, "S1009", getExceptionInterceptor());
      }
    }
  }
  






  public Statement getStatement()
    throws SQLException
  {
    try
    {
      synchronized (checkClosed().getConnectionMutex()) {
        if (wrapperStatement != null) {
          return wrapperStatement;
        }
        
        return owningStatement;
      }
      











      return owningStatement;
    }
    catch (SQLException sqlEx)
    {
      if (!retainOwningStatement) {
        throw SQLError.createSQLException("Operation not allowed on closed ResultSet. Statements can be retained over result set closure by setting the connection property \"retainStatementAfterResultSetClose\" to \"true\".", "S1000", getExceptionInterceptor());
      }
      


      if (wrapperStatement != null) {
        return wrapperStatement;
      }
    }
  }
  












  public String getString(int columnIndex)
    throws SQLException
  {
    String stringVal = getStringInternal(columnIndex, true);
    
    if ((padCharsWithSpace) && (stringVal != null)) {
      Field f = fields[(columnIndex - 1)];
      
      if (f.getMysqlType() == 254) {
        int fieldLength = (int)f.getLength() / f.getMaxBytesPerCharacter();
        
        int currentLength = stringVal.length();
        
        if (currentLength < fieldLength) {
          StringBuilder paddedBuf = new StringBuilder(fieldLength);
          paddedBuf.append(stringVal);
          
          int difference = fieldLength - currentLength;
          
          paddedBuf.append(EMPTY_SPACE, 0, difference);
          
          stringVal = paddedBuf.toString();
        }
      }
    }
    
    return stringVal;
  }
  










  public String getString(String columnName)
    throws SQLException
  {
    return getString(findColumn(columnName));
  }
  
  private String getStringForClob(int columnIndex) throws SQLException {
    String asString = null;
    
    String forcedEncoding = connection.getClobCharacterEncoding();
    
    if (forcedEncoding == null) {
      if (!isBinaryEncoded) {
        asString = getString(columnIndex);
      } else {
        asString = getNativeString(columnIndex);
      }
    } else {
      try {
        byte[] asBytes = null;
        
        if (!isBinaryEncoded) {
          asBytes = getBytes(columnIndex);
        } else {
          asBytes = getNativeBytes(columnIndex, true);
        }
        
        if (asBytes != null) {
          asString = StringUtils.toString(asBytes, forcedEncoding);
        }
      } catch (UnsupportedEncodingException uee) {
        throw SQLError.createSQLException("Unsupported character encoding " + forcedEncoding, "S1009", getExceptionInterceptor());
      }
    }
    

    return asString;
  }
  
  protected String getStringInternal(int columnIndex, boolean checkDateTypes) throws SQLException {
    if (!isBinaryEncoded) {
      checkRowPos();
      checkColumnBounds(columnIndex);
      
      if (fields == null) {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Query_generated_no_fields_for_ResultSet_99"), "S1002", getExceptionInterceptor());
      }
      



      int internalColumnIndex = columnIndex - 1;
      
      if (thisRow.isNull(internalColumnIndex)) {
        wasNullFlag = true;
        
        return null;
      }
      
      wasNullFlag = false;
      
      Field metadata = fields[internalColumnIndex];
      
      String stringVal = null;
      
      if (metadata.getMysqlType() == 16) {
        if (metadata.isSingleBit()) {
          byte[] value = thisRow.getColumnValue(internalColumnIndex);
          
          if (value.length == 0) {
            return String.valueOf(convertToZeroWithEmptyCheck());
          }
          
          return String.valueOf(value[0]);
        }
        
        return String.valueOf(getNumericRepresentationOfSQLBitType(columnIndex));
      }
      
      String encoding = metadata.getEncoding();
      
      stringVal = thisRow.getString(internalColumnIndex, encoding, connection);
      




      if (metadata.getMysqlType() == 13) {
        if (!connection.getYearIsDateType()) {
          return stringVal;
        }
        
        Date dt = getDateFromString(stringVal, columnIndex, null);
        
        if (dt == null) {
          wasNullFlag = true;
          
          return null;
        }
        
        wasNullFlag = false;
        
        return dt.toString();
      }
      


      if ((checkDateTypes) && (!connection.getNoDatetimeStringSync())) {
        switch (metadata.getSQLType()) {
        case 92: 
          Time tm = getTimeFromString(stringVal, null, columnIndex, getDefaultTimeZone(), false);
          
          if (tm == null) {
            wasNullFlag = true;
            
            return null;
          }
          
          wasNullFlag = false;
          
          return tm.toString();
        
        case 91: 
          Date dt = getDateFromString(stringVal, columnIndex, null);
          
          if (dt == null) {
            wasNullFlag = true;
            
            return null;
          }
          
          wasNullFlag = false;
          
          return dt.toString();
        case 93: 
          Timestamp ts = getTimestampFromString(columnIndex, null, stringVal, getDefaultTimeZone(), false);
          
          if (ts == null) {
            wasNullFlag = true;
            
            return null;
          }
          
          wasNullFlag = false;
          
          return ts.toString();
        }
        
      }
      

      return stringVal;
    }
    
    return getNativeString(columnIndex);
  }
  









  public Time getTime(int columnIndex)
    throws SQLException
  {
    return getTimeInternal(columnIndex, null, getDefaultTimeZone(), false);
  }
  













  public Time getTime(int columnIndex, Calendar cal)
    throws SQLException
  {
    return getTimeInternal(columnIndex, cal, cal != null ? cal.getTimeZone() : getDefaultTimeZone(), true);
  }
  









  public Time getTime(String columnName)
    throws SQLException
  {
    return getTime(findColumn(columnName));
  }
  













  public Time getTime(String columnName, Calendar cal)
    throws SQLException
  {
    return getTime(findColumn(columnName), cal);
  }
  
  private Time getTimeFromString(String timeAsString, Calendar targetCalendar, int columnIndex, TimeZone tz, boolean rollForward) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      int hr = 0;
      int min = 0;
      int sec = 0;
      
      try
      {
        if (timeAsString == null) {
          wasNullFlag = true;
          
          return null;
        }
        







        timeAsString = timeAsString.trim();
        

        int dec = timeAsString.indexOf(".");
        if (dec > -1) {
          timeAsString = timeAsString.substring(0, dec);
        }
        
        if ((timeAsString.equals("0")) || (timeAsString.equals("0000-00-00")) || (timeAsString.equals("0000-00-00 00:00:00")) || (timeAsString.equals("00000000000000")))
        {
          if ("convertToNull".equals(connection.getZeroDateTimeBehavior())) {
            wasNullFlag = true;
            
            return null; }
          if ("exception".equals(connection.getZeroDateTimeBehavior())) {
            throw SQLError.createSQLException("Value '" + timeAsString + "' can not be represented as java.sql.Time", "S1009", getExceptionInterceptor());
          }
          


          return fastTimeCreate(targetCalendar, 0, 0, 0);
        }
        
        wasNullFlag = false;
        
        Field timeColField = fields[(columnIndex - 1)];
        
        if (timeColField.getMysqlType() == 7)
        {
          int length = timeAsString.length();
          
          switch (length)
          {
          case 19: 
            hr = Integer.parseInt(timeAsString.substring(length - 8, length - 6));
            min = Integer.parseInt(timeAsString.substring(length - 5, length - 3));
            sec = Integer.parseInt(timeAsString.substring(length - 2, length));
            

            break;
          case 12: 
          case 14: 
            hr = Integer.parseInt(timeAsString.substring(length - 6, length - 4));
            min = Integer.parseInt(timeAsString.substring(length - 4, length - 2));
            sec = Integer.parseInt(timeAsString.substring(length - 2, length));
            

            break;
          
          case 10: 
            hr = Integer.parseInt(timeAsString.substring(6, 8));
            min = Integer.parseInt(timeAsString.substring(8, 10));
            sec = 0;
            

            break;
          case 11: case 13: case 15: case 16: 
          case 17: case 18: default: 
            throw SQLError.createSQLException(Messages.getString("ResultSet.Timestamp_too_small_to_convert_to_Time_value_in_column__257") + columnIndex + "(" + fields[(columnIndex - 1)] + ").", "S1009", getExceptionInterceptor());
          }
          
          
          SQLWarning precisionLost = new SQLWarning(Messages.getString("ResultSet.Precision_lost_converting_TIMESTAMP_to_Time_with_getTime()_on_column__261") + columnIndex + "(" + fields[(columnIndex - 1)] + ").");
          


          if (warningChain == null) {
            warningChain = precisionLost;
          } else {
            warningChain.setNextWarning(precisionLost);
          }
        } else if (timeColField.getMysqlType() == 12) {
          hr = Integer.parseInt(timeAsString.substring(11, 13));
          min = Integer.parseInt(timeAsString.substring(14, 16));
          sec = Integer.parseInt(timeAsString.substring(17, 19));
          
          SQLWarning precisionLost = new SQLWarning(Messages.getString("ResultSet.Precision_lost_converting_DATETIME_to_Time_with_getTime()_on_column__264") + columnIndex + "(" + fields[(columnIndex - 1)] + ").");
          


          if (warningChain == null) {
            warningChain = precisionLost;
          } else
            warningChain.setNextWarning(precisionLost);
        } else {
          if (timeColField.getMysqlType() == 10) {
            return fastTimeCreate(targetCalendar, 0, 0, 0);
          }
          

          if ((timeAsString.length() != 5) && (timeAsString.length() != 8)) {
            throw SQLError.createSQLException(Messages.getString("ResultSet.Bad_format_for_Time____267") + timeAsString + Messages.getString("ResultSet.___in_column__268") + columnIndex, "S1009", getExceptionInterceptor());
          }
          




          hr = Integer.parseInt(timeAsString.substring(0, 2));
          min = Integer.parseInt(timeAsString.substring(3, 5));
          sec = timeAsString.length() == 5 ? 0 : Integer.parseInt(timeAsString.substring(6));
        }
        
        Calendar sessionCalendar = getCalendarInstanceForSessionOrNew();
        
        return TimeUtil.changeTimezone(connection, sessionCalendar, targetCalendar, fastTimeCreate(sessionCalendar, hr, min, sec), connection.getServerTimezoneTZ(), tz, rollForward);
      }
      catch (RuntimeException ex) {
        SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", getExceptionInterceptor());
        sqlEx.initCause(ex);
        
        throw sqlEx;
      }
    }
  }
  












  private Time getTimeInternal(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward)
    throws SQLException
  {
    checkRowPos();
    
    if (isBinaryEncoded) {
      return getNativeTime(columnIndex, targetCalendar, tz, rollForward);
    }
    
    if (!useFastDateParsing) {
      String timeAsString = getStringInternal(columnIndex, false);
      
      return getTimeFromString(timeAsString, targetCalendar, columnIndex, tz, rollForward);
    }
    
    checkColumnBounds(columnIndex);
    
    int columnIndexMinusOne = columnIndex - 1;
    
    if (thisRow.isNull(columnIndexMinusOne)) {
      wasNullFlag = true;
      
      return null;
    }
    
    wasNullFlag = false;
    
    return thisRow.getTimeFast(columnIndexMinusOne, targetCalendar, tz, rollForward, connection, this);
  }
  










  public Timestamp getTimestamp(int columnIndex)
    throws SQLException
  {
    return getTimestampInternal(columnIndex, null, getDefaultTimeZone(), false);
  }
  














  public Timestamp getTimestamp(int columnIndex, Calendar cal)
    throws SQLException
  {
    return getTimestampInternal(columnIndex, cal, cal != null ? cal.getTimeZone() : getDefaultTimeZone(), true);
  }
  



  public Timestamp getTimestamp(String columnName)
    throws SQLException
  {
    return getTimestamp(findColumn(columnName));
  }
  














  public Timestamp getTimestamp(String columnName, Calendar cal)
    throws SQLException
  {
    return getTimestamp(findColumn(columnName), cal);
  }
  
  private Timestamp getTimestampFromString(int columnIndex, Calendar targetCalendar, String timestampValue, TimeZone tz, boolean rollForward) throws SQLException
  {
    try {
      wasNullFlag = false;
      
      if (timestampValue == null) {
        wasNullFlag = true;
        
        return null;
      }
      







      timestampValue = timestampValue.trim();
      
      int length = timestampValue.length();
      
      Calendar sessionCalendar = connection.getUseJDBCCompliantTimezoneShift() ? connection.getUtcCalendar() : getCalendarInstanceForSessionOrNew();
      

      if ((length > 0) && (timestampValue.charAt(0) == '0') && ((timestampValue.equals("0000-00-00")) || (timestampValue.equals("0000-00-00 00:00:00")) || (timestampValue.equals("00000000000000")) || (timestampValue.equals("0"))))
      {

        if ("convertToNull".equals(connection.getZeroDateTimeBehavior())) {
          wasNullFlag = true;
          
          return null; }
        if ("exception".equals(connection.getZeroDateTimeBehavior())) {
          throw SQLError.createSQLException("Value '" + timestampValue + "' can not be represented as java.sql.Timestamp", "S1009", getExceptionInterceptor());
        }
        


        return fastTimestampCreate(null, 1, 1, 1, 0, 0, 0, 0);
      }
      if (fields[(columnIndex - 1)].getMysqlType() == 13)
      {
        if (!useLegacyDatetimeCode) {
          return TimeUtil.fastTimestampCreate(tz, Integer.parseInt(timestampValue.substring(0, 4)), 1, 1, 0, 0, 0, 0);
        }
        
        return TimeUtil.changeTimezone(connection, sessionCalendar, targetCalendar, fastTimestampCreate(sessionCalendar, Integer.parseInt(timestampValue.substring(0, 4)), 1, 1, 0, 0, 0, 0), connection.getServerTimezoneTZ(), tz, rollForward);
      }
      




      int year = 0;
      int month = 0;
      int day = 0;
      int hour = 0;
      int minutes = 0;
      int seconds = 0;
      int nanos = 0;
      

      int decimalIndex = timestampValue.indexOf(".");
      
      if (decimalIndex == length - 1)
      {
        length--;
      }
      else if (decimalIndex != -1)
      {
        if (decimalIndex + 2 <= length) {
          nanos = Integer.parseInt(timestampValue.substring(decimalIndex + 1));
          
          int numDigits = length - (decimalIndex + 1);
          
          if (numDigits < 9) {
            int factor = (int)Math.pow(10.0D, 9 - numDigits);
            nanos *= factor;
          }
          
          length = decimalIndex;
        } else {
          throw new IllegalArgumentException();
        }
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
        year = Integer.parseInt(timestampValue.substring(0, 4));
        month = Integer.parseInt(timestampValue.substring(5, 7));
        day = Integer.parseInt(timestampValue.substring(8, 10));
        hour = Integer.parseInt(timestampValue.substring(11, 13));
        minutes = Integer.parseInt(timestampValue.substring(14, 16));
        seconds = Integer.parseInt(timestampValue.substring(17, 19));
        
        break;
      

      case 14: 
        year = Integer.parseInt(timestampValue.substring(0, 4));
        month = Integer.parseInt(timestampValue.substring(4, 6));
        day = Integer.parseInt(timestampValue.substring(6, 8));
        hour = Integer.parseInt(timestampValue.substring(8, 10));
        minutes = Integer.parseInt(timestampValue.substring(10, 12));
        seconds = Integer.parseInt(timestampValue.substring(12, 14));
        
        break;
      

      case 12: 
        year = Integer.parseInt(timestampValue.substring(0, 2));
        
        if (year <= 69) {
          year += 100;
        }
        
        year += 1900;
        
        month = Integer.parseInt(timestampValue.substring(2, 4));
        day = Integer.parseInt(timestampValue.substring(4, 6));
        hour = Integer.parseInt(timestampValue.substring(6, 8));
        minutes = Integer.parseInt(timestampValue.substring(8, 10));
        seconds = Integer.parseInt(timestampValue.substring(10, 12));
        
        break;
      

      case 10: 
        if ((fields[(columnIndex - 1)].getMysqlType() == 10) || (timestampValue.indexOf("-") != -1)) {
          year = Integer.parseInt(timestampValue.substring(0, 4));
          month = Integer.parseInt(timestampValue.substring(5, 7));
          day = Integer.parseInt(timestampValue.substring(8, 10));
          hour = 0;
          minutes = 0;
        } else {
          year = Integer.parseInt(timestampValue.substring(0, 2));
          
          if (year <= 69) {
            year += 100;
          }
          
          month = Integer.parseInt(timestampValue.substring(2, 4));
          day = Integer.parseInt(timestampValue.substring(4, 6));
          hour = Integer.parseInt(timestampValue.substring(6, 8));
          minutes = Integer.parseInt(timestampValue.substring(8, 10));
          
          year += 1900;
        }
        
        break;
      

      case 8: 
        if (timestampValue.indexOf(":") != -1) {
          hour = Integer.parseInt(timestampValue.substring(0, 2));
          minutes = Integer.parseInt(timestampValue.substring(3, 5));
          seconds = Integer.parseInt(timestampValue.substring(6, 8));
          year = 1970;
          month = 1;
          day = 1;
        }
        else
        {
          year = Integer.parseInt(timestampValue.substring(0, 4));
          month = Integer.parseInt(timestampValue.substring(4, 6));
          day = Integer.parseInt(timestampValue.substring(6, 8));
          
          year -= 1900;
          month--;
        }
        break;
      

      case 6: 
        year = Integer.parseInt(timestampValue.substring(0, 2));
        
        if (year <= 69) {
          year += 100;
        }
        
        year += 1900;
        
        month = Integer.parseInt(timestampValue.substring(2, 4));
        day = Integer.parseInt(timestampValue.substring(4, 6));
        
        break;
      

      case 4: 
        year = Integer.parseInt(timestampValue.substring(0, 2));
        
        if (year <= 69) {
          year += 100;
        }
        
        year += 1900;
        
        month = Integer.parseInt(timestampValue.substring(2, 4));
        
        day = 1;
        
        break;
      

      case 2: 
        year = Integer.parseInt(timestampValue.substring(0, 2));
        
        if (year <= 69) {
          year += 100;
        }
        
        year += 1900;
        month = 1;
        day = 1;
        
        break;
      case 3: case 5: case 7: case 9: 
      case 11: case 13: case 15: case 16: 
      case 17: case 18: default: 
        throw new SQLException("Bad format for Timestamp '" + timestampValue + "' in column " + columnIndex + ".", "S1009");
      }
      
      
      if (!useLegacyDatetimeCode) {
        return TimeUtil.fastTimestampCreate(tz, year, month, day, hour, minutes, seconds, nanos);
      }
      
      return TimeUtil.changeTimezone(connection, sessionCalendar, targetCalendar, fastTimestampCreate(sessionCalendar, year, month, day, hour, minutes, seconds, nanos), connection.getServerTimezoneTZ(), tz, rollForward);

    }
    catch (RuntimeException e)
    {
      SQLException sqlEx = SQLError.createSQLException("Cannot convert value '" + timestampValue + "' from column " + columnIndex + " to TIMESTAMP.", "S1009", getExceptionInterceptor());
      
      sqlEx.initCause(e);
      
      throw sqlEx;
    }
  }
  













  private Timestamp getTimestampInternal(int columnIndex, Calendar targetCalendar, TimeZone tz, boolean rollForward)
    throws SQLException
  {
    if (isBinaryEncoded) {
      return getNativeTimestamp(columnIndex, targetCalendar, tz, rollForward);
    }
    
    Timestamp tsVal = null;
    
    if (!useFastDateParsing) {
      String timestampValue = getStringInternal(columnIndex, false);
      
      tsVal = getTimestampFromString(columnIndex, targetCalendar, timestampValue, tz, rollForward);
    } else {
      checkClosed();
      checkRowPos();
      checkColumnBounds(columnIndex);
      
      tsVal = thisRow.getTimestampFast(columnIndex - 1, targetCalendar, tz, rollForward, connection, this);
    }
    
    if (tsVal == null) {
      wasNullFlag = true;
    } else {
      wasNullFlag = false;
    }
    
    return tsVal;
  }
  








  public int getType()
    throws SQLException
  {
    return resultSetType;
  }
  
















  @Deprecated
  public InputStream getUnicodeStream(int columnIndex)
    throws SQLException
  {
    if (!isBinaryEncoded) {
      checkRowPos();
      
      return getBinaryStream(columnIndex);
    }
    
    return getNativeBinaryStream(columnIndex);
  }
  





  @Deprecated
  public InputStream getUnicodeStream(String columnName)
    throws SQLException
  {
    return getUnicodeStream(findColumn(columnName));
  }
  
  public long getUpdateCount() {
    return updateCount;
  }
  
  public long getUpdateID() {
    return updateId;
  }
  

  public URL getURL(int colIndex)
    throws SQLException
  {
    String val = getString(colIndex);
    
    if (val == null) {
      return null;
    }
    try
    {
      return new URL(val);
    } catch (MalformedURLException mfe) {
      throw SQLError.createSQLException(Messages.getString("ResultSet.Malformed_URL____104") + val + "'", "S1009", getExceptionInterceptor());
    }
  }
  


  public URL getURL(String colName)
    throws SQLException
  {
    String val = getString(colName);
    
    if (val == null) {
      return null;
    }
    try
    {
      return new URL(val);
    } catch (MalformedURLException mfe) {
      throw SQLError.createSQLException(Messages.getString("ResultSet.Malformed_URL____107") + val + "'", "S1009", getExceptionInterceptor());
    }
  }
  


















  public SQLWarning getWarnings()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return warningChain;
    }
  }
  








  public void insertRow()
    throws SQLException
  {
    throw new NotUpdatable();
  }
  











  public boolean isAfterLast()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      boolean b = rowData.isAfterLast();
      
      return b;
    }
  }
  











  public boolean isBeforeFirst()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return rowData.isBeforeFirst();
    }
  }
  










  public boolean isFirst()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return rowData.isFirst();
    }
  }
  











  public boolean isLast()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return rowData.isLast();
    }
  }
  




  private void issueConversionViaParsingWarning(String methodName, int columnIndex, Object value, Field fieldInfo, int[] typesWithNoParseConversion)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      StringBuilder originalQueryBuf = new StringBuilder();
      
      if ((owningStatement != null) && ((owningStatement instanceof PreparedStatement))) {
        originalQueryBuf.append(Messages.getString("ResultSet.CostlyConversionCreatedFromQuery"));
        originalQueryBuf.append(owningStatement).originalSql);
        originalQueryBuf.append("\n\n");
      } else {
        originalQueryBuf.append(".");
      }
      
      StringBuilder convertibleTypesBuf = new StringBuilder();
      
      for (int i = 0; i < typesWithNoParseConversion.length; i++) {
        convertibleTypesBuf.append(MysqlDefs.typeToName(typesWithNoParseConversion[i]));
        convertibleTypesBuf.append("\n");
      }
      
      String message = Messages.getString("ResultSet.CostlyConversion", new Object[] { methodName, Integer.valueOf(columnIndex + 1), fieldInfo.getOriginalName(), fieldInfo.getOriginalTableName(), originalQueryBuf.toString(), value != null ? value.getClass().getName() : ResultSetMetaData.getClassNameForJavaType(fieldInfo.getSQLType(), fieldInfo.isUnsigned(), fieldInfo.getMysqlType(), (fieldInfo.isBinary()) || (fieldInfo.isBlob()) ? 1 : false, fieldInfo.isOpaqueBinary(), connection.getYearIsDateType()), MysqlDefs.typeToName(fieldInfo.getMysqlType()), convertibleTypesBuf.toString() });
      






      eventSink.consumeEvent(new ProfilerEvent((byte)0, "", owningStatement == null ? "N/A" : owningStatement.currentCatalog, connectionId, owningStatement == null ? -1 : owningStatement.getId(), resultId, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, message));
    }
  }
  














  public boolean last()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      boolean b = true;
      
      if (rowData.size() == 0) {
        b = false;
      }
      else {
        if (onInsertRow) {
          onInsertRow = false;
        }
        
        if (doingUpdates) {
          doingUpdates = false;
        }
        
        if (thisRow != null) {
          thisRow.closeOpenStreams();
        }
        
        rowData.beforeLast();
        thisRow = rowData.next();
      }
      
      setRowPositionValidity();
      
      return b;
    }
  }
  















  public void moveToCurrentRow()
    throws SQLException
  {
    throw new NotUpdatable();
  }
  














  public void moveToInsertRow()
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public boolean next()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (onInsertRow) {
        onInsertRow = false;
      }
      
      if (doingUpdates) {
        doingUpdates = false;
      }
      


      if (!reallyResult()) {
        throw SQLError.createSQLException(Messages.getString("ResultSet.ResultSet_is_from_UPDATE._No_Data_115"), "S1000", getExceptionInterceptor());
      }
      

      if (thisRow != null)
        thisRow.closeOpenStreams();
      boolean b;
      boolean b;
      if (rowData.size() == 0) {
        b = false;
      } else {
        thisRow = rowData.next();
        boolean b;
        if (thisRow == null) {
          b = false;
        } else {
          clearWarnings();
          
          b = true;
        }
      }
      

      setRowPositionValidity();
      
      return b;
    }
  }
  
  private int parseIntAsDouble(int columnIndex, String val) throws NumberFormatException, SQLException {
    if (val == null) {
      return 0;
    }
    
    double valueAsDouble = Double.parseDouble(val);
    
    if ((jdbcCompliantTruncationForReads) && (
      (valueAsDouble < -2.147483648E9D) || (valueAsDouble > 2.147483647E9D))) {
      throwRangeException(String.valueOf(valueAsDouble), columnIndex, 4);
    }
    

    return (int)valueAsDouble;
  }
  
  private int getIntWithOverflowCheck(int columnIndex) throws SQLException {
    int intValue = thisRow.getInt(columnIndex);
    
    checkForIntegerTruncation(columnIndex, null, intValue);
    
    return intValue;
  }
  
  private void checkForIntegerTruncation(int columnIndex, byte[] valueAsBytes, int intValue) throws SQLException {
    if ((jdbcCompliantTruncationForReads) && (
      (intValue == Integer.MIN_VALUE) || (intValue == Integer.MAX_VALUE))) {
      String valueAsString = null;
      
      if (valueAsBytes == null) {
        valueAsString = thisRow.getString(columnIndex, fields[columnIndex].getEncoding(), connection);
      }
      
      long valueAsLong = Long.parseLong(valueAsString == null ? StringUtils.toString(valueAsBytes) : valueAsString);
      
      if ((valueAsLong < -2147483648L) || (valueAsLong > 2147483647L)) {
        throwRangeException(valueAsString == null ? StringUtils.toString(valueAsBytes) : valueAsString, columnIndex + 1, 4);
      }
    }
  }
  
  private long parseLongAsDouble(int columnIndexZeroBased, String val) throws NumberFormatException, SQLException
  {
    if (val == null) {
      return 0L;
    }
    
    double valueAsDouble = Double.parseDouble(val);
    
    if ((jdbcCompliantTruncationForReads) && (
      (valueAsDouble < -9.223372036854776E18D) || (valueAsDouble > 9.223372036854776E18D))) {
      throwRangeException(val, columnIndexZeroBased + 1, -5);
    }
    

    return valueAsDouble;
  }
  
  private long getLongWithOverflowCheck(int columnIndexZeroBased, boolean doOverflowCheck) throws SQLException {
    long longValue = thisRow.getLong(columnIndexZeroBased);
    
    if (doOverflowCheck) {
      checkForLongTruncation(columnIndexZeroBased, null, longValue);
    }
    
    return longValue;
  }
  
  private long parseLongWithOverflowCheck(int columnIndexZeroBased, byte[] valueAsBytes, String valueAsString, boolean doCheck)
    throws NumberFormatException, SQLException
  {
    long longValue = 0L;
    
    if ((valueAsBytes == null) && (valueAsString == null)) {
      return 0L;
    }
    
    if (valueAsBytes != null) {
      longValue = StringUtils.getLong(valueAsBytes);



    }
    else
    {


      valueAsString = valueAsString.trim();
      
      longValue = Long.parseLong(valueAsString);
    }
    
    if ((doCheck) && (jdbcCompliantTruncationForReads)) {
      checkForLongTruncation(columnIndexZeroBased, valueAsBytes, longValue);
    }
    
    return longValue;
  }
  
  private void checkForLongTruncation(int columnIndexZeroBased, byte[] valueAsBytes, long longValue) throws SQLException {
    if ((longValue == Long.MIN_VALUE) || (longValue == Long.MAX_VALUE)) {
      String valueAsString = null;
      
      if (valueAsBytes == null) {
        valueAsString = thisRow.getString(columnIndexZeroBased, fields[columnIndexZeroBased].getEncoding(), connection);
      }
      
      double valueAsDouble = Double.parseDouble(valueAsString == null ? StringUtils.toString(valueAsBytes) : valueAsString);
      
      if ((valueAsDouble < -9.223372036854776E18D) || (valueAsDouble > 9.223372036854776E18D)) {
        throwRangeException(valueAsString == null ? StringUtils.toString(valueAsBytes) : valueAsString, columnIndexZeroBased + 1, -5);
      }
    }
  }
  
  private short parseShortAsDouble(int columnIndex, String val) throws NumberFormatException, SQLException {
    if (val == null) {
      return 0;
    }
    
    double valueAsDouble = Double.parseDouble(val);
    
    if ((jdbcCompliantTruncationForReads) && (
      (valueAsDouble < -32768.0D) || (valueAsDouble > 32767.0D))) {
      throwRangeException(String.valueOf(valueAsDouble), columnIndex, 5);
    }
    

    return (short)(int)valueAsDouble;
  }
  
  private short parseShortWithOverflowCheck(int columnIndex, byte[] valueAsBytes, String valueAsString) throws NumberFormatException, SQLException
  {
    short shortValue = 0;
    
    if ((valueAsBytes == null) && (valueAsString == null)) {
      return 0;
    }
    
    if (valueAsBytes != null) {
      shortValue = StringUtils.getShort(valueAsBytes);



    }
    else
    {


      valueAsString = valueAsString.trim();
      
      shortValue = Short.parseShort(valueAsString);
    }
    
    if ((jdbcCompliantTruncationForReads) && (
      (shortValue == Short.MIN_VALUE) || (shortValue == Short.MAX_VALUE))) {
      long valueAsLong = Long.parseLong(valueAsString == null ? StringUtils.toString(valueAsBytes) : valueAsString);
      
      if ((valueAsLong < -32768L) || (valueAsLong > 32767L)) {
        throwRangeException(valueAsString == null ? StringUtils.toString(valueAsBytes) : valueAsString, columnIndex, 5);
      }
    }
    

    return shortValue;
  }
  

















  public boolean prev()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      int rowIndex = rowData.getCurrentRowNumber();
      
      if (thisRow != null) {
        thisRow.closeOpenStreams();
      }
      
      boolean b = true;
      
      if (rowIndex - 1 >= 0) {
        rowIndex--;
        rowData.setCurrentRow(rowIndex);
        thisRow = rowData.getAt(rowIndex);
        
        b = true;
      } else if (rowIndex - 1 == -1) {
        rowIndex--;
        rowData.setCurrentRow(rowIndex);
        thisRow = null;
        
        b = false;
      } else {
        b = false;
      }
      
      setRowPositionValidity();
      
      return b;
    }
  }
  















  public boolean previous()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (onInsertRow) {
        onInsertRow = false;
      }
      
      if (doingUpdates) {
        doingUpdates = false;
      }
      
      return prev();
    }
  }
  








  public void realClose(boolean calledExplicitly)
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
      try
      {
        if (useUsageAdvisor)
        {


          if (!calledExplicitly) {
            eventSink.consumeEvent(new ProfilerEvent((byte)0, "", owningStatement == null ? "N/A" : owningStatement.currentCatalog, connectionId, owningStatement == null ? -1 : owningStatement.getId(), resultId, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, Messages.getString("ResultSet.ResultSet_implicitly_closed_by_driver")));
          }
          



          if ((rowData instanceof RowDataStatic))
          {


            if (rowData.size() > connection.getResultSetSizeThreshold()) {
              eventSink.consumeEvent(new ProfilerEvent((byte)0, "", owningStatement == null ? Messages.getString("ResultSet.N/A_159") : owningStatement.currentCatalog, connectionId, owningStatement == null ? -1 : owningStatement.getId(), resultId, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, Messages.getString("ResultSet.Too_Large_Result_Set", new Object[] { Integer.valueOf(rowData.size()), Integer.valueOf(connection.getResultSetSizeThreshold()) })));
            }
            





            if ((!isLast()) && (!isAfterLast()) && (rowData.size() != 0))
            {
              eventSink.consumeEvent(new ProfilerEvent((byte)0, "", owningStatement == null ? Messages.getString("ResultSet.N/A_159") : owningStatement.currentCatalog, connectionId, owningStatement == null ? -1 : owningStatement.getId(), resultId, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, Messages.getString("ResultSet.Possible_incomplete_traversal_of_result_set", new Object[] { Integer.valueOf(getRow()), Integer.valueOf(rowData.size()) })));
            }
          }
          









          if ((columnUsed.length > 0) && (!rowData.wasEmpty())) {
            StringBuilder buf = new StringBuilder(Messages.getString("ResultSet.The_following_columns_were_never_referenced"));
            
            boolean issueWarn = false;
            
            for (int i = 0; i < columnUsed.length; i++) {
              if (columnUsed[i] == 0) {
                if (!issueWarn) {
                  issueWarn = true;
                } else {
                  buf.append(", ");
                }
                
                buf.append(fields[i].getFullName());
              }
            }
            
            if (issueWarn) {
              eventSink.consumeEvent(new ProfilerEvent((byte)0, "", owningStatement == null ? "N/A" : owningStatement.currentCatalog, connectionId, owningStatement == null ? -1 : owningStatement.getId(), 0, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, buf.toString()));
            }
            
          }
        }
      }
      finally
      {
        if ((owningStatement != null) && (calledExplicitly)) {
          owningStatement.removeOpenResultSet(this);
        }
        
        SQLException exceptionDuringClose = null;
        
        if (rowData != null) {
          try {
            rowData.close();
          } catch (SQLException sqlEx) {
            exceptionDuringClose = sqlEx;
          }
        }
        
        if (statementUsedForFetchingRows != null) {
          try {
            statementUsedForFetchingRows.realClose(true, false);
          } catch (SQLException sqlEx) {
            if (exceptionDuringClose != null) {
              exceptionDuringClose.setNextException(sqlEx);
            } else {
              exceptionDuringClose = sqlEx;
            }
          }
        }
        
        rowData = null;
        fields = null;
        columnLabelToIndex = null;
        fullColumnNameToIndex = null;
        columnToIndexCache = null;
        eventSink = null;
        warningChain = null;
        
        if (!retainOwningStatement) {
          owningStatement = null;
        }
        
        catalog = null;
        serverInfo = null;
        thisRow = null;
        fastDefaultCal = null;
        fastClientCal = null;
        connection = null;
        
        isClosed = true;
        
        if (exceptionDuringClose != null) {
          throw exceptionDuringClose;
        }
      }
    }
  }
  

  public boolean isClosed()
    throws SQLException
  {
    return isClosed;
  }
  
  public boolean reallyResult() {
    if (rowData != null) {
      return true;
    }
    
    return reallyResult;
  }
  

















  public void refreshRow()
    throws SQLException
  {
    throw new NotUpdatable();
  }
  




















  public boolean relative(int rows)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (rowData.size() == 0) {
        setRowPositionValidity();
        
        return false;
      }
      
      if (thisRow != null) {
        thisRow.closeOpenStreams();
      }
      
      rowData.moveRowRelative(rows);
      thisRow = rowData.getAt(rowData.getCurrentRowNumber());
      
      setRowPositionValidity();
      
      return (!rowData.isAfterLast()) && (!rowData.isBeforeFirst());
    }
  }
  












  public boolean rowDeleted()
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  











  public boolean rowInserted()
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  











  public boolean rowUpdated()
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  



  protected void setBinaryEncoded()
  {
    isBinaryEncoded = true;
  }
  













  public void setFetchDirection(int direction)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if ((direction != 1000) && (direction != 1001) && (direction != 1002)) {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Illegal_value_for_fetch_direction_64"), "S1009", getExceptionInterceptor());
      }
      

      fetchDirection = direction;
    }
  }
  














  public void setFetchSize(int rows)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (rows < 0) {
        throw SQLError.createSQLException(Messages.getString("ResultSet.Value_must_be_between_0_and_getMaxRows()_66"), "S1009", getExceptionInterceptor());
      }
      

      fetchSize = rows;
    }
  }
  





  public void setFirstCharOfQuery(char c)
  {
    try
    {
      synchronized (checkClosed().getConnectionMutex()) {
        firstCharOfQuery = c;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  




  protected synchronized void setNextResultSet(ResultSetInternalMethods nextResultSet)
  {
    this.nextResultSet = nextResultSet;
  }
  
  public void setOwningStatement(StatementImpl owningStatement) {
    try {
      synchronized (checkClosed().getConnectionMutex()) {
        this.owningStatement = owningStatement;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  




  protected synchronized void setResultSetConcurrency(int concurrencyFlag)
  {
    try
    {
      synchronized (checkClosed().getConnectionMutex()) {
        resultSetConcurrency = concurrencyFlag;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  





  protected synchronized void setResultSetType(int typeFlag)
  {
    try
    {
      synchronized (checkClosed().getConnectionMutex()) {
        resultSetType = typeFlag;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  




  protected void setServerInfo(String info)
  {
    try
    {
      synchronized (checkClosed().getConnectionMutex()) {
        serverInfo = info;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  
  public synchronized void setStatementUsedForFetchingRows(PreparedStatement stmt) {
    try {
      synchronized (checkClosed().getConnectionMutex()) {
        statementUsedForFetchingRows = stmt;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  


  public synchronized void setWrapperStatement(Statement wrapperStatement)
  {
    try
    {
      synchronized (checkClosed().getConnectionMutex()) {
        this.wrapperStatement = wrapperStatement;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  
  private void throwRangeException(String valueAsString, int columnIndex, int jdbcType) throws SQLException {
    String datatype = null;
    
    switch (jdbcType) {
    case -6: 
      datatype = "TINYINT";
      break;
    case 5: 
      datatype = "SMALLINT";
      break;
    case 4: 
      datatype = "INTEGER";
      break;
    case -5: 
      datatype = "BIGINT";
      break;
    case 7: 
      datatype = "REAL";
      break;
    case 6: 
      datatype = "FLOAT";
      break;
    case 8: 
      datatype = "DOUBLE";
      break;
    case 3: 
      datatype = "DECIMAL";
      break;
    case -4: case -3: case -2: case -1: case 0: case 1: case 2: default: 
      datatype = " (JDBC type '" + jdbcType + "')";
    }
    
    throw SQLError.createSQLException("'" + valueAsString + "' in column '" + columnIndex + "' is outside valid range for the datatype " + datatype + ".", "22003", getExceptionInterceptor());
  }
  

  public String toString()
  {
    if (reallyResult) {
      return super.toString();
    }
    
    return "Result set representing update count of " + updateCount;
  }
  

  public void updateArray(int arg0, Array arg1)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  

  public void updateArray(String arg0, Array arg1)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  
















  public void updateAsciiStream(int columnIndex, InputStream x, int length)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  















  public void updateAsciiStream(String columnName, InputStream x, int length)
    throws SQLException
  {
    updateAsciiStream(findColumn(columnName), x, length);
  }
  













  public void updateBigDecimal(int columnIndex, BigDecimal x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateBigDecimal(String columnName, BigDecimal x)
    throws SQLException
  {
    updateBigDecimal(findColumn(columnName), x);
  }
  
















  public void updateBinaryStream(int columnIndex, InputStream x, int length)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  















  public void updateBinaryStream(String columnName, InputStream x, int length)
    throws SQLException
  {
    updateBinaryStream(findColumn(columnName), x, length);
  }
  

  public void updateBlob(int arg0, java.sql.Blob arg1)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  

  public void updateBlob(String arg0, java.sql.Blob arg1)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  













  public void updateBoolean(int columnIndex, boolean x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateBoolean(String columnName, boolean x)
    throws SQLException
  {
    updateBoolean(findColumn(columnName), x);
  }
  













  public void updateByte(int columnIndex, byte x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateByte(String columnName, byte x)
    throws SQLException
  {
    updateByte(findColumn(columnName), x);
  }
  













  public void updateBytes(int columnIndex, byte[] x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateBytes(String columnName, byte[] x)
    throws SQLException
  {
    updateBytes(findColumn(columnName), x);
  }
  
















  public void updateCharacterStream(int columnIndex, Reader x, int length)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  















  public void updateCharacterStream(String columnName, Reader reader, int length)
    throws SQLException
  {
    updateCharacterStream(findColumn(columnName), reader, length);
  }
  

  public void updateClob(int arg0, java.sql.Clob arg1)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  

  public void updateClob(String columnName, java.sql.Clob clob)
    throws SQLException
  {
    updateClob(findColumn(columnName), clob);
  }
  













  public void updateDate(int columnIndex, Date x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateDate(String columnName, Date x)
    throws SQLException
  {
    updateDate(findColumn(columnName), x);
  }
  













  public void updateDouble(int columnIndex, double x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateDouble(String columnName, double x)
    throws SQLException
  {
    updateDouble(findColumn(columnName), x);
  }
  













  public void updateFloat(int columnIndex, float x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateFloat(String columnName, float x)
    throws SQLException
  {
    updateFloat(findColumn(columnName), x);
  }
  













  public void updateInt(int columnIndex, int x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateInt(String columnName, int x)
    throws SQLException
  {
    updateInt(findColumn(columnName), x);
  }
  













  public void updateLong(int columnIndex, long x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateLong(String columnName, long x)
    throws SQLException
  {
    updateLong(findColumn(columnName), x);
  }
  











  public void updateNull(int columnIndex)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  










  public void updateNull(String columnName)
    throws SQLException
  {
    updateNull(findColumn(columnName));
  }
  













  public void updateObject(int columnIndex, Object x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  

















  public void updateObject(int columnIndex, Object x, int scale)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateObject(String columnName, Object x)
    throws SQLException
  {
    updateObject(findColumn(columnName), x);
  }
  
















  public void updateObject(String columnName, Object x, int scale)
    throws SQLException
  {
    updateObject(findColumn(columnName), x);
  }
  

  public void updateRef(int arg0, Ref arg1)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  

  public void updateRef(String arg0, Ref arg1)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  







  public void updateRow()
    throws SQLException
  {
    throw new NotUpdatable();
  }
  













  public void updateShort(int columnIndex, short x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateShort(String columnName, short x)
    throws SQLException
  {
    updateShort(findColumn(columnName), x);
  }
  













  public void updateString(int columnIndex, String x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateString(String columnName, String x)
    throws SQLException
  {
    updateString(findColumn(columnName), x);
  }
  













  public void updateTime(int columnIndex, Time x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateTime(String columnName, Time x)
    throws SQLException
  {
    updateTime(findColumn(columnName), x);
  }
  













  public void updateTimestamp(int columnIndex, Timestamp x)
    throws SQLException
  {
    throw new NotUpdatable();
  }
  












  public void updateTimestamp(String columnName, Timestamp x)
    throws SQLException
  {
    updateTimestamp(findColumn(columnName), x);
  }
  









  public boolean wasNull()
    throws SQLException
  {
    return wasNullFlag;
  }
  

  protected Calendar getGmtCalendar()
  {
    if (gmtCalendar == null) {
      gmtCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    }
    
    return gmtCalendar;
  }
  
  protected ExceptionInterceptor getExceptionInterceptor() {
    return exceptionInterceptor;
  }
  
  protected double getDoubleInternal(String stringVal, int colIndex)
    throws SQLException
  {
    // Byte code:
    //   0: aload_1
    //   1: ifnonnull +5 -> 6
    //   4: dconst_0
    //   5: dreturn
    //   6: aload_1
    //   7: invokevirtual 201	java/lang/String:length	()I
    //   10: ifne +9 -> 19
    //   13: aload_0
    //   14: invokespecial 249	com/mysql/jdbc/ResultSetImpl:convertToZeroWithEmptyCheck	()I
    //   17: i2d
    //   18: dreturn
    //   19: aload_1
    //   20: invokestatic 253	java/lang/Double:parseDouble	(Ljava/lang/String;)D
    //   23: dstore_3
    //   24: aload_0
    //   25: getfield 47	com/mysql/jdbc/ResultSetImpl:useStrictFloatingPoint	Z
    //   28: ifeq +120 -> 148
    //   31: dload_3
    //   32: ldc2_w 319
    //   35: dcmpl
    //   36: ifne +10 -> 46
    //   39: ldc2_w 321
    //   42: dstore_3
    //   43: goto +105 -> 148
    //   46: dload_3
    //   47: ldc2_w 323
    //   50: dcmpl
    //   51: ifne +10 -> 61
    //   54: ldc2_w 325
    //   57: dstore_3
    //   58: goto +90 -> 148
    //   61: dload_3
    //   62: ldc2_w 327
    //   65: dcmpl
    //   66: ifne +10 -> 76
    //   69: ldc2_w 329
    //   72: dstore_3
    //   73: goto +75 -> 148
    //   76: dload_3
    //   77: ldc2_w 331
    //   80: dcmpl
    //   81: ifne +10 -> 91
    //   84: ldc2_w 333
    //   87: dstore_3
    //   88: goto +60 -> 148
    //   91: dload_3
    //   92: ldc2_w 335
    //   95: dcmpl
    //   96: ifne +10 -> 106
    //   99: ldc2_w 333
    //   102: dstore_3
    //   103: goto +45 -> 148
    //   106: dload_3
    //   107: ldc2_w 337
    //   110: dcmpl
    //   111: ifne +10 -> 121
    //   114: ldc2_w 339
    //   117: dstore_3
    //   118: goto +30 -> 148
    //   121: dload_3
    //   122: ldc2_w 341
    //   125: dcmpl
    //   126: ifne +10 -> 136
    //   129: ldc2_w 343
    //   132: dstore_3
    //   133: goto +15 -> 148
    //   136: dload_3
    //   137: ldc2_w 345
    //   140: dcmpl
    //   141: ifne +7 -> 148
    //   144: ldc2_w 339
    //   147: dstore_3
    //   148: dload_3
    //   149: dreturn
    //   150: astore_3
    //   151: aload_0
    //   152: getfield 59	com/mysql/jdbc/ResultSetImpl:fields	[Lcom/mysql/jdbc/Field;
    //   155: iload_2
    //   156: iconst_1
    //   157: isub
    //   158: aaload
    //   159: invokevirtual 211	com/mysql/jdbc/Field:getMysqlType	()I
    //   162: bipush 16
    //   164: if_icmpne +14 -> 178
    //   167: aload_0
    //   168: iload_2
    //   169: invokespecial 212	com/mysql/jdbc/ResultSetImpl:getNumericRepresentationOfSQLBitType	(I)J
    //   172: lstore 4
    //   174: lload 4
    //   176: l2d
    //   177: dreturn
    //   178: ldc_w 347
    //   181: iconst_2
    //   182: anewarray 9	java/lang/Object
    //   185: dup
    //   186: iconst_0
    //   187: aload_1
    //   188: aastore
    //   189: dup
    //   190: iconst_1
    //   191: iload_2
    //   192: invokestatic 127	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   195: aastore
    //   196: invokestatic 140	com/mysql/jdbc/Messages:getString	(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    //   199: ldc -115
    //   201: aload_0
    //   202: invokevirtual 137	com/mysql/jdbc/ResultSetImpl:getExceptionInterceptor	()Lcom/mysql/jdbc/ExceptionInterceptor;
    //   205: invokestatic 138	com/mysql/jdbc/SQLError:createSQLException	(Ljava/lang/String;Ljava/lang/String;Lcom/mysql/jdbc/ExceptionInterceptor;)Ljava/sql/SQLException;
    //   208: athrow
    // Line number table:
    //   Java source line #2294	-> byte code offset #0
    //   Java source line #2295	-> byte code offset #4
    //   Java source line #2298	-> byte code offset #6
    //   Java source line #2299	-> byte code offset #13
    //   Java source line #2302	-> byte code offset #19
    //   Java source line #2304	-> byte code offset #24
    //   Java source line #2306	-> byte code offset #31
    //   Java source line #2308	-> byte code offset #39
    //   Java source line #2309	-> byte code offset #46
    //   Java source line #2311	-> byte code offset #54
    //   Java source line #2312	-> byte code offset #61
    //   Java source line #2313	-> byte code offset #69
    //   Java source line #2314	-> byte code offset #76
    //   Java source line #2315	-> byte code offset #84
    //   Java source line #2316	-> byte code offset #91
    //   Java source line #2317	-> byte code offset #99
    //   Java source line #2318	-> byte code offset #106
    //   Java source line #2319	-> byte code offset #114
    //   Java source line #2320	-> byte code offset #121
    //   Java source line #2321	-> byte code offset #129
    //   Java source line #2322	-> byte code offset #136
    //   Java source line #2323	-> byte code offset #144
    //   Java source line #2327	-> byte code offset #148
    //   Java source line #2328	-> byte code offset #150
    //   Java source line #2329	-> byte code offset #151
    //   Java source line #2330	-> byte code offset #167
    //   Java source line #2332	-> byte code offset #174
    //   Java source line #2335	-> byte code offset #178
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	209	0	this	ResultSetImpl
    //   0	209	1	stringVal	String
    //   0	209	2	colIndex	int
    //   23	126	3	d	double
    //   150	2	3	e	NumberFormatException
    //   172	3	4	valueAsLong	long
    // Exception table:
    //   from	to	target	type
    //   0	5	150	java/lang/NumberFormatException
    //   6	18	150	java/lang/NumberFormatException
    //   19	149	150	java/lang/NumberFormatException
  }
  
  /* Error */
  public char getFirstCharOfQuery()
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 85	com/mysql/jdbc/ResultSetImpl:checkClosed	()Lcom/mysql/jdbc/MySQLConnection;
    //   4: invokeinterface 86 1 0
    //   9: dup
    //   10: astore_1
    //   11: monitorenter
    //   12: aload_0
    //   13: getfield 348	com/mysql/jdbc/ResultSetImpl:firstCharOfQuery	C
    //   16: aload_1
    //   17: monitorexit
    //   18: ireturn
    //   19: astore_2
    //   20: aload_1
    //   21: monitorexit
    //   22: aload_2
    //   23: athrow
    //   24: astore_1
    //   25: new 349	java/lang/RuntimeException
    //   28: dup
    //   29: aload_1
    //   30: invokespecial 350	java/lang/RuntimeException:<init>	(Ljava/lang/Throwable;)V
    //   33: athrow
    // Line number table:
    //   Java source line #2376	-> byte code offset #0
    //   Java source line #2377	-> byte code offset #12
    //   Java source line #2378	-> byte code offset #19
    //   Java source line #2379	-> byte code offset #24
    //   Java source line #2380	-> byte code offset #25
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	34	0	this	ResultSetImpl
    //   24	6	1	e	SQLException
    //   19	4	2	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   12	18	19	finally
    //   19	22	19	finally
    //   0	18	24	java/sql/SQLException
    //   19	24	24	java/sql/SQLException
  }
  
  /* Error */
  public String getServerInfo()
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 85	com/mysql/jdbc/ResultSetImpl:checkClosed	()Lcom/mysql/jdbc/MySQLConnection;
    //   4: invokeinterface 86 1 0
    //   9: dup
    //   10: astore_1
    //   11: monitorenter
    //   12: aload_0
    //   13: getfield 42	com/mysql/jdbc/ResultSetImpl:serverInfo	Ljava/lang/String;
    //   16: aload_1
    //   17: monitorexit
    //   18: areturn
    //   19: astore_2
    //   20: aload_1
    //   21: monitorexit
    //   22: aload_2
    //   23: athrow
    //   24: astore_1
    //   25: new 349	java/lang/RuntimeException
    //   28: dup
    //   29: aload_1
    //   30: invokespecial 350	java/lang/RuntimeException:<init>	(Ljava/lang/Throwable;)V
    //   33: athrow
    // Line number table:
    //   Java source line #4918	-> byte code offset #0
    //   Java source line #4919	-> byte code offset #12
    //   Java source line #4920	-> byte code offset #19
    //   Java source line #4921	-> byte code offset #24
    //   Java source line #4922	-> byte code offset #25
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	34	0	this	ResultSetImpl
    //   24	6	1	e	SQLException
    //   19	4	2	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   12	18	19	finally
    //   19	22	19	finally
    //   0	18	24	java/sql/SQLException
    //   19	24	24	java/sql/SQLException
  }
}
