package com.mysql.jdbc;

import java.io.CharArrayWriter;


























class WatchableWriter
  extends CharArrayWriter
{
  private WriterWatcher watcher;
  
  WatchableWriter() {}
  
  public void close()
  {
    super.close();
    

    if (watcher != null) {
      watcher.writerClosed(this);
    }
  }
  


  public void setWatcher(WriterWatcher watcher)
  {
    this.watcher = watcher;
  }
}
