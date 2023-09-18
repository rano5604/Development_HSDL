package com.mysql.fabric.xmlrpc.base;









public class Param
{
  protected Value value;
  








  public Param() {}
  







  public Param(Value value)
  {
    this.value = value;
  }
  


  public Value getValue()
  {
    return value;
  }
  


  public void setValue(Value value)
  {
    this.value = value;
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("<param>");
    sb.append(value.toString());
    sb.append("</param>");
    return sb.toString();
  }
}
