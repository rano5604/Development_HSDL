package com.mysql.fabric.xmlrpc.base;







public class Member
{
  protected String name;
  





  protected Value value;
  






  public Member() {}
  





  public Member(String name, Value value)
  {
    setName(name);
    setValue(value);
  }
  


  public String getName()
  {
    return name;
  }
  


  public void setName(String value)
  {
    name = value;
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
    StringBuilder sb = new StringBuilder();
    sb.append("<member>");
    sb.append("<name>" + name + "</name>");
    sb.append(value.toString());
    sb.append("</member>");
    return sb.toString();
  }
}
