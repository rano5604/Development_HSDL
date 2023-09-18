package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Iterator;

























public abstract class IterateBlock<T>
{
  DatabaseMetaData.IteratorWithCleanup<T> iteratorWithCleanup;
  Iterator<T> javaIterator;
  boolean stopIterating = false;
  
  IterateBlock(DatabaseMetaData.IteratorWithCleanup<T> i) {
    iteratorWithCleanup = i;
    javaIterator = null;
  }
  
  IterateBlock(Iterator<T> i) {
    javaIterator = i;
    iteratorWithCleanup = null;
  }
  
  public void doForAll() throws SQLException {
    if (iteratorWithCleanup != null) {
      try {
        while (iteratorWithCleanup.hasNext()) {
          forEach(iteratorWithCleanup.next());
          
          if (stopIterating) {
            break;
          }
        }
      } finally {
        iteratorWithCleanup.close();
      }
    } else {
      while (javaIterator.hasNext()) {
        forEach(javaIterator.next());
        
        if (stopIterating) {
          break;
        }
      }
    }
  }
  
  abstract void forEach(T paramT) throws SQLException;
  
  public final boolean fullIteration() {
    return !stopIterating;
  }
}
