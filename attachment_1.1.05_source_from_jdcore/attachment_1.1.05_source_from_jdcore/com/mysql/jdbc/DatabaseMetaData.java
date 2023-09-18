package com.mysql.jdbc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;



















public class DatabaseMetaData
  implements java.sql.DatabaseMetaData
{
  protected static final int MAX_IDENTIFIER_LENGTH = 64;
  private static final int DEFERRABILITY = 13;
  private static final int DELETE_RULE = 10;
  private static final int FK_NAME = 11;
  private static final int FKCOLUMN_NAME = 7;
  private static final int FKTABLE_CAT = 4;
  private static final int FKTABLE_NAME = 6;
  private static final int FKTABLE_SCHEM = 5;
  private static final int KEY_SEQ = 8;
  private static final int PK_NAME = 12;
  private static final int PKCOLUMN_NAME = 3;
  private static final int PKTABLE_CAT = 0;
  private static final int PKTABLE_NAME = 2;
  private static final int PKTABLE_SCHEM = 1;
  private static final String SUPPORTS_FK = "SUPPORTS_FK";
  
  protected abstract class IteratorWithCleanup<T>
  {
    protected IteratorWithCleanup() {}
    
    abstract void close()
      throws SQLException;
    
    abstract boolean hasNext()
      throws SQLException;
    
    abstract T next()
      throws SQLException;
  }
  
  class LocalAndReferencedColumns
  {
    String constraintName;
    List<String> localColumnsList;
    String referencedCatalog;
    List<String> referencedColumnsList;
    String referencedTable;
    
    LocalAndReferencedColumns(List<String> localColumns, String refColumns, String constName, String refCatalog)
    {
      localColumnsList = localColumns;
      referencedColumnsList = refColumns;
      constraintName = constName;
      referencedTable = refTable;
      referencedCatalog = refCatalog;
    }
  }
  
  protected class ResultSetIterator extends DatabaseMetaData.IteratorWithCleanup<String> {
    int colIndex;
    ResultSet resultSet;
    
    ResultSetIterator(ResultSet rs, int index) {
      super();
      resultSet = rs;
      colIndex = index;
    }
    
    void close() throws SQLException
    {
      resultSet.close();
    }
    
    boolean hasNext() throws SQLException
    {
      return resultSet.next();
    }
    
    String next() throws SQLException
    {
      return resultSet.getObject(colIndex).toString();
    }
  }
  
  protected class SingleStringIterator extends DatabaseMetaData.IteratorWithCleanup<String> {
    boolean onFirst = true;
    String value;
    
    SingleStringIterator(String s) {
      super();
      value = s;
    }
    

    void close()
      throws SQLException
    {}
    
    boolean hasNext()
      throws SQLException
    {
      return onFirst;
    }
    
    String next() throws SQLException
    {
      onFirst = false;
      return value;
    }
  }
  


  class TypeDescriptor
  {
    int bufferLength;
    

    int charOctetLength;
    
    Integer columnSize;
    
    short dataType;
    
    Integer decimalDigits;
    
    String isNullable;
    
    int nullability;
    
    int numPrecRadix = 10;
    String typeName;
    
    TypeDescriptor(String typeInfo, String nullabilityInfo) throws SQLException
    {
      if (typeInfo == null) {
        throw SQLError.createSQLException("NULL typeinfo not supported.", "S1009", getExceptionInterceptor());
      }
      
      String mysqlType = "";
      String fullMysqlType = null;
      
      if (typeInfo.indexOf("(") != -1) {
        mysqlType = typeInfo.substring(0, typeInfo.indexOf("(")).trim();
      } else {
        mysqlType = typeInfo;
      }
      
      int indexOfUnsignedInMysqlType = StringUtils.indexOfIgnoreCase(mysqlType, "unsigned");
      
      if (indexOfUnsignedInMysqlType != -1) {
        mysqlType = mysqlType.substring(0, indexOfUnsignedInMysqlType - 1);
      }
      


      boolean isUnsigned = false;
      
      if ((StringUtils.indexOfIgnoreCase(typeInfo, "unsigned") != -1) && (StringUtils.indexOfIgnoreCase(typeInfo, "set") != 0) && (StringUtils.indexOfIgnoreCase(typeInfo, "enum") != 0))
      {
        fullMysqlType = mysqlType + " unsigned";
        isUnsigned = true;
      } else {
        fullMysqlType = mysqlType;
      }
      
      if (conn.getCapitalizeTypeNames()) {
        fullMysqlType = fullMysqlType.toUpperCase(Locale.ENGLISH);
      }
      
      dataType = ((short)MysqlDefs.mysqlToJavaType(mysqlType));
      
      typeName = fullMysqlType;
      


      if (StringUtils.startsWithIgnoreCase(typeInfo, "enum")) {
        String temp = typeInfo.substring(typeInfo.indexOf("("), typeInfo.lastIndexOf(")"));
        StringTokenizer tokenizer = new StringTokenizer(temp, ",");
        int maxLength = 0;
        
        while (tokenizer.hasMoreTokens()) {
          maxLength = Math.max(maxLength, tokenizer.nextToken().length() - 2);
        }
        
        columnSize = Integer.valueOf(maxLength);
        decimalDigits = null;
      } else if (StringUtils.startsWithIgnoreCase(typeInfo, "set")) {
        String temp = typeInfo.substring(typeInfo.indexOf("(") + 1, typeInfo.lastIndexOf(")"));
        StringTokenizer tokenizer = new StringTokenizer(temp, ",");
        int maxLength = 0;
        
        int numElements = tokenizer.countTokens();
        
        if (numElements > 0) {
          maxLength += numElements - 1;
        }
        
        while (tokenizer.hasMoreTokens()) {
          String setMember = tokenizer.nextToken().trim();
          
          if ((setMember.startsWith("'")) && (setMember.endsWith("'"))) {
            maxLength += setMember.length() - 2;
          } else {
            maxLength += setMember.length();
          }
        }
        
        columnSize = Integer.valueOf(maxLength);
        decimalDigits = null;
      } else if (typeInfo.indexOf(",") != -1)
      {
        columnSize = Integer.valueOf(typeInfo.substring(typeInfo.indexOf("(") + 1, typeInfo.indexOf(",")).trim());
        decimalDigits = Integer.valueOf(typeInfo.substring(typeInfo.indexOf(",") + 1, typeInfo.indexOf(")")).trim());
      } else {
        columnSize = null;
        decimalDigits = null;
        

        if (((StringUtils.indexOfIgnoreCase(typeInfo, "char") != -1) || (StringUtils.indexOfIgnoreCase(typeInfo, "text") != -1) || (StringUtils.indexOfIgnoreCase(typeInfo, "blob") != -1) || (StringUtils.indexOfIgnoreCase(typeInfo, "binary") != -1) || (StringUtils.indexOfIgnoreCase(typeInfo, "bit") != -1)) && (typeInfo.indexOf("(") != -1))
        {

          int endParenIndex = typeInfo.indexOf(")");
          
          if (endParenIndex == -1) {
            endParenIndex = typeInfo.length();
          }
          
          columnSize = Integer.valueOf(typeInfo.substring(typeInfo.indexOf("(") + 1, endParenIndex).trim());
          

          if ((conn.getTinyInt1isBit()) && (columnSize.intValue() == 1) && (StringUtils.startsWithIgnoreCase(typeInfo, 0, "tinyint")))
          {
            if (conn.getTransformedBitIsBoolean()) {
              dataType = 16;
              typeName = "BOOLEAN";
            } else {
              dataType = -7;
              typeName = "BIT";
            }
          }
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "tinyint")) {
          if ((conn.getTinyInt1isBit()) && (typeInfo.indexOf("(1)") != -1)) {
            if (conn.getTransformedBitIsBoolean()) {
              dataType = 16;
              typeName = "BOOLEAN";
            } else {
              dataType = -7;
              typeName = "BIT";
            }
          } else {
            columnSize = Integer.valueOf(3);
            decimalDigits = Integer.valueOf(0);
          }
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "smallint")) {
          columnSize = Integer.valueOf(5);
          decimalDigits = Integer.valueOf(0);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "mediumint")) {
          columnSize = Integer.valueOf(isUnsigned ? 8 : 7);
          decimalDigits = Integer.valueOf(0);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "int")) {
          columnSize = Integer.valueOf(10);
          decimalDigits = Integer.valueOf(0);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "integer")) {
          columnSize = Integer.valueOf(10);
          decimalDigits = Integer.valueOf(0);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "bigint")) {
          columnSize = Integer.valueOf(isUnsigned ? 20 : 19);
          decimalDigits = Integer.valueOf(0);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "int24")) {
          columnSize = Integer.valueOf(19);
          decimalDigits = Integer.valueOf(0);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "real")) {
          columnSize = Integer.valueOf(12);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "float")) {
          columnSize = Integer.valueOf(12);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "decimal")) {
          columnSize = Integer.valueOf(12);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "numeric")) {
          columnSize = Integer.valueOf(12);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "double")) {
          columnSize = Integer.valueOf(22);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "char")) {
          columnSize = Integer.valueOf(1);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "varchar")) {
          columnSize = Integer.valueOf(255);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "timestamp")) {
          columnSize = Integer.valueOf(19);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "datetime")) {
          columnSize = Integer.valueOf(19);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "date")) {
          columnSize = Integer.valueOf(10);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "time")) {
          columnSize = Integer.valueOf(8);
        }
        else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "tinyblob")) {
          columnSize = Integer.valueOf(255);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "blob")) {
          columnSize = Integer.valueOf(65535);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "mediumblob")) {
          columnSize = Integer.valueOf(16777215);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "longblob")) {
          columnSize = Integer.valueOf(Integer.MAX_VALUE);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "tinytext")) {
          columnSize = Integer.valueOf(255);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "text")) {
          columnSize = Integer.valueOf(65535);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "mediumtext")) {
          columnSize = Integer.valueOf(16777215);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "longtext")) {
          columnSize = Integer.valueOf(Integer.MAX_VALUE);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "enum")) {
          columnSize = Integer.valueOf(255);
        } else if (StringUtils.startsWithIgnoreCaseAndWs(typeInfo, "set")) {
          columnSize = Integer.valueOf(255);
        }
      }
      


      bufferLength = MysqlIO.getMaxBuf();
      

      numPrecRadix = 10;
      

      if (nullabilityInfo != null) {
        if (nullabilityInfo.equals("YES")) {
          nullability = 1;
          isNullable = "YES";
        }
        else if (nullabilityInfo.equals("UNKNOWN")) {
          nullability = 2;
          isNullable = "";
        }
        else
        {
          nullability = 0;
          isNullable = "NO";
        }
      } else {
        nullability = 0;
        isNullable = "NO";
      }
    }
  }
  
  protected class IndexMetaDataKey
    implements Comparable<IndexMetaDataKey>
  {
    Boolean columnNonUnique;
    Short columnType;
    String columnIndexName;
    Short columnOrdinalPosition;
    
    IndexMetaDataKey(boolean columnNonUnique, short columnType, String columnIndexName, short columnOrdinalPosition)
    {
      this.columnNonUnique = Boolean.valueOf(columnNonUnique);
      this.columnType = Short.valueOf(columnType);
      this.columnIndexName = columnIndexName;
      this.columnOrdinalPosition = Short.valueOf(columnOrdinalPosition);
    }
    
    public int compareTo(IndexMetaDataKey indexInfoKey)
    {
      int compareResult;
      if ((compareResult = columnNonUnique.compareTo(columnNonUnique)) != 0) {
        return compareResult;
      }
      if ((compareResult = columnType.compareTo(columnType)) != 0) {
        return compareResult;
      }
      if ((compareResult = columnIndexName.compareTo(columnIndexName)) != 0) {
        return compareResult;
      }
      return columnOrdinalPosition.compareTo(columnOrdinalPosition);
    }
    
    public boolean equals(Object obj)
    {
      if (obj == null) {
        return false;
      }
      
      if (obj == this) {
        return true;
      }
      
      if (!(obj instanceof IndexMetaDataKey)) {
        return false;
      }
      return compareTo((IndexMetaDataKey)obj) == 0;
    }
    
    public int hashCode()
    {
      if (!$assertionsDisabled) throw new AssertionError("hashCode not designed");
      return 0;
    }
  }
  
  protected class TableMetaDataKey
    implements Comparable<TableMetaDataKey>
  {
    String tableType;
    String tableCat;
    String tableSchem;
    String tableName;
    
    TableMetaDataKey(String tableType, String tableCat, String tableSchem, String tableName)
    {
      this.tableType = (tableType == null ? "" : tableType);
      this.tableCat = (tableCat == null ? "" : tableCat);
      this.tableSchem = (tableSchem == null ? "" : tableSchem);
      this.tableName = (tableName == null ? "" : tableName);
    }
    
    public int compareTo(TableMetaDataKey tablesKey)
    {
      int compareResult;
      if ((compareResult = tableType.compareTo(tableType)) != 0) {
        return compareResult;
      }
      if ((compareResult = tableCat.compareTo(tableCat)) != 0) {
        return compareResult;
      }
      if ((compareResult = tableSchem.compareTo(tableSchem)) != 0) {
        return compareResult;
      }
      return tableName.compareTo(tableName);
    }
    
    public boolean equals(Object obj)
    {
      if (obj == null) {
        return false;
      }
      
      if (obj == this) {
        return true;
      }
      
      if (!(obj instanceof TableMetaDataKey)) {
        return false;
      }
      return compareTo((TableMetaDataKey)obj) == 0;
    }
    
    public int hashCode()
    {
      if (!$assertionsDisabled) throw new AssertionError("hashCode not designed");
      return 0;
    }
  }
  
  protected class ComparableWrapper<K,  extends Comparable<? super K>, V>
    implements Comparable<ComparableWrapper<K, V>>
  {
    K key;
    V value;
    
    public ComparableWrapper(V key)
    {
      this.key = key;
      this.value = value;
    }
    
    public K getKey() {
      return key;
    }
    
    public V getValue() {
      return value;
    }
    
    public int compareTo(ComparableWrapper<K, V> other) {
      return ((Comparable)getKey()).compareTo(other.getKey());
    }
    
    public boolean equals(Object obj)
    {
      if (obj == null) {
        return false;
      }
      
      if (obj == this) {
        return true;
      }
      
      if (!(obj instanceof ComparableWrapper)) {
        return false;
      }
      
      Object otherKey = ((ComparableWrapper)obj).getKey();
      return key.equals(otherKey);
    }
    
    public int hashCode()
    {
      if (!$assertionsDisabled) throw new AssertionError("hashCode not designed");
      return 0;
    }
    
    public String toString()
    {
      return "{KEY:" + key + "; VALUE:" + value + "}";
    }
  }
  


  protected static enum TableType
  {
    LOCAL_TEMPORARY("LOCAL TEMPORARY"),  SYSTEM_TABLE("SYSTEM TABLE"),  SYSTEM_VIEW("SYSTEM VIEW"),  TABLE("TABLE", new String[] { "BASE TABLE" }), 
    VIEW("VIEW"),  UNKNOWN("UNKNOWN");
    
    private String name;
    private byte[] nameAsBytes;
    private String[] synonyms;
    
    private TableType(String tableTypeName) {
      this(tableTypeName, null);
    }
    
    private TableType(String tableTypeName, String[] tableTypeSynonyms) {
      name = tableTypeName;
      nameAsBytes = tableTypeName.getBytes();
      synonyms = tableTypeSynonyms;
    }
    
    String getName() {
      return name;
    }
    
    byte[] asBytes() {
      return nameAsBytes;
    }
    
    boolean equalsTo(String tableTypeName) {
      return name.equalsIgnoreCase(tableTypeName);
    }
    
    static TableType getTableTypeEqualTo(String tableTypeName) {
      for (TableType tableType : ) {
        if (tableType.equalsTo(tableTypeName)) {
          return tableType;
        }
      }
      return UNKNOWN;
    }
    
    boolean compliesWith(String tableTypeName) {
      if (equalsTo(tableTypeName)) {
        return true;
      }
      if (synonyms != null) {
        for (String synonym : synonyms) {
          if (synonym.equalsIgnoreCase(tableTypeName)) {
            return true;
          }
        }
      }
      return false;
    }
    
    static TableType getTableTypeCompliantWith(String tableTypeName) {
      for (TableType tableType : ) {
        if (tableType.compliesWith(tableTypeName)) {
          return tableType;
        }
      }
      return UNKNOWN;
    }
  }
  


  protected static enum ProcedureType
  {
    PROCEDURE,  FUNCTION;
    
















    private ProcedureType() {}
  }
  
















  protected static final byte[] TABLE_AS_BYTES = "TABLE".getBytes();
  
  protected static final byte[] SYSTEM_TABLE_AS_BYTES = "SYSTEM TABLE".getBytes();
  
  private static final int UPDATE_RULE = 9;
  
  protected static final byte[] VIEW_AS_BYTES = "VIEW".getBytes();
  
  private static final Constructor<?> JDBC_4_DBMD_SHOW_CTOR;
  private static final Constructor<?> JDBC_4_DBMD_IS_CTOR;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        JDBC_4_DBMD_SHOW_CTOR = Class.forName("com.mysql.jdbc.JDBC4DatabaseMetaData").getConstructor(new Class[] { MySQLConnection.class, String.class });
        
        JDBC_4_DBMD_IS_CTOR = Class.forName("com.mysql.jdbc.JDBC4DatabaseMetaDataUsingInfoSchema").getConstructor(new Class[] { MySQLConnection.class, String.class });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDBC_4_DBMD_IS_CTOR = null;
      JDBC_4_DBMD_SHOW_CTOR = null;
    }
  }
  

  private static final String[] MYSQL_KEYWORDS = { "ACCESSIBLE", "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ASENSITIVE", "BEFORE", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOTH", "BY", "CALL", "CASCADE", "CASE", "CHANGE", "CHAR", "CHARACTER", "CHECK", "COLLATE", "COLUMN", "CONDITION", "CONSTRAINT", "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATABASE", "DATABASES", "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELAYED", "DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT", "DISTINCTROW", "DIV", "DOUBLE", "DROP", "DUAL", "EACH", "ELSE", "ELSEIF", "ENCLOSED", "ESCAPED", "EXISTS", "EXIT", "EXPLAIN", "FALSE", "FETCH", "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE", "FOREIGN", "FROM", "FULLTEXT", "GENERATED", "GET", "GRANT", "GROUP", "HAVING", "HIGH_PRIORITY", "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", "IF", "IGNORE", "IN", "INDEX", "INFILE", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT", "INT1", "INT2", "INT3", "INT4", "INT8", "INTEGER", "INTERVAL", "INTO", "IO_AFTER_GTIDS", "IO_BEFORE_GTIDS", "IS", "ITERATE", "JOIN", "KEY", "KEYS", "KILL", "LEADING", "LEAVE", "LEFT", "LIKE", "LIMIT", "LINEAR", "LINES", "LOAD", "LOCALTIME", "LOCALTIMESTAMP", "LOCK", "LONG", "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY", "MASTER_BIND", "MASTER_SSL_VERIFY_SERVER_CERT", "MATCH", "MAXVALUE", "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MIDDLEINT", "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD", "MODIFIES", "NATURAL", "NOT", "NO_WRITE_TO_BINLOG", "NULL", "NUMERIC", "ON", "OPTIMIZE", "OPTIMIZER_COSTS", "OPTION", "OPTIONALLY", "OR", "ORDER", "OUT", "OUTER", "OUTFILE", "PARTITION", "PRECISION", "PRIMARY", "PROCEDURE", "PURGE", "RANGE", "READ", "READS", "READ_WRITE", "REAL", "REFERENCES", "REGEXP", "RELEASE", "RENAME", "REPEAT", "REPLACE", "REQUIRE", "RESIGNAL", "RESTRICT", "RETURN", "REVOKE", "RIGHT", "RLIKE", "SCHEMA", "SCHEMAS", "SECOND_MICROSECOND", "SELECT", "SENSITIVE", "SEPARATOR", "SET", "SHOW", "SIGNAL", "SMALLINT", "SPATIAL", "SPECIFIC", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SSL", "STARTING", "STORED", "STRAIGHT_JOIN", "TABLE", "TERMINATED", "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO", "TRAILING", "TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE", "UNLOCK", "UNSIGNED", "UPDATE", "USAGE", "USE", "USING", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER", "VARYING", "VIRTUAL", "WHEN", "WHERE", "WHILE", "WITH", "WRITE", "XOR", "YEAR_MONTH", "ZEROFILL" };
  



















  private static final String[] SQL92_KEYWORDS = { "ABSOLUTE", "ACTION", "ADD", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "AS", "ASC", "ASSERTION", "AT", "AUTHORIZATION", "AVG", "BEGIN", "BETWEEN", "BIT", "BIT_LENGTH", "BOTH", "BY", "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG", "CHAR", "CHARACTER", "CHARACTER_LENGTH", "CHAR_LENGTH", "CHECK", "CLOSE", "COALESCE", "COLLATE", "COLLATION", "COLUMN", "COMMIT", "CONNECT", "CONNECTION", "CONSTRAINT", "CONSTRAINTS", "CONTINUE", "CONVERT", "CORRESPONDING", "COUNT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DEFERRABLE", "DEFERRED", "DELETE", "DESC", "DESCRIBE", "DESCRIPTOR", "DIAGNOSTICS", "DISCONNECT", "DISTINCT", "DOMAIN", "DOUBLE", "DROP", "ELSE", "END", "END-EXEC", "ESCAPE", "EXCEPT", "EXCEPTION", "EXEC", "EXECUTE", "EXISTS", "EXTERNAL", "EXTRACT", "FALSE", "FETCH", "FIRST", "FLOAT", "FOR", "FOREIGN", "FOUND", "FROM", "FULL", "GET", "GLOBAL", "GO", "GOTO", "GRANT", "GROUP", "HAVING", "HOUR", "IDENTITY", "IMMEDIATE", "IN", "INDICATOR", "INITIALLY", "INNER", "INPUT", "INSENSITIVE", "INSERT", "INT", "INTEGER", "INTERSECT", "INTERVAL", "INTO", "IS", "ISOLATION", "JOIN", "KEY", "LANGUAGE", "LAST", "LEADING", "LEFT", "LEVEL", "LIKE", "LOCAL", "LOWER", "MATCH", "MAX", "MIN", "MINUTE", "MODULE", "MONTH", "NAMES", "NATIONAL", "NATURAL", "NCHAR", "NEXT", "NO", "NOT", "NULL", "NULLIF", "NUMERIC", "OCTET_LENGTH", "OF", "ON", "ONLY", "OPEN", "OPTION", "OR", "ORDER", "OUTER", "OUTPUT", "OVERLAPS", "PAD", "PARTIAL", "POSITION", "PRECISION", "PREPARE", "PRESERVE", "PRIMARY", "PRIOR", "PRIVILEGES", "PROCEDURE", "PUBLIC", "READ", "REAL", "REFERENCES", "RELATIVE", "RESTRICT", "REVOKE", "RIGHT", "ROLLBACK", "ROWS", "SCHEMA", "SCROLL", "SECOND", "SECTION", "SELECT", "SESSION", "SESSION_USER", "SET", "SIZE", "SMALLINT", "SOME", "SPACE", "SQL", "SQLCODE", "SQLERROR", "SQLSTATE", "SUBSTRING", "SUM", "SYSTEM_USER", "TABLE", "TEMPORARY", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO", "TRAILING", "TRANSACTION", "TRANSLATE", "TRANSLATION", "TRIM", "TRUE", "UNION", "UNIQUE", "UNKNOWN", "UPDATE", "UPPER", "USAGE", "USER", "USING", "VALUE", "VALUES", "VARCHAR", "VARYING", "VIEW", "WHEN", "WHENEVER", "WHERE", "WITH", "WORK", "WRITE", "YEAR", "ZONE" };
  

















  private static final String[] SQL2003_KEYWORDS = { "ABS", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "ARRAY", "AS", "ASENSITIVE", "ASYMMETRIC", "AT", "ATOMIC", "AUTHORIZATION", "AVG", "BEGIN", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOOLEAN", "BOTH", "BY", "CALL", "CALLED", "CARDINALITY", "CASCADED", "CASE", "CAST", "CEIL", "CEILING", "CHAR", "CHARACTER", "CHARACTER_LENGTH", "CHAR_LENGTH", "CHECK", "CLOB", "CLOSE", "COALESCE", "COLLATE", "COLLECT", "COLUMN", "COMMIT", "CONDITION", "CONNECT", "CONSTRAINT", "CONVERT", "CORR", "CORRESPONDING", "COUNT", "COVAR_POP", "COVAR_SAMP", "CREATE", "CROSS", "CUBE", "CUME_DIST", "CURRENT", "CURRENT_DATE", "CURRENT_DEFAULT_TRANSFORM_GROUP", "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_TRANSFORM_GROUP_FOR_TYPE", "CURRENT_USER", "CURSOR", "CYCLE", "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELETE", "DENSE_RANK", "DEREF", "DESCRIBE", "DETERMINISTIC", "DISCONNECT", "DISTINCT", "DOUBLE", "DROP", "DYNAMIC", "EACH", "ELEMENT", "ELSE", "END", "END-EXEC", "ESCAPE", "EVERY", "EXCEPT", "EXEC", "EXECUTE", "EXISTS", "EXP", "EXTERNAL", "EXTRACT", "FALSE", "FETCH", "FILTER", "FLOAT", "FLOOR", "FOR", "FOREIGN", "FREE", "FROM", "FULL", "FUNCTION", "FUSION", "GET", "GLOBAL", "GRANT", "GROUP", "GROUPING", "HAVING", "HOLD", "HOUR", "IDENTITY", "IN", "INDICATOR", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT", "INTEGER", "INTERSECT", "INTERSECTION", "INTERVAL", "INTO", "IS", "JOIN", "LANGUAGE", "LARGE", "LATERAL", "LEADING", "LEFT", "LIKE", "LN", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP", "LOWER", "MATCH", "MAX", "MEMBER", "MERGE", "METHOD", "MIN", "MINUTE", "MOD", "MODIFIES", "MODULE", "MONTH", "MULTISET", "NATIONAL", "NATURAL", "NCHAR", "NCLOB", "NEW", "NO", "NONE", "NORMALIZE", "NOT", "NULL", "NULLIF", "NUMERIC", "OCTET_LENGTH", "OF", "OLD", "ON", "ONLY", "OPEN", "OR", "ORDER", "OUT", "OUTER", "OVER", "OVERLAPS", "OVERLAY", "PARAMETER", "PARTITION", "PERCENTILE_CONT", "PERCENTILE_DISC", "PERCENT_RANK", "POSITION", "POWER", "PRECISION", "PREPARE", "PRIMARY", "PROCEDURE", "RANGE", "RANK", "READS", "REAL", "RECURSIVE", "REF", "REFERENCES", "REFERENCING", "REGR_AVGX", "REGR_AVGY", "REGR_COUNT", "REGR_INTERCEPT", "REGR_R2", "REGR_SLOPE", "REGR_SXX", "REGR_SXY", "REGR_SYY", "RELEASE", "RESULT", "RETURN", "RETURNS", "REVOKE", "RIGHT", "ROLLBACK", "ROLLUP", "ROW", "ROWS", "ROW_NUMBER", "SAVEPOINT", "SCOPE", "SCROLL", "SEARCH", "SECOND", "SELECT", "SENSITIVE", "SESSION_USER", "SET", "SIMILAR", "SMALLINT", "SOME", "SPECIFIC", "SPECIFICTYPE", "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SQRT", "START", "STATIC", "STDDEV_POP", "STDDEV_SAMP", "SUBMULTISET", "SUBSTRING", "SUM", "SYMMETRIC", "SYSTEM", "SYSTEM_USER", "TABLE", "TABLESAMPLE", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO", "TRAILING", "TRANSLATE", "TRANSLATION", "TREAT", "TRIGGER", "TRIM", "TRUE", "UESCAPE", "UNION", "UNIQUE", "UNKNOWN", "UNNEST", "UPDATE", "UPPER", "USER", "USING", "VALUE", "VALUES", "VARCHAR", "VARYING", "VAR_POP", "VAR_SAMP", "WHEN", "WHENEVER", "WHERE", "WIDTH_BUCKET", "WINDOW", "WITH", "WITHIN", "WITHOUT", "YEAR" };
  






















  private static volatile String mysqlKeywords = null;
  

  protected MySQLConnection conn;
  

  protected String database = null;
  



  protected final String quotedId;
  


  private ExceptionInterceptor exceptionInterceptor;
  



  protected static DatabaseMetaData getInstance(MySQLConnection connToSet, String databaseToSet, boolean checkForInfoSchema)
    throws SQLException
  {
    if (!Util.isJdbc4()) {
      if ((checkForInfoSchema) && (connToSet.getUseInformationSchema()) && (connToSet.versionMeetsMinimum(5, 0, 7))) {
        return new DatabaseMetaDataUsingInfoSchema(connToSet, databaseToSet);
      }
      
      return new DatabaseMetaData(connToSet, databaseToSet);
    }
    
    if ((checkForInfoSchema) && (connToSet.getUseInformationSchema()) && (connToSet.versionMeetsMinimum(5, 0, 7)))
    {
      return (DatabaseMetaData)Util.handleNewInstance(JDBC_4_DBMD_IS_CTOR, new Object[] { connToSet, databaseToSet }, connToSet.getExceptionInterceptor());
    }
    

    return (DatabaseMetaData)Util.handleNewInstance(JDBC_4_DBMD_SHOW_CTOR, new Object[] { connToSet, databaseToSet }, connToSet.getExceptionInterceptor());
  }
  





  protected DatabaseMetaData(MySQLConnection connToSet, String databaseToSet)
  {
    conn = connToSet;
    database = databaseToSet;
    exceptionInterceptor = conn.getExceptionInterceptor();
    
    String identifierQuote = null;
    try {
      identifierQuote = getIdentifierQuoteString();
    }
    catch (SQLException sqlEx) {
      AssertionFailedException.shouldNotHappen(sqlEx);
    } finally {
      quotedId = identifierQuote;
    }
  }
  





  public boolean allProceduresAreCallable()
    throws SQLException
  {
    return false;
  }
  




  public boolean allTablesAreSelectable()
    throws SQLException
  {
    return false;
  }
  
  private ResultSet buildResultSet(Field[] fields, ArrayList<ResultSetRow> rows) throws SQLException {
    return buildResultSet(fields, rows, conn);
  }
  
  static ResultSet buildResultSet(Field[] fields, ArrayList<ResultSetRow> rows, MySQLConnection c) throws SQLException {
    int fieldsLength = fields.length;
    
    for (int i = 0; i < fieldsLength; i++) {
      int jdbcType = fields[i].getSQLType();
      
      switch (jdbcType) {
      case -1: 
      case 1: 
      case 12: 
        fields[i].setEncoding(c.getCharacterSetMetadata(), c);
        break;
      }
      
      

      fields[i].setConnection(c);
      fields[i].setUseOldNameMetadata(true);
    }
    
    return ResultSetImpl.getInstance(c.getCatalog(), fields, new RowDataStatic(rows), c, null, false);
  }
  
  protected void convertToJdbcFunctionList(String catalog, ResultSet proceduresRs, boolean needsClientFiltering, String db, List<ComparableWrapper<String, ResultSetRow>> procedureRows, int nameIndex, Field[] fields) throws SQLException
  {
    while (proceduresRs.next()) {
      boolean shouldAdd = true;
      
      if (needsClientFiltering) {
        shouldAdd = false;
        
        String procDb = proceduresRs.getString(1);
        
        if ((db == null) && (procDb == null)) {
          shouldAdd = true;
        } else if ((db != null) && (db.equals(procDb))) {
          shouldAdd = true;
        }
      }
      
      if (shouldAdd) {
        String functionName = proceduresRs.getString(nameIndex);
        
        byte[][] rowData = (byte[][])null;
        
        if ((fields != null) && (fields.length == 9))
        {
          rowData = new byte[9][];
          rowData[0] = (catalog == null ? null : s2b(catalog));
          rowData[1] = null;
          rowData[2] = s2b(functionName);
          rowData[3] = null;
          rowData[4] = null;
          rowData[5] = null;
          rowData[6] = s2b(proceduresRs.getString("comment"));
          rowData[7] = s2b(Integer.toString(2));
          rowData[8] = s2b(functionName);
        }
        else {
          rowData = new byte[6][];
          
          rowData[0] = (catalog == null ? null : s2b(catalog));
          rowData[1] = null;
          rowData[2] = s2b(functionName);
          rowData[3] = s2b(proceduresRs.getString("comment"));
          rowData[4] = s2b(Integer.toString(getJDBC4FunctionNoTableConstant()));
          rowData[5] = s2b(functionName);
        }
        
        procedureRows.add(new ComparableWrapper(getFullyQualifiedName(catalog, functionName), new ByteArrayRow(rowData, getExceptionInterceptor())));
      }
    }
  }
  



  protected String getFullyQualifiedName(String catalog, String entity)
  {
    StringBuilder fullyQualifiedName = new StringBuilder(StringUtils.quoteIdentifier(catalog == null ? "" : catalog, quotedId, conn.getPedantic()));
    
    fullyQualifiedName.append('.');
    fullyQualifiedName.append(StringUtils.quoteIdentifier(entity, quotedId, conn.getPedantic()));
    return fullyQualifiedName.toString();
  }
  





  protected int getJDBC4FunctionNoTableConstant()
  {
    return 0;
  }
  
  protected void convertToJdbcProcedureList(boolean fromSelect, String catalog, ResultSet proceduresRs, boolean needsClientFiltering, String db, List<ComparableWrapper<String, ResultSetRow>> procedureRows, int nameIndex) throws SQLException
  {
    while (proceduresRs.next()) {
      boolean shouldAdd = true;
      
      if (needsClientFiltering) {
        shouldAdd = false;
        
        String procDb = proceduresRs.getString(1);
        
        if ((db == null) && (procDb == null)) {
          shouldAdd = true;
        } else if ((db != null) && (db.equals(procDb))) {
          shouldAdd = true;
        }
      }
      
      if (shouldAdd) {
        String procedureName = proceduresRs.getString(nameIndex);
        byte[][] rowData = new byte[9][];
        rowData[0] = (catalog == null ? null : s2b(catalog));
        rowData[1] = null;
        rowData[2] = s2b(procedureName);
        rowData[3] = null;
        rowData[4] = null;
        rowData[5] = null;
        rowData[6] = s2b(proceduresRs.getString("comment"));
        
        boolean isFunction = fromSelect ? "FUNCTION".equalsIgnoreCase(proceduresRs.getString("type")) : false;
        rowData[7] = s2b(isFunction ? Integer.toString(2) : Integer.toString(1));
        
        rowData[8] = s2b(procedureName);
        
        procedureRows.add(new ComparableWrapper(getFullyQualifiedName(catalog, procedureName), new ByteArrayRow(rowData, getExceptionInterceptor())));
      }
    }
  }
  
  private ResultSetRow convertTypeDescriptorToProcedureRow(byte[] procNameAsBytes, byte[] procCatAsBytes, String paramName, boolean isOutParam, boolean isInParam, boolean isReturnParam, TypeDescriptor typeDesc, boolean forGetFunctionColumns, int ordinal)
    throws SQLException
  {
    byte[][] row = forGetFunctionColumns ? new byte[17][] : new byte[20][];
    row[0] = procCatAsBytes;
    row[1] = null;
    row[2] = procNameAsBytes;
    row[3] = s2b(paramName);
    row[4] = s2b(String.valueOf(getColumnType(isOutParam, isInParam, isReturnParam, forGetFunctionColumns)));
    row[5] = s2b(Short.toString(dataType));
    row[6] = s2b(typeName);
    row[7] = (columnSize == null ? null : s2b(columnSize.toString()));
    row[8] = row[7];
    row[9] = (decimalDigits == null ? null : s2b(decimalDigits.toString()));
    row[10] = s2b(Integer.toString(numPrecRadix));
    
    switch (nullability) {
    case 0: 
      row[11] = s2b(String.valueOf(0));
      break;
    
    case 1: 
      row[11] = s2b(String.valueOf(1));
      break;
    
    case 2: 
      row[11] = s2b(String.valueOf(2));
      break;
    
    default: 
      throw SQLError.createSQLException("Internal error while parsing callable statement metadata (unknown nullability value fount)", "S1000", getExceptionInterceptor());
    }
    
    
    row[12] = null;
    
    if (forGetFunctionColumns)
    {
      row[13] = null;
      

      row[14] = s2b(String.valueOf(ordinal));
      

      row[15] = s2b(isNullable);
      

      row[16] = procNameAsBytes;
    }
    else {
      row[13] = null;
      

      row[14] = null;
      

      row[15] = null;
      

      row[16] = null;
      

      row[17] = s2b(String.valueOf(ordinal));
      

      row[18] = s2b(isNullable);
      

      row[19] = procNameAsBytes;
    }
    
    return new ByteArrayRow(row, getExceptionInterceptor());
  }
  














  protected int getColumnType(boolean isOutParam, boolean isInParam, boolean isReturnParam, boolean forGetFunctionColumns)
  {
    if ((isInParam) && (isOutParam))
      return 2;
    if (isInParam)
      return 1;
    if (isOutParam)
      return 4;
    if (isReturnParam) {
      return 5;
    }
    return 0;
  }
  


  protected ExceptionInterceptor getExceptionInterceptor()
  {
    return exceptionInterceptor;
  }
  





  public boolean dataDefinitionCausesTransactionCommit()
    throws SQLException
  {
    return true;
  }
  




  public boolean dataDefinitionIgnoredInTransactions()
    throws SQLException
  {
    return false;
  }
  









  public boolean deletesAreDetected(int type)
    throws SQLException
  {
    return false;
  }
  






  public boolean doesMaxRowSizeIncludeBlobs()
    throws SQLException
  {
    return true;
  }
  











  public List<ResultSetRow> extractForeignKeyForTable(ArrayList<ResultSetRow> rows, ResultSet rs, String catalog)
    throws SQLException
  {
    byte[][] row = new byte[3][];
    row[0] = rs.getBytes(1);
    row[1] = s2b("SUPPORTS_FK");
    
    String createTableString = rs.getString(2);
    StringTokenizer lineTokenizer = new StringTokenizer(createTableString, "\n");
    StringBuilder commentBuf = new StringBuilder("comment; ");
    boolean firstTime = true;
    
    while (lineTokenizer.hasMoreTokens()) {
      String line = lineTokenizer.nextToken().trim();
      
      String constraintName = null;
      
      if (StringUtils.startsWithIgnoreCase(line, "CONSTRAINT")) {
        boolean usingBackTicks = true;
        int beginPos = StringUtils.indexOfQuoteDoubleAware(line, quotedId, 0);
        
        if (beginPos == -1) {
          beginPos = line.indexOf("\"");
          usingBackTicks = false;
        }
        
        if (beginPos != -1) {
          int endPos = -1;
          
          if (usingBackTicks) {
            endPos = StringUtils.indexOfQuoteDoubleAware(line, quotedId, beginPos + 1);
          } else {
            endPos = StringUtils.indexOfQuoteDoubleAware(line, "\"", beginPos + 1);
          }
          
          if (endPos != -1) {
            constraintName = line.substring(beginPos + 1, endPos);
            line = line.substring(endPos + 1, line.length()).trim();
          }
        }
      }
      
      if (line.startsWith("FOREIGN KEY")) {
        if (line.endsWith(",")) {
          line = line.substring(0, line.length() - 1);
        }
        
        int indexOfFK = line.indexOf("FOREIGN KEY");
        
        String localColumnName = null;
        String referencedCatalogName = StringUtils.quoteIdentifier(catalog, quotedId, conn.getPedantic());
        String referencedTableName = null;
        String referencedColumnName = null;
        
        if (indexOfFK != -1) {
          int afterFk = indexOfFK + "FOREIGN KEY".length();
          
          int indexOfRef = StringUtils.indexOfIgnoreCase(afterFk, line, "REFERENCES", quotedId, quotedId, StringUtils.SEARCH_MODE__ALL);
          
          if (indexOfRef != -1)
          {
            int indexOfParenOpen = line.indexOf('(', afterFk);
            int indexOfParenClose = StringUtils.indexOfIgnoreCase(indexOfParenOpen, line, ")", quotedId, quotedId, StringUtils.SEARCH_MODE__ALL);
            

            if ((indexOfParenOpen != -1) && (indexOfParenClose == -1)) {}
            


            localColumnName = line.substring(indexOfParenOpen + 1, indexOfParenClose);
            
            int afterRef = indexOfRef + "REFERENCES".length();
            
            int referencedColumnBegin = StringUtils.indexOfIgnoreCase(afterRef, line, "(", quotedId, quotedId, StringUtils.SEARCH_MODE__ALL);
            

            if (referencedColumnBegin != -1) {
              referencedTableName = line.substring(afterRef, referencedColumnBegin);
              
              int referencedColumnEnd = StringUtils.indexOfIgnoreCase(referencedColumnBegin + 1, line, ")", quotedId, quotedId, StringUtils.SEARCH_MODE__ALL);
              

              if (referencedColumnEnd != -1) {
                referencedColumnName = line.substring(referencedColumnBegin + 1, referencedColumnEnd);
              }
              
              int indexOfCatalogSep = StringUtils.indexOfIgnoreCase(0, referencedTableName, ".", quotedId, quotedId, StringUtils.SEARCH_MODE__ALL);
              

              if (indexOfCatalogSep != -1) {
                referencedCatalogName = referencedTableName.substring(0, indexOfCatalogSep);
                referencedTableName = referencedTableName.substring(indexOfCatalogSep + 1);
              }
            }
          }
        }
        
        if (!firstTime) {
          commentBuf.append("; ");
        } else {
          firstTime = false;
        }
        
        if (constraintName != null) {
          commentBuf.append(constraintName);
        } else {
          commentBuf.append("not_available");
        }
        
        commentBuf.append("(");
        commentBuf.append(localColumnName);
        commentBuf.append(") REFER ");
        commentBuf.append(referencedCatalogName);
        commentBuf.append("/");
        commentBuf.append(referencedTableName);
        commentBuf.append("(");
        commentBuf.append(referencedColumnName);
        commentBuf.append(")");
        
        int lastParenIndex = line.lastIndexOf(")");
        
        if (lastParenIndex != line.length() - 1) {
          String cascadeOptions = line.substring(lastParenIndex + 1);
          commentBuf.append(" ");
          commentBuf.append(cascadeOptions);
        }
      }
    }
    
    row[2] = s2b(commentBuf.toString());
    rows.add(new ByteArrayRow(row, getExceptionInterceptor()));
    
    return rows;
  }
  














  public ResultSet extractForeignKeyFromCreateTable(String catalog, String tableName)
    throws SQLException
  {
    ArrayList<String> tableList = new ArrayList();
    ResultSet rs = null;
    java.sql.Statement stmt = null;
    
    if (tableName != null) {
      tableList.add(tableName);
    } else {
      try {
        rs = getTables(catalog, "", "%", new String[] { "TABLE" });
        
        while (rs.next()) {
          tableList.add(rs.getString("TABLE_NAME"));
        }
      } finally {
        if (rs != null) {
          rs.close();
        }
        
        rs = null;
      }
    }
    
    Object rows = new ArrayList();
    Field[] fields = new Field[3];
    fields[0] = new Field("", "Name", 1, Integer.MAX_VALUE);
    fields[1] = new Field("", "Type", 1, 255);
    fields[2] = new Field("", "Comment", 1, Integer.MAX_VALUE);
    
    int numTables = tableList.size();
    stmt = conn.getMetadataSafeStatement();
    try
    {
      for (int i = 0; i < numTables; i++) {
        String tableToExtract = (String)tableList.get(i);
        
        String query = "SHOW CREATE TABLE " + getFullyQualifiedName(catalog, tableToExtract);
        try
        {
          rs = stmt.executeQuery(query);
        }
        catch (SQLException sqlEx) {
          String sqlState = sqlEx.getSQLState();
          
          if ((!"42S02".equals(sqlState)) && (sqlEx.getErrorCode() != 1146)) {
            throw sqlEx;
          }
          
          continue;
        }
        
        while (rs.next()) {
          extractForeignKeyForTable((ArrayList)rows, rs, catalog);
        }
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
      
      rs = null;
      
      if (stmt != null) {
        stmt.close();
      }
      
      stmt = null;
    }
    
    return buildResultSet(fields, (ArrayList)rows);
  }
  

  public ResultSet getAttributes(String arg0, String arg1, String arg2, String arg3)
    throws SQLException
  {
    Field[] fields = new Field[21];
    fields[0] = new Field("", "TYPE_CAT", 1, 32);
    fields[1] = new Field("", "TYPE_SCHEM", 1, 32);
    fields[2] = new Field("", "TYPE_NAME", 1, 32);
    fields[3] = new Field("", "ATTR_NAME", 1, 32);
    fields[4] = new Field("", "DATA_TYPE", 5, 32);
    fields[5] = new Field("", "ATTR_TYPE_NAME", 1, 32);
    fields[6] = new Field("", "ATTR_SIZE", 4, 32);
    fields[7] = new Field("", "DECIMAL_DIGITS", 4, 32);
    fields[8] = new Field("", "NUM_PREC_RADIX", 4, 32);
    fields[9] = new Field("", "NULLABLE ", 4, 32);
    fields[10] = new Field("", "REMARKS", 1, 32);
    fields[11] = new Field("", "ATTR_DEF", 1, 32);
    fields[12] = new Field("", "SQL_DATA_TYPE", 4, 32);
    fields[13] = new Field("", "SQL_DATETIME_SUB", 4, 32);
    fields[14] = new Field("", "CHAR_OCTET_LENGTH", 4, 32);
    fields[15] = new Field("", "ORDINAL_POSITION", 4, 32);
    fields[16] = new Field("", "IS_NULLABLE", 1, 32);
    fields[17] = new Field("", "SCOPE_CATALOG", 1, 32);
    fields[18] = new Field("", "SCOPE_SCHEMA", 1, 32);
    fields[19] = new Field("", "SCOPE_TABLE", 1, 32);
    fields[20] = new Field("", "SOURCE_DATA_TYPE", 5, 32);
    
    return buildResultSet(fields, new ArrayList());
  }
  








































  public ResultSet getBestRowIdentifier(String catalog, String schema, final String table, int scope, boolean nullable)
    throws SQLException
  {
    if (table == null) {
      throw SQLError.createSQLException("Table not specified.", "S1009", getExceptionInterceptor());
    }
    
    Field[] fields = new Field[8];
    fields[0] = new Field("", "SCOPE", 5, 5);
    fields[1] = new Field("", "COLUMN_NAME", 1, 32);
    fields[2] = new Field("", "DATA_TYPE", 4, 32);
    fields[3] = new Field("", "TYPE_NAME", 1, 32);
    fields[4] = new Field("", "COLUMN_SIZE", 4, 10);
    fields[5] = new Field("", "BUFFER_LENGTH", 4, 10);
    fields[6] = new Field("", "DECIMAL_DIGITS", 5, 10);
    fields[7] = new Field("", "PSEUDO_COLUMN", 5, 5);
    
    final ArrayList<ResultSetRow> rows = new ArrayList();
    final java.sql.Statement stmt = conn.getMetadataSafeStatement();
    
    try
    {
      new IterateBlock(getCatalogIterator(catalog))
      {
        void forEach(String catalogStr) throws SQLException {
          ResultSet results = null;
          try
          {
            StringBuilder queryBuf = new StringBuilder("SHOW COLUMNS FROM ");
            queryBuf.append(StringUtils.quoteIdentifier(table, quotedId, conn.getPedantic()));
            queryBuf.append(" FROM ");
            queryBuf.append(StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()));
            
            results = stmt.executeQuery(queryBuf.toString());
            
            while (results.next()) {
              String keyType = results.getString("Key");
              
              if ((keyType != null) && 
                (StringUtils.startsWithIgnoreCase(keyType, "PRI"))) {
                byte[][] rowVal = new byte[8][];
                rowVal[0] = Integer.toString(2).getBytes();
                rowVal[1] = results.getBytes("Field");
                
                String type = results.getString("Type");
                int size = MysqlIO.getMaxBuf();
                int decimals = 0;
                



                if (type.indexOf("enum") != -1) {
                  String temp = type.substring(type.indexOf("("), type.indexOf(")"));
                  StringTokenizer tokenizer = new StringTokenizer(temp, ",");
                  int maxLength = 0;
                  
                  while (tokenizer.hasMoreTokens()) {
                    maxLength = Math.max(maxLength, tokenizer.nextToken().length() - 2);
                  }
                  
                  size = maxLength;
                  decimals = 0;
                  type = "enum";
                } else if (type.indexOf("(") != -1) {
                  if (type.indexOf(",") != -1) {
                    size = Integer.parseInt(type.substring(type.indexOf("(") + 1, type.indexOf(",")));
                    decimals = Integer.parseInt(type.substring(type.indexOf(",") + 1, type.indexOf(")")));
                  } else {
                    size = Integer.parseInt(type.substring(type.indexOf("(") + 1, type.indexOf(")")));
                  }
                  
                  type = type.substring(0, type.indexOf("("));
                }
                
                rowVal[2] = s2b(String.valueOf(MysqlDefs.mysqlToJavaType(type)));
                rowVal[3] = s2b(type);
                rowVal[4] = Integer.toString(size + decimals).getBytes();
                rowVal[5] = Integer.toString(size + decimals).getBytes();
                rowVal[6] = Integer.toString(decimals).getBytes();
                rowVal[7] = Integer.toString(1).getBytes();
                
                rows.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
              }
            }
          }
          catch (SQLException sqlEx) {
            if (!"42S02".equals(sqlEx.getSQLState())) {
              throw sqlEx;
            }
          } finally {
            if (results != null) {
              try {
                results.close();
              }
              catch (Exception ex) {}
              
              results = null;
            }
          }
        }
      }.doForAll();
    } finally {
      if (stmt != null) {
        stmt.close();
      }
    }
    
    ResultSet results = buildResultSet(fields, rows);
    
    return results;
  }
  






  private void getCallStmtParameterTypes(String catalog, String quotedProcName, ProcedureType procType, String parameterNamePattern, List<ResultSetRow> resultRows, boolean forGetFunctionColumns)
    throws SQLException
  {
    java.sql.Statement paramRetrievalStmt = null;
    ResultSet paramRetrievalRs = null;
    
    if (parameterNamePattern == null) {
      if (conn.getNullNamePatternMatchesAll()) {
        parameterNamePattern = "%";
      } else {
        throw SQLError.createSQLException("Parameter/Column name pattern can not be NULL or empty.", "S1009", getExceptionInterceptor());
      }
    }
    

    String parameterDef = null;
    
    byte[] procNameAsBytes = null;
    byte[] procCatAsBytes = null;
    
    boolean isProcedureInAnsiMode = false;
    String storageDefnDelims = null;
    String storageDefnClosures = null;
    try
    {
      paramRetrievalStmt = conn.getMetadataSafeStatement();
      
      String oldCatalog = conn.getCatalog();
      if ((conn.lowerCaseTableNames()) && (catalog != null) && (catalog.length() != 0) && (oldCatalog != null) && (oldCatalog.length() != 0))
      {

        ResultSet rs = null;
        try
        {
          conn.setCatalog(StringUtils.unQuoteIdentifier(catalog, quotedId));
          rs = paramRetrievalStmt.executeQuery("SELECT DATABASE()");
          rs.next();
          
          catalog = rs.getString(1);
        }
        finally
        {
          conn.setCatalog(oldCatalog);
          
          if (rs != null) {
            rs.close();
          }
        }
      }
      
      if (paramRetrievalStmt.getMaxRows() != 0) {
        paramRetrievalStmt.setMaxRows(0);
      }
      
      int dotIndex = -1;
      
      if (!" ".equals(quotedId)) {
        dotIndex = StringUtils.indexOfIgnoreCase(0, quotedProcName, ".", quotedId, quotedId, conn.isNoBackslashEscapesSet() ? StringUtils.SEARCH_MODE__MRK_COM_WS : StringUtils.SEARCH_MODE__ALL);
      }
      else {
        dotIndex = quotedProcName.indexOf(".");
      }
      
      String dbName = null;
      
      if ((dotIndex != -1) && (dotIndex + 1 < quotedProcName.length())) {
        dbName = quotedProcName.substring(0, dotIndex);
        quotedProcName = quotedProcName.substring(dotIndex + 1);
      } else {
        dbName = StringUtils.quoteIdentifier(catalog, quotedId, conn.getPedantic());
      }
      


      String tmpProcName = StringUtils.unQuoteIdentifier(quotedProcName, quotedId);
      try {
        procNameAsBytes = StringUtils.getBytes(tmpProcName, "UTF-8");
      } catch (UnsupportedEncodingException ueEx) {
        procNameAsBytes = s2b(tmpProcName);
      }
      


      tmpProcName = StringUtils.unQuoteIdentifier(dbName, quotedId);
      try {
        procCatAsBytes = StringUtils.getBytes(tmpProcName, "UTF-8");
      } catch (UnsupportedEncodingException ueEx) {
        procCatAsBytes = s2b(tmpProcName);
      }
      



      StringBuilder procNameBuf = new StringBuilder();
      procNameBuf.append(dbName);
      procNameBuf.append('.');
      procNameBuf.append(quotedProcName);
      
      String fieldName = null;
      if (procType == ProcedureType.PROCEDURE) {
        paramRetrievalRs = paramRetrievalStmt.executeQuery("SHOW CREATE PROCEDURE " + procNameBuf.toString());
        fieldName = "Create Procedure";
      } else {
        paramRetrievalRs = paramRetrievalStmt.executeQuery("SHOW CREATE FUNCTION " + procNameBuf.toString());
        fieldName = "Create Function";
      }
      
      if (paramRetrievalRs.next()) {
        String procedureDef = paramRetrievalRs.getString(fieldName);
        
        if ((!conn.getNoAccessToProcedureBodies()) && ((procedureDef == null) || (procedureDef.length() == 0))) {
          throw SQLError.createSQLException("User does not have access to metadata required to determine stored procedure parameter types. If rights can not be granted, configure connection with \"noAccessToProcedureBodies=true\" to have driver generate parameters that represent INOUT strings irregardless of actual parameter types.", "S1000", getExceptionInterceptor());
        }
        



        try
        {
          String sqlMode = paramRetrievalRs.getString("sql_mode");
          
          if (StringUtils.indexOfIgnoreCase(sqlMode, "ANSI") != -1) {
            isProcedureInAnsiMode = true;
          }
        }
        catch (SQLException sqlEx) {}
        

        String identifierMarkers = isProcedureInAnsiMode ? "`\"" : "`";
        String identifierAndStringMarkers = "'" + identifierMarkers;
        storageDefnDelims = "(" + identifierMarkers;
        storageDefnClosures = ")" + identifierMarkers;
        
        if ((procedureDef != null) && (procedureDef.length() != 0))
        {
          procedureDef = StringUtils.stripComments(procedureDef, identifierAndStringMarkers, identifierAndStringMarkers, true, false, true, true);
          
          int openParenIndex = StringUtils.indexOfIgnoreCase(0, procedureDef, "(", quotedId, quotedId, conn.isNoBackslashEscapesSet() ? StringUtils.SEARCH_MODE__MRK_COM_WS : StringUtils.SEARCH_MODE__ALL);
          
          int endOfParamDeclarationIndex = 0;
          
          endOfParamDeclarationIndex = endPositionOfParameterDeclaration(openParenIndex, procedureDef, quotedId);
          
          if (procType == ProcedureType.FUNCTION)
          {


            int returnsIndex = StringUtils.indexOfIgnoreCase(0, procedureDef, " RETURNS ", quotedId, quotedId, conn.isNoBackslashEscapesSet() ? StringUtils.SEARCH_MODE__MRK_COM_WS : StringUtils.SEARCH_MODE__ALL);
            

            int endReturnsDef = findEndOfReturnsClause(procedureDef, returnsIndex);
            


            int declarationStart = returnsIndex + "RETURNS ".length();
            
            while ((declarationStart < procedureDef.length()) && 
              (Character.isWhitespace(procedureDef.charAt(declarationStart)))) {
              declarationStart++;
            }
            



            String returnsDefn = procedureDef.substring(declarationStart, endReturnsDef).trim();
            TypeDescriptor returnDescriptor = new TypeDescriptor(returnsDefn, "YES");
            
            resultRows.add(convertTypeDescriptorToProcedureRow(procNameAsBytes, procCatAsBytes, "", false, false, true, returnDescriptor, forGetFunctionColumns, 0));
          }
          

          if ((openParenIndex == -1) || (endOfParamDeclarationIndex == -1))
          {
            throw SQLError.createSQLException("Internal error when parsing callable statement metadata", "S1000", getExceptionInterceptor());
          }
          

          parameterDef = procedureDef.substring(openParenIndex + 1, endOfParamDeclarationIndex);
        }
      }
    }
    finally {
      SQLException sqlExRethrow = null;
      
      if (paramRetrievalRs != null) {
        try {
          paramRetrievalRs.close();
        } catch (SQLException sqlEx) {
          sqlExRethrow = sqlEx;
        }
        
        paramRetrievalRs = null;
      }
      
      if (paramRetrievalStmt != null) {
        try {
          paramRetrievalStmt.close();
        } catch (SQLException sqlEx) {
          sqlExRethrow = sqlEx;
        }
        
        paramRetrievalStmt = null;
      }
      
      if (sqlExRethrow != null) {
        throw sqlExRethrow;
      }
    }
    
    if (parameterDef != null) {
      int ordinal = 1;
      
      List<String> parseList = StringUtils.split(parameterDef, ",", storageDefnDelims, storageDefnClosures, true);
      
      int parseListLen = parseList.size();
      
      for (int i = 0; i < parseListLen; i++) {
        String declaration = (String)parseList.get(i);
        
        if (declaration.trim().length() == 0) {
          break;
        }
        

        declaration = declaration.replaceAll("[\\t\\n\\x0B\\f\\r]", " ");
        StringTokenizer declarationTok = new StringTokenizer(declaration, " \t");
        
        String paramName = null;
        boolean isOutParam = false;
        boolean isInParam = false;
        
        if (declarationTok.hasMoreTokens()) {
          String possibleParamName = declarationTok.nextToken();
          
          if (possibleParamName.equalsIgnoreCase("OUT")) {
            isOutParam = true;
            
            if (declarationTok.hasMoreTokens()) {
              paramName = declarationTok.nextToken();
            } else {
              throw SQLError.createSQLException("Internal error when parsing callable statement metadata (missing parameter name)", "S1000", getExceptionInterceptor());
            }
          }
          else if (possibleParamName.equalsIgnoreCase("INOUT")) {
            isOutParam = true;
            isInParam = true;
            
            if (declarationTok.hasMoreTokens()) {
              paramName = declarationTok.nextToken();
            } else {
              throw SQLError.createSQLException("Internal error when parsing callable statement metadata (missing parameter name)", "S1000", getExceptionInterceptor());
            }
          }
          else if (possibleParamName.equalsIgnoreCase("IN")) {
            isOutParam = false;
            isInParam = true;
            
            if (declarationTok.hasMoreTokens()) {
              paramName = declarationTok.nextToken();
            } else {
              throw SQLError.createSQLException("Internal error when parsing callable statement metadata (missing parameter name)", "S1000", getExceptionInterceptor());
            }
          }
          else {
            isOutParam = false;
            isInParam = true;
            
            paramName = possibleParamName;
          }
          
          TypeDescriptor typeDesc = null;
          
          if (declarationTok.hasMoreTokens()) {
            StringBuilder typeInfoBuf = new StringBuilder(declarationTok.nextToken());
            
            while (declarationTok.hasMoreTokens()) {
              typeInfoBuf.append(" ");
              typeInfoBuf.append(declarationTok.nextToken());
            }
            
            String typeInfo = typeInfoBuf.toString();
            
            typeDesc = new TypeDescriptor(typeInfo, "YES");
          } else {
            throw SQLError.createSQLException("Internal error when parsing callable statement metadata (missing parameter type)", "S1000", getExceptionInterceptor());
          }
          

          if (((paramName.startsWith("`")) && (paramName.endsWith("`"))) || ((isProcedureInAnsiMode) && (paramName.startsWith("\"")) && (paramName.endsWith("\""))))
          {
            paramName = paramName.substring(1, paramName.length() - 1);
          }
          
          if (StringUtils.wildCompareIgnoreCase(paramName, parameterNamePattern)) {
            ResultSetRow row = convertTypeDescriptorToProcedureRow(procNameAsBytes, procCatAsBytes, paramName, isOutParam, isInParam, false, typeDesc, forGetFunctionColumns, ordinal++);
            

            resultRows.add(row);
          }
        } else {
          throw SQLError.createSQLException("Internal error when parsing callable statement metadata (unknown output from 'SHOW CREATE PROCEDURE')", "S1000", getExceptionInterceptor());
        }
      }
    }
  }
  

















  private int endPositionOfParameterDeclaration(int beginIndex, String procedureDef, String quoteChar)
    throws SQLException
  {
    int currentPos = beginIndex + 1;
    int parenDepth = 1;
    
    while ((parenDepth > 0) && (currentPos < procedureDef.length())) {
      int closedParenIndex = StringUtils.indexOfIgnoreCase(currentPos, procedureDef, ")", quoteChar, quoteChar, conn.isNoBackslashEscapesSet() ? StringUtils.SEARCH_MODE__MRK_COM_WS : StringUtils.SEARCH_MODE__ALL);
      

      if (closedParenIndex != -1) {
        int nextOpenParenIndex = StringUtils.indexOfIgnoreCase(currentPos, procedureDef, "(", quoteChar, quoteChar, conn.isNoBackslashEscapesSet() ? StringUtils.SEARCH_MODE__MRK_COM_WS : StringUtils.SEARCH_MODE__ALL);
        

        if ((nextOpenParenIndex != -1) && (nextOpenParenIndex < closedParenIndex)) {
          parenDepth++;
          currentPos = closedParenIndex + 1;
        } else {
          parenDepth--;
          currentPos = closedParenIndex;
        }
      }
      else {
        throw SQLError.createSQLException("Internal error when parsing callable statement metadata", "S1000", getExceptionInterceptor());
      }
    }
    

    return currentPos;
  }
  

















  private int findEndOfReturnsClause(String procedureDefn, int positionOfReturnKeyword)
    throws SQLException
  {
    String openingMarkers = quotedId + "(";
    String closingMarkers = quotedId + ")";
    
    String[] tokens = { "LANGUAGE", "NOT", "DETERMINISTIC", "CONTAINS", "NO", "READ", "MODIFIES", "SQL", "COMMENT", "BEGIN", "RETURN" };
    
    int startLookingAt = positionOfReturnKeyword + "RETURNS".length() + 1;
    
    int endOfReturn = -1;
    
    for (int i = 0; i < tokens.length; i++) {
      int nextEndOfReturn = StringUtils.indexOfIgnoreCase(startLookingAt, procedureDefn, tokens[i], openingMarkers, closingMarkers, conn.isNoBackslashEscapesSet() ? StringUtils.SEARCH_MODE__MRK_COM_WS : StringUtils.SEARCH_MODE__ALL);
      

      if ((nextEndOfReturn != -1) && (
        (endOfReturn == -1) || (nextEndOfReturn < endOfReturn))) {
        endOfReturn = nextEndOfReturn;
      }
    }
    

    if (endOfReturn != -1) {
      return endOfReturn;
    }
    

    endOfReturn = StringUtils.indexOfIgnoreCase(startLookingAt, procedureDefn, ":", openingMarkers, closingMarkers, conn.isNoBackslashEscapesSet() ? StringUtils.SEARCH_MODE__MRK_COM_WS : StringUtils.SEARCH_MODE__ALL);
    

    if (endOfReturn != -1)
    {
      for (int i = endOfReturn; i > 0; i--) {
        if (Character.isWhitespace(procedureDefn.charAt(i))) {
          return i;
        }
      }
    }
    


    throw SQLError.createSQLException("Internal error when parsing callable statement metadata", "S1000", getExceptionInterceptor());
  }
  








  private int getCascadeDeleteOption(String cascadeOptions)
  {
    int onDeletePos = cascadeOptions.indexOf("ON DELETE");
    
    if (onDeletePos != -1) {
      String deleteOptions = cascadeOptions.substring(onDeletePos, cascadeOptions.length());
      
      if (deleteOptions.startsWith("ON DELETE CASCADE"))
        return 0;
      if (deleteOptions.startsWith("ON DELETE SET NULL"))
        return 2;
      if (deleteOptions.startsWith("ON DELETE RESTRICT"))
        return 1;
      if (deleteOptions.startsWith("ON DELETE NO ACTION")) {
        return 3;
      }
    }
    
    return 3;
  }
  







  private int getCascadeUpdateOption(String cascadeOptions)
  {
    int onUpdatePos = cascadeOptions.indexOf("ON UPDATE");
    
    if (onUpdatePos != -1) {
      String updateOptions = cascadeOptions.substring(onUpdatePos, cascadeOptions.length());
      
      if (updateOptions.startsWith("ON UPDATE CASCADE"))
        return 0;
      if (updateOptions.startsWith("ON UPDATE SET NULL"))
        return 2;
      if (updateOptions.startsWith("ON UPDATE RESTRICT"))
        return 1;
      if (updateOptions.startsWith("ON UPDATE NO ACTION")) {
        return 3;
      }
    }
    
    return 3;
  }
  
  protected IteratorWithCleanup<String> getCatalogIterator(String catalogSpec) throws SQLException { IteratorWithCleanup<String> allCatalogsIter;
    IteratorWithCleanup<String> allCatalogsIter;
    if (catalogSpec != null) { IteratorWithCleanup<String> allCatalogsIter;
      if (!catalogSpec.equals("")) { IteratorWithCleanup<String> allCatalogsIter;
        if (conn.getPedantic()) {
          allCatalogsIter = new SingleStringIterator(catalogSpec);
        } else {
          allCatalogsIter = new SingleStringIterator(StringUtils.unQuoteIdentifier(catalogSpec, quotedId));
        }
      }
      else {
        allCatalogsIter = new SingleStringIterator(database);
      } } else { IteratorWithCleanup<String> allCatalogsIter;
      if (conn.getNullCatalogMeansCurrent())
      {
        allCatalogsIter = new SingleStringIterator(database);
      } else {
        allCatalogsIter = new ResultSetIterator(getCatalogs(), 1);
      }
    }
    return allCatalogsIter;
  }
  












  public ResultSet getCatalogs()
    throws SQLException
  {
    ResultSet results = null;
    java.sql.Statement stmt = null;
    try
    {
      stmt = conn.getMetadataSafeStatement();
      results = stmt.executeQuery("SHOW DATABASES");
      
      int catalogsCount = 0;
      if (results.last()) {
        catalogsCount = results.getRow();
        results.beforeFirst();
      }
      
      List<String> resultsAsList = new ArrayList(catalogsCount);
      while (results.next()) {
        resultsAsList.add(results.getString(1));
      }
      Collections.sort(resultsAsList);
      
      Field[] fields = new Field[1];
      fields[0] = new Field("", "TABLE_CAT", 12, results.getMetaData().getColumnDisplaySize(1));
      
      ArrayList<ResultSetRow> tuples = new ArrayList(catalogsCount);
      for (String cat : resultsAsList) {
        byte[][] rowVal = new byte[1][];
        rowVal[0] = s2b(cat);
        tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
      }
      
      return buildResultSet(fields, tuples);
    } finally {
      if (results != null) {
        try {
          results.close();
        } catch (SQLException sqlEx) {
          AssertionFailedException.shouldNotHappen(sqlEx);
        }
        
        results = null;
      }
      
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException sqlEx) {
          AssertionFailedException.shouldNotHappen(sqlEx);
        }
        
        stmt = null;
      }
    }
  }
  




  public String getCatalogSeparator()
    throws SQLException
  {
    return ".";
  }
  







  public String getCatalogTerm()
    throws SQLException
  {
    return "database";
  }
  






























  public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern)
    throws SQLException
  {
    Field[] fields = new Field[8];
    fields[0] = new Field("", "TABLE_CAT", 1, 64);
    fields[1] = new Field("", "TABLE_SCHEM", 1, 1);
    fields[2] = new Field("", "TABLE_NAME", 1, 64);
    fields[3] = new Field("", "COLUMN_NAME", 1, 64);
    fields[4] = new Field("", "GRANTOR", 1, 77);
    fields[5] = new Field("", "GRANTEE", 1, 77);
    fields[6] = new Field("", "PRIVILEGE", 1, 64);
    fields[7] = new Field("", "IS_GRANTABLE", 1, 3);
    
    String grantQuery = "SELECT c.host, c.db, t.grantor, c.user, c.table_name, c.column_name, c.column_priv FROM mysql.columns_priv c, mysql.tables_priv t WHERE c.host = t.host AND c.db = t.db AND c.table_name = t.table_name AND c.db LIKE ? AND c.table_name = ? AND c.column_name LIKE ?";
    


    PreparedStatement pStmt = null;
    ResultSet results = null;
    ArrayList<ResultSetRow> grantRows = new ArrayList();
    try
    {
      pStmt = prepareMetaDataSafeStatement(grantQuery);
      
      pStmt.setString(1, (catalog != null) && (catalog.length() != 0) ? catalog : "%");
      pStmt.setString(2, table);
      pStmt.setString(3, columnNamePattern);
      
      results = pStmt.executeQuery();
      
      while (results.next()) {
        String host = results.getString(1);
        String db = results.getString(2);
        String grantor = results.getString(3);
        String user = results.getString(4);
        
        if ((user == null) || (user.length() == 0)) {
          user = "%";
        }
        
        StringBuilder fullUser = new StringBuilder(user);
        
        if ((host != null) && (conn.getUseHostsInPrivileges())) {
          fullUser.append("@");
          fullUser.append(host);
        }
        
        String columnName = results.getString(6);
        String allPrivileges = results.getString(7);
        
        if (allPrivileges != null) {
          allPrivileges = allPrivileges.toUpperCase(Locale.ENGLISH);
          
          StringTokenizer st = new StringTokenizer(allPrivileges, ",");
          
          while (st.hasMoreTokens()) {
            String privilege = st.nextToken().trim();
            byte[][] tuple = new byte[8][];
            tuple[0] = s2b(db);
            tuple[1] = null;
            tuple[2] = s2b(table);
            tuple[3] = s2b(columnName);
            
            if (grantor != null) {
              tuple[4] = s2b(grantor);
            } else {
              tuple[4] = null;
            }
            
            tuple[5] = s2b(fullUser.toString());
            tuple[6] = s2b(privilege);
            tuple[7] = null;
            grantRows.add(new ByteArrayRow(tuple, getExceptionInterceptor()));
          }
        }
      }
    } finally {
      if (results != null) {
        try {
          results.close();
        }
        catch (Exception ex) {}
        
        results = null;
      }
      
      if (pStmt != null) {
        try {
          pStmt.close();
        }
        catch (Exception ex) {}
        
        pStmt = null;
      }
    }
    
    return buildResultSet(fields, grantRows);
  }
  



















































  public ResultSet getColumns(String catalog, final String schemaPattern, final String tableNamePattern, String columnNamePattern)
    throws SQLException
  {
    if (columnNamePattern == null) {
      if (conn.getNullNamePatternMatchesAll()) {
        columnNamePattern = "%";
      } else {
        throw SQLError.createSQLException("Column name pattern can not be NULL or empty.", "S1009", getExceptionInterceptor());
      }
    }
    

    final String colPattern = columnNamePattern;
    
    Field[] fields = createColumnsFields();
    
    final ArrayList<ResultSetRow> rows = new ArrayList();
    final java.sql.Statement stmt = conn.getMetadataSafeStatement();
    
    try
    {
      new IterateBlock(getCatalogIterator(catalog))
      {
        void forEach(String catalogStr) throws SQLException
        {
          ArrayList<String> tableNameList = new ArrayList();
          
          if (tableNamePattern == null)
          {
            ResultSet tables = null;
            try
            {
              tables = getTables(catalogStr, schemaPattern, "%", new String[0]);
              
              while (tables.next()) {
                String tableNameFromList = tables.getString("TABLE_NAME");
                tableNameList.add(tableNameFromList);
              }
            } finally {
              if (tables != null) {
                try {
                  tables.close();
                } catch (Exception sqlEx) {
                  AssertionFailedException.shouldNotHappen(sqlEx);
                }
                
                tables = null;
              }
            }
          } else {
            ResultSet tables = null;
            try
            {
              tables = getTables(catalogStr, schemaPattern, tableNamePattern, new String[0]);
              
              while (tables.next()) {
                String tableNameFromList = tables.getString("TABLE_NAME");
                tableNameList.add(tableNameFromList);
              }
            } finally {
              if (tables != null) {
                try {
                  tables.close();
                } catch (SQLException sqlEx) {
                  AssertionFailedException.shouldNotHappen(sqlEx);
                }
                
                tables = null;
              }
            }
          }
          
          for (String tableName : tableNameList)
          {
            ResultSet results = null;
            try
            {
              StringBuilder queryBuf = new StringBuilder("SHOW ");
              
              if (conn.versionMeetsMinimum(4, 1, 0)) {
                queryBuf.append("FULL ");
              }
              
              queryBuf.append("COLUMNS FROM ");
              queryBuf.append(StringUtils.quoteIdentifier(tableName, quotedId, conn.getPedantic()));
              queryBuf.append(" FROM ");
              queryBuf.append(StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()));
              queryBuf.append(" LIKE ");
              queryBuf.append(StringUtils.quoteIdentifier(colPattern, "'", true));
              



              boolean fixUpOrdinalsRequired = false;
              Object ordinalFixUpMap = null;
              
              if (!colPattern.equals("%")) {
                fixUpOrdinalsRequired = true;
                
                StringBuilder fullColumnQueryBuf = new StringBuilder("SHOW ");
                
                if (conn.versionMeetsMinimum(4, 1, 0)) {
                  fullColumnQueryBuf.append("FULL ");
                }
                
                fullColumnQueryBuf.append("COLUMNS FROM ");
                fullColumnQueryBuf.append(StringUtils.quoteIdentifier(tableName, quotedId, conn.getPedantic()));
                
                fullColumnQueryBuf.append(" FROM ");
                fullColumnQueryBuf.append(StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()));
                

                results = stmt.executeQuery(fullColumnQueryBuf.toString());
                
                ordinalFixUpMap = new HashMap();
                
                int fullOrdinalPos = 1;
                
                while (results.next()) {
                  String fullOrdColName = results.getString("Field");
                  
                  ((Map)ordinalFixUpMap).put(fullOrdColName, Integer.valueOf(fullOrdinalPos++));
                }
              }
              
              results = stmt.executeQuery(queryBuf.toString());
              
              int ordPos = 1;
              
              while (results.next()) {
                byte[][] rowVal = new byte[24][];
                rowVal[0] = s2b(catalogStr);
                rowVal[1] = null;
                

                rowVal[2] = s2b(tableName);
                rowVal[3] = results.getBytes("Field");
                
                DatabaseMetaData.TypeDescriptor typeDesc = new DatabaseMetaData.TypeDescriptor(DatabaseMetaData.this, results.getString("Type"), results.getString("Null"));
                
                rowVal[4] = Short.toString(dataType).getBytes();
                

                rowVal[5] = s2b(typeName);
                
                if (columnSize == null) {
                  rowVal[6] = null;
                } else {
                  String collation = results.getString("Collation");
                  int mbminlen = 1;
                  if ((collation != null) && (("TEXT".equals(typeName)) || ("TINYTEXT".equals(typeName)) || ("MEDIUMTEXT".equals(typeName))))
                  {
                    if ((collation.indexOf("ucs2") > -1) || (collation.indexOf("utf16") > -1)) {
                      mbminlen = 2;
                    } else if (collation.indexOf("utf32") > -1) {
                      mbminlen = 4;
                    }
                  }
                  rowVal[6] = (mbminlen == 1 ? s2b(columnSize.toString()) : s2b(Integer.valueOf(columnSize.intValue() / mbminlen).toString()));
                }
                
                rowVal[7] = s2b(Integer.toString(bufferLength));
                rowVal[8] = (decimalDigits == null ? null : s2b(decimalDigits.toString()));
                rowVal[9] = s2b(Integer.toString(numPrecRadix));
                rowVal[10] = s2b(Integer.toString(nullability));
                





                try
                {
                  if (conn.versionMeetsMinimum(4, 1, 0)) {
                    rowVal[11] = results.getBytes("Comment");
                  } else {
                    rowVal[11] = results.getBytes("Extra");
                  }
                } catch (Exception E) {
                  rowVal[11] = new byte[0];
                }
                

                rowVal[12] = results.getBytes("Default");
                
                rowVal[13] = { 48 };
                rowVal[14] = { 48 };
                
                if ((StringUtils.indexOfIgnoreCase(typeName, "CHAR") != -1) || (StringUtils.indexOfIgnoreCase(typeName, "BLOB") != -1) || (StringUtils.indexOfIgnoreCase(typeName, "TEXT") != -1) || (StringUtils.indexOfIgnoreCase(typeName, "BINARY") != -1))
                {


                  rowVal[15] = rowVal[6];
                } else {
                  rowVal[15] = null;
                }
                

                if (!fixUpOrdinalsRequired) {
                  rowVal[16] = Integer.toString(ordPos++).getBytes();
                } else {
                  String origColName = results.getString("Field");
                  Integer realOrdinal = (Integer)((Map)ordinalFixUpMap).get(origColName);
                  
                  if (realOrdinal != null) {
                    rowVal[16] = realOrdinal.toString().getBytes();
                  } else {
                    throw SQLError.createSQLException("Can not find column in full column list to determine true ordinal position.", "S1000", getExceptionInterceptor());
                  }
                }
                

                rowVal[17] = s2b(isNullable);
                

                rowVal[18] = null;
                rowVal[19] = null;
                rowVal[20] = null;
                rowVal[21] = null;
                
                rowVal[22] = s2b("");
                
                String extra = results.getString("Extra");
                
                if (extra != null) {
                  rowVal[22] = s2b(StringUtils.indexOfIgnoreCase(extra, "auto_increment") != -1 ? "YES" : "NO");
                  rowVal[23] = s2b(StringUtils.indexOfIgnoreCase(extra, "generated") != -1 ? "YES" : "NO");
                }
                
                rows.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
              }
            } finally {
              if (results != null) {
                try {
                  results.close();
                }
                catch (Exception ex) {}
                
                results = null;
              }
            }
          }
        }
      }.doForAll();
    } finally {
      if (stmt != null) {
        stmt.close();
      }
    }
    
    ResultSet results = buildResultSet(fields, rows);
    
    return results;
  }
  
  protected Field[] createColumnsFields() {
    Field[] fields = new Field[24];
    fields[0] = new Field("", "TABLE_CAT", 1, 255);
    fields[1] = new Field("", "TABLE_SCHEM", 1, 0);
    fields[2] = new Field("", "TABLE_NAME", 1, 255);
    fields[3] = new Field("", "COLUMN_NAME", 1, 32);
    fields[4] = new Field("", "DATA_TYPE", 4, 5);
    fields[5] = new Field("", "TYPE_NAME", 1, 16);
    fields[6] = new Field("", "COLUMN_SIZE", 4, Integer.toString(Integer.MAX_VALUE).length());
    fields[7] = new Field("", "BUFFER_LENGTH", 4, 10);
    fields[8] = new Field("", "DECIMAL_DIGITS", 4, 10);
    fields[9] = new Field("", "NUM_PREC_RADIX", 4, 10);
    fields[10] = new Field("", "NULLABLE", 4, 10);
    fields[11] = new Field("", "REMARKS", 1, 0);
    fields[12] = new Field("", "COLUMN_DEF", 1, 0);
    fields[13] = new Field("", "SQL_DATA_TYPE", 4, 10);
    fields[14] = new Field("", "SQL_DATETIME_SUB", 4, 10);
    fields[15] = new Field("", "CHAR_OCTET_LENGTH", 4, Integer.toString(Integer.MAX_VALUE).length());
    fields[16] = new Field("", "ORDINAL_POSITION", 4, 10);
    fields[17] = new Field("", "IS_NULLABLE", 1, 3);
    fields[18] = new Field("", "SCOPE_CATALOG", 1, 255);
    fields[19] = new Field("", "SCOPE_SCHEMA", 1, 255);
    fields[20] = new Field("", "SCOPE_TABLE", 1, 255);
    fields[21] = new Field("", "SOURCE_DATA_TYPE", 5, 10);
    fields[22] = new Field("", "IS_AUTOINCREMENT", 1, 3);
    fields[23] = new Field("", "IS_GENERATEDCOLUMN", 1, 3);
    return fields;
  }
  





  public Connection getConnection()
    throws SQLException
  {
    return conn;
  }
  





















































  public ResultSet getCrossReference(final String primaryCatalog, final String primarySchema, final String primaryTable, final String foreignCatalog, final String foreignSchema, final String foreignTable)
    throws SQLException
  {
    if (primaryTable == null) {
      throw SQLError.createSQLException("Table not specified.", "S1009", getExceptionInterceptor());
    }
    
    Field[] fields = createFkMetadataFields();
    
    final ArrayList<ResultSetRow> tuples = new ArrayList();
    
    if (conn.versionMeetsMinimum(3, 23, 0))
    {
      final java.sql.Statement stmt = conn.getMetadataSafeStatement();
      
      try
      {
        new IterateBlock(getCatalogIterator(foreignCatalog))
        {
          void forEach(String catalogStr) throws SQLException
          {
            ResultSet fkresults = null;
            



            try
            {
              if (conn.versionMeetsMinimum(3, 23, 50)) {
                fkresults = extractForeignKeyFromCreateTable(catalogStr, null);
              } else {
                StringBuilder queryBuf = new StringBuilder("SHOW TABLE STATUS FROM ");
                queryBuf.append(StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()));
                

                fkresults = stmt.executeQuery(queryBuf.toString());
              }
              
              String foreignTableWithCase = getTableNameWithCase(foreignTable);
              String primaryTableWithCase = getTableNameWithCase(primaryTable);
              






              while (fkresults.next()) {
                String tableType = fkresults.getString("Type");
                
                if ((tableType != null) && ((tableType.equalsIgnoreCase("innodb")) || (tableType.equalsIgnoreCase("SUPPORTS_FK")))) {
                  String comment = fkresults.getString("Comment").trim();
                  
                  if (comment != null) {
                    StringTokenizer commentTokens = new StringTokenizer(comment, ";", false);
                    
                    if (commentTokens.hasMoreTokens()) {
                      String str1 = commentTokens.nextToken();
                    }
                    


                    while (commentTokens.hasMoreTokens()) {
                      String keys = commentTokens.nextToken();
                      DatabaseMetaData.LocalAndReferencedColumns parsedInfo = parseTableStatusIntoLocalAndReferencedColumns(keys);
                      
                      int keySeq = 0;
                      
                      Iterator<String> referencingColumns = localColumnsList.iterator();
                      Iterator<String> referencedColumns = referencedColumnsList.iterator();
                      
                      while (referencingColumns.hasNext()) {
                        String referencingColumn = StringUtils.unQuoteIdentifier((String)referencingColumns.next(), quotedId);
                        


                        byte[][] tuple = new byte[14][];
                        tuple[4] = (foreignCatalog == null ? null : s2b(foreignCatalog));
                        tuple[5] = (foreignSchema == null ? null : s2b(foreignSchema));
                        String dummy = fkresults.getString("Name");
                        
                        if (dummy.compareTo(foreignTableWithCase) == 0)
                        {


                          tuple[6] = s2b(dummy);
                          
                          tuple[7] = s2b(referencingColumn);
                          tuple[0] = (primaryCatalog == null ? null : s2b(primaryCatalog));
                          tuple[1] = (primarySchema == null ? null : s2b(primarySchema));
                          

                          if (referencedTable.compareTo(primaryTableWithCase) == 0)
                          {


                            tuple[2] = s2b(referencedTable);
                            tuple[3] = s2b(StringUtils.unQuoteIdentifier((String)referencedColumns.next(), quotedId));
                            tuple[8] = Integer.toString(keySeq).getBytes();
                            
                            int[] actions = getForeignKeyActions(keys);
                            
                            tuple[9] = Integer.toString(actions[1]).getBytes();
                            tuple[10] = Integer.toString(actions[0]).getBytes();
                            tuple[11] = null;
                            tuple[12] = null;
                            tuple[13] = Integer.toString(7).getBytes();
                            tuples.add(new ByteArrayRow(tuple, getExceptionInterceptor()));
                            keySeq++;
                          }
                        }
                      }
                    }
                  }
                }
              }
            } finally { if (fkresults != null) {
                try {
                  fkresults.close();
                } catch (Exception sqlEx) {
                  AssertionFailedException.shouldNotHappen(sqlEx);
                }
                
                fkresults = null;
              }
            }
          }
        }.doForAll();
      } finally {
        if (stmt != null) {
          stmt.close();
        }
      }
    }
    
    ResultSet results = buildResultSet(fields, tuples);
    
    return results;
  }
  
  protected Field[] createFkMetadataFields() {
    Field[] fields = new Field[14];
    fields[0] = new Field("", "PKTABLE_CAT", 1, 255);
    fields[1] = new Field("", "PKTABLE_SCHEM", 1, 0);
    fields[2] = new Field("", "PKTABLE_NAME", 1, 255);
    fields[3] = new Field("", "PKCOLUMN_NAME", 1, 32);
    fields[4] = new Field("", "FKTABLE_CAT", 1, 255);
    fields[5] = new Field("", "FKTABLE_SCHEM", 1, 0);
    fields[6] = new Field("", "FKTABLE_NAME", 1, 255);
    fields[7] = new Field("", "FKCOLUMN_NAME", 1, 32);
    fields[8] = new Field("", "KEY_SEQ", 5, 2);
    fields[9] = new Field("", "UPDATE_RULE", 5, 2);
    fields[10] = new Field("", "DELETE_RULE", 5, 2);
    fields[11] = new Field("", "FK_NAME", 1, 0);
    fields[12] = new Field("", "PK_NAME", 1, 0);
    fields[13] = new Field("", "DEFERRABILITY", 5, 2);
    return fields;
  }
  

  public int getDatabaseMajorVersion()
    throws SQLException
  {
    return conn.getServerMajorVersion();
  }
  

  public int getDatabaseMinorVersion()
    throws SQLException
  {
    return conn.getServerMinorVersion();
  }
  




  public String getDatabaseProductName()
    throws SQLException
  {
    return "MySQL";
  }
  




  public String getDatabaseProductVersion()
    throws SQLException
  {
    return conn.getServerVersion();
  }
  







  public int getDefaultTransactionIsolation()
    throws SQLException
  {
    if (conn.supportsIsolationLevel()) {
      return 2;
    }
    
    return 0;
  }
  




  public int getDriverMajorVersion()
  {
    return NonRegisteringDriver.getMajorVersionInternal();
  }
  




  public int getDriverMinorVersion()
  {
    return NonRegisteringDriver.getMinorVersionInternal();
  }
  




  public String getDriverName()
    throws SQLException
  {
    return "MySQL Connector Java";
  }
  




  public String getDriverVersion()
    throws SQLException
  {
    return "mysql-connector-java-5.1.46 ( Revision: 9cc87a48e75c2d2e87c1a293b2862ce651cb256e )";
  }
  












































  public ResultSet getExportedKeys(String catalog, String schema, final String table)
    throws SQLException
  {
    if (table == null) {
      throw SQLError.createSQLException("Table not specified.", "S1009", getExceptionInterceptor());
    }
    
    Field[] fields = createFkMetadataFields();
    
    final ArrayList<ResultSetRow> rows = new ArrayList();
    
    if (conn.versionMeetsMinimum(3, 23, 0))
    {
      final java.sql.Statement stmt = conn.getMetadataSafeStatement();
      
      try
      {
        new IterateBlock(getCatalogIterator(catalog))
        {
          void forEach(String catalogStr) throws SQLException {
            ResultSet fkresults = null;
            



            try
            {
              if (conn.versionMeetsMinimum(3, 23, 50))
              {

                fkresults = extractForeignKeyFromCreateTable(catalogStr, null);
              } else {
                StringBuilder queryBuf = new StringBuilder("SHOW TABLE STATUS FROM ");
                queryBuf.append(StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()));
                

                fkresults = stmt.executeQuery(queryBuf.toString());
              }
              

              String tableNameWithCase = getTableNameWithCase(table);
              




              while (fkresults.next()) {
                String tableType = fkresults.getString("Type");
                
                if ((tableType != null) && ((tableType.equalsIgnoreCase("innodb")) || (tableType.equalsIgnoreCase("SUPPORTS_FK")))) {
                  String comment = fkresults.getString("Comment").trim();
                  
                  if (comment != null) {
                    StringTokenizer commentTokens = new StringTokenizer(comment, ";", false);
                    
                    if (commentTokens.hasMoreTokens()) {
                      commentTokens.nextToken();
                      


                      while (commentTokens.hasMoreTokens()) {
                        String keys = commentTokens.nextToken();
                        getExportKeyResults(catalogStr, tableNameWithCase, keys, rows, fkresults.getString("Name"));
                      }
                    }
                  }
                }
              }
            }
            finally {
              if (fkresults != null) {
                try {
                  fkresults.close();
                } catch (SQLException sqlEx) {
                  AssertionFailedException.shouldNotHappen(sqlEx);
                }
                
                fkresults = null;
              }
            }
          }
        }.doForAll();
      } finally {
        if (stmt != null) {
          stmt.close();
        }
      }
    }
    
    ResultSet results = buildResultSet(fields, rows);
    
    return results;
  }
  

















  protected void getExportKeyResults(String catalog, String exportingTable, String keysComment, List<ResultSetRow> tuples, String fkTableName)
    throws SQLException
  {
    getResultsImpl(catalog, exportingTable, keysComment, tuples, fkTableName, true);
  }
  





  public String getExtraNameCharacters()
    throws SQLException
  {
    return "#@";
  }
  








  protected int[] getForeignKeyActions(String commentString)
  {
    int[] actions = { 3, 3 };
    
    int lastParenIndex = commentString.lastIndexOf(")");
    
    if (lastParenIndex != commentString.length() - 1) {
      String cascadeOptions = commentString.substring(lastParenIndex + 1).trim().toUpperCase(Locale.ENGLISH);
      
      actions[0] = getCascadeDeleteOption(cascadeOptions);
      actions[1] = getCascadeUpdateOption(cascadeOptions);
    }
    
    return actions;
  }
  






  public String getIdentifierQuoteString()
    throws SQLException
  {
    if (conn.supportsQuotedIdentifiers()) {
      return conn.useAnsiQuotedIdentifiers() ? "\"" : "`";
    }
    

    return " ";
  }
  












































  public ResultSet getImportedKeys(String catalog, String schema, final String table)
    throws SQLException
  {
    if (table == null) {
      throw SQLError.createSQLException("Table not specified.", "S1009", getExceptionInterceptor());
    }
    
    Field[] fields = createFkMetadataFields();
    
    final ArrayList<ResultSetRow> rows = new ArrayList();
    
    if (conn.versionMeetsMinimum(3, 23, 0))
    {
      final java.sql.Statement stmt = conn.getMetadataSafeStatement();
      
      try
      {
        new IterateBlock(getCatalogIterator(catalog))
        {
          void forEach(String catalogStr) throws SQLException {
            ResultSet fkresults = null;
            



            try
            {
              if (conn.versionMeetsMinimum(3, 23, 50))
              {

                fkresults = extractForeignKeyFromCreateTable(catalogStr, table);
              } else {
                StringBuilder queryBuf = new StringBuilder("SHOW TABLE STATUS ");
                queryBuf.append(" FROM ");
                queryBuf.append(StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()));
                
                queryBuf.append(" LIKE ");
                queryBuf.append(StringUtils.quoteIdentifier(table, "'", true));
                
                fkresults = stmt.executeQuery(queryBuf.toString());
              }
              




              while (fkresults.next()) {
                String tableType = fkresults.getString("Type");
                
                if ((tableType != null) && ((tableType.equalsIgnoreCase("innodb")) || (tableType.equalsIgnoreCase("SUPPORTS_FK")))) {
                  String comment = fkresults.getString("Comment").trim();
                  
                  if (comment != null) {
                    StringTokenizer commentTokens = new StringTokenizer(comment, ";", false);
                    
                    if (commentTokens.hasMoreTokens()) {
                      commentTokens.nextToken();
                      
                      while (commentTokens.hasMoreTokens()) {
                        String keys = commentTokens.nextToken();
                        getImportKeyResults(catalogStr, table, keys, rows);
                      }
                    }
                  }
                }
              }
            } finally {
              if (fkresults != null) {
                try {
                  fkresults.close();
                } catch (SQLException sqlEx) {
                  AssertionFailedException.shouldNotHappen(sqlEx);
                }
                
                fkresults = null;
              }
            }
          }
        }.doForAll();
      } finally {
        if (stmt != null) {
          stmt.close();
        }
      }
    }
    
    ResultSet results = buildResultSet(fields, rows);
    
    return results;
  }
  















  protected void getImportKeyResults(String catalog, String importingTable, String keysComment, List<ResultSetRow> tuples)
    throws SQLException
  {
    getResultsImpl(catalog, importingTable, keysComment, tuples, null, false);
  }
  

















































  public ResultSet getIndexInfo(String catalog, String schema, final String table, final boolean unique, boolean approximate)
    throws SQLException
  {
    Field[] fields = createIndexInfoFields();
    
    final SortedMap<IndexMetaDataKey, ResultSetRow> sortedRows = new TreeMap();
    ArrayList<ResultSetRow> rows = new ArrayList();
    final java.sql.Statement stmt = conn.getMetadataSafeStatement();
    
    try
    {
      new IterateBlock(getCatalogIterator(catalog))
      {
        void forEach(String catalogStr) throws SQLException
        {
          ResultSet results = null;
          try
          {
            StringBuilder queryBuf = new StringBuilder("SHOW INDEX FROM ");
            queryBuf.append(StringUtils.quoteIdentifier(table, quotedId, conn.getPedantic()));
            queryBuf.append(" FROM ");
            queryBuf.append(StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()));
            try
            {
              results = stmt.executeQuery(queryBuf.toString());
            } catch (SQLException sqlEx) {
              int errorCode = sqlEx.getErrorCode();
              

              if (!"42S02".equals(sqlEx.getSQLState()))
              {
                if (errorCode != 1146) {
                  throw sqlEx;
                }
              }
            }
            
            while ((results != null) && (results.next())) {
              byte[][] row = new byte[14][];
              row[0] = (catalogStr == null ? new byte[0] : s2b(catalogStr));
              row[1] = null;
              row[2] = results.getBytes("Table");
              
              boolean indexIsUnique = results.getInt("Non_unique") == 0;
              
              row[3] = (!indexIsUnique ? s2b("true") : s2b("false"));
              row[4] = new byte[0];
              row[5] = results.getBytes("Key_name");
              short indexType = 3;
              row[6] = Integer.toString(indexType).getBytes();
              row[7] = results.getBytes("Seq_in_index");
              row[8] = results.getBytes("Column_name");
              row[9] = results.getBytes("Collation");
              
              long cardinality = results.getLong("Cardinality");
              

              if ((!Util.isJdbc42()) && (cardinality > 2147483647L)) {
                cardinality = 2147483647L;
              }
              
              row[10] = s2b(String.valueOf(cardinality));
              row[11] = s2b("0");
              row[12] = null;
              
              DatabaseMetaData.IndexMetaDataKey indexInfoKey = new DatabaseMetaData.IndexMetaDataKey(DatabaseMetaData.this, !indexIsUnique, indexType, results.getString("Key_name").toLowerCase(), results.getShort("Seq_in_index"));
              

              if (unique) {
                if (indexIsUnique) {
                  sortedRows.put(indexInfoKey, new ByteArrayRow(row, getExceptionInterceptor()));
                }
              }
              else {
                sortedRows.put(indexInfoKey, new ByteArrayRow(row, getExceptionInterceptor()));
              }
            }
          } finally {
            if (results != null) {
              try {
                results.close();
              }
              catch (Exception ex) {}
              
              results = null;
            }
            
          }
        }
      }.doForAll();
      Iterator<ResultSetRow> sortedRowsIterator = sortedRows.values().iterator();
      while (sortedRowsIterator.hasNext()) {
        rows.add(sortedRowsIterator.next());
      }
      
      ResultSet indexInfo = buildResultSet(fields, rows);
      
      return indexInfo;
    } finally {
      if (stmt != null) {
        stmt.close();
      }
    }
  }
  
  protected Field[] createIndexInfoFields() {
    Field[] fields = new Field[13];
    fields[0] = new Field("", "TABLE_CAT", 1, 255);
    fields[1] = new Field("", "TABLE_SCHEM", 1, 0);
    fields[2] = new Field("", "TABLE_NAME", 1, 255);
    fields[3] = new Field("", "NON_UNIQUE", 16, 4);
    fields[4] = new Field("", "INDEX_QUALIFIER", 1, 1);
    fields[5] = new Field("", "INDEX_NAME", 1, 32);
    fields[6] = new Field("", "TYPE", 5, 32);
    fields[7] = new Field("", "ORDINAL_POSITION", 5, 5);
    fields[8] = new Field("", "COLUMN_NAME", 1, 32);
    fields[9] = new Field("", "ASC_OR_DESC", 1, 1);
    if (Util.isJdbc42()) {
      fields[10] = new Field("", "CARDINALITY", -5, 20);
      fields[11] = new Field("", "PAGES", -5, 20);
    } else {
      fields[10] = new Field("", "CARDINALITY", 4, 20);
      fields[11] = new Field("", "PAGES", 4, 10);
    }
    fields[12] = new Field("", "FILTER_CONDITION", 1, 32);
    return fields;
  }
  

  public int getJDBCMajorVersion()
    throws SQLException
  {
    return 4;
  }
  

  public int getJDBCMinorVersion()
    throws SQLException
  {
    return 0;
  }
  




  public int getMaxBinaryLiteralLength()
    throws SQLException
  {
    return 16777208;
  }
  




  public int getMaxCatalogNameLength()
    throws SQLException
  {
    return 32;
  }
  




  public int getMaxCharLiteralLength()
    throws SQLException
  {
    return 16777208;
  }
  




  public int getMaxColumnNameLength()
    throws SQLException
  {
    return 64;
  }
  




  public int getMaxColumnsInGroupBy()
    throws SQLException
  {
    return 64;
  }
  




  public int getMaxColumnsInIndex()
    throws SQLException
  {
    return 16;
  }
  




  public int getMaxColumnsInOrderBy()
    throws SQLException
  {
    return 64;
  }
  




  public int getMaxColumnsInSelect()
    throws SQLException
  {
    return 256;
  }
  




  public int getMaxColumnsInTable()
    throws SQLException
  {
    return 512;
  }
  




  public int getMaxConnections()
    throws SQLException
  {
    return 0;
  }
  




  public int getMaxCursorNameLength()
    throws SQLException
  {
    return 64;
  }
  




  public int getMaxIndexLength()
    throws SQLException
  {
    return 256;
  }
  




  public int getMaxProcedureNameLength()
    throws SQLException
  {
    return 0;
  }
  




  public int getMaxRowSize()
    throws SQLException
  {
    return 2147483639;
  }
  




  public int getMaxSchemaNameLength()
    throws SQLException
  {
    return 0;
  }
  




  public int getMaxStatementLength()
    throws SQLException
  {
    return MysqlIO.getMaxBuf() - 4;
  }
  




  public int getMaxStatements()
    throws SQLException
  {
    return 0;
  }
  




  public int getMaxTableNameLength()
    throws SQLException
  {
    return 64;
  }
  




  public int getMaxTablesInSelect()
    throws SQLException
  {
    return 256;
  }
  




  public int getMaxUserNameLength()
    throws SQLException
  {
    return 16;
  }
  




  public String getNumericFunctions()
    throws SQLException
  {
    return "ABS,ACOS,ASIN,ATAN,ATAN2,BIT_COUNT,CEILING,COS,COT,DEGREES,EXP,FLOOR,LOG,LOG10,MAX,MIN,MOD,PI,POW,POWER,RADIANS,RAND,ROUND,SIN,SQRT,TAN,TRUNCATE";
  }
  























  public ResultSet getPrimaryKeys(String catalog, String schema, final String table)
    throws SQLException
  {
    Field[] fields = new Field[6];
    fields[0] = new Field("", "TABLE_CAT", 1, 255);
    fields[1] = new Field("", "TABLE_SCHEM", 1, 0);
    fields[2] = new Field("", "TABLE_NAME", 1, 255);
    fields[3] = new Field("", "COLUMN_NAME", 1, 32);
    fields[4] = new Field("", "KEY_SEQ", 5, 5);
    fields[5] = new Field("", "PK_NAME", 1, 32);
    
    if (table == null) {
      throw SQLError.createSQLException("Table not specified.", "S1009", getExceptionInterceptor());
    }
    
    final ArrayList<ResultSetRow> rows = new ArrayList();
    final java.sql.Statement stmt = conn.getMetadataSafeStatement();
    
    try
    {
      new IterateBlock(getCatalogIterator(catalog))
      {
        void forEach(String catalogStr) throws SQLException {
          ResultSet rs = null;
          
          try
          {
            StringBuilder queryBuf = new StringBuilder("SHOW KEYS FROM ");
            queryBuf.append(StringUtils.quoteIdentifier(table, quotedId, conn.getPedantic()));
            queryBuf.append(" FROM ");
            queryBuf.append(StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()));
            
            rs = stmt.executeQuery(queryBuf.toString());
            
            TreeMap<String, byte[][]> sortMap = new TreeMap();
            
            while (rs.next()) {
              String keyType = rs.getString("Key_name");
              
              if ((keyType != null) && (
                (keyType.equalsIgnoreCase("PRIMARY")) || (keyType.equalsIgnoreCase("PRI")))) {
                byte[][] tuple = new byte[6][];
                tuple[0] = (catalogStr == null ? new byte[0] : s2b(catalogStr));
                tuple[1] = null;
                tuple[2] = s2b(table);
                
                String columnName = rs.getString("Column_name");
                tuple[3] = s2b(columnName);
                tuple[4] = s2b(rs.getString("Seq_in_index"));
                tuple[5] = s2b(keyType);
                sortMap.put(columnName, tuple);
              }
            }
            


            Iterator<byte[][]> sortedIterator = sortMap.values().iterator();
            
            while (sortedIterator.hasNext()) {
              rows.add(new ByteArrayRow((byte[][])sortedIterator.next(), getExceptionInterceptor()));
            }
          }
          finally {
            if (rs != null) {
              try {
                rs.close();
              }
              catch (Exception ex) {}
              
              rs = null;
            }
          }
        }
      }.doForAll();
    } finally {
      if (stmt != null) {
        stmt.close();
      }
    }
    
    ResultSet results = buildResultSet(fields, rows);
    
    return results;
  }
  







































































  public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern)
    throws SQLException
  {
    Field[] fields = createProcedureColumnsFields();
    
    return getProcedureOrFunctionColumns(fields, catalog, schemaPattern, procedureNamePattern, columnNamePattern, true, true);
  }
  
  protected Field[] createProcedureColumnsFields() {
    Field[] fields = new Field[20];
    
    fields[0] = new Field("", "PROCEDURE_CAT", 1, 512);
    fields[1] = new Field("", "PROCEDURE_SCHEM", 1, 512);
    fields[2] = new Field("", "PROCEDURE_NAME", 1, 512);
    fields[3] = new Field("", "COLUMN_NAME", 1, 512);
    fields[4] = new Field("", "COLUMN_TYPE", 1, 64);
    fields[5] = new Field("", "DATA_TYPE", 5, 6);
    fields[6] = new Field("", "TYPE_NAME", 1, 64);
    fields[7] = new Field("", "PRECISION", 4, 12);
    fields[8] = new Field("", "LENGTH", 4, 12);
    fields[9] = new Field("", "SCALE", 5, 12);
    fields[10] = new Field("", "RADIX", 5, 6);
    fields[11] = new Field("", "NULLABLE", 5, 6);
    fields[12] = new Field("", "REMARKS", 1, 512);
    fields[13] = new Field("", "COLUMN_DEF", 1, 512);
    fields[14] = new Field("", "SQL_DATA_TYPE", 4, 12);
    fields[15] = new Field("", "SQL_DATETIME_SUB", 4, 12);
    fields[16] = new Field("", "CHAR_OCTET_LENGTH", 4, 12);
    fields[17] = new Field("", "ORDINAL_POSITION", 4, 12);
    fields[18] = new Field("", "IS_NULLABLE", 1, 512);
    fields[19] = new Field("", "SPECIFIC_NAME", 1, 512);
    return fields;
  }
  
  protected ResultSet getProcedureOrFunctionColumns(Field[] fields, String catalog, String schemaPattern, String procedureOrFunctionNamePattern, String columnNamePattern, boolean returnProcedures, boolean returnFunctions)
    throws SQLException
  {
    List<ComparableWrapper<String, ProcedureType>> procsOrFuncsToExtractList = new ArrayList();
    
    ResultSet procsAndOrFuncsRs = null;
    
    if (supportsStoredProcedures()) {
      try
      {
        String tmpProcedureOrFunctionNamePattern = null;
        
        if ((procedureOrFunctionNamePattern != null) && (!procedureOrFunctionNamePattern.equals("%"))) {
          tmpProcedureOrFunctionNamePattern = StringUtils.sanitizeProcOrFuncName(procedureOrFunctionNamePattern);
        }
        

        if (tmpProcedureOrFunctionNamePattern == null) {
          tmpProcedureOrFunctionNamePattern = procedureOrFunctionNamePattern;
        }
        else
        {
          String tmpCatalog = catalog;
          List<String> parseList = StringUtils.splitDBdotName(tmpProcedureOrFunctionNamePattern, tmpCatalog, quotedId, conn.isNoBackslashEscapesSet());
          


          if (parseList.size() == 2) {
            tmpCatalog = (String)parseList.get(0);
            tmpProcedureOrFunctionNamePattern = (String)parseList.get(1);
          }
        }
        


        procsAndOrFuncsRs = getProceduresAndOrFunctions(createFieldMetadataForGetProcedures(), catalog, schemaPattern, tmpProcedureOrFunctionNamePattern, returnProcedures, returnFunctions);
        

        boolean hasResults = false;
        while (procsAndOrFuncsRs.next()) {
          procsOrFuncsToExtractList.add(new ComparableWrapper(getFullyQualifiedName(procsAndOrFuncsRs.getString(1), procsAndOrFuncsRs.getString(3)), procsAndOrFuncsRs.getShort(8) == 1 ? ProcedureType.PROCEDURE : ProcedureType.FUNCTION));
          

          hasResults = true;
        }
        

        if (hasResults)
        {





          Collections.sort(procsOrFuncsToExtractList);
        }
        

      }
      finally
      {
        SQLException rethrowSqlEx = null;
        
        if (procsAndOrFuncsRs != null) {
          try {
            procsAndOrFuncsRs.close();
          } catch (SQLException sqlEx) {
            rethrowSqlEx = sqlEx;
          }
        }
        
        if (rethrowSqlEx != null) {
          throw rethrowSqlEx;
        }
      }
    }
    
    ArrayList<ResultSetRow> resultRows = new ArrayList();
    int idx = 0;
    String procNameToCall = "";
    
    for (Object procOrFunc : procsOrFuncsToExtractList) {
      String procName = (String)((ComparableWrapper)procOrFunc).getKey();
      ProcedureType procType = (ProcedureType)((ComparableWrapper)procOrFunc).getValue();
      

      if (!" ".equals(quotedId)) {
        idx = StringUtils.indexOfIgnoreCase(0, procName, ".", quotedId, quotedId, conn.isNoBackslashEscapesSet() ? StringUtils.SEARCH_MODE__MRK_COM_WS : StringUtils.SEARCH_MODE__ALL);
      }
      else {
        idx = procName.indexOf(".");
      }
      
      if (idx > 0) {
        catalog = StringUtils.unQuoteIdentifier(procName.substring(0, idx), quotedId);
        procNameToCall = procName;
      }
      else {
        procNameToCall = procName;
      }
      getCallStmtParameterTypes(catalog, procNameToCall, procType, columnNamePattern, resultRows, fields.length == 17);
    }
    
    return buildResultSet(fields, resultRows);
  }
  


































  public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern)
    throws SQLException
  {
    Field[] fields = createFieldMetadataForGetProcedures();
    
    return getProceduresAndOrFunctions(fields, catalog, schemaPattern, procedureNamePattern, true, true);
  }
  
  protected Field[] createFieldMetadataForGetProcedures() {
    Field[] fields = new Field[9];
    fields[0] = new Field("", "PROCEDURE_CAT", 1, 255);
    fields[1] = new Field("", "PROCEDURE_SCHEM", 1, 255);
    fields[2] = new Field("", "PROCEDURE_NAME", 1, 255);
    fields[3] = new Field("", "reserved1", 1, 0);
    fields[4] = new Field("", "reserved2", 1, 0);
    fields[5] = new Field("", "reserved3", 1, 0);
    fields[6] = new Field("", "REMARKS", 1, 255);
    fields[7] = new Field("", "PROCEDURE_TYPE", 5, 6);
    fields[8] = new Field("", "SPECIFIC_NAME", 1, 255);
    
    return fields;
  }
  








  protected ResultSet getProceduresAndOrFunctions(final Field[] fields, String catalog, String schemaPattern, String procedureNamePattern, final boolean returnProcedures, final boolean returnFunctions)
    throws SQLException
  {
    if ((procedureNamePattern == null) || (procedureNamePattern.length() == 0)) {
      if (conn.getNullNamePatternMatchesAll()) {
        procedureNamePattern = "%";
      } else {
        throw SQLError.createSQLException("Procedure name pattern can not be NULL or empty.", "S1009", getExceptionInterceptor());
      }
    }
    

    ArrayList<ResultSetRow> procedureRows = new ArrayList();
    
    if (supportsStoredProcedures()) {
      final String procNamePattern = procedureNamePattern;
      
      final List<ComparableWrapper<String, ResultSetRow>> procedureRowsToSort = new ArrayList();
      
      new IterateBlock(getCatalogIterator(catalog))
      {
        /* Error */
        void forEach(String catalogStr)
          throws SQLException
        {
          // Byte code:
          //   0: aload_1
          //   1: astore_2
          //   2: aconst_null
          //   3: astore_3
          //   4: iconst_1
          //   5: istore 4
          //   7: new 8	java/lang/StringBuilder
          //   10: dup
          //   11: invokespecial 9	java/lang/StringBuilder:<init>	()V
          //   14: astore 5
          //   16: aload 5
          //   18: ldc 10
          //   20: invokevirtual 11	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
          //   23: pop
          //   24: aload_0
          //   25: getfield 2	com/mysql/jdbc/DatabaseMetaData$8:val$returnProcedures	Z
          //   28: ifeq +21 -> 49
          //   31: aload_0
          //   32: getfield 3	com/mysql/jdbc/DatabaseMetaData$8:val$returnFunctions	Z
          //   35: ifne +14 -> 49
          //   38: aload 5
          //   40: ldc 12
          //   42: invokevirtual 11	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
          //   45: pop
          //   46: goto +25 -> 71
          //   49: aload_0
          //   50: getfield 2	com/mysql/jdbc/DatabaseMetaData$8:val$returnProcedures	Z
          //   53: ifne +18 -> 71
          //   56: aload_0
          //   57: getfield 3	com/mysql/jdbc/DatabaseMetaData$8:val$returnFunctions	Z
          //   60: ifeq +11 -> 71
          //   63: aload 5
          //   65: ldc 13
          //   67: invokevirtual 11	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
          //   70: pop
          //   71: aload 5
          //   73: ldc 14
          //   75: invokevirtual 11	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
          //   78: pop
          //   79: aload_0
          //   80: getfield 1	com/mysql/jdbc/DatabaseMetaData$8:this$0	Lcom/mysql/jdbc/DatabaseMetaData;
          //   83: aload 5
          //   85: invokevirtual 15	java/lang/StringBuilder:toString	()Ljava/lang/String;
          //   88: invokevirtual 16	com/mysql/jdbc/DatabaseMetaData:prepareMetaDataSafeStatement	(Ljava/lang/String;)Ljava/sql/PreparedStatement;
          //   91: astore 6
          //   93: aload_2
          //   94: ifnull +35 -> 129
          //   97: aload_0
          //   98: getfield 1	com/mysql/jdbc/DatabaseMetaData$8:this$0	Lcom/mysql/jdbc/DatabaseMetaData;
          //   101: getfield 17	com/mysql/jdbc/DatabaseMetaData:conn	Lcom/mysql/jdbc/MySQLConnection;
          //   104: invokeinterface 18 1 0
          //   109: ifeq +8 -> 117
          //   112: aload_2
          //   113: invokevirtual 19	java/lang/String:toLowerCase	()Ljava/lang/String;
          //   116: astore_2
          //   117: aload 6
          //   119: iconst_2
          //   120: aload_2
          //   121: invokeinterface 20 3 0
          //   126: goto +13 -> 139
          //   129: aload 6
          //   131: iconst_2
          //   132: bipush 12
          //   134: invokeinterface 21 3 0
          //   139: iconst_1
          //   140: istore 7
          //   142: aload 6
          //   144: iconst_1
          //   145: aload_0
          //   146: getfield 4	com/mysql/jdbc/DatabaseMetaData$8:val$procNamePattern	Ljava/lang/String;
          //   149: invokeinterface 20 3 0
          //   154: aload 6
          //   156: invokeinterface 22 1 0
          //   161: astore_3
          //   162: iconst_0
          //   163: istore 4
          //   165: aload_0
          //   166: getfield 2	com/mysql/jdbc/DatabaseMetaData$8:val$returnProcedures	Z
          //   169: ifeq +22 -> 191
          //   172: aload_0
          //   173: getfield 1	com/mysql/jdbc/DatabaseMetaData$8:this$0	Lcom/mysql/jdbc/DatabaseMetaData;
          //   176: iconst_1
          //   177: aload_2
          //   178: aload_3
          //   179: iload 4
          //   181: aload_2
          //   182: aload_0
          //   183: getfield 5	com/mysql/jdbc/DatabaseMetaData$8:val$procedureRowsToSort	Ljava/util/List;
          //   186: iload 7
          //   188: invokevirtual 23	com/mysql/jdbc/DatabaseMetaData:convertToJdbcProcedureList	(ZLjava/lang/String;Ljava/sql/ResultSet;ZLjava/lang/String;Ljava/util/List;I)V
          //   191: aload_0
          //   192: getfield 3	com/mysql/jdbc/DatabaseMetaData$8:val$returnFunctions	Z
          //   195: ifeq +25 -> 220
          //   198: aload_0
          //   199: getfield 1	com/mysql/jdbc/DatabaseMetaData$8:this$0	Lcom/mysql/jdbc/DatabaseMetaData;
          //   202: aload_2
          //   203: aload_3
          //   204: iload 4
          //   206: aload_2
          //   207: aload_0
          //   208: getfield 5	com/mysql/jdbc/DatabaseMetaData$8:val$procedureRowsToSort	Ljava/util/List;
          //   211: iload 7
          //   213: aload_0
          //   214: getfield 6	com/mysql/jdbc/DatabaseMetaData$8:val$fields	[Lcom/mysql/jdbc/Field;
          //   217: invokevirtual 24	com/mysql/jdbc/DatabaseMetaData:convertToJdbcFunctionList	(Ljava/lang/String;Ljava/sql/ResultSet;ZLjava/lang/String;Ljava/util/List;I[Lcom/mysql/jdbc/Field;)V
          //   220: goto +161 -> 381
          //   223: astore 8
          //   225: aload_0
          //   226: getfield 1	com/mysql/jdbc/DatabaseMetaData$8:this$0	Lcom/mysql/jdbc/DatabaseMetaData;
          //   229: getfield 17	com/mysql/jdbc/DatabaseMetaData:conn	Lcom/mysql/jdbc/MySQLConnection;
          //   232: iconst_5
          //   233: iconst_0
          //   234: iconst_1
          //   235: invokeinterface 26 4 0
          //   240: ifeq +7 -> 247
          //   243: iconst_2
          //   244: goto +4 -> 248
          //   247: iconst_1
          //   248: istore 7
          //   250: aload_0
          //   251: getfield 3	com/mysql/jdbc/DatabaseMetaData$8:val$returnFunctions	Z
          //   254: ifeq +63 -> 317
          //   257: aload 6
          //   259: invokeinterface 27 1 0
          //   264: aload_0
          //   265: getfield 1	com/mysql/jdbc/DatabaseMetaData$8:this$0	Lcom/mysql/jdbc/DatabaseMetaData;
          //   268: ldc 28
          //   270: invokevirtual 16	com/mysql/jdbc/DatabaseMetaData:prepareMetaDataSafeStatement	(Ljava/lang/String;)Ljava/sql/PreparedStatement;
          //   273: astore 6
          //   275: aload 6
          //   277: iconst_1
          //   278: aload_0
          //   279: getfield 4	com/mysql/jdbc/DatabaseMetaData$8:val$procNamePattern	Ljava/lang/String;
          //   282: invokeinterface 20 3 0
          //   287: aload 6
          //   289: invokeinterface 22 1 0
          //   294: astore_3
          //   295: aload_0
          //   296: getfield 1	com/mysql/jdbc/DatabaseMetaData$8:this$0	Lcom/mysql/jdbc/DatabaseMetaData;
          //   299: aload_2
          //   300: aload_3
          //   301: iload 4
          //   303: aload_2
          //   304: aload_0
          //   305: getfield 5	com/mysql/jdbc/DatabaseMetaData$8:val$procedureRowsToSort	Ljava/util/List;
          //   308: iload 7
          //   310: aload_0
          //   311: getfield 6	com/mysql/jdbc/DatabaseMetaData$8:val$fields	[Lcom/mysql/jdbc/Field;
          //   314: invokevirtual 24	com/mysql/jdbc/DatabaseMetaData:convertToJdbcFunctionList	(Ljava/lang/String;Ljava/sql/ResultSet;ZLjava/lang/String;Ljava/util/List;I[Lcom/mysql/jdbc/Field;)V
          //   317: aload_0
          //   318: getfield 2	com/mysql/jdbc/DatabaseMetaData$8:val$returnProcedures	Z
          //   321: ifeq +60 -> 381
          //   324: aload 6
          //   326: invokeinterface 27 1 0
          //   331: aload_0
          //   332: getfield 1	com/mysql/jdbc/DatabaseMetaData$8:this$0	Lcom/mysql/jdbc/DatabaseMetaData;
          //   335: ldc 29
          //   337: invokevirtual 16	com/mysql/jdbc/DatabaseMetaData:prepareMetaDataSafeStatement	(Ljava/lang/String;)Ljava/sql/PreparedStatement;
          //   340: astore 6
          //   342: aload 6
          //   344: iconst_1
          //   345: aload_0
          //   346: getfield 4	com/mysql/jdbc/DatabaseMetaData$8:val$procNamePattern	Ljava/lang/String;
          //   349: invokeinterface 20 3 0
          //   354: aload 6
          //   356: invokeinterface 22 1 0
          //   361: astore_3
          //   362: aload_0
          //   363: getfield 1	com/mysql/jdbc/DatabaseMetaData$8:this$0	Lcom/mysql/jdbc/DatabaseMetaData;
          //   366: iconst_0
          //   367: aload_2
          //   368: aload_3
          //   369: iload 4
          //   371: aload_2
          //   372: aload_0
          //   373: getfield 5	com/mysql/jdbc/DatabaseMetaData$8:val$procedureRowsToSort	Ljava/util/List;
          //   376: iload 7
          //   378: invokevirtual 23	com/mysql/jdbc/DatabaseMetaData:convertToJdbcProcedureList	(ZLjava/lang/String;Ljava/sql/ResultSet;ZLjava/lang/String;Ljava/util/List;I)V
          //   381: jsr +14 -> 395
          //   384: goto +66 -> 450
          //   387: astore 9
          //   389: jsr +6 -> 395
          //   392: aload 9
          //   394: athrow
          //   395: astore 10
          //   397: aconst_null
          //   398: astore 11
          //   400: aload_3
          //   401: ifnull +18 -> 419
          //   404: aload_3
          //   405: invokeinterface 30 1 0
          //   410: goto +9 -> 419
          //   413: astore 12
          //   415: aload 12
          //   417: astore 11
          //   419: aload 6
          //   421: ifnull +19 -> 440
          //   424: aload 6
          //   426: invokeinterface 27 1 0
          //   431: goto +9 -> 440
          //   434: astore 12
          //   436: aload 12
          //   438: astore 11
          //   440: aload 11
          //   442: ifnull +6 -> 448
          //   445: aload 11
          //   447: athrow
          //   448: ret 10
          //   450: return
          // Line number table:
          //   Java source line #4088	-> byte code offset #0
          //   Java source line #4090	-> byte code offset #2
          //   Java source line #4091	-> byte code offset #4
          //   Java source line #4093	-> byte code offset #7
          //   Java source line #4095	-> byte code offset #16
          //   Java source line #4096	-> byte code offset #24
          //   Java source line #4097	-> byte code offset #38
          //   Java source line #4098	-> byte code offset #49
          //   Java source line #4099	-> byte code offset #63
          //   Java source line #4101	-> byte code offset #71
          //   Java source line #4103	-> byte code offset #79
          //   Java source line #4109	-> byte code offset #93
          //   Java source line #4110	-> byte code offset #97
          //   Java source line #4111	-> byte code offset #112
          //   Java source line #4113	-> byte code offset #117
          //   Java source line #4115	-> byte code offset #129
          //   Java source line #4118	-> byte code offset #139
          //   Java source line #4120	-> byte code offset #142
          //   Java source line #4123	-> byte code offset #154
          //   Java source line #4124	-> byte code offset #162
          //   Java source line #4126	-> byte code offset #165
          //   Java source line #4127	-> byte code offset #172
          //   Java source line #4130	-> byte code offset #191
          //   Java source line #4131	-> byte code offset #198
          //   Java source line #4159	-> byte code offset #220
          //   Java source line #4134	-> byte code offset #223
          //   Java source line #4135	-> byte code offset #225
          //   Java source line #4139	-> byte code offset #250
          //   Java source line #4140	-> byte code offset #257
          //   Java source line #4142	-> byte code offset #264
          //   Java source line #4143	-> byte code offset #275
          //   Java source line #4144	-> byte code offset #287
          //   Java source line #4146	-> byte code offset #295
          //   Java source line #4150	-> byte code offset #317
          //   Java source line #4151	-> byte code offset #324
          //   Java source line #4153	-> byte code offset #331
          //   Java source line #4154	-> byte code offset #342
          //   Java source line #4155	-> byte code offset #354
          //   Java source line #4157	-> byte code offset #362
          //   Java source line #4161	-> byte code offset #381
          //   Java source line #4183	-> byte code offset #384
          //   Java source line #4162	-> byte code offset #387
          //   Java source line #4164	-> byte code offset #400
          //   Java source line #4166	-> byte code offset #404
          //   Java source line #4169	-> byte code offset #410
          //   Java source line #4167	-> byte code offset #413
          //   Java source line #4168	-> byte code offset #415
          //   Java source line #4172	-> byte code offset #419
          //   Java source line #4174	-> byte code offset #424
          //   Java source line #4177	-> byte code offset #431
          //   Java source line #4175	-> byte code offset #434
          //   Java source line #4176	-> byte code offset #436
          //   Java source line #4180	-> byte code offset #440
          //   Java source line #4181	-> byte code offset #445
          //   Java source line #4183	-> byte code offset #448
          //   Java source line #4184	-> byte code offset #450
          // Local variable table:
          //   start	length	slot	name	signature
          //   0	451	0	this	8
          //   0	451	1	catalogStr	String
          //   1	371	2	db	String
          //   3	402	3	proceduresRs	ResultSet
          //   5	365	4	needsClientFiltering	boolean
          //   14	70	5	selectFromMySQLProcSQL	StringBuilder
          //   91	334	6	proceduresStmt	PreparedStatement
          //   140	237	7	nameIndex	int
          //   223	3	8	sqlEx	SQLException
          //   387	6	9	localObject1	Object
          //   395	1	10	localObject2	Object
          //   398	48	11	rethrowSqlEx	SQLException
          //   413	3	12	sqlEx	SQLException
          //   434	3	12	sqlEx	SQLException
          // Exception table:
          //   from	to	target	type
          //   154	220	223	java/sql/SQLException
          //   93	384	387	finally
          //   387	392	387	finally
          //   404	410	413	java/sql/SQLException
          //   424	431	434	java/sql/SQLException
        }
      }.doForAll();
      Collections.sort(procedureRowsToSort);
      for (ComparableWrapper<String, ResultSetRow> procRow : procedureRowsToSort) {
        procedureRows.add(procRow.getValue());
      }
    }
    
    return buildResultSet(fields, procedureRows);
  }
  





  public String getProcedureTerm()
    throws SQLException
  {
    return "PROCEDURE";
  }
  

  public int getResultSetHoldability()
    throws SQLException
  {
    return 1;
  }
  
  private void getResultsImpl(String catalog, String table, String keysComment, List<ResultSetRow> tuples, String fkTableName, boolean isExport)
    throws SQLException
  {
    LocalAndReferencedColumns parsedInfo = parseTableStatusIntoLocalAndReferencedColumns(keysComment);
    
    if ((isExport) && (!referencedTable.equals(table))) {
      return;
    }
    
    if (localColumnsList.size() != referencedColumnsList.size()) {
      throw SQLError.createSQLException("Error parsing foreign keys definition, number of local and referenced columns is not the same.", "S1000", getExceptionInterceptor());
    }
    

    Iterator<String> localColumnNames = localColumnsList.iterator();
    Iterator<String> referColumnNames = referencedColumnsList.iterator();
    
    int keySeqIndex = 1;
    
    while (localColumnNames.hasNext()) {
      byte[][] tuple = new byte[14][];
      String lColumnName = StringUtils.unQuoteIdentifier((String)localColumnNames.next(), quotedId);
      String rColumnName = StringUtils.unQuoteIdentifier((String)referColumnNames.next(), quotedId);
      tuple[4] = (catalog == null ? new byte[0] : s2b(catalog));
      tuple[5] = null;
      tuple[6] = s2b(isExport ? fkTableName : table);
      tuple[7] = s2b(lColumnName);
      tuple[0] = s2b(referencedCatalog);
      tuple[1] = null;
      tuple[2] = s2b(isExport ? table : referencedTable);
      tuple[3] = s2b(rColumnName);
      tuple[8] = s2b(Integer.toString(keySeqIndex++));
      
      int[] actions = getForeignKeyActions(keysComment);
      
      tuple[9] = s2b(Integer.toString(actions[1]));
      tuple[10] = s2b(Integer.toString(actions[0]));
      tuple[11] = s2b(constraintName);
      tuple[12] = null;
      tuple[13] = s2b(Integer.toString(7));
      tuples.add(new ByteArrayRow(tuple, getExceptionInterceptor()));
    }
  }
  












  public ResultSet getSchemas()
    throws SQLException
  {
    Field[] fields = new Field[2];
    fields[0] = new Field("", "TABLE_SCHEM", 1, 0);
    fields[1] = new Field("", "TABLE_CATALOG", 1, 0);
    
    ArrayList<ResultSetRow> tuples = new ArrayList();
    ResultSet results = buildResultSet(fields, tuples);
    
    return results;
  }
  




  public String getSchemaTerm()
    throws SQLException
  {
    return "";
  }
  











  public String getSearchStringEscape()
    throws SQLException
  {
    return "\\";
  }
  




  public String getSQLKeywords()
    throws SQLException
  {
    if (mysqlKeywords != null) {
      return mysqlKeywords;
    }
    
    synchronized (DatabaseMetaData.class)
    {
      if (mysqlKeywords != null) {
        return mysqlKeywords;
      }
      
      Set<String> mysqlKeywordSet = new TreeSet();
      StringBuilder mysqlKeywordsBuffer = new StringBuilder();
      
      Collections.addAll(mysqlKeywordSet, MYSQL_KEYWORDS);
      mysqlKeywordSet.removeAll(Arrays.asList(Util.isJdbc4() ? SQL2003_KEYWORDS : SQL92_KEYWORDS));
      
      for (String keyword : mysqlKeywordSet) {
        mysqlKeywordsBuffer.append(",").append(keyword);
      }
      
      mysqlKeywords = mysqlKeywordsBuffer.substring(1);
      return mysqlKeywords;
    }
  }
  

  public int getSQLStateType()
    throws SQLException
  {
    if (conn.versionMeetsMinimum(4, 1, 0)) {
      return 2;
    }
    
    if (conn.getUseSqlStateCodes()) {
      return 2;
    }
    
    return 1;
  }
  




  public String getStringFunctions()
    throws SQLException
  {
    return "ASCII,BIN,BIT_LENGTH,CHAR,CHARACTER_LENGTH,CHAR_LENGTH,CONCAT,CONCAT_WS,CONV,ELT,EXPORT_SET,FIELD,FIND_IN_SET,HEX,INSERT,INSTR,LCASE,LEFT,LENGTH,LOAD_FILE,LOCATE,LOCATE,LOWER,LPAD,LTRIM,MAKE_SET,MATCH,MID,OCT,OCTET_LENGTH,ORD,POSITION,QUOTE,REPEAT,REPLACE,REVERSE,RIGHT,RPAD,RTRIM,SOUNDEX,SPACE,STRCMP,SUBSTRING,SUBSTRING,SUBSTRING,SUBSTRING,SUBSTRING_INDEX,TRIM,UCASE,UPPER";
  }
  




  public ResultSet getSuperTables(String arg0, String arg1, String arg2)
    throws SQLException
  {
    Field[] fields = new Field[4];
    fields[0] = new Field("", "TABLE_CAT", 1, 32);
    fields[1] = new Field("", "TABLE_SCHEM", 1, 32);
    fields[2] = new Field("", "TABLE_NAME", 1, 32);
    fields[3] = new Field("", "SUPERTABLE_NAME", 1, 32);
    
    return buildResultSet(fields, new ArrayList());
  }
  

  public ResultSet getSuperTypes(String arg0, String arg1, String arg2)
    throws SQLException
  {
    Field[] fields = new Field[6];
    fields[0] = new Field("", "TYPE_CAT", 1, 32);
    fields[1] = new Field("", "TYPE_SCHEM", 1, 32);
    fields[2] = new Field("", "TYPE_NAME", 1, 32);
    fields[3] = new Field("", "SUPERTYPE_CAT", 1, 32);
    fields[4] = new Field("", "SUPERTYPE_SCHEM", 1, 32);
    fields[5] = new Field("", "SUPERTYPE_NAME", 1, 32);
    
    return buildResultSet(fields, new ArrayList());
  }
  




  public String getSystemFunctions()
    throws SQLException
  {
    return "DATABASE,USER,SYSTEM_USER,SESSION_USER,PASSWORD,ENCRYPT,LAST_INSERT_ID,VERSION";
  }
  
  protected String getTableNameWithCase(String table) {
    String tableNameWithCase = conn.lowerCaseTableNames() ? table.toLowerCase() : table;
    
    return tableNameWithCase;
  }
  






























  public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern)
    throws SQLException
  {
    if (tableNamePattern == null) {
      if (conn.getNullNamePatternMatchesAll()) {
        tableNamePattern = "%";
      } else {
        throw SQLError.createSQLException("Table name pattern can not be NULL or empty.", "S1009", getExceptionInterceptor());
      }
    }
    

    Field[] fields = new Field[7];
    fields[0] = new Field("", "TABLE_CAT", 1, 64);
    fields[1] = new Field("", "TABLE_SCHEM", 1, 1);
    fields[2] = new Field("", "TABLE_NAME", 1, 64);
    fields[3] = new Field("", "GRANTOR", 1, 77);
    fields[4] = new Field("", "GRANTEE", 1, 77);
    fields[5] = new Field("", "PRIVILEGE", 1, 64);
    fields[6] = new Field("", "IS_GRANTABLE", 1, 3);
    
    String grantQuery = "SELECT host,db,table_name,grantor,user,table_priv FROM mysql.tables_priv WHERE db LIKE ? AND table_name LIKE ?";
    
    ResultSet results = null;
    ArrayList<ResultSetRow> grantRows = new ArrayList();
    PreparedStatement pStmt = null;
    try
    {
      pStmt = prepareMetaDataSafeStatement(grantQuery);
      
      pStmt.setString(1, (catalog != null) && (catalog.length() != 0) ? catalog : "%");
      pStmt.setString(2, tableNamePattern);
      
      results = pStmt.executeQuery();
      
      while (results.next()) {
        String host = results.getString(1);
        String db = results.getString(2);
        String table = results.getString(3);
        String grantor = results.getString(4);
        String user = results.getString(5);
        
        if ((user == null) || (user.length() == 0)) {
          user = "%";
        }
        
        StringBuilder fullUser = new StringBuilder(user);
        
        if ((host != null) && (conn.getUseHostsInPrivileges())) {
          fullUser.append("@");
          fullUser.append(host);
        }
        
        String allPrivileges = results.getString(6);
        
        if (allPrivileges != null) {
          allPrivileges = allPrivileges.toUpperCase(Locale.ENGLISH);
          
          StringTokenizer st = new StringTokenizer(allPrivileges, ",");
          
          while (st.hasMoreTokens()) {
            String privilege = st.nextToken().trim();
            

            ResultSet columnResults = null;
            try
            {
              columnResults = getColumns(catalog, schemaPattern, table, "%");
              
              while (columnResults.next()) {
                byte[][] tuple = new byte[8][];
                tuple[0] = s2b(db);
                tuple[1] = null;
                tuple[2] = s2b(table);
                
                if (grantor != null) {
                  tuple[3] = s2b(grantor);
                } else {
                  tuple[3] = null;
                }
                
                tuple[4] = s2b(fullUser.toString());
                tuple[5] = s2b(privilege);
                tuple[6] = null;
                grantRows.add(new ByteArrayRow(tuple, getExceptionInterceptor()));
              }
            } finally {
              if (columnResults != null) {
                try {
                  columnResults.close();
                }
                catch (Exception ex) {}
              }
            }
          }
        }
      }
    } finally {
      if (results != null) {
        try {
          results.close();
        }
        catch (Exception ex) {}
        
        results = null;
      }
      
      if (pStmt != null) {
        try {
          pStmt.close();
        }
        catch (Exception ex) {}
        
        pStmt = null;
      }
    }
    
    return buildResultSet(fields, grantRows);
  }
  
































  public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, final String[] types)
    throws SQLException
  {
    if (tableNamePattern == null) {
      if (conn.getNullNamePatternMatchesAll()) {
        tableNamePattern = "%";
      } else {
        throw SQLError.createSQLException("Table name pattern can not be NULL or empty.", "S1009", getExceptionInterceptor());
      }
    }
    

    final SortedMap<TableMetaDataKey, ResultSetRow> sortedRows = new TreeMap();
    ArrayList<ResultSetRow> tuples = new ArrayList();
    
    final java.sql.Statement stmt = conn.getMetadataSafeStatement();
    

    String tmpCat = "";
    
    if ((catalog == null) || (catalog.length() == 0)) {
      if (conn.getNullCatalogMeansCurrent()) {
        tmpCat = database;
      }
    } else {
      tmpCat = catalog;
    }
    
    List<String> parseList = StringUtils.splitDBdotName(tableNamePattern, tmpCat, quotedId, conn.isNoBackslashEscapesSet());
    String tableNamePat;
    final String tableNamePat; if (parseList.size() == 2) {
      tableNamePat = (String)parseList.get(1);
    } else {
      tableNamePat = tableNamePattern;
    }
    try
    {
      new IterateBlock(getCatalogIterator(catalog))
      {
        void forEach(String catalogStr) throws SQLException {
          boolean operatingOnSystemDB = ("information_schema".equalsIgnoreCase(catalogStr)) || ("mysql".equalsIgnoreCase(catalogStr)) || ("performance_schema".equalsIgnoreCase(catalogStr));
          

          ResultSet results = null;
          try
          {
            try
            {
              results = stmt.executeQuery((!conn.versionMeetsMinimum(5, 0, 2) ? "SHOW TABLES FROM " : "SHOW FULL TABLES FROM ") + StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()) + " LIKE " + StringUtils.quoteIdentifier(tableNamePat, "'", true));

            }
            catch (SQLException sqlEx)
            {
              if ("08S01".equals(sqlEx.getSQLState())) {
                throw sqlEx;
              }
              
              return;
            }
            
            boolean shouldReportTables = false;
            boolean shouldReportViews = false;
            boolean shouldReportSystemTables = false;
            boolean shouldReportSystemViews = false;
            boolean shouldReportLocalTemporaries = false;
            
            if ((types == null) || (types.length == 0)) {
              shouldReportTables = true;
              shouldReportViews = true;
              shouldReportSystemTables = true;
              shouldReportSystemViews = true;
              shouldReportLocalTemporaries = true;
            } else {
              for (int i = 0; i < types.length; i++) {
                if (DatabaseMetaData.TableType.TABLE.equalsTo(types[i])) {
                  shouldReportTables = true;
                }
                else if (DatabaseMetaData.TableType.VIEW.equalsTo(types[i])) {
                  shouldReportViews = true;
                }
                else if (DatabaseMetaData.TableType.SYSTEM_TABLE.equalsTo(types[i])) {
                  shouldReportSystemTables = true;
                }
                else if (DatabaseMetaData.TableType.SYSTEM_VIEW.equalsTo(types[i])) {
                  shouldReportSystemViews = true;
                }
                else if (DatabaseMetaData.TableType.LOCAL_TEMPORARY.equalsTo(types[i])) {
                  shouldReportLocalTemporaries = true;
                }
              }
            }
            
            int typeColumnIndex = 1;
            boolean hasTableTypes = false;
            
            if (conn.versionMeetsMinimum(5, 0, 2)) {
              try
              {
                typeColumnIndex = results.findColumn("table_type");
                hasTableTypes = true;

              }
              catch (SQLException sqlEx)
              {
                try
                {
                  typeColumnIndex = results.findColumn("Type");
                  hasTableTypes = true;
                } catch (SQLException sqlEx2) {
                  hasTableTypes = false;
                }
              }
            }
            
            while (results.next()) {
              byte[][] row = new byte[10][];
              row[0] = (catalogStr == null ? null : s2b(catalogStr));
              row[1] = null;
              row[2] = results.getBytes(1);
              row[4] = new byte[0];
              row[5] = null;
              row[6] = null;
              row[7] = null;
              row[8] = null;
              row[9] = null;
              
              if (hasTableTypes) {
                String tableType = results.getString(typeColumnIndex);
                
                switch (DatabaseMetaData.11.$SwitchMap$com$mysql$jdbc$DatabaseMetaData$TableType[DatabaseMetaData.TableType.getTableTypeCompliantWith(tableType).ordinal()]) {
                case 1: 
                  boolean reportTable = false;
                  DatabaseMetaData.TableMetaDataKey tablesKey = null;
                  
                  if ((operatingOnSystemDB) && (shouldReportSystemTables)) {
                    row[3] = DatabaseMetaData.TableType.SYSTEM_TABLE.asBytes();
                    tablesKey = new DatabaseMetaData.TableMetaDataKey(DatabaseMetaData.this, DatabaseMetaData.TableType.SYSTEM_TABLE.getName(), catalogStr, null, results.getString(1));
                    reportTable = true;
                  }
                  else if ((!operatingOnSystemDB) && (shouldReportTables)) {
                    row[3] = DatabaseMetaData.TableType.TABLE.asBytes();
                    tablesKey = new DatabaseMetaData.TableMetaDataKey(DatabaseMetaData.this, DatabaseMetaData.TableType.TABLE.getName(), catalogStr, null, results.getString(1));
                    reportTable = true;
                  }
                  
                  if (reportTable) {
                    sortedRows.put(tablesKey, new ByteArrayRow(row, getExceptionInterceptor()));
                  }
                  
                  break;
                case 2: 
                  if (shouldReportViews) {
                    row[3] = DatabaseMetaData.TableType.VIEW.asBytes();
                    sortedRows.put(new DatabaseMetaData.TableMetaDataKey(DatabaseMetaData.this, DatabaseMetaData.TableType.VIEW.getName(), catalogStr, null, results.getString(1)), new ByteArrayRow(row, getExceptionInterceptor()));
                  }
                  

                  break;
                case 3: 
                  if (shouldReportSystemTables) {
                    row[3] = DatabaseMetaData.TableType.SYSTEM_TABLE.asBytes();
                    sortedRows.put(new DatabaseMetaData.TableMetaDataKey(DatabaseMetaData.this, DatabaseMetaData.TableType.SYSTEM_TABLE.getName(), catalogStr, null, results.getString(1)), new ByteArrayRow(row, getExceptionInterceptor()));
                  }
                  

                  break;
                case 4: 
                  if (shouldReportSystemViews) {
                    row[3] = DatabaseMetaData.TableType.SYSTEM_VIEW.asBytes();
                    sortedRows.put(new DatabaseMetaData.TableMetaDataKey(DatabaseMetaData.this, DatabaseMetaData.TableType.SYSTEM_VIEW.getName(), catalogStr, null, results.getString(1)), new ByteArrayRow(row, getExceptionInterceptor()));
                  }
                  

                  break;
                case 5: 
                  if (shouldReportLocalTemporaries) {
                    row[3] = DatabaseMetaData.TableType.LOCAL_TEMPORARY.asBytes();
                    sortedRows.put(new DatabaseMetaData.TableMetaDataKey(DatabaseMetaData.this, DatabaseMetaData.TableType.LOCAL_TEMPORARY.getName(), catalogStr, null, results.getString(1)), new ByteArrayRow(row, getExceptionInterceptor()));
                  }
                  

                  break;
                default: 
                  row[3] = DatabaseMetaData.TableType.TABLE.asBytes();
                  sortedRows.put(new DatabaseMetaData.TableMetaDataKey(DatabaseMetaData.this, DatabaseMetaData.TableType.TABLE.getName(), catalogStr, null, results.getString(1)), new ByteArrayRow(row, getExceptionInterceptor()));
                
                }
                
              }
              else if (shouldReportTables)
              {
                row[3] = DatabaseMetaData.TableType.TABLE.asBytes();
                sortedRows.put(new DatabaseMetaData.TableMetaDataKey(DatabaseMetaData.this, DatabaseMetaData.TableType.TABLE.getName(), catalogStr, null, results.getString(1)), new ByteArrayRow(row, getExceptionInterceptor()));
              }
              
            }
          }
          finally
          {
            if (results != null) {
              try {
                results.close();
              }
              catch (Exception ex) {}
              
              results = null;
            }
          }
        }
      }.doForAll();
    } finally {
      if (stmt != null) {
        stmt.close();
      }
    }
    
    tuples.addAll(sortedRows.values());
    ResultSet tables = buildResultSet(createTablesFields(), tuples);
    
    return tables;
  }
  
  protected Field[] createTablesFields() {
    Field[] fields = new Field[10];
    fields[0] = new Field("", "TABLE_CAT", 12, 255);
    fields[1] = new Field("", "TABLE_SCHEM", 12, 0);
    fields[2] = new Field("", "TABLE_NAME", 12, 255);
    fields[3] = new Field("", "TABLE_TYPE", 12, 5);
    fields[4] = new Field("", "REMARKS", 12, 0);
    fields[5] = new Field("", "TYPE_CAT", 12, 0);
    fields[6] = new Field("", "TYPE_SCHEM", 12, 0);
    fields[7] = new Field("", "TYPE_NAME", 12, 0);
    fields[8] = new Field("", "SELF_REFERENCING_COL_NAME", 12, 0);
    fields[9] = new Field("", "REF_GENERATION", 12, 0);
    return fields;
  }
  













  public ResultSet getTableTypes()
    throws SQLException
  {
    ArrayList<ResultSetRow> tuples = new ArrayList();
    Field[] fields = { new Field("", "TABLE_TYPE", 12, 256) };
    
    boolean minVersion5_0_1 = conn.versionMeetsMinimum(5, 0, 1);
    
    tuples.add(new ByteArrayRow(new byte[][] { TableType.LOCAL_TEMPORARY.asBytes() }, getExceptionInterceptor()));
    tuples.add(new ByteArrayRow(new byte[][] { TableType.SYSTEM_TABLE.asBytes() }, getExceptionInterceptor()));
    if (minVersion5_0_1) {
      tuples.add(new ByteArrayRow(new byte[][] { TableType.SYSTEM_VIEW.asBytes() }, getExceptionInterceptor()));
    }
    tuples.add(new ByteArrayRow(new byte[][] { TableType.TABLE.asBytes() }, getExceptionInterceptor()));
    if (minVersion5_0_1) {
      tuples.add(new ByteArrayRow(new byte[][] { TableType.VIEW.asBytes() }, getExceptionInterceptor()));
    }
    
    return buildResultSet(fields, tuples);
  }
  




  public String getTimeDateFunctions()
    throws SQLException
  {
    return "DAYOFWEEK,WEEKDAY,DAYOFMONTH,DAYOFYEAR,MONTH,DAYNAME,MONTHNAME,QUARTER,WEEK,YEAR,HOUR,MINUTE,SECOND,PERIOD_ADD,PERIOD_DIFF,TO_DAYS,FROM_DAYS,DATE_FORMAT,TIME_FORMAT,CURDATE,CURRENT_DATE,CURTIME,CURRENT_TIME,NOW,SYSDATE,CURRENT_TIMESTAMP,UNIX_TIMESTAMP,FROM_UNIXTIME,SEC_TO_TIME,TIME_TO_SEC";
  }
  
























































































  public ResultSet getTypeInfo()
    throws SQLException
  {
    Field[] fields = new Field[18];
    fields[0] = new Field("", "TYPE_NAME", 1, 32);
    fields[1] = new Field("", "DATA_TYPE", 4, 5);
    fields[2] = new Field("", "PRECISION", 4, 10);
    fields[3] = new Field("", "LITERAL_PREFIX", 1, 4);
    fields[4] = new Field("", "LITERAL_SUFFIX", 1, 4);
    fields[5] = new Field("", "CREATE_PARAMS", 1, 32);
    fields[6] = new Field("", "NULLABLE", 5, 5);
    fields[7] = new Field("", "CASE_SENSITIVE", 16, 3);
    fields[8] = new Field("", "SEARCHABLE", 5, 3);
    fields[9] = new Field("", "UNSIGNED_ATTRIBUTE", 16, 3);
    fields[10] = new Field("", "FIXED_PREC_SCALE", 16, 3);
    fields[11] = new Field("", "AUTO_INCREMENT", 16, 3);
    fields[12] = new Field("", "LOCAL_TYPE_NAME", 1, 32);
    fields[13] = new Field("", "MINIMUM_SCALE", 5, 5);
    fields[14] = new Field("", "MAXIMUM_SCALE", 5, 5);
    fields[15] = new Field("", "SQL_DATA_TYPE", 4, 10);
    fields[16] = new Field("", "SQL_DATETIME_SUB", 4, 10);
    fields[17] = new Field("", "NUM_PREC_RADIX", 4, 10);
    
    byte[][] rowVal = (byte[][])null;
    ArrayList<ResultSetRow> tuples = new ArrayList();
    






    rowVal = new byte[18][];
    rowVal[0] = s2b("BIT");
    rowVal[1] = Integer.toString(-7).getBytes();
    

    rowVal[2] = s2b("1");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("true");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("BIT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("BOOL");
    rowVal[1] = Integer.toString(-7).getBytes();
    

    rowVal[2] = s2b("1");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("true");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("BOOL");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("TINYINT");
    rowVal[1] = Integer.toString(-6).getBytes();
    

    rowVal[2] = s2b("3");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [UNSIGNED] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("TINYINT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    
    rowVal = new byte[18][];
    rowVal[0] = s2b("TINYINT UNSIGNED");
    rowVal[1] = Integer.toString(-6).getBytes();
    

    rowVal[2] = s2b("3");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [UNSIGNED] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("TINYINT UNSIGNED");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("BIGINT");
    rowVal[1] = Integer.toString(-5).getBytes();
    

    rowVal[2] = s2b("19");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [UNSIGNED] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("BIGINT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    
    rowVal = new byte[18][];
    rowVal[0] = s2b("BIGINT UNSIGNED");
    rowVal[1] = Integer.toString(-5).getBytes();
    

    rowVal[2] = s2b("20");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("BIGINT UNSIGNED");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("LONG VARBINARY");
    rowVal[1] = Integer.toString(-4).getBytes();
    

    rowVal[2] = s2b("16777215");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("true");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("LONG VARBINARY");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("MEDIUMBLOB");
    rowVal[1] = Integer.toString(-4).getBytes();
    

    rowVal[2] = s2b("16777215");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("true");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("MEDIUMBLOB");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("LONGBLOB");
    rowVal[1] = Integer.toString(-4).getBytes();
    

    rowVal[2] = Integer.toString(Integer.MAX_VALUE).getBytes();
    

    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("true");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("LONGBLOB");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("BLOB");
    rowVal[1] = Integer.toString(-4).getBytes();
    

    rowVal[2] = s2b("65535");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("true");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("BLOB");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("TINYBLOB");
    rowVal[1] = Integer.toString(-4).getBytes();
    

    rowVal[2] = s2b("255");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("true");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("TINYBLOB");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    




    rowVal = new byte[18][];
    rowVal[0] = s2b("VARBINARY");
    rowVal[1] = Integer.toString(-3).getBytes();
    

    rowVal[2] = s2b(conn.versionMeetsMinimum(5, 0, 3) ? "65535" : "255");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("(M)");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("true");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("VARBINARY");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    




    rowVal = new byte[18][];
    rowVal[0] = s2b("BINARY");
    rowVal[1] = Integer.toString(-2).getBytes();
    

    rowVal[2] = s2b("255");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("(M)");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("true");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("BINARY");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("LONG VARCHAR");
    rowVal[1] = Integer.toString(-1).getBytes();
    

    rowVal[2] = s2b("16777215");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("LONG VARCHAR");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("MEDIUMTEXT");
    rowVal[1] = Integer.toString(-1).getBytes();
    

    rowVal[2] = s2b("16777215");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("MEDIUMTEXT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("LONGTEXT");
    rowVal[1] = Integer.toString(-1).getBytes();
    

    rowVal[2] = Integer.toString(Integer.MAX_VALUE).getBytes();
    

    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("LONGTEXT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("TEXT");
    rowVal[1] = Integer.toString(-1).getBytes();
    

    rowVal[2] = s2b("65535");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("TEXT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("TINYTEXT");
    rowVal[1] = Integer.toString(-1).getBytes();
    

    rowVal[2] = s2b("255");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("TINYTEXT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("CHAR");
    rowVal[1] = Integer.toString(1).getBytes();
    

    rowVal[2] = s2b("255");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("(M)");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("CHAR");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    


    int decimalPrecision = 254;
    
    if (conn.versionMeetsMinimum(5, 0, 3)) {
      if (conn.versionMeetsMinimum(5, 0, 6)) {
        decimalPrecision = 65;
      } else {
        decimalPrecision = 64;
      }
    }
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("NUMERIC");
    rowVal[1] = Integer.toString(2).getBytes();
    

    rowVal[2] = s2b(String.valueOf(decimalPrecision));
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M[,D])] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("NUMERIC");
    rowVal[13] = s2b("-308");
    rowVal[14] = s2b("308");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("DECIMAL");
    rowVal[1] = Integer.toString(3).getBytes();
    

    rowVal[2] = s2b(String.valueOf(decimalPrecision));
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M[,D])] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("DECIMAL");
    rowVal[13] = s2b("-308");
    rowVal[14] = s2b("308");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("INTEGER");
    rowVal[1] = Integer.toString(4).getBytes();
    

    rowVal[2] = s2b("10");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [UNSIGNED] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("INTEGER");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    
    rowVal = new byte[18][];
    rowVal[0] = s2b("INTEGER UNSIGNED");
    rowVal[1] = Integer.toString(4).getBytes();
    

    rowVal[2] = s2b("10");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("INTEGER UNSIGNED");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("INT");
    rowVal[1] = Integer.toString(4).getBytes();
    

    rowVal[2] = s2b("10");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [UNSIGNED] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("INT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    
    rowVal = new byte[18][];
    rowVal[0] = s2b("INT UNSIGNED");
    rowVal[1] = Integer.toString(4).getBytes();
    

    rowVal[2] = s2b("10");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("INT UNSIGNED");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("MEDIUMINT");
    rowVal[1] = Integer.toString(4).getBytes();
    

    rowVal[2] = s2b("7");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [UNSIGNED] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("MEDIUMINT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    
    rowVal = new byte[18][];
    rowVal[0] = s2b("MEDIUMINT UNSIGNED");
    rowVal[1] = Integer.toString(4).getBytes();
    

    rowVal[2] = s2b("8");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("MEDIUMINT UNSIGNED");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("SMALLINT");
    rowVal[1] = Integer.toString(5).getBytes();
    

    rowVal[2] = s2b("5");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [UNSIGNED] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("SMALLINT");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    
    rowVal = new byte[18][];
    rowVal[0] = s2b("SMALLINT UNSIGNED");
    rowVal[1] = Integer.toString(5).getBytes();
    

    rowVal[2] = s2b("5");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M)] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("true");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("SMALLINT UNSIGNED");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    




    rowVal = new byte[18][];
    rowVal[0] = s2b("FLOAT");
    rowVal[1] = Integer.toString(7).getBytes();
    

    rowVal[2] = s2b("10");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M,D)] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("FLOAT");
    rowVal[13] = s2b("-38");
    rowVal[14] = s2b("38");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("DOUBLE");
    rowVal[1] = Integer.toString(8).getBytes();
    

    rowVal[2] = s2b("17");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M,D)] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("DOUBLE");
    rowVal[13] = s2b("-308");
    rowVal[14] = s2b("308");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("DOUBLE PRECISION");
    rowVal[1] = Integer.toString(8).getBytes();
    

    rowVal[2] = s2b("17");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M,D)] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("DOUBLE PRECISION");
    rowVal[13] = s2b("-308");
    rowVal[14] = s2b("308");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("REAL");
    rowVal[1] = Integer.toString(8).getBytes();
    

    rowVal[2] = s2b("17");
    rowVal[3] = s2b("");
    rowVal[4] = s2b("");
    rowVal[5] = s2b("[(M,D)] [ZEROFILL]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("true");
    rowVal[12] = s2b("REAL");
    rowVal[13] = s2b("-308");
    rowVal[14] = s2b("308");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("VARCHAR");
    rowVal[1] = Integer.toString(12).getBytes();
    

    rowVal[2] = s2b(conn.versionMeetsMinimum(5, 0, 3) ? "65535" : "255");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("(M)");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("VARCHAR");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("ENUM");
    rowVal[1] = Integer.toString(12).getBytes();
    

    rowVal[2] = s2b("65535");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("ENUM");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("SET");
    rowVal[1] = Integer.toString(12).getBytes();
    

    rowVal[2] = s2b("64");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("SET");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("DATE");
    rowVal[1] = Integer.toString(91).getBytes();
    

    rowVal[2] = s2b("0");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("DATE");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("TIME");
    rowVal[1] = Integer.toString(92).getBytes();
    

    rowVal[2] = s2b("0");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("TIME");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("DATETIME");
    rowVal[1] = Integer.toString(93).getBytes();
    

    rowVal[2] = s2b("0");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("DATETIME");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    



    rowVal = new byte[18][];
    rowVal[0] = s2b("TIMESTAMP");
    rowVal[1] = Integer.toString(93).getBytes();
    

    rowVal[2] = s2b("0");
    rowVal[3] = s2b("'");
    rowVal[4] = s2b("'");
    rowVal[5] = s2b("[(M)]");
    rowVal[6] = Integer.toString(1).getBytes();
    

    rowVal[7] = s2b("false");
    rowVal[8] = Integer.toString(3).getBytes();
    

    rowVal[9] = s2b("false");
    rowVal[10] = s2b("false");
    rowVal[11] = s2b("false");
    rowVal[12] = s2b("TIMESTAMP");
    rowVal[13] = s2b("0");
    rowVal[14] = s2b("0");
    rowVal[15] = s2b("0");
    rowVal[16] = s2b("0");
    rowVal[17] = s2b("10");
    tuples.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
    
    return buildResultSet(fields, tuples);
  }
  



































  public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types)
    throws SQLException
  {
    Field[] fields = new Field[7];
    fields[0] = new Field("", "TYPE_CAT", 12, 32);
    fields[1] = new Field("", "TYPE_SCHEM", 12, 32);
    fields[2] = new Field("", "TYPE_NAME", 12, 32);
    fields[3] = new Field("", "CLASS_NAME", 12, 32);
    fields[4] = new Field("", "DATA_TYPE", 4, 10);
    fields[5] = new Field("", "REMARKS", 12, 32);
    fields[6] = new Field("", "BASE_TYPE", 5, 10);
    
    ArrayList<ResultSetRow> tuples = new ArrayList();
    
    return buildResultSet(fields, tuples);
  }
  




  public String getURL()
    throws SQLException
  {
    return conn.getURL();
  }
  




  public String getUserName()
    throws SQLException
  {
    if (conn.getUseHostsInPrivileges()) {
      java.sql.Statement stmt = null;
      ResultSet rs = null;
      try
      {
        stmt = conn.getMetadataSafeStatement();
        
        rs = stmt.executeQuery("SELECT USER()");
        rs.next();
        
        return rs.getString(1);
      } finally {
        if (rs != null) {
          try {
            rs.close();
          } catch (Exception ex) {
            AssertionFailedException.shouldNotHappen(ex);
          }
          
          rs = null;
        }
        
        if (stmt != null) {
          try {
            stmt.close();
          } catch (Exception ex) {
            AssertionFailedException.shouldNotHappen(ex);
          }
          
          stmt = null;
        }
      }
    }
    
    return conn.getUser();
  }
  































  public ResultSet getVersionColumns(String catalog, String schema, final String table)
    throws SQLException
  {
    if (table == null) {
      throw SQLError.createSQLException("Table not specified.", "S1009", getExceptionInterceptor());
    }
    
    Field[] fields = new Field[8];
    fields[0] = new Field("", "SCOPE", 5, 5);
    fields[1] = new Field("", "COLUMN_NAME", 1, 32);
    fields[2] = new Field("", "DATA_TYPE", 4, 5);
    fields[3] = new Field("", "TYPE_NAME", 1, 16);
    fields[4] = new Field("", "COLUMN_SIZE", 4, 16);
    fields[5] = new Field("", "BUFFER_LENGTH", 4, 16);
    fields[6] = new Field("", "DECIMAL_DIGITS", 5, 16);
    fields[7] = new Field("", "PSEUDO_COLUMN", 5, 5);
    
    final ArrayList<ResultSetRow> rows = new ArrayList();
    
    final java.sql.Statement stmt = conn.getMetadataSafeStatement();
    
    try
    {
      new IterateBlock(getCatalogIterator(catalog))
      {
        void forEach(String catalogStr) throws SQLException
        {
          ResultSet results = null;
          boolean with_where = conn.versionMeetsMinimum(5, 0, 0);
          try
          {
            StringBuilder whereBuf = new StringBuilder(" Extra LIKE '%on update CURRENT_TIMESTAMP%'");
            List<String> rsFields = new ArrayList();
            


            if (!conn.versionMeetsMinimum(5, 1, 23))
            {
              whereBuf = new StringBuilder();
              boolean firstTime = true;
              
              String query = "SHOW CREATE TABLE " + getFullyQualifiedName(catalogStr, table);
              
              results = stmt.executeQuery(query);
              while (results.next()) {
                String createTableString = results.getString(2);
                StringTokenizer lineTokenizer = new StringTokenizer(createTableString, "\n");
                
                while (lineTokenizer.hasMoreTokens()) {
                  String line = lineTokenizer.nextToken().trim();
                  if (StringUtils.indexOfIgnoreCase(line, "on update CURRENT_TIMESTAMP") > -1) {
                    boolean usingBackTicks = true;
                    int beginPos = line.indexOf(quotedId);
                    
                    if (beginPos == -1) {
                      beginPos = line.indexOf("\"");
                      usingBackTicks = false;
                    }
                    
                    if (beginPos != -1) {
                      int endPos = -1;
                      
                      if (usingBackTicks) {
                        endPos = line.indexOf(quotedId, beginPos + 1);
                      } else {
                        endPos = line.indexOf("\"", beginPos + 1);
                      }
                      
                      if (endPos != -1) {
                        if (with_where) {
                          if (!firstTime) {
                            whereBuf.append(" or");
                          } else {
                            firstTime = false;
                          }
                          whereBuf.append(" Field='");
                          whereBuf.append(line.substring(beginPos + 1, endPos));
                          whereBuf.append("'");
                        } else {
                          rsFields.add(line.substring(beginPos + 1, endPos));
                        }
                      }
                    }
                  }
                }
              }
            }
            
            if ((whereBuf.length() > 0) || (rsFields.size() > 0)) {
              StringBuilder queryBuf = new StringBuilder("SHOW COLUMNS FROM ");
              queryBuf.append(StringUtils.quoteIdentifier(table, quotedId, conn.getPedantic()));
              queryBuf.append(" FROM ");
              queryBuf.append(StringUtils.quoteIdentifier(catalogStr, quotedId, conn.getPedantic()));
              if (with_where) {
                queryBuf.append(" WHERE");
                queryBuf.append(whereBuf.toString());
              }
              
              results = stmt.executeQuery(queryBuf.toString());
              
              while (results.next()) {
                if ((with_where) || (rsFields.contains(results.getString("Field")))) {
                  DatabaseMetaData.TypeDescriptor typeDesc = new DatabaseMetaData.TypeDescriptor(DatabaseMetaData.this, results.getString("Type"), results.getString("Null"));
                  byte[][] rowVal = new byte[8][];
                  
                  rowVal[0] = null;
                  
                  rowVal[1] = results.getBytes("Field");
                  
                  rowVal[2] = Short.toString(dataType).getBytes();
                  
                  rowVal[3] = s2b(typeName);
                  
                  rowVal[4] = (columnSize == null ? null : s2b(columnSize.toString()));
                  
                  rowVal[5] = s2b(Integer.toString(bufferLength));
                  
                  rowVal[6] = (decimalDigits == null ? null : s2b(decimalDigits.toString()));
                  
                  rowVal[7] = Integer.toString(1).getBytes();
                  
                  rows.add(new ByteArrayRow(rowVal, getExceptionInterceptor()));
                }
              }
            }
          } catch (SQLException sqlEx) {
            if (!"42S02".equals(sqlEx.getSQLState())) {
              throw sqlEx;
            }
          } finally {
            if (results != null) {
              try {
                results.close();
              }
              catch (Exception ex) {}
              
              results = null;
            }
          }
        }
      }.doForAll();
    }
    finally {
      if (stmt != null) {
        stmt.close();
      }
    }
    
    return buildResultSet(fields, rows);
  }
  








  public boolean insertsAreDetected(int type)
    throws SQLException
  {
    return false;
  }
  





  public boolean isCatalogAtStart()
    throws SQLException
  {
    return true;
  }
  




  public boolean isReadOnly()
    throws SQLException
  {
    return false;
  }
  

  public boolean locatorsUpdateCopy()
    throws SQLException
  {
    return !conn.getEmulateLocators();
  }
  





  public boolean nullPlusNonNullIsNull()
    throws SQLException
  {
    return true;
  }
  




  public boolean nullsAreSortedAtEnd()
    throws SQLException
  {
    return false;
  }
  




  public boolean nullsAreSortedAtStart()
    throws SQLException
  {
    return (conn.versionMeetsMinimum(4, 0, 2)) && (!conn.versionMeetsMinimum(4, 0, 11));
  }
  




  public boolean nullsAreSortedHigh()
    throws SQLException
  {
    return false;
  }
  




  public boolean nullsAreSortedLow()
    throws SQLException
  {
    return !nullsAreSortedHigh();
  }
  


  public boolean othersDeletesAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  


  public boolean othersInsertsAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  







  public boolean othersUpdatesAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  


  public boolean ownDeletesAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  


  public boolean ownInsertsAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  







  public boolean ownUpdatesAreVisible(int type)
    throws SQLException
  {
    return false;
  }
  









  protected LocalAndReferencedColumns parseTableStatusIntoLocalAndReferencedColumns(String keysComment)
    throws SQLException
  {
    String columnsDelimitter = ",";
    
    int indexOfOpenParenLocalColumns = StringUtils.indexOfIgnoreCase(0, keysComment, "(", quotedId, quotedId, StringUtils.SEARCH_MODE__ALL);
    
    if (indexOfOpenParenLocalColumns == -1) {
      throw SQLError.createSQLException("Error parsing foreign keys definition, couldn't find start of local columns list.", "S1000", getExceptionInterceptor());
    }
    

    String constraintName = StringUtils.unQuoteIdentifier(keysComment.substring(0, indexOfOpenParenLocalColumns).trim(), quotedId);
    keysComment = keysComment.substring(indexOfOpenParenLocalColumns, keysComment.length());
    
    String keysCommentTrimmed = keysComment.trim();
    
    int indexOfCloseParenLocalColumns = StringUtils.indexOfIgnoreCase(0, keysCommentTrimmed, ")", quotedId, quotedId, StringUtils.SEARCH_MODE__ALL);
    

    if (indexOfCloseParenLocalColumns == -1) {
      throw SQLError.createSQLException("Error parsing foreign keys definition, couldn't find end of local columns list.", "S1000", getExceptionInterceptor());
    }
    

    String localColumnNamesString = keysCommentTrimmed.substring(1, indexOfCloseParenLocalColumns);
    
    int indexOfRefer = StringUtils.indexOfIgnoreCase(0, keysCommentTrimmed, "REFER ", quotedId, quotedId, StringUtils.SEARCH_MODE__ALL);
    
    if (indexOfRefer == -1) {
      throw SQLError.createSQLException("Error parsing foreign keys definition, couldn't find start of referenced tables list.", "S1000", getExceptionInterceptor());
    }
    

    int indexOfOpenParenReferCol = StringUtils.indexOfIgnoreCase(indexOfRefer, keysCommentTrimmed, "(", quotedId, quotedId, StringUtils.SEARCH_MODE__MRK_COM_WS);
    

    if (indexOfOpenParenReferCol == -1) {
      throw SQLError.createSQLException("Error parsing foreign keys definition, couldn't find start of referenced columns list.", "S1000", getExceptionInterceptor());
    }
    

    String referCatalogTableString = keysCommentTrimmed.substring(indexOfRefer + "REFER ".length(), indexOfOpenParenReferCol);
    
    int indexOfSlash = StringUtils.indexOfIgnoreCase(0, referCatalogTableString, "/", quotedId, quotedId, StringUtils.SEARCH_MODE__MRK_COM_WS);
    
    if (indexOfSlash == -1) {
      throw SQLError.createSQLException("Error parsing foreign keys definition, couldn't find name of referenced catalog.", "S1000", getExceptionInterceptor());
    }
    

    String referCatalog = StringUtils.unQuoteIdentifier(referCatalogTableString.substring(0, indexOfSlash), quotedId);
    String referTable = StringUtils.unQuoteIdentifier(referCatalogTableString.substring(indexOfSlash + 1).trim(), quotedId);
    
    int indexOfCloseParenRefer = StringUtils.indexOfIgnoreCase(indexOfOpenParenReferCol, keysCommentTrimmed, ")", quotedId, quotedId, StringUtils.SEARCH_MODE__ALL);
    

    if (indexOfCloseParenRefer == -1) {
      throw SQLError.createSQLException("Error parsing foreign keys definition, couldn't find end of referenced columns list.", "S1000", getExceptionInterceptor());
    }
    

    String referColumnNamesString = keysCommentTrimmed.substring(indexOfOpenParenReferCol + 1, indexOfCloseParenRefer);
    
    List<String> referColumnsList = StringUtils.split(referColumnNamesString, columnsDelimitter, quotedId, quotedId, false);
    List<String> localColumnsList = StringUtils.split(localColumnNamesString, columnsDelimitter, quotedId, quotedId, false);
    
    return new LocalAndReferencedColumns(localColumnsList, referColumnsList, constraintName, referCatalog, referTable);
  }
  




  protected byte[] s2b(String s)
    throws SQLException
  {
    if (s == null) {
      return null;
    }
    
    return StringUtils.getBytes(s, conn.getCharacterSetMetadata(), conn.getServerCharset(), conn.parserKnowsUnicode(), conn, getExceptionInterceptor());
  }
  






  public boolean storesLowerCaseIdentifiers()
    throws SQLException
  {
    return conn.storesLowerCaseTableName();
  }
  





  public boolean storesLowerCaseQuotedIdentifiers()
    throws SQLException
  {
    return conn.storesLowerCaseTableName();
  }
  





  public boolean storesMixedCaseIdentifiers()
    throws SQLException
  {
    return !conn.storesLowerCaseTableName();
  }
  





  public boolean storesMixedCaseQuotedIdentifiers()
    throws SQLException
  {
    return !conn.storesLowerCaseTableName();
  }
  





  public boolean storesUpperCaseIdentifiers()
    throws SQLException
  {
    return false;
  }
  





  public boolean storesUpperCaseQuotedIdentifiers()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsAlterTableWithAddColumn()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsAlterTableWithDropColumn()
    throws SQLException
  {
    return true;
  }
  





  public boolean supportsANSI92EntryLevelSQL()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsANSI92FullSQL()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsANSI92IntermediateSQL()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsBatchUpdates()
    throws SQLException
  {
    return true;
  }
  





  public boolean supportsCatalogsInDataManipulation()
    throws SQLException
  {
    return conn.versionMeetsMinimum(3, 22, 0);
  }
  





  public boolean supportsCatalogsInIndexDefinitions()
    throws SQLException
  {
    return conn.versionMeetsMinimum(3, 22, 0);
  }
  





  public boolean supportsCatalogsInPrivilegeDefinitions()
    throws SQLException
  {
    return conn.versionMeetsMinimum(3, 22, 0);
  }
  





  public boolean supportsCatalogsInProcedureCalls()
    throws SQLException
  {
    return conn.versionMeetsMinimum(3, 22, 0);
  }
  





  public boolean supportsCatalogsInTableDefinitions()
    throws SQLException
  {
    return conn.versionMeetsMinimum(3, 22, 0);
  }
  








  public boolean supportsColumnAliasing()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsConvert()
    throws SQLException
  {
    return false;
  }
  










  public boolean supportsConvert(int fromType, int toType)
    throws SQLException
  {
    switch (fromType)
    {



    case -4: 
    case -3: 
    case -2: 
    case -1: 
    case 1: 
    case 12: 
      switch (toType) {
      case -6: 
      case -5: 
      case -4: 
      case -3: 
      case -2: 
      case -1: 
      case 1: 
      case 2: 
      case 3: 
      case 4: 
      case 5: 
      case 6: 
      case 7: 
      case 8: 
      case 12: 
      case 91: 
      case 92: 
      case 93: 
      case 1111: 
        return true;
      }
      
      return false;
    




    case -7: 
      return false;
    




    case -6: 
    case -5: 
    case 2: 
    case 3: 
    case 4: 
    case 5: 
    case 6: 
    case 7: 
    case 8: 
      switch (toType) {
      case -6: 
      case -5: 
      case -4: 
      case -3: 
      case -2: 
      case -1: 
      case 1: 
      case 2: 
      case 3: 
      case 4: 
      case 5: 
      case 6: 
      case 7: 
      case 8: 
      case 12: 
        return true;
      }
      
      return false;
    


    case 0: 
      return false;
    




    case 1111: 
      switch (toType) {
      case -4: 
      case -3: 
      case -2: 
      case -1: 
      case 1: 
      case 12: 
        return true;
      }
      
      return false;
    



    case 91: 
      switch (toType) {
      case -4: 
      case -3: 
      case -2: 
      case -1: 
      case 1: 
      case 12: 
        return true;
      }
      
      return false;
    



    case 92: 
      switch (toType) {
      case -4: 
      case -3: 
      case -2: 
      case -1: 
      case 1: 
      case 12: 
        return true;
      }
      
      return false;
    





    case 93: 
      switch (toType) {
      case -4: 
      case -3: 
      case -2: 
      case -1: 
      case 1: 
      case 12: 
      case 91: 
      case 92: 
        return true;
      }
      
      return false;
    }
    
    

    return false;
  }
  





  public boolean supportsCoreSQLGrammar()
    throws SQLException
  {
    return true;
  }
  





  public boolean supportsCorrelatedSubqueries()
    throws SQLException
  {
    return conn.versionMeetsMinimum(4, 1, 0);
  }
  





  public boolean supportsDataDefinitionAndDataManipulationTransactions()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsDataManipulationTransactionsOnly()
    throws SQLException
  {
    return false;
  }
  






  public boolean supportsDifferentTableCorrelationNames()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsExpressionsInOrderBy()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsExtendedSQLGrammar()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsFullOuterJoins()
    throws SQLException
  {
    return false;
  }
  


  public boolean supportsGetGeneratedKeys()
  {
    return true;
  }
  




  public boolean supportsGroupBy()
    throws SQLException
  {
    return true;
  }
  





  public boolean supportsGroupByBeyondSelect()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsGroupByUnrelated()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsIntegrityEnhancementFacility()
    throws SQLException
  {
    if (!conn.getOverrideSupportsIntegrityEnhancementFacility()) {
      return false;
    }
    
    return true;
  }
  





  public boolean supportsLikeEscapeClause()
    throws SQLException
  {
    return true;
  }
  





  public boolean supportsLimitedOuterJoins()
    throws SQLException
  {
    return true;
  }
  





  public boolean supportsMinimumSQLGrammar()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsMixedCaseIdentifiers()
    throws SQLException
  {
    return !conn.lowerCaseTableNames();
  }
  





  public boolean supportsMixedCaseQuotedIdentifiers()
    throws SQLException
  {
    return !conn.lowerCaseTableNames();
  }
  

  public boolean supportsMultipleOpenResults()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsMultipleResultSets()
    throws SQLException
  {
    return conn.versionMeetsMinimum(4, 1, 0);
  }
  





  public boolean supportsMultipleTransactions()
    throws SQLException
  {
    return true;
  }
  

  public boolean supportsNamedParameters()
    throws SQLException
  {
    return false;
  }
  





  public boolean supportsNonNullableColumns()
    throws SQLException
  {
    return true;
  }
  






  public boolean supportsOpenCursorsAcrossCommit()
    throws SQLException
  {
    return false;
  }
  






  public boolean supportsOpenCursorsAcrossRollback()
    throws SQLException
  {
    return false;
  }
  






  public boolean supportsOpenStatementsAcrossCommit()
    throws SQLException
  {
    return false;
  }
  






  public boolean supportsOpenStatementsAcrossRollback()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsOrderByUnrelated()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsOuterJoins()
    throws SQLException
  {
    return true;
  }
  




  public boolean supportsPositionedDelete()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsPositionedUpdate()
    throws SQLException
  {
    return false;
  }
  











  public boolean supportsResultSetConcurrency(int type, int concurrency)
    throws SQLException
  {
    switch (type) {
    case 1004: 
      if ((concurrency == 1007) || (concurrency == 1008)) {
        return true;
      }
      throw SQLError.createSQLException("Illegal arguments to supportsResultSetConcurrency()", "S1009", getExceptionInterceptor());
    

    case 1003: 
      if ((concurrency == 1007) || (concurrency == 1008)) {
        return true;
      }
      throw SQLError.createSQLException("Illegal arguments to supportsResultSetConcurrency()", "S1009", getExceptionInterceptor());
    

    case 1005: 
      return false;
    }
    throw SQLError.createSQLException("Illegal arguments to supportsResultSetConcurrency()", "S1009", getExceptionInterceptor());
  }
  




  public boolean supportsResultSetHoldability(int holdability)
    throws SQLException
  {
    return holdability == 1;
  }
  








  public boolean supportsResultSetType(int type)
    throws SQLException
  {
    return type == 1004;
  }
  


  public boolean supportsSavepoints()
    throws SQLException
  {
    return (conn.versionMeetsMinimum(4, 0, 14)) || (conn.versionMeetsMinimum(4, 1, 1));
  }
  




  public boolean supportsSchemasInDataManipulation()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsSchemasInIndexDefinitions()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsSchemasInPrivilegeDefinitions()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsSchemasInProcedureCalls()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsSchemasInTableDefinitions()
    throws SQLException
  {
    return false;
  }
  




  public boolean supportsSelectForUpdate()
    throws SQLException
  {
    return conn.versionMeetsMinimum(4, 0, 0);
  }
  

  public boolean supportsStatementPooling()
    throws SQLException
  {
    return false;
  }
  





  public boolean supportsStoredProcedures()
    throws SQLException
  {
    return conn.versionMeetsMinimum(5, 0, 0);
  }
  





  public boolean supportsSubqueriesInComparisons()
    throws SQLException
  {
    return conn.versionMeetsMinimum(4, 1, 0);
  }
  





  public boolean supportsSubqueriesInExists()
    throws SQLException
  {
    return conn.versionMeetsMinimum(4, 1, 0);
  }
  





  public boolean supportsSubqueriesInIns()
    throws SQLException
  {
    return conn.versionMeetsMinimum(4, 1, 0);
  }
  





  public boolean supportsSubqueriesInQuantifieds()
    throws SQLException
  {
    return conn.versionMeetsMinimum(4, 1, 0);
  }
  





  public boolean supportsTableCorrelationNames()
    throws SQLException
  {
    return true;
  }
  








  public boolean supportsTransactionIsolationLevel(int level)
    throws SQLException
  {
    if (conn.supportsIsolationLevel()) {
      switch (level) {
      case 1: 
      case 2: 
      case 4: 
      case 8: 
        return true;
      }
      
      return false;
    }
    

    return false;
  }
  





  public boolean supportsTransactions()
    throws SQLException
  {
    return conn.supportsTransactions();
  }
  




  public boolean supportsUnion()
    throws SQLException
  {
    return conn.versionMeetsMinimum(4, 0, 0);
  }
  




  public boolean supportsUnionAll()
    throws SQLException
  {
    return conn.versionMeetsMinimum(4, 0, 0);
  }
  








  public boolean updatesAreDetected(int type)
    throws SQLException
  {
    return false;
  }
  




  public boolean usesLocalFilePerTable()
    throws SQLException
  {
    return false;
  }
  




  public boolean usesLocalFiles()
    throws SQLException
  {
    return false;
  }
  


























  public ResultSet getClientInfoProperties()
    throws SQLException
  {
    Field[] fields = new Field[4];
    fields[0] = new Field("", "NAME", 12, 255);
    fields[1] = new Field("", "MAX_LEN", 4, 10);
    fields[2] = new Field("", "DEFAULT_VALUE", 12, 255);
    fields[3] = new Field("", "DESCRIPTION", 12, 255);
    
    return buildResultSet(fields, new ArrayList(), conn);
  }
  





  public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern)
    throws SQLException
  {
    Field[] fields = createFunctionColumnsFields();
    
    return getProcedureOrFunctionColumns(fields, catalog, schemaPattern, functionNamePattern, columnNamePattern, false, true);
  }
  
  protected Field[] createFunctionColumnsFields() {
    Field[] fields = { new Field("", "FUNCTION_CAT", 12, 512), new Field("", "FUNCTION_SCHEM", 12, 512), new Field("", "FUNCTION_NAME", 12, 512), new Field("", "COLUMN_NAME", 12, 512), new Field("", "COLUMN_TYPE", 12, 64), new Field("", "DATA_TYPE", 5, 6), new Field("", "TYPE_NAME", 12, 64), new Field("", "PRECISION", 4, 12), new Field("", "LENGTH", 4, 12), new Field("", "SCALE", 5, 12), new Field("", "RADIX", 5, 6), new Field("", "NULLABLE", 5, 6), new Field("", "REMARKS", 12, 512), new Field("", "CHAR_OCTET_LENGTH", 4, 32), new Field("", "ORDINAL_POSITION", 4, 32), new Field("", "IS_NULLABLE", 12, 12), new Field("", "SPECIFIC_NAME", 12, 64) };
    





    return fields;
  }
  










































  public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern)
    throws SQLException
  {
    Field[] fields = new Field[6];
    
    fields[0] = new Field("", "FUNCTION_CAT", 1, 255);
    fields[1] = new Field("", "FUNCTION_SCHEM", 1, 255);
    fields[2] = new Field("", "FUNCTION_NAME", 1, 255);
    fields[3] = new Field("", "REMARKS", 1, 255);
    fields[4] = new Field("", "FUNCTION_TYPE", 5, 6);
    fields[5] = new Field("", "SPECIFIC_NAME", 1, 255);
    
    return getProceduresAndOrFunctions(fields, catalog, schemaPattern, functionNamePattern, false, true);
  }
  
  public boolean providesQueryObjectGenerator() throws SQLException {
    return false;
  }
  



  public ResultSet getSchemas(String catalog, String schemaPattern)
    throws SQLException
  {
    Field[] fields = { new Field("", "TABLE_SCHEM", 12, 255), new Field("", "TABLE_CATALOG", 12, 255) };
    
    return buildResultSet(fields, new ArrayList());
  }
  
  public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
    return true;
  }
  





  protected PreparedStatement prepareMetaDataSafeStatement(String sql)
    throws SQLException
  {
    PreparedStatement pStmt = conn.clientPrepareStatement(sql);
    
    if (pStmt.getMaxRows() != 0) {
      pStmt.setMaxRows(0);
    }
    
    ((Statement)pStmt).setHoldResultsOpenOverClose(true);
    
    return pStmt;
  }
  







  public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
    throws SQLException
  {
    Field[] fields = { new Field("", "TABLE_CAT", 12, 512), new Field("", "TABLE_SCHEM", 12, 512), new Field("", "TABLE_NAME", 12, 512), new Field("", "COLUMN_NAME", 12, 512), new Field("", "DATA_TYPE", 4, 12), new Field("", "COLUMN_SIZE", 4, 12), new Field("", "DECIMAL_DIGITS", 4, 12), new Field("", "NUM_PREC_RADIX", 4, 12), new Field("", "COLUMN_USAGE", 12, 512), new Field("", "REMARKS", 12, 512), new Field("", "CHAR_OCTET_LENGTH", 4, 12), new Field("", "IS_NULLABLE", 12, 512) };
    





    return buildResultSet(fields, new ArrayList());
  }
  
  public boolean generatedKeyAlwaysReturned() throws SQLException
  {
    return true;
  }
}
