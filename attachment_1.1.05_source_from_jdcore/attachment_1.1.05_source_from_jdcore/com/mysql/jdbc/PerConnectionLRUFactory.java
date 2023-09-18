package com.mysql.jdbc;

import com.mysql.jdbc.util.LRUCache;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;























public class PerConnectionLRUFactory
  implements CacheAdapterFactory<String, PreparedStatement.ParseInfo>
{
  public PerConnectionLRUFactory() {}
  
  public CacheAdapter<String, PreparedStatement.ParseInfo> getInstance(Connection forConnection, String url, int cacheMaxSize, int maxKeySize, Properties connectionProperties)
    throws SQLException
  {
    return new PerConnectionLRU(forConnection, cacheMaxSize, maxKeySize);
  }
  
  class PerConnectionLRU implements CacheAdapter<String, PreparedStatement.ParseInfo> {
    private final int cacheSqlLimit;
    private final LRUCache<String, PreparedStatement.ParseInfo> cache;
    private final Connection conn;
    
    protected PerConnectionLRU(Connection forConnection, int cacheMaxSize, int maxKeySize) {
      int cacheSize = cacheMaxSize;
      cacheSqlLimit = maxKeySize;
      cache = new LRUCache(cacheSize);
      conn = forConnection;
    }
    
    public PreparedStatement.ParseInfo get(String key) {
      if ((key == null) || (key.length() > cacheSqlLimit)) {
        return null;
      }
      
      synchronized (conn.getConnectionMutex()) {
        return (PreparedStatement.ParseInfo)cache.get(key);
      }
    }
    
    public void put(String key, PreparedStatement.ParseInfo value) {
      if ((key == null) || (key.length() > cacheSqlLimit)) {
        return;
      }
      
      synchronized (conn.getConnectionMutex()) {
        cache.put(key, value);
      }
    }
    
    public void invalidate(String key) {
      synchronized (conn.getConnectionMutex()) {
        cache.remove(key);
      }
    }
    
    public void invalidateAll(Set<String> keys) {
      synchronized (conn.getConnectionMutex()) {
        for (String key : keys) {
          cache.remove(key);
        }
      }
    }
    
    public void invalidateAll()
    {
      synchronized (conn.getConnectionMutex()) {
        cache.clear();
      }
    }
  }
}
