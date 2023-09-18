package com.mysql.jdbc;

import java.sql.SQLException;























public class ResultSetMetaData
  implements java.sql.ResultSetMetaData
{
  Field[] fields;
  
  private static int clampedGetLength(Field f)
  {
    long fieldLength = f.getLength();
    
    if (fieldLength > 2147483647L) {
      fieldLength = 2147483647L;
    }
    
    return (int)fieldLength;
  }
  





  private static final boolean isDecimalType(int type)
  {
    switch (type) {
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
      return true;
    }
    
    return false;
  }
  

  boolean useOldAliasBehavior = false;
  boolean treatYearAsDate = true;
  


  private ExceptionInterceptor exceptionInterceptor;
  



  public ResultSetMetaData(Field[] fields, boolean useOldAliasBehavior, boolean treatYearAsDate, ExceptionInterceptor exceptionInterceptor)
  {
    this.fields = fields;
    this.useOldAliasBehavior = useOldAliasBehavior;
    this.treatYearAsDate = treatYearAsDate;
    this.exceptionInterceptor = exceptionInterceptor;
  }
  









  public String getCatalogName(int column)
    throws SQLException
  {
    Field f = getField(column);
    
    String database = f.getDatabaseName();
    
    return database == null ? "" : database;
  }
  











  public String getColumnCharacterEncoding(int column)
    throws SQLException
  {
    String mysqlName = getColumnCharacterSet(column);
    
    String javaName = null;
    
    if (mysqlName != null) {
      try {
        javaName = CharsetMapping.getJavaEncodingForMysqlCharset(mysqlName);
      } catch (RuntimeException ex) {
        SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
        sqlEx.initCause(ex);
        throw sqlEx;
      }
    }
    
    return javaName;
  }
  









  public String getColumnCharacterSet(int column)
    throws SQLException
  {
    return getField(column).getEncoding();
  }
  


















  public String getColumnClassName(int column)
    throws SQLException
  {
    Field f = getField(column);
    
    return getClassNameForJavaType(f.getSQLType(), f.isUnsigned(), f.getMysqlType(), (f.isBinary()) || (f.isBlob()), f.isOpaqueBinary(), treatYearAsDate);
  }
  






  public int getColumnCount()
    throws SQLException
  {
    return fields.length;
  }
  









  public int getColumnDisplaySize(int column)
    throws SQLException
  {
    Field f = getField(column);
    
    int lengthInBytes = clampedGetLength(f);
    
    return lengthInBytes / f.getMaxBytesPerCharacter();
  }
  









  public String getColumnLabel(int column)
    throws SQLException
  {
    if (useOldAliasBehavior) {
      return getColumnName(column);
    }
    
    return getField(column).getColumnLabel();
  }
  









  public String getColumnName(int column)
    throws SQLException
  {
    if (useOldAliasBehavior) {
      return getField(column).getName();
    }
    
    String name = getField(column).getNameNoAliases();
    
    if ((name != null) && (name.length() == 0)) {
      return getField(column).getName();
    }
    
    return name;
  }
  











  public int getColumnType(int column)
    throws SQLException
  {
    return getField(column).getSQLType();
  }
  









  public String getColumnTypeName(int column)
    throws SQLException
  {
    Field field = getField(column);
    
    int mysqlType = field.getMysqlType();
    int jdbcType = field.getSQLType();
    
    switch (mysqlType) {
    case 16: 
      return "BIT";
    case 0: 
    case 246: 
      return field.isUnsigned() ? "DECIMAL UNSIGNED" : "DECIMAL";
    
    case 1: 
      return field.isUnsigned() ? "TINYINT UNSIGNED" : "TINYINT";
    
    case 2: 
      return field.isUnsigned() ? "SMALLINT UNSIGNED" : "SMALLINT";
    
    case 3: 
      return field.isUnsigned() ? "INT UNSIGNED" : "INT";
    
    case 4: 
      return field.isUnsigned() ? "FLOAT UNSIGNED" : "FLOAT";
    
    case 5: 
      return field.isUnsigned() ? "DOUBLE UNSIGNED" : "DOUBLE";
    
    case 6: 
      return "NULL";
    
    case 7: 
      return "TIMESTAMP";
    
    case 8: 
      return field.isUnsigned() ? "BIGINT UNSIGNED" : "BIGINT";
    
    case 9: 
      return field.isUnsigned() ? "MEDIUMINT UNSIGNED" : "MEDIUMINT";
    
    case 10: 
      return "DATE";
    
    case 11: 
      return "TIME";
    
    case 12: 
      return "DATETIME";
    
    case 249: 
      return "TINYBLOB";
    
    case 250: 
      return "MEDIUMBLOB";
    
    case 251: 
      return "LONGBLOB";
    
    case 252: 
      if (getField(column).isBinary()) {
        return "BLOB";
      }
      
      return "TEXT";
    
    case 15: 
      return "VARCHAR";
    
    case 253: 
      if (jdbcType == -3) {
        return "VARBINARY";
      }
      
      return "VARCHAR";
    
    case 254: 
      if (jdbcType == -2) {
        return "BINARY";
      }
      
      return "CHAR";
    
    case 247: 
      return "ENUM";
    
    case 13: 
      return "YEAR";
    
    case 248: 
      return "SET";
    
    case 255: 
      return "GEOMETRY";
    
    case 245: 
      return "JSON";
    }
    
    return "UNKNOWN";
  }
  










  protected Field getField(int columnIndex)
    throws SQLException
  {
    if ((columnIndex < 1) || (columnIndex > fields.length)) {
      throw SQLError.createSQLException(Messages.getString("ResultSetMetaData.46"), "S1002", exceptionInterceptor);
    }
    
    return fields[(columnIndex - 1)];
  }
  









  public int getPrecision(int column)
    throws SQLException
  {
    Field f = getField(column);
    




    if (isDecimalType(f.getSQLType())) {
      if (f.getDecimals() > 0) {
        return clampedGetLength(f) - 1 + f.getPrecisionAdjustFactor();
      }
      
      return clampedGetLength(f) + f.getPrecisionAdjustFactor();
    }
    
    switch (f.getMysqlType()) {
    case 249: 
    case 250: 
    case 251: 
    case 252: 
      return clampedGetLength(f);
    }
    
    return clampedGetLength(f) / f.getMaxBytesPerCharacter();
  }
  











  public int getScale(int column)
    throws SQLException
  {
    Field f = getField(column);
    
    if (isDecimalType(f.getSQLType())) {
      return f.getDecimals();
    }
    
    return 0;
  }
  











  public String getSchemaName(int column)
    throws SQLException
  {
    return "";
  }
  









  public String getTableName(int column)
    throws SQLException
  {
    if (useOldAliasBehavior) {
      return getField(column).getTableName();
    }
    
    return getField(column).getTableNameNoAliases();
  }
  









  public boolean isAutoIncrement(int column)
    throws SQLException
  {
    Field f = getField(column);
    
    return f.isAutoIncrement();
  }
  









  public boolean isCaseSensitive(int column)
    throws SQLException
  {
    Field field = getField(column);
    
    int sqlType = field.getSQLType();
    
    switch (sqlType) {
    case -7: 
    case -6: 
    case -5: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 8: 
    case 91: 
    case 92: 
    case 93: 
      return false;
    

    case -1: 
    case 1: 
    case 12: 
      if (field.isBinary()) {
        return true;
      }
      
      String collationName = field.getCollation();
      
      return (collationName != null) && (!collationName.endsWith("_ci"));
    }
    
    return true;
  }
  










  public boolean isCurrency(int column)
    throws SQLException
  {
    return false;
  }
  









  public boolean isDefinitelyWritable(int column)
    throws SQLException
  {
    return isWritable(column);
  }
  









  public int isNullable(int column)
    throws SQLException
  {
    if (!getField(column).isNotNull()) {
      return 1;
    }
    
    return 0;
  }
  









  public boolean isReadOnly(int column)
    throws SQLException
  {
    return getField(column).isReadOnly();
  }
  













  public boolean isSearchable(int column)
    throws SQLException
  {
    return true;
  }
  









  public boolean isSigned(int column)
    throws SQLException
  {
    Field f = getField(column);
    int sqlType = f.getSQLType();
    
    switch (sqlType) {
    case -6: 
    case -5: 
    case 2: 
    case 3: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 8: 
      return !f.isUnsigned();
    
    case 91: 
    case 92: 
    case 93: 
      return false;
    }
    
    return false;
  }
  










  public boolean isWritable(int column)
    throws SQLException
  {
    return !isReadOnly(column);
  }
  





  public String toString()
  {
    StringBuilder toStringBuf = new StringBuilder();
    toStringBuf.append(super.toString());
    toStringBuf.append(" - Field level information: ");
    
    for (int i = 0; i < fields.length; i++) {
      toStringBuf.append("\n\t");
      toStringBuf.append(fields[i].toString());
    }
    
    return toStringBuf.toString();
  }
  
  static String getClassNameForJavaType(int javaType, boolean isUnsigned, int mysqlTypeIfKnown, boolean isBinaryOrBlob, boolean isOpaqueBinary, boolean treatYearAsDate)
  {
    switch (javaType) {
    case -7: 
    case 16: 
      return "java.lang.Boolean";
    

    case -6: 
      if (isUnsigned) {
        return "java.lang.Integer";
      }
      
      return "java.lang.Integer";
    

    case 5: 
      if (isUnsigned) {
        return "java.lang.Integer";
      }
      
      return "java.lang.Integer";
    

    case 4: 
      if ((!isUnsigned) || (mysqlTypeIfKnown == 9)) {
        return "java.lang.Integer";
      }
      
      return "java.lang.Long";
    

    case -5: 
      if (!isUnsigned) {
        return "java.lang.Long";
      }
      
      return "java.math.BigInteger";
    
    case 2: 
    case 3: 
      return "java.math.BigDecimal";
    
    case 7: 
      return "java.lang.Float";
    
    case 6: 
    case 8: 
      return "java.lang.Double";
    
    case -1: 
    case 1: 
    case 12: 
      if (!isOpaqueBinary) {
        return "java.lang.String";
      }
      
      return "[B";
    

    case -4: 
    case -3: 
    case -2: 
      if (mysqlTypeIfKnown == 255)
        return "[B";
      if (isBinaryOrBlob) {
        return "[B";
      }
      return "java.lang.String";
    

    case 91: 
      return (treatYearAsDate) || (mysqlTypeIfKnown != 13) ? "java.sql.Date" : "java.lang.Short";
    
    case 92: 
      return "java.sql.Time";
    
    case 93: 
      return "java.sql.Timestamp";
    }
    
    return "java.lang.Object";
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
      throw SQLError.createSQLException("Unable to unwrap to " + iface.toString(), "S1009", exceptionInterceptor);
    }
  }
}
