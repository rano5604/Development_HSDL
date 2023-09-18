package com.mysql.fabric.xmlrpc.base;









public class Fault
{
  protected Value value;
  








  public Fault() {}
  







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
    StringBuilder sb = new StringBuilder();
    if (value != null) {
      sb.append("<fault>");
      sb.append(value.toString());
      sb.append("</fault>");
    }
    return sb.toString();
  }
}
