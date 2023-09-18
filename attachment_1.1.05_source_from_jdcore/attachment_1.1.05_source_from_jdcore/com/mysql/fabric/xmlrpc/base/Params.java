package com.mysql.fabric.xmlrpc.base;

import java.util.ArrayList;
import java.util.List;





















public class Params
{
  protected List<Param> param;
  
  public Params() {}
  
  public List<Param> getParam()
  {
    if (param == null) {
      param = new ArrayList();
    }
    return param;
  }
  
  public void addParam(Param p) {
    getParam().add(p);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    if (param != null) {
      sb.append("<params>");
      for (int i = 0; i < param.size(); i++) {
        sb.append(((Param)param.get(i)).toString());
      }
      sb.append("</params>");
    }
    return sb.toString();
  }
}
