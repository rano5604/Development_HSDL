package com.mysql.fabric.xmlrpc.base;









public class Array
{
  protected Data data;
  








  public Array() {}
  







  public Data getData()
  {
    return data;
  }
  


  public void setData(Data value)
  {
    data = value;
  }
  
  public void addValue(Value v) {
    if (data == null) {
      data = new Data();
    }
    data.addValue(v);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder("<array>");
    sb.append(data.toString());
    sb.append("</array>");
    return sb.toString();
  }
}
