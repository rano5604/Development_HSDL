package com.mysql.jdbc;

abstract interface OutputStreamWatcher
{
  public abstract void streamClosed(WatchableOutputStream paramWatchableOutputStream);
}
