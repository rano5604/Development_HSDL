package com.mysql.jdbc;

import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import java.sql.SQLException;
import java.sql.Statement;




























public class RowDataDynamic
  implements RowData
{
  private int columnCount;
  private Field[] metadata;
  private int index = -1;
  
  private MysqlIO io;
  
  private boolean isAfterEnd = false;
  
  private boolean noMoreRows = false;
  
  private boolean isBinaryEncoded = false;
  
  private ResultSetRow nextRow;
  
  private ResultSetImpl owner;
  
  private boolean streamerClosed = false;
  
  private boolean wasEmpty = false;
  



  private boolean useBufferRowExplicit;
  



  private boolean moreResultsExisted;
  



  private ExceptionInterceptor exceptionInterceptor;
  



  public RowDataDynamic(MysqlIO io, int colCount, Field[] fields, boolean isBinaryEncoded)
    throws SQLException
  {
    this.io = io;
    columnCount = colCount;
    this.isBinaryEncoded = isBinaryEncoded;
    metadata = fields;
    exceptionInterceptor = this.io.getExceptionInterceptor();
    useBufferRowExplicit = MysqlIO.useBufferRowExplicit(metadata);
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
    Object mutex = this;
    
    MySQLConnection conn = null;
    
    if (owner != null) {
      conn = owner.connection;
      
      if (conn != null) {
        mutex = conn.getConnectionMutex();
      }
    }
    
    boolean hadMore = false;
    int howMuchMore = 0;
    
    synchronized (mutex)
    {
      while (next() != null) {
        hadMore = true;
        howMuchMore++;
        
        if (howMuchMore % 100 == 0) {
          Thread.yield();
        }
      }
      
      if (conn != null) {
        if ((!conn.getClobberStreamingResults()) && (conn.getNetTimeoutForStreamingResults() > 0)) {
          String oldValue = conn.getServerVariable("net_write_timeout");
          
          if ((oldValue == null) || (oldValue.length() == 0)) {
            oldValue = "60";
          }
          
          io.clearInputStream();
          
          Statement stmt = null;
          try
          {
            stmt = conn.createStatement();
            ((StatementImpl)stmt).executeSimpleNonQuery(conn, "SET net_write_timeout=" + oldValue);
          } finally {
            if (stmt != null) {
              stmt.close();
            }
          }
        }
        
        if ((conn.getUseUsageAdvisor()) && 
          (hadMore))
        {
          ProfilerEventHandler eventSink = ProfilerEventHandlerFactory.getInstance(conn);
          
          eventSink.consumeEvent(new ProfilerEvent((byte)0, "", owner.owningStatement == null ? "N/A" : owner.owningStatement.currentCatalog, owner.connectionId, owner.owningStatement == null ? -1 : owner.owningStatement.getId(), -1, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, null, Messages.getString("RowDataDynamic.2") + howMuchMore + Messages.getString("RowDataDynamic.3") + Messages.getString("RowDataDynamic.4") + Messages.getString("RowDataDynamic.5") + Messages.getString("RowDataDynamic.6") + owner.pointOfOrigin));
        }
      }
    }
    







    metadata = null;
    owner = null;
  }
  







  public ResultSetRow getAt(int ind)
    throws SQLException
  {
    notSupported();
    
    return null;
  }
  





  public int getCurrentRowNumber()
    throws SQLException
  {
    notSupported();
    
    return -1;
  }
  


  public ResultSetInternalMethods getOwner()
  {
    return owner;
  }
  





  public boolean hasNext()
    throws SQLException
  {
    boolean hasNext = nextRow != null;
    
    if ((!hasNext) && (!streamerClosed)) {
      io.closeStreamer(this);
      streamerClosed = true;
    }
    
    return hasNext;
  }
  





  public boolean isAfterLast()
    throws SQLException
  {
    return isAfterEnd;
  }
  





  public boolean isBeforeFirst()
    throws SQLException
  {
    return index < 0;
  }
  







  public boolean isDynamic()
  {
    return true;
  }
  





  public boolean isEmpty()
    throws SQLException
  {
    notSupported();
    
    return false;
  }
  





  public boolean isFirst()
    throws SQLException
  {
    notSupported();
    
    return false;
  }
  





  public boolean isLast()
    throws SQLException
  {
    notSupported();
    
    return false;
  }
  






  public void moveRowRelative(int rows)
    throws SQLException
  {
    notSupported();
  }
  






  public ResultSetRow next()
    throws SQLException
  {
    nextRecord();
    
    if ((nextRow == null) && (!streamerClosed) && (!moreResultsExisted)) {
      io.closeStreamer(this);
      streamerClosed = true;
    }
    
    if ((nextRow != null) && 
      (index != Integer.MAX_VALUE)) {
      index += 1;
    }
    

    return nextRow;
  }
  
  private void nextRecord() throws SQLException
  {
    try {
      if (!noMoreRows) {
        nextRow = io.nextRow(metadata, columnCount, isBinaryEncoded, 1007, true, useBufferRowExplicit, true, null);
        

        if (nextRow == null) {
          noMoreRows = true;
          isAfterEnd = true;
          moreResultsExisted = io.tackOnMoreStreamingResults(owner);
          
          if (index == -1) {
            wasEmpty = true;
          }
        }
      } else {
        nextRow = null;
        isAfterEnd = true;
      }
    } catch (SQLException sqlEx) {
      if ((sqlEx instanceof StreamingNotifiable)) {
        ((StreamingNotifiable)sqlEx).setWasStreamingResults();
      }
      

      noMoreRows = true;
      

      throw sqlEx;
    } catch (Exception ex) {
      String exceptionType = ex.getClass().getName();
      String exceptionMessage = ex.getMessage();
      
      exceptionMessage = exceptionMessage + Messages.getString("RowDataDynamic.7");
      exceptionMessage = exceptionMessage + Util.stackTraceToString(ex);
      
      SQLException sqlEx = SQLError.createSQLException(Messages.getString("RowDataDynamic.8") + exceptionType + Messages.getString("RowDataDynamic.9") + exceptionMessage, "S1000", exceptionInterceptor);
      

      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
  }
  
  private void notSupported() throws SQLException {
    throw new OperationNotSupportedException();
  }
  






  public void removeRow(int ind)
    throws SQLException
  {
    notSupported();
  }
  






  public void setCurrentRow(int rowNumber)
    throws SQLException
  {
    notSupported();
  }
  


  public void setOwner(ResultSetImpl rs)
  {
    owner = rs;
  }
  




  public int size()
  {
    return -1;
  }
  
  public boolean wasEmpty() {
    return wasEmpty;
  }
  
  public void setMetadata(Field[] metadata) {
    this.metadata = metadata;
  }
}
