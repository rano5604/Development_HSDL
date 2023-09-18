package com.mysql.jdbc;

import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
















public class UpdatableResultSet
  extends ResultSetImpl
{
  static final byte[] STREAM_DATA_MARKER = StringUtils.getBytes("** STREAM DATA **");
  

  protected SingleByteCharsetConverter charConverter;
  

  private String charEncoding;
  
  private byte[][] defaultColumnValue;
  
  private PreparedStatement deleter = null;
  
  private String deleteSQL = null;
  
  private boolean initializedCharConverter = false;
  

  protected PreparedStatement inserter = null;
  
  private String insertSQL = null;
  

  private boolean isUpdatable = false;
  

  private String notUpdatableReason = null;
  

  private List<Integer> primaryKeyIndicies = null;
  
  private String qualifiedAndQuotedTableName;
  
  private String quotedIdChar = null;
  

  private PreparedStatement refresher;
  
  private String refreshSQL = null;
  

  private ResultSetRow savedCurrentRow;
  

  protected PreparedStatement updater = null;
  

  private String updateSQL = null;
  
  private boolean populateInserterWithDefaultValues = false;
  
  private Map<String, Map<String, Map<String, Integer>>> databasesUsedToTablesUsed = null;
  













  protected UpdatableResultSet(String catalog, Field[] fields, RowData tuples, MySQLConnection conn, StatementImpl creatorStmt)
    throws SQLException
  {
    super(catalog, fields, tuples, conn, creatorStmt);
    checkUpdatability();
    populateInserterWithDefaultValues = connection.getPopulateInsertRowWithDefaultValues();
  }
  































  public boolean absolute(int row)
    throws SQLException
  {
    return super.absolute(row);
  }
  










  public void afterLast()
    throws SQLException
  {
    super.afterLast();
  }
  










  public void beforeFirst()
    throws SQLException
  {
    super.beforeFirst();
  }
  









  public void cancelRowUpdates()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (doingUpdates) {
        doingUpdates = false;
        updater.clearParameters();
      }
    }
  }
  




  protected void checkRowPos()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        super.checkRowPos();
      }
    }
  }
  


  protected void checkUpdatability()
    throws SQLException
  {
    try
    {
      if (fields == null)
      {


        return;
      }
      
      String singleTableName = null;
      String catalogName = null;
      
      int primaryKeyCount = 0;
      


      if ((catalog == null) || (catalog.length() == 0)) {
        catalog = fields[0].getDatabaseName();
        
        if ((catalog == null) || (catalog.length() == 0)) {
          throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.43"), "S1009", getExceptionInterceptor());
        }
      }
      

      if (fields.length > 0) {
        singleTableName = fields[0].getOriginalTableName();
        catalogName = fields[0].getDatabaseName();
        
        if (singleTableName == null) {
          singleTableName = fields[0].getTableName();
          catalogName = catalog;
        }
        
        if ((singleTableName != null) && (singleTableName.length() == 0)) {
          isUpdatable = false;
          notUpdatableReason = Messages.getString("NotUpdatableReason.3");
          
          return;
        }
        
        if (fields[0].isPrimaryKey()) {
          primaryKeyCount++;
        }
        



        for (int i = 1; i < fields.length; i++) {
          String otherTableName = fields[i].getOriginalTableName();
          String otherCatalogName = fields[i].getDatabaseName();
          
          if (otherTableName == null) {
            otherTableName = fields[i].getTableName();
            otherCatalogName = catalog;
          }
          
          if ((otherTableName != null) && (otherTableName.length() == 0)) {
            isUpdatable = false;
            notUpdatableReason = Messages.getString("NotUpdatableReason.3");
            
            return;
          }
          
          if ((singleTableName == null) || (!otherTableName.equals(singleTableName))) {
            isUpdatable = false;
            notUpdatableReason = Messages.getString("NotUpdatableReason.0");
            
            return;
          }
          

          if ((catalogName == null) || (!otherCatalogName.equals(catalogName))) {
            isUpdatable = false;
            notUpdatableReason = Messages.getString("NotUpdatableReason.1");
            
            return;
          }
          
          if (fields[i].isPrimaryKey()) {
            primaryKeyCount++;
          }
        }
        
        if ((singleTableName == null) || (singleTableName.length() == 0)) {
          isUpdatable = false;
          notUpdatableReason = Messages.getString("NotUpdatableReason.2");
        }
      }
      else
      {
        isUpdatable = false;
        notUpdatableReason = Messages.getString("NotUpdatableReason.3");
        
        return;
      }
      
      if (connection.getStrictUpdates()) {
        DatabaseMetaData dbmd = connection.getMetaData();
        
        ResultSet rs = null;
        HashMap<String, String> primaryKeyNames = new HashMap();
        try
        {
          rs = dbmd.getPrimaryKeys(catalogName, null, singleTableName);
          
          while (rs.next()) {
            String keyName = rs.getString(4);
            keyName = keyName.toUpperCase();
            primaryKeyNames.put(keyName, keyName);
          }
        } finally {
          if (rs != null) {
            try {
              rs.close();
            } catch (Exception ex) {
              AssertionFailedException.shouldNotHappen(ex);
            }
            
            rs = null;
          }
        }
        
        int existingPrimaryKeysCount = primaryKeyNames.size();
        
        if (existingPrimaryKeysCount == 0) {
          isUpdatable = false;
          notUpdatableReason = Messages.getString("NotUpdatableReason.5");
          
          return;
        }
        



        for (int i = 0; i < fields.length; i++) {
          if (fields[i].isPrimaryKey()) {
            String columnNameUC = fields[i].getName().toUpperCase();
            
            if (primaryKeyNames.remove(columnNameUC) == null)
            {
              String originalName = fields[i].getOriginalName();
              
              if ((originalName != null) && 
                (primaryKeyNames.remove(originalName.toUpperCase()) == null))
              {
                isUpdatable = false;
                notUpdatableReason = Messages.getString("NotUpdatableReason.6", new Object[] { originalName });
                
                return;
              }
            }
          }
        }
        

        isUpdatable = primaryKeyNames.isEmpty();
        
        if (!isUpdatable) {
          if (existingPrimaryKeysCount > 1) {
            notUpdatableReason = Messages.getString("NotUpdatableReason.7");
          } else {
            notUpdatableReason = Messages.getString("NotUpdatableReason.4");
          }
          
          return;
        }
      }
      



      if (primaryKeyCount == 0) {
        isUpdatable = false;
        notUpdatableReason = Messages.getString("NotUpdatableReason.4");
        
        return;
      }
      
      isUpdatable = true;
      notUpdatableReason = null;
      
      return;
    } catch (SQLException sqlEx) {
      isUpdatable = false;
      notUpdatableReason = sqlEx.getMessage();
    }
  }
  









  public void deleteRow()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!isUpdatable) {
        throw new NotUpdatable(notUpdatableReason);
      }
      
      if (onInsertRow)
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.1"), getExceptionInterceptor());
      if (rowData.size() == 0)
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.2"), getExceptionInterceptor());
      if (isBeforeFirst())
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.3"), getExceptionInterceptor());
      if (isAfterLast()) {
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.4"), getExceptionInterceptor());
      }
      
      if (deleter == null) {
        if (deleteSQL == null) {
          generateStatements();
        }
        
        deleter = ((PreparedStatement)connection.clientPrepareStatement(deleteSQL));
      }
      
      deleter.clearParameters();
      
      int numKeys = primaryKeyIndicies.size();
      
      if (numKeys == 1) {
        int index = ((Integer)primaryKeyIndicies.get(0)).intValue();
        setParamValue(deleter, 1, thisRow, index, fields[index].getSQLType());
      } else {
        for (int i = 0; i < numKeys; i++) {
          int index = ((Integer)primaryKeyIndicies.get(i)).intValue();
          setParamValue(deleter, i + 1, thisRow, index, fields[index].getSQLType());
        }
      }
      

      deleter.executeUpdate();
      rowData.removeRow(rowData.getCurrentRowNumber());
      

      previous();
    }
  }
  
  private void setParamValue(PreparedStatement ps, int psIdx, ResultSetRow row, int rsIdx, int sqlType) throws SQLException {
    byte[] val = row.getColumnValue(rsIdx);
    if (val == null) {
      ps.setNull(psIdx, 0);
      return;
    }
    switch (sqlType) {
    case 0: 
      ps.setNull(psIdx, 0);
      break;
    case -6: 
    case 4: 
    case 5: 
      ps.setInt(psIdx, row.getInt(rsIdx));
      break;
    case -5: 
      ps.setLong(psIdx, row.getLong(rsIdx));
      break;
    case -1: 
    case 1: 
    case 2: 
    case 3: 
    case 12: 
      ps.setString(psIdx, row.getString(rsIdx, charEncoding, connection));
      break;
    case 91: 
      ps.setDate(psIdx, row.getDateFast(rsIdx, connection, this, fastDefaultCal), fastDefaultCal);
      break;
    case 93: 
      ps.setTimestamp(psIdx, row.getTimestampFast(rsIdx, fastDefaultCal, connection.getDefaultTimeZone(), false, connection, this));
      break;
    case 92: 
      ps.setTime(psIdx, row.getTimeFast(rsIdx, fastDefaultCal, connection.getDefaultTimeZone(), false, connection, this));
      break;
    case 6: 
    case 7: 
    case 8: 
    case 16: 
      ps.setBytesNoEscapeNoQuotes(psIdx, val);
      break;
    




    default: 
      ps.setBytes(psIdx, val);
    }
  }
  
  private void extractDefaultValues()
    throws SQLException
  {
    DatabaseMetaData dbmd = connection.getMetaData();
    defaultColumnValue = new byte[fields.length][];
    
    ResultSet columnsResultSet = null;
    
    for (Map.Entry<String, Map<String, Map<String, Integer>>> dbEntry : databasesUsedToTablesUsed.entrySet())
    {
      for (Map.Entry<String, Map<String, Integer>> tableEntry : ((Map)dbEntry.getValue()).entrySet()) {
        String tableName = (String)tableEntry.getKey();
        Map<String, Integer> columnNamesToIndices = (Map)tableEntry.getValue();
        try
        {
          columnsResultSet = dbmd.getColumns(catalog, null, tableName, "%");
          
          while (columnsResultSet.next()) {
            String columnName = columnsResultSet.getString("COLUMN_NAME");
            byte[] defaultValue = columnsResultSet.getBytes("COLUMN_DEF");
            
            if (columnNamesToIndices.containsKey(columnName)) {
              int localColumnIndex = ((Integer)columnNamesToIndices.get(columnName)).intValue();
              
              defaultColumnValue[localColumnIndex] = defaultValue;
            }
          }
        } finally {
          if (columnsResultSet != null) {
            columnsResultSet.close();
            
            columnsResultSet = null;
          }
        }
      }
    }
  }
  












  public boolean first()
    throws SQLException
  {
    return super.first();
  }
  





  protected void generateStatements()
    throws SQLException
  {
    if (!isUpdatable) {
      doingUpdates = false;
      onInsertRow = false;
      
      throw new NotUpdatable(notUpdatableReason);
    }
    
    String quotedId = getQuotedIdChar();
    
    Map<String, String> tableNamesSoFar = null;
    
    if (connection.lowerCaseTableNames()) {
      tableNamesSoFar = new TreeMap(String.CASE_INSENSITIVE_ORDER);
      databasesUsedToTablesUsed = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    } else {
      tableNamesSoFar = new TreeMap();
      databasesUsedToTablesUsed = new TreeMap();
    }
    
    primaryKeyIndicies = new ArrayList();
    
    StringBuilder fieldValues = new StringBuilder();
    StringBuilder keyValues = new StringBuilder();
    StringBuilder columnNames = new StringBuilder();
    StringBuilder insertPlaceHolders = new StringBuilder();
    StringBuilder allTablesBuf = new StringBuilder();
    Map<Integer, String> columnIndicesToTable = new HashMap();
    
    boolean firstTime = true;
    boolean keysFirstTime = true;
    
    String equalsStr = connection.versionMeetsMinimum(3, 23, 0) ? "<=>" : "=";
    
    for (int i = 0; i < fields.length; i++) {
      StringBuilder tableNameBuffer = new StringBuilder();
      Map<String, Integer> updColumnNameToIndex = null;
      

      if (fields[i].getOriginalTableName() != null)
      {
        String databaseName = fields[i].getDatabaseName();
        
        if ((databaseName != null) && (databaseName.length() > 0)) {
          tableNameBuffer.append(quotedId);
          tableNameBuffer.append(databaseName);
          tableNameBuffer.append(quotedId);
          tableNameBuffer.append('.');
        }
        
        String tableOnlyName = fields[i].getOriginalTableName();
        
        tableNameBuffer.append(quotedId);
        tableNameBuffer.append(tableOnlyName);
        tableNameBuffer.append(quotedId);
        
        String fqTableName = tableNameBuffer.toString();
        
        if (!tableNamesSoFar.containsKey(fqTableName)) {
          if (!tableNamesSoFar.isEmpty()) {
            allTablesBuf.append(',');
          }
          
          allTablesBuf.append(fqTableName);
          tableNamesSoFar.put(fqTableName, fqTableName);
        }
        
        columnIndicesToTable.put(Integer.valueOf(i), fqTableName);
        
        updColumnNameToIndex = getColumnsToIndexMapForTableAndDB(databaseName, tableOnlyName);
      } else {
        String tableOnlyName = fields[i].getTableName();
        
        if (tableOnlyName != null) {
          tableNameBuffer.append(quotedId);
          tableNameBuffer.append(tableOnlyName);
          tableNameBuffer.append(quotedId);
          
          String fqTableName = tableNameBuffer.toString();
          
          if (!tableNamesSoFar.containsKey(fqTableName)) {
            if (!tableNamesSoFar.isEmpty()) {
              allTablesBuf.append(',');
            }
            
            allTablesBuf.append(fqTableName);
            tableNamesSoFar.put(fqTableName, fqTableName);
          }
          
          columnIndicesToTable.put(Integer.valueOf(i), fqTableName);
          
          updColumnNameToIndex = getColumnsToIndexMapForTableAndDB(catalog, tableOnlyName);
        }
      }
      
      String originalColumnName = fields[i].getOriginalName();
      String columnName = null;
      
      if ((connection.getIO().hasLongColumnInfo()) && (originalColumnName != null) && (originalColumnName.length() > 0)) {
        columnName = originalColumnName;
      } else {
        columnName = fields[i].getName();
      }
      
      if ((updColumnNameToIndex != null) && (columnName != null)) {
        updColumnNameToIndex.put(columnName, Integer.valueOf(i));
      }
      
      String originalTableName = fields[i].getOriginalTableName();
      String tableName = null;
      
      if ((connection.getIO().hasLongColumnInfo()) && (originalTableName != null) && (originalTableName.length() > 0)) {
        tableName = originalTableName;
      } else {
        tableName = fields[i].getTableName();
      }
      
      StringBuilder fqcnBuf = new StringBuilder();
      String databaseName = fields[i].getDatabaseName();
      
      if ((databaseName != null) && (databaseName.length() > 0)) {
        fqcnBuf.append(quotedId);
        fqcnBuf.append(databaseName);
        fqcnBuf.append(quotedId);
        fqcnBuf.append('.');
      }
      
      fqcnBuf.append(quotedId);
      fqcnBuf.append(tableName);
      fqcnBuf.append(quotedId);
      fqcnBuf.append('.');
      fqcnBuf.append(quotedId);
      fqcnBuf.append(columnName);
      fqcnBuf.append(quotedId);
      
      String qualifiedColumnName = fqcnBuf.toString();
      
      if (fields[i].isPrimaryKey()) {
        primaryKeyIndicies.add(Integer.valueOf(i));
        
        if (!keysFirstTime) {
          keyValues.append(" AND ");
        } else {
          keysFirstTime = false;
        }
        
        keyValues.append(qualifiedColumnName);
        keyValues.append(equalsStr);
        keyValues.append("?");
      }
      
      if (firstTime) {
        firstTime = false;
        fieldValues.append("SET ");
      } else {
        fieldValues.append(",");
        columnNames.append(",");
        insertPlaceHolders.append(",");
      }
      
      insertPlaceHolders.append("?");
      
      columnNames.append(qualifiedColumnName);
      
      fieldValues.append(qualifiedColumnName);
      fieldValues.append("=?");
    }
    
    qualifiedAndQuotedTableName = allTablesBuf.toString();
    
    updateSQL = ("UPDATE " + qualifiedAndQuotedTableName + " " + fieldValues.toString() + " WHERE " + keyValues.toString());
    insertSQL = ("INSERT INTO " + qualifiedAndQuotedTableName + " (" + columnNames.toString() + ") VALUES (" + insertPlaceHolders.toString() + ")");
    refreshSQL = ("SELECT " + columnNames.toString() + " FROM " + qualifiedAndQuotedTableName + " WHERE " + keyValues.toString());
    deleteSQL = ("DELETE FROM " + qualifiedAndQuotedTableName + " WHERE " + keyValues.toString());
  }
  
  private Map<String, Integer> getColumnsToIndexMapForTableAndDB(String databaseName, String tableName)
  {
    Map<String, Map<String, Integer>> tablesUsedToColumnsMap = (Map)databasesUsedToTablesUsed.get(databaseName);
    
    if (tablesUsedToColumnsMap == null) {
      if (connection.lowerCaseTableNames()) {
        tablesUsedToColumnsMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
      } else {
        tablesUsedToColumnsMap = new TreeMap();
      }
      
      databasesUsedToTablesUsed.put(databaseName, tablesUsedToColumnsMap);
    }
    
    Map<String, Integer> nameToIndex = (Map)tablesUsedToColumnsMap.get(tableName);
    
    if (nameToIndex == null) {
      nameToIndex = new HashMap();
      tablesUsedToColumnsMap.put(tableName, nameToIndex);
    }
    
    return nameToIndex;
  }
  
  private SingleByteCharsetConverter getCharConverter() throws SQLException {
    if (!initializedCharConverter) {
      initializedCharConverter = true;
      
      if (connection.getUseUnicode()) {
        charEncoding = connection.getEncoding();
        charConverter = connection.getCharsetConverter(charEncoding);
      }
    }
    
    return charConverter;
  }
  








  public int getConcurrency()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return isUpdatable ? 1008 : 1007;
    }
  }
  
  private String getQuotedIdChar() throws SQLException {
    if (quotedIdChar == null) {
      boolean useQuotedIdentifiers = connection.supportsQuotedIdentifiers();
      
      if (useQuotedIdentifiers) {
        DatabaseMetaData dbmd = connection.getMetaData();
        quotedIdChar = dbmd.getIdentifierQuoteString();
      } else {
        quotedIdChar = "";
      }
    }
    
    return quotedIdChar;
  }
  








  public void insertRow()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.7"), getExceptionInterceptor());
      }
      
      inserter.executeUpdate();
      
      long autoIncrementId = inserter.getLastInsertID();
      int numFields = fields.length;
      byte[][] newRow = new byte[numFields][];
      
      for (int i = 0; i < numFields; i++) {
        if (inserter.isNull(i)) {
          newRow[i] = null;
        } else {
          newRow[i] = inserter.getBytesRepresentation(i);
        }
        



        if ((fields[i].isAutoIncrement()) && (autoIncrementId > 0L)) {
          newRow[i] = StringUtils.getBytes(String.valueOf(autoIncrementId));
          inserter.setBytesNoEscapeNoQuotes(i + 1, newRow[i]);
        }
      }
      
      ResultSetRow resultSetRow = new ByteArrayRow(newRow, getExceptionInterceptor());
      
      refreshRow(inserter, resultSetRow);
      
      rowData.addRow(resultSetRow);
      resetInserter();
    }
  }
  












  public boolean isAfterLast()
    throws SQLException
  {
    return super.isAfterLast();
  }
  












  public boolean isBeforeFirst()
    throws SQLException
  {
    return super.isBeforeFirst();
  }
  











  public boolean isFirst()
    throws SQLException
  {
    return super.isFirst();
  }
  












  public boolean isLast()
    throws SQLException
  {
    return super.isLast();
  }
  
  boolean isUpdatable() {
    return isUpdatable;
  }
  












  public boolean last()
    throws SQLException
  {
    return super.last();
  }
  









  public void moveToCurrentRow()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!isUpdatable) {
        throw new NotUpdatable(notUpdatableReason);
      }
      
      if (onInsertRow) {
        onInsertRow = false;
        thisRow = savedCurrentRow;
      }
    }
  }
  















  public void moveToInsertRow()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!isUpdatable) {
        throw new NotUpdatable(notUpdatableReason);
      }
      
      if (inserter == null) {
        if (insertSQL == null) {
          generateStatements();
        }
        
        inserter = ((PreparedStatement)connection.clientPrepareStatement(insertSQL));
        if (populateInserterWithDefaultValues) {
          extractDefaultValues();
        }
        
        resetInserter();
      } else {
        resetInserter();
      }
      
      int numFields = fields.length;
      
      onInsertRow = true;
      doingUpdates = false;
      savedCurrentRow = thisRow;
      byte[][] newRowData = new byte[numFields][];
      thisRow = new ByteArrayRow(newRowData, getExceptionInterceptor());
      thisRow.setMetadata(fields);
      
      for (int i = 0; i < numFields; i++) {
        if (!populateInserterWithDefaultValues) {
          inserter.setBytesNoEscapeNoQuotes(i + 1, StringUtils.getBytes("DEFAULT"));
          newRowData = (byte[][])null;
        }
        else if (defaultColumnValue[i] != null) {
          Field f = fields[i];
          
          switch (f.getMysqlType())
          {
          case 7: 
          case 10: 
          case 11: 
          case 12: 
          case 14: 
            if ((defaultColumnValue[i].length > 7) && (defaultColumnValue[i][0] == 67) && (defaultColumnValue[i][1] == 85) && (defaultColumnValue[i][2] == 82) && (defaultColumnValue[i][3] == 82) && (defaultColumnValue[i][4] == 69) && (defaultColumnValue[i][5] == 78) && (defaultColumnValue[i][6] == 84) && (defaultColumnValue[i][7] == 95))
            {



              inserter.setBytesNoEscapeNoQuotes(i + 1, defaultColumnValue[i]);
            }
            else
            {
              inserter.setBytes(i + 1, defaultColumnValue[i], false, false); }
            break;
          case 8: case 9: 
          case 13: default: 
            inserter.setBytes(i + 1, defaultColumnValue[i], false, false);
          }
          
          
          byte[] defaultValueCopy = new byte[defaultColumnValue[i].length];
          System.arraycopy(defaultColumnValue[i], 0, defaultValueCopy, 0, defaultValueCopy.length);
          newRowData[i] = defaultValueCopy;
        } else {
          inserter.setNull(i + 1, 0);
          newRowData[i] = null;
        }
      }
    }
  }
  


















  public boolean next()
    throws SQLException
  {
    return super.next();
  }
  













  public boolean prev()
    throws SQLException
  {
    return super.prev();
  }
  
















  public boolean previous()
    throws SQLException
  {
    return super.previous();
  }
  









  public void realClose(boolean calledExplicitly)
    throws SQLException
  {
    MySQLConnection locallyScopedConn = connection;
    
    if (locallyScopedConn == null) {
      return;
    }
    
    synchronized (checkClosed().getConnectionMutex()) {
      SQLException sqlEx = null;
      
      if ((useUsageAdvisor) && 
        (deleter == null) && (inserter == null) && (refresher == null) && (updater == null)) {
        eventSink = ProfilerEventHandlerFactory.getInstance(connection);
        
        String message = Messages.getString("UpdatableResultSet.34");
        
        eventSink.consumeEvent(new ProfilerEvent((byte)0, "", owningStatement == null ? "N/A" : owningStatement.currentCatalog, connectionId, owningStatement == null ? -1 : owningStatement.getId(), resultId, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, message));
      }
      



      try
      {
        if (deleter != null) {
          deleter.close();
        }
      } catch (SQLException ex) {
        sqlEx = ex;
      }
      try
      {
        if (inserter != null) {
          inserter.close();
        }
      } catch (SQLException ex) {
        sqlEx = ex;
      }
      try
      {
        if (refresher != null) {
          refresher.close();
        }
      } catch (SQLException ex) {
        sqlEx = ex;
      }
      try
      {
        if (updater != null) {
          updater.close();
        }
      } catch (SQLException ex) {
        sqlEx = ex;
      }
      
      super.realClose(calledExplicitly);
      
      if (sqlEx != null) {
        throw sqlEx;
      }
    }
  }
  


















  public void refreshRow()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!isUpdatable) {
        throw new NotUpdatable();
      }
      
      if (onInsertRow)
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.8"), getExceptionInterceptor());
      if (rowData.size() == 0)
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.9"), getExceptionInterceptor());
      if (isBeforeFirst())
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.10"), getExceptionInterceptor());
      if (isAfterLast()) {
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.11"), getExceptionInterceptor());
      }
      
      refreshRow(updater, thisRow);
    }
  }
  
  private void refreshRow(PreparedStatement updateInsertStmt, ResultSetRow rowToRefresh) throws SQLException {
    if (refresher == null) {
      if (refreshSQL == null) {
        generateStatements();
      }
      
      refresher = ((PreparedStatement)connection.clientPrepareStatement(refreshSQL));
    }
    
    refresher.clearParameters();
    
    int numKeys = primaryKeyIndicies.size();
    
    if (numKeys == 1) {
      byte[] dataFrom = null;
      int index = ((Integer)primaryKeyIndicies.get(0)).intValue();
      
      if ((!doingUpdates) && (!onInsertRow)) {
        dataFrom = rowToRefresh.getColumnValue(index);
      } else {
        dataFrom = updateInsertStmt.getBytesRepresentation(index);
        

        if ((updateInsertStmt.isNull(index)) || (dataFrom.length == 0)) {
          dataFrom = rowToRefresh.getColumnValue(index);
        } else {
          dataFrom = stripBinaryPrefix(dataFrom);
        }
      }
      
      if (fields[index].getvalueNeedsQuoting()) {
        refresher.setBytesNoEscape(1, dataFrom);
      } else {
        refresher.setBytesNoEscapeNoQuotes(1, dataFrom);
      }
    }
    else {
      for (int i = 0; i < numKeys; i++) {
        byte[] dataFrom = null;
        int index = ((Integer)primaryKeyIndicies.get(i)).intValue();
        
        if ((!doingUpdates) && (!onInsertRow)) {
          dataFrom = rowToRefresh.getColumnValue(index);
        } else {
          dataFrom = updateInsertStmt.getBytesRepresentation(index);
          

          if ((updateInsertStmt.isNull(index)) || (dataFrom.length == 0)) {
            dataFrom = rowToRefresh.getColumnValue(index);
          } else {
            dataFrom = stripBinaryPrefix(dataFrom);
          }
        }
        
        refresher.setBytesNoEscape(i + 1, dataFrom);
      }
    }
    
    ResultSet rs = null;
    try
    {
      rs = refresher.executeQuery();
      
      int numCols = rs.getMetaData().getColumnCount();
      
      if (rs.next()) {
        for (int i = 0; i < numCols; i++) {
          byte[] val = rs.getBytes(i + 1);
          
          if ((val == null) || (rs.wasNull())) {
            rowToRefresh.setColumnValue(i, null);
          } else {
            rowToRefresh.setColumnValue(i, rs.getBytes(i + 1));
          }
        }
      } else {
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.12"), "S1000", getExceptionInterceptor());
      }
    } finally {
      if (rs != null) {
        try {
          rs.close();
        }
        catch (SQLException ex) {}
      }
    }
  }
  





















  public boolean relative(int rows)
    throws SQLException
  {
    return super.relative(rows);
  }
  
  private void resetInserter() throws SQLException {
    inserter.clearParameters();
    
    for (int i = 0; i < fields.length; i++) {
      inserter.setNull(i + 1, 0);
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
  






  protected void setResultSetConcurrency(int concurrencyFlag)
  {
    super.setResultSetConcurrency(concurrencyFlag);
  }
  









  private byte[] stripBinaryPrefix(byte[] dataFrom)
  {
    return StringUtils.stripEnclosure(dataFrom, "_binary'", "'");
  }
  




  protected void syncUpdate()
    throws SQLException
  {
    if (updater == null) {
      if (updateSQL == null) {
        generateStatements();
      }
      
      updater = ((PreparedStatement)connection.clientPrepareStatement(updateSQL));
    }
    
    int numFields = fields.length;
    updater.clearParameters();
    
    for (int i = 0; i < numFields; i++) {
      if (thisRow.getColumnValue(i) != null)
      {
        if (fields[i].getvalueNeedsQuoting()) {
          updater.setBytes(i + 1, thisRow.getColumnValue(i), fields[i].isBinary(), false);
        } else {
          updater.setBytesNoEscapeNoQuotes(i + 1, thisRow.getColumnValue(i));
        }
      } else {
        updater.setNull(i + 1, 0);
      }
    }
    
    int numKeys = primaryKeyIndicies.size();
    
    if (numKeys == 1) {
      int index = ((Integer)primaryKeyIndicies.get(0)).intValue();
      setParamValue(updater, numFields + 1, thisRow, index, fields[index].getSQLType());
    } else {
      for (int i = 0; i < numKeys; i++) {
        int idx = ((Integer)primaryKeyIndicies.get(i)).intValue();
        setParamValue(updater, numFields + i + 1, thisRow, idx, fields[idx].getSQLType());
      }
    }
  }
  
















  public void updateAsciiStream(int columnIndex, InputStream x, int length)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setAsciiStream(columnIndex, x, length);
      } else {
        inserter.setAsciiStream(columnIndex, x, length);
        thisRow.setColumnValue(columnIndex - 1, STREAM_DATA_MARKER);
      }
    }
  }
  
















  public void updateAsciiStream(String columnName, InputStream x, int length)
    throws SQLException
  {
    updateAsciiStream(findColumn(columnName), x, length);
  }
  













  public void updateBigDecimal(int columnIndex, BigDecimal x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setBigDecimal(columnIndex, x);
      } else {
        inserter.setBigDecimal(columnIndex, x);
        
        if (x == null) {
          thisRow.setColumnValue(columnIndex - 1, null);
        } else {
          thisRow.setColumnValue(columnIndex - 1, StringUtils.getBytes(x.toString()));
        }
      }
    }
  }
  













  public void updateBigDecimal(String columnName, BigDecimal x)
    throws SQLException
  {
    updateBigDecimal(findColumn(columnName), x);
  }
  
















  public void updateBinaryStream(int columnIndex, InputStream x, int length)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setBinaryStream(columnIndex, x, length);
      } else {
        inserter.setBinaryStream(columnIndex, x, length);
        
        if (x == null) {
          thisRow.setColumnValue(columnIndex - 1, null);
        } else {
          thisRow.setColumnValue(columnIndex - 1, STREAM_DATA_MARKER);
        }
      }
    }
  }
  
















  public void updateBinaryStream(String columnName, InputStream x, int length)
    throws SQLException
  {
    updateBinaryStream(findColumn(columnName), x, length);
  }
  


  public void updateBlob(int columnIndex, Blob blob)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setBlob(columnIndex, blob);
      } else {
        inserter.setBlob(columnIndex, blob);
        
        if (blob == null) {
          thisRow.setColumnValue(columnIndex - 1, null);
        } else {
          thisRow.setColumnValue(columnIndex - 1, STREAM_DATA_MARKER);
        }
      }
    }
  }
  


  public void updateBlob(String columnName, Blob blob)
    throws SQLException
  {
    updateBlob(findColumn(columnName), blob);
  }
  













  public void updateBoolean(int columnIndex, boolean x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setBoolean(columnIndex, x);
      } else {
        inserter.setBoolean(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateBoolean(String columnName, boolean x)
    throws SQLException
  {
    updateBoolean(findColumn(columnName), x);
  }
  













  public void updateByte(int columnIndex, byte x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setByte(columnIndex, x);
      } else {
        inserter.setByte(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateByte(String columnName, byte x)
    throws SQLException
  {
    updateByte(findColumn(columnName), x);
  }
  













  public void updateBytes(int columnIndex, byte[] x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setBytes(columnIndex, x);
      } else {
        inserter.setBytes(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, x);
      }
    }
  }
  













  public void updateBytes(String columnName, byte[] x)
    throws SQLException
  {
    updateBytes(findColumn(columnName), x);
  }
  
















  public void updateCharacterStream(int columnIndex, Reader x, int length)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setCharacterStream(columnIndex, x, length);
      } else {
        inserter.setCharacterStream(columnIndex, x, length);
        
        if (x == null) {
          thisRow.setColumnValue(columnIndex - 1, null);
        } else {
          thisRow.setColumnValue(columnIndex - 1, STREAM_DATA_MARKER);
        }
      }
    }
  }
  
















  public void updateCharacterStream(String columnName, Reader reader, int length)
    throws SQLException
  {
    updateCharacterStream(findColumn(columnName), reader, length);
  }
  


  public void updateClob(int columnIndex, Clob clob)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (clob == null) {
        updateNull(columnIndex);
      } else {
        updateCharacterStream(columnIndex, clob.getCharacterStream(), (int)clob.length());
      }
    }
  }
  













  public void updateDate(int columnIndex, Date x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setDate(columnIndex, x);
      } else {
        inserter.setDate(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateDate(String columnName, Date x)
    throws SQLException
  {
    updateDate(findColumn(columnName), x);
  }
  













  public void updateDouble(int columnIndex, double x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setDouble(columnIndex, x);
      } else {
        inserter.setDouble(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateDouble(String columnName, double x)
    throws SQLException
  {
    updateDouble(findColumn(columnName), x);
  }
  













  public void updateFloat(int columnIndex, float x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setFloat(columnIndex, x);
      } else {
        inserter.setFloat(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateFloat(String columnName, float x)
    throws SQLException
  {
    updateFloat(findColumn(columnName), x);
  }
  













  public void updateInt(int columnIndex, int x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setInt(columnIndex, x);
      } else {
        inserter.setInt(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateInt(String columnName, int x)
    throws SQLException
  {
    updateInt(findColumn(columnName), x);
  }
  













  public void updateLong(int columnIndex, long x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setLong(columnIndex, x);
      } else {
        inserter.setLong(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateLong(String columnName, long x)
    throws SQLException
  {
    updateLong(findColumn(columnName), x);
  }
  











  public void updateNull(int columnIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setNull(columnIndex, 0);
      } else {
        inserter.setNull(columnIndex, 0);
        
        thisRow.setColumnValue(columnIndex - 1, null);
      }
    }
  }
  











  public void updateNull(String columnName)
    throws SQLException
  {
    updateNull(findColumn(columnName));
  }
  













  public void updateObject(int columnIndex, Object x)
    throws SQLException
  {
    updateObjectInternal(columnIndex, x, null, 0);
  }
  

















  public void updateObject(int columnIndex, Object x, int scale)
    throws SQLException
  {
    updateObjectInternal(columnIndex, x, null, scale);
  }
  








  protected void updateObjectInternal(int columnIndex, Object x, Integer targetType, int scaleOrLength)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        if (targetType == null) {
          updater.setObject(columnIndex, x);
        } else {
          updater.setObject(columnIndex, x, targetType.intValue());
        }
      } else {
        if (targetType == null) {
          inserter.setObject(columnIndex, x);
        } else {
          inserter.setObject(columnIndex, x, targetType.intValue());
        }
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
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
  








  public void updateRow()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!isUpdatable) {
        throw new NotUpdatable(notUpdatableReason);
      }
      
      if (doingUpdates) {
        updater.executeUpdate();
        refreshRow();
        doingUpdates = false;
      } else if (onInsertRow) {
        throw SQLError.createSQLException(Messages.getString("UpdatableResultSet.44"), getExceptionInterceptor());
      }
      



      syncUpdate();
    }
  }
  













  public void updateShort(int columnIndex, short x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setShort(columnIndex, x);
      } else {
        inserter.setShort(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateShort(String columnName, short x)
    throws SQLException
  {
    updateShort(findColumn(columnName), x);
  }
  













  public void updateString(int columnIndex, String x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setString(columnIndex, x);
      } else {
        inserter.setString(columnIndex, x);
        
        if (x == null) {
          thisRow.setColumnValue(columnIndex - 1, null);
        }
        else if (getCharConverter() != null) {
          thisRow.setColumnValue(columnIndex - 1, StringUtils.getBytes(x, charConverter, charEncoding, connection.getServerCharset(), connection.parserKnowsUnicode(), getExceptionInterceptor()));
        }
        else {
          thisRow.setColumnValue(columnIndex - 1, StringUtils.getBytes(x));
        }
      }
    }
  }
  














  public void updateString(String columnName, String x)
    throws SQLException
  {
    updateString(findColumn(columnName), x);
  }
  













  public void updateTime(int columnIndex, Time x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setTime(columnIndex, x);
      } else {
        inserter.setTime(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateTime(String columnName, Time x)
    throws SQLException
  {
    updateTime(findColumn(columnName), x);
  }
  













  public void updateTimestamp(int columnIndex, Timestamp x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!onInsertRow) {
        if (!doingUpdates) {
          doingUpdates = true;
          syncUpdate();
        }
        
        updater.setTimestamp(columnIndex, x);
      } else {
        inserter.setTimestamp(columnIndex, x);
        
        thisRow.setColumnValue(columnIndex - 1, inserter.getBytesRepresentation(columnIndex - 1));
      }
    }
  }
  













  public void updateTimestamp(String columnName, Timestamp x)
    throws SQLException
  {
    updateTimestamp(findColumn(columnName), x);
  }
}
