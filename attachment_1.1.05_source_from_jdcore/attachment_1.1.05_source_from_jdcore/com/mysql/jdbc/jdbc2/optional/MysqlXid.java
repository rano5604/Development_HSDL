package com.mysql.jdbc.jdbc2.optional;

import javax.transaction.xa.Xid;


























public class MysqlXid
  implements Xid
{
  int hash = 0;
  
  byte[] myBqual;
  
  int myFormatId;
  byte[] myGtrid;
  
  public MysqlXid(byte[] gtrid, byte[] bqual, int formatId)
  {
    myGtrid = gtrid;
    myBqual = bqual;
    myFormatId = formatId;
  }
  

  public boolean equals(Object another)
  {
    if ((another instanceof Xid)) {
      Xid anotherAsXid = (Xid)another;
      
      if (myFormatId != anotherAsXid.getFormatId()) {
        return false;
      }
      
      byte[] otherBqual = anotherAsXid.getBranchQualifier();
      byte[] otherGtrid = anotherAsXid.getGlobalTransactionId();
      
      if ((otherGtrid != null) && (otherGtrid.length == myGtrid.length)) {
        int length = otherGtrid.length;
        
        for (int i = 0; i < length; i++) {
          if (otherGtrid[i] != myGtrid[i]) {
            return false;
          }
        }
        
        if ((otherBqual != null) && (otherBqual.length == myBqual.length)) {
          length = otherBqual.length;
          
          for (int i = 0; i < length; i++) {
            if (otherBqual[i] != myBqual[i]) {
              return false;
            }
          }
        } else {
          return false;
        }
        
        return true;
      }
    }
    
    return false;
  }
  
  public byte[] getBranchQualifier() {
    return myBqual;
  }
  
  public int getFormatId() {
    return myFormatId;
  }
  
  public byte[] getGlobalTransactionId() {
    return myGtrid;
  }
  
  public synchronized int hashCode()
  {
    if (hash == 0) {
      for (int i = 0; i < myGtrid.length; i++) {
        hash = (33 * hash + myGtrid[i]);
      }
    }
    
    return hash;
  }
}
