package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;



































public class RowDataCursor
  implements RowData
{
  private static final int BEFORE_START_OF_ROWS = -1;
  private List<ResultSetRow> fetchedRows;
  private int currentPositionInEntireResult = -1;
  




  private int currentPositionInFetchedRows = -1;
  



  private ResultSetImpl owner;
  



  private boolean lastRowFetched = false;
  




  private Field[] metadata;
  




  private MysqlIO mysql;
  




  private long statementIdOnServer;
  



  private ServerPreparedStatement prepStmt;
  



  private static final int SERVER_STATUS_LAST_ROW_SENT = 128;
  



  private boolean firstFetchCompleted = false;
  
  private boolean wasEmpty = false;
  
  private boolean useBufferRowExplicit = false;
  









  public RowDataCursor(MysqlIO ioChannel, ServerPreparedStatement creatingStatement, Field[] metadata)
  {
    currentPositionInEntireResult = -1;
    this.metadata = metadata;
    mysql = ioChannel;
    statementIdOnServer = creatingStatement.getServerStatementId();
    prepStmt = creatingStatement;
    useBufferRowExplicit = MysqlIO.useBufferRowExplicit(this.metadata);
  }
  



  public boolean isAfterLast()
  {
    return (lastRowFetched) && (currentPositionInFetchedRows > fetchedRows.size());
  }
  







  public ResultSetRow getAt(int ind)
    throws SQLException
  {
    notSupported();
    
    return null;
  }
  





  public boolean isBeforeFirst()
    throws SQLException
  {
    return currentPositionInEntireResult < 0;
  }
  






  public void setCurrentRow(int rowNumber)
    throws SQLException
  {
    notSupported();
  }
  





  public int getCurrentRowNumber()
    throws SQLException
  {
    return currentPositionInEntireResult + 1;
  }
  







  public boolean isDynamic()
  {
    return true;
  }
  





  public boolean isEmpty()
    throws SQLException
  {
    return (isBeforeFirst()) && (isAfterLast());
  }
  





  public boolean isFirst()
    throws SQLException
  {
    return currentPositionInEntireResult == 0;
  }
  





  public boolean isLast()
    throws SQLException
  {
    return (lastRowFetched) && (currentPositionInFetchedRows == fetchedRows.size() - 1);
  }
  






  public void addRow(ResultSetRow row)
    throws SQLException
  {
    notSupported();
  }
  




  public void afterLast()
    throws SQLException
  {
    notSupported();
  }
  




  public void beforeFirst()
    throws SQLException
  {
    notSupported();
  }
  




  public void beforeLast()
    throws SQLException
  {
    notSupported();
  }
  





  public void close()
    throws SQLException
  {
    metadata = null;
    owner = null;
  }
  






  public boolean hasNext()
    throws SQLException
  {
    if ((fetchedRows != null) && (fetchedRows.size() == 0)) {
      return false;
    }
    
    if ((owner != null) && (owner.owningStatement != null)) {
      int maxRows = owner.owningStatement.maxRows;
      
      if ((maxRows != -1) && (currentPositionInEntireResult + 1 > maxRows)) {
        return false;
      }
    }
    
    if (currentPositionInEntireResult != -1)
    {
      if (currentPositionInFetchedRows < fetchedRows.size() - 1)
        return true;
      if ((currentPositionInFetchedRows == fetchedRows.size()) && (lastRowFetched)) {
        return false;
      }
      
      fetchMoreRows();
      
      return fetchedRows.size() > 0;
    }
    



    fetchMoreRows();
    
    return fetchedRows.size() > 0;
  }
  






  public void moveRowRelative(int rows)
    throws SQLException
  {
    notSupported();
  }
  





  public ResultSetRow next()
    throws SQLException
  {
    if ((fetchedRows == null) && (currentPositionInEntireResult != -1)) {
      throw SQLError.createSQLException(Messages.getString("ResultSet.Operation_not_allowed_after_ResultSet_closed_144"), "S1000", mysql.getExceptionInterceptor());
    }
    

    if (!hasNext()) {
      return null;
    }
    
    currentPositionInEntireResult += 1;
    currentPositionInFetchedRows += 1;
    

    if ((fetchedRows != null) && (fetchedRows.size() == 0)) {
      return null;
    }
    
    if ((fetchedRows == null) || (currentPositionInFetchedRows > fetchedRows.size() - 1)) {
      fetchMoreRows();
      currentPositionInFetchedRows = 0;
    }
    
    ResultSetRow row = (ResultSetRow)fetchedRows.get(currentPositionInFetchedRows);
    
    row.setMetadata(metadata);
    
    return row;
  }
  
  private void fetchMoreRows()
    throws SQLException
  {
    if (lastRowFetched) {
      fetchedRows = new ArrayList(0);
      return;
    }
    
    synchronized (owner.connection.getConnectionMutex()) {
      boolean oldFirstFetchCompleted = firstFetchCompleted;
      
      if (!firstFetchCompleted) {
        firstFetchCompleted = true;
      }
      
      int numRowsToFetch = owner.getFetchSize();
      
      if (numRowsToFetch == 0) {
        numRowsToFetch = prepStmt.getFetchSize();
      }
      
      if (numRowsToFetch == Integer.MIN_VALUE)
      {

        numRowsToFetch = 1;
      }
      
      fetchedRows = mysql.fetchRowsViaCursor(fetchedRows, statementIdOnServer, metadata, numRowsToFetch, useBufferRowExplicit);
      
      currentPositionInFetchedRows = -1;
      
      if ((mysql.getServerStatus() & 0x80) != 0) {
        lastRowFetched = true;
        
        if ((!oldFirstFetchCompleted) && (fetchedRows.size() == 0)) {
          wasEmpty = true;
        }
      }
    }
  }
  






  public void removeRow(int ind)
    throws SQLException
  {
    notSupported();
  }
  




  public int size()
  {
    return -1;
  }
  
  protected void nextRecord() throws SQLException
  {}
  
  private void notSupported() throws SQLException
  {
    throw new OperationNotSupportedException();
  }
  




  public void setOwner(ResultSetImpl rs)
  {
    owner = rs;
  }
  




  public ResultSetInternalMethods getOwner()
  {
    return owner;
  }
  
  public boolean wasEmpty() {
    return wasEmpty;
  }
  
  public void setMetadata(Field[] metadata) {
    this.metadata = metadata;
  }
}
