package com.mysql.fabric;


























public enum ServerRole
{
  FAULTY,  SPARE,  SECONDARY,  PRIMARY,  CONFIGURING;
  
  private ServerRole() {}
  public static ServerRole getFromConstant(Integer constant) { return values()[constant.intValue()]; }
}
