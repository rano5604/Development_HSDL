package com.mysql.fabric.xmlrpc.exceptions;

import com.mysql.fabric.xmlrpc.base.Fault;
import com.mysql.fabric.xmlrpc.base.Member;
import com.mysql.fabric.xmlrpc.base.Struct;
import com.mysql.fabric.xmlrpc.base.Value;
import java.sql.SQLException;
import java.util.List;






















public class MySQLFabricException
  extends SQLException
{
  static final long serialVersionUID = -8776763137552613517L;
  
  public MySQLFabricException() {}
  
  public MySQLFabricException(Fault fault)
  {
    super((String)((Member)((Struct)fault.getValue().getValue()).getMember().get(1)).getValue().getValue(), "", ((Integer)((Member)((Struct)fault.getValue().getValue()).getMember().get(0)).getValue().getValue()).intValue());
  }
  
  public MySQLFabricException(String reason, String SQLState, int vendorCode)
  {
    super(reason, SQLState, vendorCode);
  }
  
  public MySQLFabricException(String reason, String SQLState) {
    super(reason, SQLState);
  }
  
  public MySQLFabricException(String reason) {
    super(reason);
  }
}
