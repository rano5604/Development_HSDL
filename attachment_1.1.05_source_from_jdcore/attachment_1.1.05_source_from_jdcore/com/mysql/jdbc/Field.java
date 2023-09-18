package com.mysql.jdbc;

import java.io.UnsupportedEncodingException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.PatternSyntaxException;




























public class Field
{
  private static final int AUTO_INCREMENT_FLAG = 512;
  private static final int NO_CHARSET_INFO = -1;
  private byte[] buffer;
  private int collationIndex = 0;
  
  private String encoding = null;
  
  private int colDecimals;
  
  private short colFlag;
  
  private String collationName = null;
  
  private MySQLConnection connection = null;
  
  private String databaseName = null;
  
  private int databaseNameLength = -1;
  

  private int databaseNameStart = -1;
  
  protected int defaultValueLength = -1;
  

  protected int defaultValueStart = -1;
  
  private String fullName = null;
  
  private String fullOriginalName = null;
  
  private boolean isImplicitTempTable = false;
  
  private long length;
  
  private int mysqlType = -1;
  
  private String name;
  
  private int nameLength;
  
  private int nameStart;
  
  private String originalColumnName = null;
  
  private int originalColumnNameLength = -1;
  

  private int originalColumnNameStart = -1;
  
  private String originalTableName = null;
  
  private int originalTableNameLength = -1;
  

  private int originalTableNameStart = -1;
  
  private int precisionAdjustFactor = 0;
  
  private int sqlType = -1;
  
  private String tableName;
  
  private int tableNameLength;
  
  private int tableNameStart;
  
  private boolean useOldNameMetadata = false;
  

  private boolean isSingleBit;
  

  private int maxBytesPerChar;
  
  private final boolean valueNeedsQuoting;
  

  Field(MySQLConnection conn, byte[] buffer, int databaseNameStart, int databaseNameLength, int tableNameStart, int tableNameLength, int originalTableNameStart, int originalTableNameLength, int nameStart, int nameLength, int originalColumnNameStart, int originalColumnNameLength, long length, int mysqlType, short colFlag, int colDecimals, int defaultValueStart, int defaultValueLength, int charsetIndex)
    throws SQLException
  {
    connection = conn;
    this.buffer = buffer;
    this.nameStart = nameStart;
    this.nameLength = nameLength;
    this.tableNameStart = tableNameStart;
    this.tableNameLength = tableNameLength;
    this.length = length;
    this.colFlag = colFlag;
    this.colDecimals = colDecimals;
    this.mysqlType = mysqlType;
    

    this.databaseNameStart = databaseNameStart;
    this.databaseNameLength = databaseNameLength;
    
    this.originalTableNameStart = originalTableNameStart;
    this.originalTableNameLength = originalTableNameLength;
    
    this.originalColumnNameStart = originalColumnNameStart;
    this.originalColumnNameLength = originalColumnNameLength;
    
    this.defaultValueStart = defaultValueStart;
    this.defaultValueLength = defaultValueLength;
    

    collationIndex = charsetIndex;
    

    sqlType = MysqlDefs.mysqlToJavaType(this.mysqlType);
    
    checkForImplicitTemporaryTable();
    
    boolean isFromFunction = this.originalTableNameLength == 0;
    
    if (this.mysqlType == 252) {
      if ((connection.getBlobsAreStrings()) || ((connection.getFunctionsNeverReturnBlobs()) && (isFromFunction))) {
        sqlType = 12;
        this.mysqlType = 15;
      } else if ((collationIndex == 63) || (!connection.versionMeetsMinimum(4, 1, 0))) {
        if ((connection.getUseBlobToStoreUTF8OutsideBMP()) && (shouldSetupForUtf8StringInBlob())) {
          setupForUtf8StringInBlob();
        } else {
          setBlobTypeBasedOnLength();
          sqlType = MysqlDefs.mysqlToJavaType(this.mysqlType);
        }
      }
      else {
        this.mysqlType = 253;
        sqlType = -1;
      }
    }
    
    if ((sqlType == -6) && (this.length == 1L) && (connection.getTinyInt1isBit()))
    {
      if (conn.getTinyInt1isBit()) {
        if (conn.getTransformedBitIsBoolean()) {
          sqlType = 16;
        } else {
          sqlType = -7;
        }
      }
    }
    

    if ((!isNativeNumericType()) && (!isNativeDateTimeType())) {
      encoding = connection.getEncodingForIndex(collationIndex);
      


      if ("UnicodeBig".equals(encoding)) {
        encoding = "UTF-16";
      }
      

      if (this.mysqlType == 245) {
        encoding = "UTF-8";
      }
      


      boolean isBinary = isBinary();
      
      if ((connection.versionMeetsMinimum(4, 1, 0)) && (this.mysqlType == 253) && (isBinary) && (collationIndex == 63))
      {
        if ((connection.getFunctionsNeverReturnBlobs()) && (isFromFunction)) {
          sqlType = 12;
          this.mysqlType = 15;
        } else if (isOpaqueBinary()) {
          sqlType = -3;
        }
      }
      
      if ((connection.versionMeetsMinimum(4, 1, 0)) && (this.mysqlType == 254) && (isBinary) && (collationIndex == 63))
      {





        if ((isOpaqueBinary()) && (!connection.getBlobsAreStrings())) {
          sqlType = -2;
        }
      }
      
      if (this.mysqlType == 16) {
        isSingleBit = ((this.length == 0L) || ((this.length == 1L) && ((connection.versionMeetsMinimum(5, 0, 21)) || (connection.versionMeetsMinimum(5, 1, 10)))));
        

        if (!isSingleBit) {
          this.colFlag = ((short)(this.colFlag | 0x80));
          this.colFlag = ((short)(this.colFlag | 0x10));
          isBinary = true;
        }
      }
      



      if ((sqlType == -4) && (!isBinary)) {
        sqlType = -1;
      } else if ((sqlType == -3) && (!isBinary)) {
        sqlType = 12;
      }
    } else {
      encoding = "US-ASCII";
    }
    



    if (!isUnsigned()) {
      switch (this.mysqlType) {
      case 0: 
      case 246: 
        precisionAdjustFactor = -1;
        
        break;
      case 4: 
      case 5: 
        precisionAdjustFactor = 1;
      
      }
      
    } else {
      switch (this.mysqlType) {
      case 4: 
      case 5: 
        precisionAdjustFactor = 1;
      }
      
    }
    
    valueNeedsQuoting = determineNeedsQuoting();
  }
  
  private boolean shouldSetupForUtf8StringInBlob() throws SQLException {
    String includePattern = connection.getUtf8OutsideBmpIncludedColumnNamePattern();
    String excludePattern = connection.getUtf8OutsideBmpExcludedColumnNamePattern();
    
    if ((excludePattern != null) && (!StringUtils.isEmptyOrWhitespaceOnly(excludePattern))) {
      try {
        if (getOriginalName().matches(excludePattern)) {
          if ((includePattern != null) && (!StringUtils.isEmptyOrWhitespaceOnly(includePattern))) {
            try {
              if (getOriginalName().matches(includePattern)) {
                return true;
              }
            } catch (PatternSyntaxException pse) {
              SQLException sqlEx = SQLError.createSQLException("Illegal regex specified for \"utf8OutsideBmpIncludedColumnNamePattern\"", "S1009", connection.getExceptionInterceptor());
              

              if (!connection.getParanoid()) {
                sqlEx.initCause(pse);
              }
              
              throw sqlEx;
            }
          }
          
          return false;
        }
      } catch (PatternSyntaxException pse) {
        SQLException sqlEx = SQLError.createSQLException("Illegal regex specified for \"utf8OutsideBmpExcludedColumnNamePattern\"", "S1009", connection.getExceptionInterceptor());
        

        if (!connection.getParanoid()) {
          sqlEx.initCause(pse);
        }
        
        throw sqlEx;
      }
    }
    
    return true;
  }
  
  private void setupForUtf8StringInBlob() {
    if ((length == 255L) || (length == 65535L)) {
      mysqlType = 15;
      sqlType = 12;
    } else {
      mysqlType = 253;
      sqlType = -1;
    }
    
    collationIndex = 33;
  }
  


  Field(MySQLConnection conn, byte[] buffer, int nameStart, int nameLength, int tableNameStart, int tableNameLength, int length, int mysqlType, short colFlag, int colDecimals)
    throws SQLException
  {
    this(conn, buffer, -1, -1, tableNameStart, tableNameLength, -1, -1, nameStart, nameLength, -1, -1, length, mysqlType, colFlag, colDecimals, -1, -1, -1);
  }
  



  Field(String tableName, String columnName, int jdbcType, int length)
  {
    this.tableName = tableName;
    name = columnName;
    this.length = length;
    sqlType = jdbcType;
    colFlag = 0;
    colDecimals = 0;
    valueNeedsQuoting = determineNeedsQuoting();
  }
  















  Field(String tableName, String columnName, int charsetIndex, int jdbcType, int length)
  {
    this.tableName = tableName;
    name = columnName;
    this.length = length;
    sqlType = jdbcType;
    colFlag = 0;
    colDecimals = 0;
    collationIndex = charsetIndex;
    valueNeedsQuoting = determineNeedsQuoting();
    
    switch (sqlType) {
    case -3: 
    case -2: 
      colFlag = ((short)(colFlag | 0x80));
      colFlag = ((short)(colFlag | 0x10));
    }
  }
  
  private void checkForImplicitTemporaryTable()
  {
    isImplicitTempTable = ((tableNameLength > 5) && (buffer[tableNameStart] == 35) && (buffer[(tableNameStart + 1)] == 115) && (buffer[(tableNameStart + 2)] == 113) && (buffer[(tableNameStart + 3)] == 108) && (buffer[(tableNameStart + 4)] == 95));
  }
  





  public String getEncoding()
    throws SQLException
  {
    return encoding;
  }
  
  public void setEncoding(String javaEncodingName, Connection conn) throws SQLException {
    encoding = javaEncodingName;
    try {
      collationIndex = CharsetMapping.getCollationIndexForJavaEncoding(javaEncodingName, conn);
    } catch (RuntimeException ex) {
      SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
      sqlEx.initCause(ex);
      throw sqlEx;
    }
  }
  
  public synchronized String getCollation() throws SQLException {
    if ((collationName == null) && 
      (connection != null) && 
      (connection.versionMeetsMinimum(4, 1, 0))) {
      if (connection.getUseDynamicCharsetInfo()) {
        DatabaseMetaData dbmd = connection.getMetaData();
        
        String quotedIdStr = dbmd.getIdentifierQuoteString();
        
        if (" ".equals(quotedIdStr)) {
          quotedIdStr = "";
        }
        
        String csCatalogName = getDatabaseName();
        String csTableName = getOriginalTableName();
        String csColumnName = getOriginalName();
        
        if ((csCatalogName != null) && (csCatalogName.length() != 0) && (csTableName != null) && (csTableName.length() != 0) && (csColumnName != null) && (csColumnName.length() != 0))
        {
          StringBuilder queryBuf = new StringBuilder(csCatalogName.length() + csTableName.length() + 28);
          queryBuf.append("SHOW FULL COLUMNS FROM ");
          queryBuf.append(quotedIdStr);
          queryBuf.append(csCatalogName);
          queryBuf.append(quotedIdStr);
          queryBuf.append(".");
          queryBuf.append(quotedIdStr);
          queryBuf.append(csTableName);
          queryBuf.append(quotedIdStr);
          
          Statement collationStmt = null;
          ResultSet collationRs = null;
          try
          {
            collationStmt = connection.createStatement();
            
            collationRs = collationStmt.executeQuery(queryBuf.toString());
            
            while (collationRs.next()) {
              if (csColumnName.equals(collationRs.getString("Field"))) {
                collationName = collationRs.getString("Collation");
              }
            }
          }
          finally
          {
            if (collationRs != null) {
              collationRs.close();
              collationRs = null;
            }
            
            if (collationStmt != null) {
              collationStmt.close();
              collationStmt = null;
            }
          }
        }
      } else {
        try {
          collationName = CharsetMapping.COLLATION_INDEX_TO_COLLATION_NAME[collationIndex];
        } catch (RuntimeException ex) {
          SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1009", null);
          sqlEx.initCause(ex);
          throw sqlEx;
        }
      }
    }
    


    return collationName;
  }
  
  public String getColumnLabel() throws SQLException {
    return getName();
  }
  
  public String getDatabaseName() throws SQLException {
    if ((databaseName == null) && (databaseNameStart != -1) && (databaseNameLength != -1)) {
      databaseName = getStringFromBytes(databaseNameStart, databaseNameLength);
    }
    
    return databaseName;
  }
  
  int getDecimals() {
    return colDecimals;
  }
  
  public String getFullName() throws SQLException {
    if (fullName == null) {
      StringBuilder fullNameBuf = new StringBuilder(getTableName().length() + 1 + getName().length());
      fullNameBuf.append(tableName);
      

      fullNameBuf.append('.');
      fullNameBuf.append(name);
      fullName = fullNameBuf.toString();
      fullNameBuf = null;
    }
    
    return fullName;
  }
  
  public String getFullOriginalName() throws SQLException {
    getOriginalName();
    
    if (originalColumnName == null) {
      return null;
    }
    
    if (fullName == null) {
      StringBuilder fullOriginalNameBuf = new StringBuilder(getOriginalTableName().length() + 1 + getOriginalName().length());
      fullOriginalNameBuf.append(originalTableName);
      

      fullOriginalNameBuf.append('.');
      fullOriginalNameBuf.append(originalColumnName);
      fullOriginalName = fullOriginalNameBuf.toString();
      fullOriginalNameBuf = null;
    }
    
    return fullOriginalName;
  }
  
  public long getLength() {
    return length;
  }
  
  public synchronized int getMaxBytesPerCharacter() throws SQLException {
    if (maxBytesPerChar == 0) {
      maxBytesPerChar = connection.getMaxBytesPerChar(Integer.valueOf(collationIndex), getEncoding());
    }
    return maxBytesPerChar;
  }
  
  public int getMysqlType() {
    return mysqlType;
  }
  
  public String getName() throws SQLException {
    if (name == null) {
      name = getStringFromBytes(nameStart, nameLength);
    }
    
    return name;
  }
  
  public String getNameNoAliases() throws SQLException {
    if (useOldNameMetadata) {
      return getName();
    }
    
    if ((connection != null) && (connection.versionMeetsMinimum(4, 1, 0))) {
      return getOriginalName();
    }
    
    return getName();
  }
  
  public String getOriginalName() throws SQLException {
    if ((originalColumnName == null) && (originalColumnNameStart != -1) && (originalColumnNameLength != -1)) {
      originalColumnName = getStringFromBytes(originalColumnNameStart, originalColumnNameLength);
    }
    
    return originalColumnName;
  }
  
  public String getOriginalTableName() throws SQLException {
    if ((originalTableName == null) && (originalTableNameStart != -1) && (originalTableNameLength != -1)) {
      originalTableName = getStringFromBytes(originalTableNameStart, originalTableNameLength);
    }
    
    return originalTableName;
  }
  







  public int getPrecisionAdjustFactor()
  {
    return precisionAdjustFactor;
  }
  
  public int getSQLType() {
    return sqlType;
  }
  


  private String getStringFromBytes(int stringStart, int stringLength)
    throws SQLException
  {
    if ((stringStart == -1) || (stringLength == -1)) {
      return null;
    }
    
    if (stringLength == 0) {
      return "";
    }
    
    String stringVal = null;
    
    if (connection != null) {
      if (connection.getUseUnicode()) {
        String javaEncoding = connection.getCharacterSetMetadata();
        
        if (javaEncoding == null) {
          javaEncoding = connection.getEncoding();
        }
        
        if (javaEncoding != null) {
          SingleByteCharsetConverter converter = null;
          
          if (connection != null) {
            converter = connection.getCharsetConverter(javaEncoding);
          }
          
          if (converter != null) {
            stringVal = converter.toString(buffer, stringStart, stringLength);
          } else {
            try
            {
              stringVal = StringUtils.toString(buffer, stringStart, stringLength, javaEncoding);
            } catch (UnsupportedEncodingException ue) {
              throw new RuntimeException(Messages.getString("Field.12") + javaEncoding + Messages.getString("Field.13"));
            }
          }
        }
        else {
          stringVal = StringUtils.toAsciiString(buffer, stringStart, stringLength);
        }
      }
      else {
        stringVal = StringUtils.toAsciiString(buffer, stringStart, stringLength);
      }
    }
    else {
      stringVal = StringUtils.toAsciiString(buffer, stringStart, stringLength);
    }
    
    return stringVal;
  }
  
  public String getTable() throws SQLException {
    return getTableName();
  }
  
  public String getTableName() throws SQLException {
    if (tableName == null) {
      tableName = getStringFromBytes(tableNameStart, tableNameLength);
    }
    
    return tableName;
  }
  
  public String getTableNameNoAliases() throws SQLException {
    if (connection.versionMeetsMinimum(4, 1, 0)) {
      return getOriginalTableName();
    }
    
    return getTableName();
  }
  
  public boolean isAutoIncrement() {
    return (colFlag & 0x200) > 0;
  }
  
  public boolean isBinary() {
    return (colFlag & 0x80) > 0;
  }
  
  public boolean isBlob() {
    return (colFlag & 0x10) > 0;
  }
  


  private boolean isImplicitTemporaryTable()
  {
    return isImplicitTempTable;
  }
  
  public boolean isMultipleKey() {
    return (colFlag & 0x8) > 0;
  }
  
  boolean isNotNull() {
    return (colFlag & 0x1) > 0;
  }
  



  boolean isOpaqueBinary()
    throws SQLException
  {
    if ((collationIndex == 63) && (isBinary()) && ((getMysqlType() == 254) || (getMysqlType() == 253)))
    {

      if ((originalTableNameLength == 0) && (connection != null) && (!connection.versionMeetsMinimum(5, 0, 25))) {
        return false;
      }
      


      return !isImplicitTemporaryTable();
    }
    
    return (connection.versionMeetsMinimum(4, 1, 0)) && ("binary".equalsIgnoreCase(getEncoding()));
  }
  
  public boolean isPrimaryKey()
  {
    return (colFlag & 0x2) > 0;
  }
  




  boolean isReadOnly()
    throws SQLException
  {
    if (connection.versionMeetsMinimum(4, 1, 0)) {
      String orgColumnName = getOriginalName();
      String orgTableName = getOriginalTableName();
      
      return (orgColumnName == null) || (orgColumnName.length() <= 0) || (orgTableName == null) || (orgTableName.length() <= 0);
    }
    
    return false;
  }
  
  public boolean isUniqueKey() {
    return (colFlag & 0x4) > 0;
  }
  
  public boolean isUnsigned() {
    return (colFlag & 0x20) > 0;
  }
  
  public void setUnsigned() {
    colFlag = ((short)(colFlag | 0x20));
  }
  
  public boolean isZeroFill() {
    return (colFlag & 0x40) > 0;
  }
  



  private void setBlobTypeBasedOnLength()
  {
    if (length == 255L) {
      mysqlType = 249;
    } else if (length == 65535L) {
      mysqlType = 252;
    } else if (length == 16777215L) {
      mysqlType = 250;
    } else if (length == 4294967295L) {
      mysqlType = 251;
    }
  }
  
  private boolean isNativeNumericType() {
    return ((mysqlType >= 1) && (mysqlType <= 5)) || (mysqlType == 8) || (mysqlType == 13);
  }
  
  private boolean isNativeDateTimeType()
  {
    return (mysqlType == 10) || (mysqlType == 14) || (mysqlType == 12) || (mysqlType == 11) || (mysqlType == 7);
  }
  
  public void setConnection(MySQLConnection conn)
  {
    connection = conn;
    
    if ((encoding == null) || (collationIndex == 0)) {
      encoding = connection.getEncoding();
    }
  }
  
  void setMysqlType(int type) {
    mysqlType = type;
    sqlType = MysqlDefs.mysqlToJavaType(mysqlType);
  }
  
  protected void setUseOldNameMetadata(boolean useOldNameMetadata) {
    this.useOldNameMetadata = useOldNameMetadata;
  }
  
  public String toString()
  {
    try {
      StringBuilder asString = new StringBuilder();
      asString.append(super.toString());
      asString.append("[");
      asString.append("catalog=");
      asString.append(getDatabaseName());
      asString.append(",tableName=");
      asString.append(getTableName());
      asString.append(",originalTableName=");
      asString.append(getOriginalTableName());
      asString.append(",columnName=");
      asString.append(getName());
      asString.append(",originalColumnName=");
      asString.append(getOriginalName());
      asString.append(",mysqlType=");
      asString.append(getMysqlType());
      asString.append("(");
      asString.append(MysqlDefs.typeToName(getMysqlType()));
      asString.append(")");
      asString.append(",flags=");
      
      if (isAutoIncrement()) {
        asString.append(" AUTO_INCREMENT");
      }
      
      if (isPrimaryKey()) {
        asString.append(" PRIMARY_KEY");
      }
      
      if (isUniqueKey()) {
        asString.append(" UNIQUE_KEY");
      }
      
      if (isBinary()) {
        asString.append(" BINARY");
      }
      
      if (isBlob()) {
        asString.append(" BLOB");
      }
      
      if (isMultipleKey()) {
        asString.append(" MULTI_KEY");
      }
      
      if (isUnsigned()) {
        asString.append(" UNSIGNED");
      }
      
      if (isZeroFill()) {
        asString.append(" ZEROFILL");
      }
      
      asString.append(", charsetIndex=");
      asString.append(collationIndex);
      asString.append(", charsetName=");
      asString.append(encoding);
      






      asString.append("]");
      
      return asString.toString();
    } catch (Throwable t) {}
    return super.toString();
  }
  
  protected boolean isSingleBit()
  {
    return isSingleBit;
  }
  
  protected boolean getvalueNeedsQuoting() {
    return valueNeedsQuoting;
  }
  
  private boolean determineNeedsQuoting() {
    boolean retVal = false;
    
    switch (sqlType) {
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
      retVal = false;
      break;
    case -4: case -3: case -2: case -1: case 0: case 1: default: 
      retVal = true;
    }
    return retVal;
  }
}
