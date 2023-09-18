package com.mysql.jdbc;

abstract interface WriterWatcher
{
  public abstract void writerClosed(WatchableWriter paramWatchableWriter);
}
