package com.mysql.fabric.xmlrpc.base;

import java.util.ArrayList;
import java.util.List;





















public class Struct
{
  protected List<Member> member;
  
  public Struct() {}
  
  public List<Member> getMember()
  {
    if (member == null) {
      member = new ArrayList();
    }
    return member;
  }
  
  public void addMember(Member m) {
    getMember().add(m);
  }
  
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    if (member != null) {
      sb.append("<struct>");
      for (int i = 0; i < member.size(); i++) {
        sb.append(((Member)member.get(i)).toString());
      }
      sb.append("</struct>");
    }
    return sb.toString();
  }
}
