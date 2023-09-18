package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.List;
































public class RowDataStatic
  implements RowData
{
  private Field[] metadata;
  private int index;
  ResultSetImpl owner;
  private List<ResultSetRow> rows;
  
  public RowDataStatic(List<ResultSetRow> rows)
  {
    index = -1;
    this.rows = rows;
  }
  
  public void addRow(ResultSetRow row) {
    rows.add(row);
  }
  


  public void afterLast()
  {
    if (rows.size() > 0) {
      index = rows.size();
    }
  }
  


  public void beforeFirst()
  {
    if (rows.size() > 0) {
      index = -1;
    }
  }
  
  public void beforeLast() {
    if (rows.size() > 0) {
      index = (rows.size() - 2);
    }
  }
  
  public void close() {}
  
  public ResultSetRow getAt(int atIndex) throws SQLException
  {
    if ((atIndex < 0) || (atIndex >= rows.size())) {
      return null;
    }
    
    return ((ResultSetRow)rows.get(atIndex)).setMetadata(metadata);
  }
  
  public int getCurrentRowNumber() {
    return index;
  }
  


  public ResultSetInternalMethods getOwner()
  {
    return owner;
  }
  
  public boolean hasNext() {
    boolean hasMore = index + 1 < rows.size();
    
    return hasMore;
  }
  


  public boolean isAfterLast()
  {
    return (index >= rows.size()) && (rows.size() != 0);
  }
  


  public boolean isBeforeFirst()
  {
    return (index == -1) && (rows.size() != 0);
  }
  
  public boolean isDynamic() {
    return false;
  }
  
  public boolean isEmpty() {
    return rows.size() == 0;
  }
  
  public boolean isFirst() {
    return index == 0;
  }
  


  public boolean isLast()
  {
    if (rows.size() == 0) {
      return false;
    }
    
    return index == rows.size() - 1;
  }
  
  public void moveRowRelative(int rowsToMove) {
    if (rows.size() > 0) {
      index += rowsToMove;
      if (index < -1) {
        beforeFirst();
      } else if (index > rows.size()) {
        afterLast();
      }
    }
  }
  
  public ResultSetRow next() throws SQLException {
    index += 1;
    
    if (index > rows.size()) {
      afterLast();
    } else if (index < rows.size()) {
      ResultSetRow row = (ResultSetRow)rows.get(index);
      
      return row.setMetadata(metadata);
    }
    
    return null;
  }
  
  public void removeRow(int atIndex) {
    rows.remove(atIndex);
  }
  
  public void setCurrentRow(int newIndex) {
    index = newIndex;
  }
  


  public void setOwner(ResultSetImpl rs)
  {
    owner = rs;
  }
  
  public int size() {
    return rows.size();
  }
  
  public boolean wasEmpty() {
    return (rows != null) && (rows.size() == 0);
  }
  
  public void setMetadata(Field[] metadata) {
    this.metadata = metadata;
  }
}
