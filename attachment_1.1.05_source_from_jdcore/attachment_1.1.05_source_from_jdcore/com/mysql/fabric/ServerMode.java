package com.mysql.fabric;



























public enum ServerMode
{
  OFFLINE,  READ_ONLY,  WRITE_ONLY,  READ_WRITE;
  
  private ServerMode() {}
  public static ServerMode getFromConstant(Integer constant) { return values()[constant.intValue()]; }
}
