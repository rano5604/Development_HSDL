package com.mysql.jdbc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


























public class BlobFromLocator
  implements Blob
{
  private List<String> primaryKeyColumns = null;
  
  private List<String> primaryKeyValues = null;
  

  private ResultSetImpl creatorResultSet;
  
  private String blobColumnName = null;
  
  private String tableName = null;
  
  private int numColsInResultSet = 0;
  
  private int numPrimaryKeys = 0;
  
  private String quotedId;
  
  private ExceptionInterceptor exceptionInterceptor;
  

  BlobFromLocator(ResultSetImpl creatorResultSetToSet, int blobColumnIndex, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    this.exceptionInterceptor = exceptionInterceptor;
    creatorResultSet = creatorResultSetToSet;
    
    numColsInResultSet = creatorResultSet.fields.length;
    quotedId = creatorResultSet.connection.getMetaData().getIdentifierQuoteString();
    
    if (numColsInResultSet > 1) {
      primaryKeyColumns = new ArrayList();
      primaryKeyValues = new ArrayList();
      
      for (int i = 0; i < numColsInResultSet; i++) {
        if (creatorResultSet.fields[i].isPrimaryKey()) {
          StringBuilder keyName = new StringBuilder();
          keyName.append(quotedId);
          
          String originalColumnName = creatorResultSet.fields[i].getOriginalName();
          
          if ((originalColumnName != null) && (originalColumnName.length() > 0)) {
            keyName.append(originalColumnName);
          } else {
            keyName.append(creatorResultSet.fields[i].getName());
          }
          
          keyName.append(quotedId);
          
          primaryKeyColumns.add(keyName.toString());
          primaryKeyValues.add(creatorResultSet.getString(i + 1));
        }
      }
    } else {
      notEnoughInformationInQuery();
    }
    
    numPrimaryKeys = primaryKeyColumns.size();
    
    if (numPrimaryKeys == 0) {
      notEnoughInformationInQuery();
    }
    
    if (creatorResultSet.fields[0].getOriginalTableName() != null) {
      StringBuilder tableNameBuffer = new StringBuilder();
      
      String databaseName = creatorResultSet.fields[0].getDatabaseName();
      
      if ((databaseName != null) && (databaseName.length() > 0)) {
        tableNameBuffer.append(quotedId);
        tableNameBuffer.append(databaseName);
        tableNameBuffer.append(quotedId);
        tableNameBuffer.append('.');
      }
      
      tableNameBuffer.append(quotedId);
      tableNameBuffer.append(creatorResultSet.fields[0].getOriginalTableName());
      tableNameBuffer.append(quotedId);
      
      tableName = tableNameBuffer.toString();
    } else {
      StringBuilder tableNameBuffer = new StringBuilder();
      
      tableNameBuffer.append(quotedId);
      tableNameBuffer.append(creatorResultSet.fields[0].getTableName());
      tableNameBuffer.append(quotedId);
      
      tableName = tableNameBuffer.toString();
    }
    
    blobColumnName = (quotedId + creatorResultSet.getString(blobColumnIndex) + quotedId);
  }
  
  private void notEnoughInformationInQuery() throws SQLException {
    throw SQLError.createSQLException("Emulated BLOB locators must come from a ResultSet with only one table selected, and all primary keys selected", "S1000", exceptionInterceptor);
  }
  


  public OutputStream setBinaryStream(long indexToWriteAt)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  







  public InputStream getBinaryStream()
    throws SQLException
  {
    return new BufferedInputStream(new LocatorInputStream(), creatorResultSet.connection.getLocatorFetchBufferSize());
  }
  

  public int setBytes(long writeAt, byte[] bytes, int offset, int length)
    throws SQLException
  {
    PreparedStatement pStmt = null;
    
    if (offset + length > bytes.length) {
      length = bytes.length - offset;
    }
    
    byte[] bytesToWrite = new byte[length];
    System.arraycopy(bytes, offset, bytesToWrite, 0, length);
    

    StringBuilder query = new StringBuilder("UPDATE ");
    query.append(tableName);
    query.append(" SET ");
    query.append(blobColumnName);
    query.append(" = INSERT(");
    query.append(blobColumnName);
    query.append(", ");
    query.append(writeAt);
    query.append(", ");
    query.append(length);
    query.append(", ?) WHERE ");
    
    query.append((String)primaryKeyColumns.get(0));
    query.append(" = ?");
    
    for (int i = 1; i < numPrimaryKeys; i++) {
      query.append(" AND ");
      query.append((String)primaryKeyColumns.get(i));
      query.append(" = ?");
    }
    
    try
    {
      pStmt = creatorResultSet.connection.prepareStatement(query.toString());
      
      pStmt.setBytes(1, bytesToWrite);
      
      for (int i = 0; i < numPrimaryKeys; i++) {
        pStmt.setString(i + 2, (String)primaryKeyValues.get(i));
      }
      
      int rowsUpdated = pStmt.executeUpdate();
      
      if (rowsUpdated != 1) {
        throw SQLError.createSQLException("BLOB data not found! Did primary keys change?", "S1000", exceptionInterceptor);
      }
    } finally {
      if (pStmt != null) {
        try {
          pStmt.close();
        }
        catch (SQLException sqlEx) {}
        

        pStmt = null;
      }
    }
    
    return (int)length();
  }
  

  public int setBytes(long writeAt, byte[] bytes)
    throws SQLException
  {
    return setBytes(writeAt, bytes, 0, bytes.length);
  }
  












  public byte[] getBytes(long pos, int length)
    throws SQLException
  {
    PreparedStatement pStmt = null;
    
    try
    {
      pStmt = createGetBytesStatement();
      
      return getBytesInternal(pStmt, pos, length);
    } finally {
      if (pStmt != null) {
        try {
          pStmt.close();
        }
        catch (SQLException sqlEx) {}
        

        pStmt = null;
      }
    }
  }
  







  public long length()
    throws SQLException
  {
    ResultSet blobRs = null;
    PreparedStatement pStmt = null;
    

    StringBuilder query = new StringBuilder("SELECT LENGTH(");
    query.append(blobColumnName);
    query.append(") FROM ");
    query.append(tableName);
    query.append(" WHERE ");
    
    query.append((String)primaryKeyColumns.get(0));
    query.append(" = ?");
    
    for (int i = 1; i < numPrimaryKeys; i++) {
      query.append(" AND ");
      query.append((String)primaryKeyColumns.get(i));
      query.append(" = ?");
    }
    
    try
    {
      pStmt = creatorResultSet.connection.prepareStatement(query.toString());
      
      for (int i = 0; i < numPrimaryKeys; i++) {
        pStmt.setString(i + 1, (String)primaryKeyValues.get(i));
      }
      
      blobRs = pStmt.executeQuery();
      
      if (blobRs.next()) {
        return blobRs.getLong(1);
      }
      
      throw SQLError.createSQLException("BLOB data not found! Did primary keys change?", "S1000", exceptionInterceptor);
    } finally {
      if (blobRs != null) {
        try {
          blobRs.close();
        }
        catch (SQLException sqlEx) {}
        

        blobRs = null;
      }
      
      if (pStmt != null) {
        try {
          pStmt.close();
        }
        catch (SQLException sqlEx) {}
        

        pStmt = null;
      }
    }
  }
  












  public long position(Blob pattern, long start)
    throws SQLException
  {
    return position(pattern.getBytes(0L, (int)pattern.length()), start);
  }
  

  public long position(byte[] pattern, long start)
    throws SQLException
  {
    ResultSet blobRs = null;
    PreparedStatement pStmt = null;
    

    StringBuilder query = new StringBuilder("SELECT LOCATE(");
    query.append("?, ");
    query.append(blobColumnName);
    query.append(", ");
    query.append(start);
    query.append(") FROM ");
    query.append(tableName);
    query.append(" WHERE ");
    
    query.append((String)primaryKeyColumns.get(0));
    query.append(" = ?");
    
    for (int i = 1; i < numPrimaryKeys; i++) {
      query.append(" AND ");
      query.append((String)primaryKeyColumns.get(i));
      query.append(" = ?");
    }
    
    try
    {
      pStmt = creatorResultSet.connection.prepareStatement(query.toString());
      pStmt.setBytes(1, pattern);
      
      for (int i = 0; i < numPrimaryKeys; i++) {
        pStmt.setString(i + 2, (String)primaryKeyValues.get(i));
      }
      
      blobRs = pStmt.executeQuery();
      
      if (blobRs.next()) {
        return blobRs.getLong(1);
      }
      
      throw SQLError.createSQLException("BLOB data not found! Did primary keys change?", "S1000", exceptionInterceptor);
    } finally {
      if (blobRs != null) {
        try {
          blobRs.close();
        }
        catch (SQLException sqlEx) {}
        

        blobRs = null;
      }
      
      if (pStmt != null) {
        try {
          pStmt.close();
        }
        catch (SQLException sqlEx) {}
        

        pStmt = null;
      }
    }
  }
  

  public void truncate(long length)
    throws SQLException
  {
    PreparedStatement pStmt = null;
    

    StringBuilder query = new StringBuilder("UPDATE ");
    query.append(tableName);
    query.append(" SET ");
    query.append(blobColumnName);
    query.append(" = LEFT(");
    query.append(blobColumnName);
    query.append(", ");
    query.append(length);
    query.append(") WHERE ");
    
    query.append((String)primaryKeyColumns.get(0));
    query.append(" = ?");
    
    for (int i = 1; i < numPrimaryKeys; i++) {
      query.append(" AND ");
      query.append((String)primaryKeyColumns.get(i));
      query.append(" = ?");
    }
    
    try
    {
      pStmt = creatorResultSet.connection.prepareStatement(query.toString());
      
      for (int i = 0; i < numPrimaryKeys; i++) {
        pStmt.setString(i + 1, (String)primaryKeyValues.get(i));
      }
      
      int rowsUpdated = pStmt.executeUpdate();
      
      if (rowsUpdated != 1) {
        throw SQLError.createSQLException("BLOB data not found! Did primary keys change?", "S1000", exceptionInterceptor);
      }
    } finally {
      if (pStmt != null) {
        try {
          pStmt.close();
        }
        catch (SQLException sqlEx) {}
        

        pStmt = null;
      }
    }
  }
  
  PreparedStatement createGetBytesStatement() throws SQLException {
    StringBuilder query = new StringBuilder("SELECT SUBSTRING(");
    
    query.append(blobColumnName);
    query.append(", ");
    query.append("?");
    query.append(", ");
    query.append("?");
    query.append(") FROM ");
    query.append(tableName);
    query.append(" WHERE ");
    
    query.append((String)primaryKeyColumns.get(0));
    query.append(" = ?");
    
    for (int i = 1; i < numPrimaryKeys; i++) {
      query.append(" AND ");
      query.append((String)primaryKeyColumns.get(i));
      query.append(" = ?");
    }
    
    return creatorResultSet.connection.prepareStatement(query.toString());
  }
  
  byte[] getBytesInternal(PreparedStatement pStmt, long pos, int length) throws SQLException
  {
    ResultSet blobRs = null;
    
    try
    {
      pStmt.setLong(1, pos);
      pStmt.setInt(2, length);
      
      for (int i = 0; i < numPrimaryKeys; i++) {
        pStmt.setString(i + 3, (String)primaryKeyValues.get(i));
      }
      
      blobRs = pStmt.executeQuery();
      
      if (blobRs.next()) {
        return ((ResultSetImpl)blobRs).getBytes(1, true);
      }
      
      throw SQLError.createSQLException("BLOB data not found! Did primary keys change?", "S1000", exceptionInterceptor);
    } finally {
      if (blobRs != null) {
        try {
          blobRs.close();
        }
        catch (SQLException sqlEx) {}
        

        blobRs = null;
      }
    }
  }
  
  class LocatorInputStream extends InputStream {
    long currentPositionInBlob = 0L;
    
    long length = 0L;
    
    PreparedStatement pStmt = null;
    
    LocatorInputStream() throws SQLException {
      length = length();
      pStmt = createGetBytesStatement();
    }
    
    LocatorInputStream(long pos, long len) throws SQLException
    {
      length = (pos + len);
      currentPositionInBlob = pos;
      long blobLength = length();
      
      if (pos + len > blobLength) {
        throw SQLError.createSQLException(Messages.getString("Blob.invalidStreamLength", new Object[] { Long.valueOf(blobLength), Long.valueOf(pos), Long.valueOf(len) }), "S1009", exceptionInterceptor);
      }
      


      if (pos < 1L) {
        throw SQLError.createSQLException(Messages.getString("Blob.invalidStreamPos"), "S1009", exceptionInterceptor);
      }
      

      if (pos > blobLength) {
        throw SQLError.createSQLException(Messages.getString("Blob.invalidStreamPos"), "S1009", exceptionInterceptor);
      }
    }
    
    public int read()
      throws IOException
    {
      if (currentPositionInBlob + 1L > length) {
        return -1;
      }
      try
      {
        byte[] asBytes = getBytesInternal(pStmt, currentPositionInBlob++ + 1L, 1);
        
        if (asBytes == null) {
          return -1;
        }
        
        return asBytes[0];
      } catch (SQLException sqlEx) {
        throw new IOException(sqlEx.toString());
      }
    }
    




    public int read(byte[] b, int off, int len)
      throws IOException
    {
      if (currentPositionInBlob + 1L > length) {
        return -1;
      }
      try
      {
        byte[] asBytes = getBytesInternal(pStmt, currentPositionInBlob + 1L, len);
        
        if (asBytes == null) {
          return -1;
        }
        
        System.arraycopy(asBytes, 0, b, off, asBytes.length);
        
        currentPositionInBlob += asBytes.length;
        
        return asBytes.length;
      } catch (SQLException sqlEx) {
        throw new IOException(sqlEx.toString());
      }
    }
    




    public int read(byte[] b)
      throws IOException
    {
      if (currentPositionInBlob + 1L > length) {
        return -1;
      }
      try
      {
        byte[] asBytes = getBytesInternal(pStmt, currentPositionInBlob + 1L, b.length);
        
        if (asBytes == null) {
          return -1;
        }
        
        System.arraycopy(asBytes, 0, b, 0, asBytes.length);
        
        currentPositionInBlob += asBytes.length;
        
        return asBytes.length;
      } catch (SQLException sqlEx) {
        throw new IOException(sqlEx.toString());
      }
    }
    




    public void close()
      throws IOException
    {
      if (pStmt != null) {
        try {
          pStmt.close();
        } catch (SQLException sqlEx) {
          throw new IOException(sqlEx.toString());
        }
      }
      
      super.close();
    }
  }
  
  public void free() throws SQLException {
    creatorResultSet = null;
    primaryKeyColumns = null;
    primaryKeyValues = null;
  }
  
  public InputStream getBinaryStream(long pos, long length) throws SQLException {
    return new LocatorInputStream(pos, length);
  }
}
