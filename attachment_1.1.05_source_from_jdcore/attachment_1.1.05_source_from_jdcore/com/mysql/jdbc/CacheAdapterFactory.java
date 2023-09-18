package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Properties;

public abstract interface CacheAdapterFactory<K, V>
{
  public abstract CacheAdapter<K, V> getInstance(Connection paramConnection, String paramString, int paramInt1, int paramInt2, Properties paramProperties)
    throws SQLException;
}
