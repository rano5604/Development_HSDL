package com.mysql.jdbc;

import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;



























public class JDBC4DatabaseMetaData
  extends DatabaseMetaData
{
  public JDBC4DatabaseMetaData(MySQLConnection connToSet, String databaseToSet)
  {
    super(connToSet, databaseToSet);
  }
  
  public RowIdLifetime getRowIdLifetime() throws SQLException {
    return RowIdLifetime.ROWID_UNSUPPORTED;
  }
  
















  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    return iface.isInstance(this);
  }
  














  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    try
    {
      return iface.cast(this);
    } catch (ClassCastException cce) {
      throw SQLError.createSQLException("Unable to unwrap to " + iface.toString(), "S1009", conn
        .getExceptionInterceptor());
    }
  }
  
  public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
    return false;
  }
  






  public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
    throws SQLException
  {
    Field[] fields = createProcedureColumnsFields();
    
    return getProcedureOrFunctionColumns(fields, catalog, schemaPattern, procedureNamePattern, columnNamePattern, true, conn
      .getGetProceduresReturnsFunctions());
  }
  





  public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
    throws SQLException
  {
    Field[] fields = createFieldMetadataForGetProcedures();
    
    return getProceduresAndOrFunctions(fields, catalog, schemaPattern, procedureNamePattern, true, conn.getGetProceduresReturnsFunctions());
  }
  




  protected int getJDBC4FunctionNoTableConstant()
  {
    return 1;
  }
  




  protected int getColumnType(boolean isOutParam, boolean isInParam, boolean isReturnParam, boolean forGetFunctionColumns)
  {
    return getProcedureOrFunctionColumnType(isOutParam, isInParam, isReturnParam, forGetFunctionColumns);
  }
  














  protected static int getProcedureOrFunctionColumnType(boolean isOutParam, boolean isInParam, boolean isReturnParam, boolean forGetFunctionColumns)
  {
    if ((isInParam) && (isOutParam))
      return forGetFunctionColumns ? 2 : 2;
    if (isInParam)
      return forGetFunctionColumns ? 1 : 1;
    if (isOutParam)
      return forGetFunctionColumns ? 3 : 4;
    if (isReturnParam) {
      return forGetFunctionColumns ? 4 : 5;
    }
    return forGetFunctionColumns ? 0 : 0;
  }
}
