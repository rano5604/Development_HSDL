package com.mysql.fabric.xmlrpc.base;

import java.util.ArrayList;
import java.util.List;





















public class Data
{
  protected List<Value> value;
  
  public Data() {}
  
  public List<Value> getValue()
  {
    if (value == null) {
      value = new ArrayList();
    }
    return value;
  }
  
  public void addValue(Value v) {
    getValue().add(v);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    if (value != null) {
      sb.append("<data>");
      for (int i = 0; i < value.size(); i++) {
        sb.append(((Value)value.get(i)).toString());
      }
      sb.append("</data>");
    }
    return sb.toString();
  }
}
