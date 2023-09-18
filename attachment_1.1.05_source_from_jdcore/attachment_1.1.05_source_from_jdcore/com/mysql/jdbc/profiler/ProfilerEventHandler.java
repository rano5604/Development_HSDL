package com.mysql.jdbc.profiler;

import com.mysql.jdbc.Extension;

public abstract interface ProfilerEventHandler
  extends Extension
{
  public abstract void consumeEvent(ProfilerEvent paramProfilerEvent);
}
