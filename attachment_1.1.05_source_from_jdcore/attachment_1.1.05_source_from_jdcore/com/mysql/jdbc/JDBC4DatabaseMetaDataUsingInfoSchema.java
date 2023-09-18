package com.mysql.jdbc;

import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;


























public class JDBC4DatabaseMetaDataUsingInfoSchema
  extends DatabaseMetaDataUsingInfoSchema
{
  public JDBC4DatabaseMetaDataUsingInfoSchema(MySQLConnection connToSet, String databaseToSet)
    throws SQLException
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
  






  protected ResultSet getProcedureColumnsNoISParametersView(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
    throws SQLException
  {
    Field[] fields = createProcedureColumnsFields();
    
    return getProcedureOrFunctionColumns(fields, catalog, schemaPattern, procedureNamePattern, columnNamePattern, true, conn
      .getGetProceduresReturnsFunctions());
  }
  





  protected String getRoutineTypeConditionForGetProcedures()
  {
    return conn.getGetProceduresReturnsFunctions() ? "" : "ROUTINE_TYPE = 'PROCEDURE' AND ";
  }
  






  protected String getRoutineTypeConditionForGetProcedureColumns()
  {
    return conn.getGetProceduresReturnsFunctions() ? "" : "ROUTINE_TYPE = 'PROCEDURE' AND ";
  }
  







  protected int getJDBC4FunctionConstant(DatabaseMetaDataUsingInfoSchema.JDBC4FunctionConstant constant)
  {
    switch (1.$SwitchMap$com$mysql$jdbc$DatabaseMetaDataUsingInfoSchema$JDBC4FunctionConstant[constant.ordinal()]) {
    case 1: 
      return 1;
    case 2: 
      return 2;
    case 3: 
      return 3;
    case 4: 
      return 4;
    case 5: 
      return 5;
    case 6: 
      return 0;
    case 7: 
      return 0;
    case 8: 
      return 1;
    case 9: 
      return 2;
    }
    return -1;
  }
  





  protected int getJDBC4FunctionNoTableConstant()
  {
    return 1;
  }
  




  protected int getColumnType(boolean isOutParam, boolean isInParam, boolean isReturnParam, boolean forGetFunctionColumns)
  {
    return JDBC4DatabaseMetaData.getProcedureOrFunctionColumnType(isOutParam, isInParam, isReturnParam, forGetFunctionColumns);
  }
}
