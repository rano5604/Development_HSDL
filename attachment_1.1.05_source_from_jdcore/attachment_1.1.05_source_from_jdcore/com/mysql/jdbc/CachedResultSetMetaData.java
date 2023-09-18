package com.mysql.jdbc;

import java.sql.ResultSetMetaData;
import java.util.Map;























public class CachedResultSetMetaData
{
  Map<String, Integer> columnNameToIndex = null;
  

  Field[] fields;
  

  Map<String, Integer> fullColumnNameToIndex = null;
  ResultSetMetaData metadata;
  
  public CachedResultSetMetaData() {}
  
  public Map<String, Integer> getColumnNameToIndex() {
    return columnNameToIndex;
  }
  
  public Field[] getFields() {
    return fields;
  }
  
  public Map<String, Integer> getFullColumnNameToIndex() {
    return fullColumnNameToIndex;
  }
  
  public ResultSetMetaData getMetadata() {
    return metadata;
  }
}
