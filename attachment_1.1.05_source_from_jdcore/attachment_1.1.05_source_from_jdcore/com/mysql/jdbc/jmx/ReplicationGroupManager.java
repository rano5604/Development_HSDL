package com.mysql.jdbc.jmx;

import com.mysql.jdbc.ReplicationConnectionGroup;
import com.mysql.jdbc.ReplicationConnectionGroupManager;
import com.mysql.jdbc.SQLError;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.Collection;
import javax.management.MBeanServer;
import javax.management.ObjectName;





















public class ReplicationGroupManager
  implements ReplicationGroupManagerMBean
{
  public ReplicationGroupManager() {}
  
  private boolean isJmxRegistered = false;
  
  public synchronized void registerJmx() throws SQLException {
    if (isJmxRegistered) {
      return;
    }
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    try {
      ObjectName name = new ObjectName("com.mysql.jdbc.jmx:type=ReplicationGroupManager");
      mbs.registerMBean(this, name);
      isJmxRegistered = true;
    } catch (Exception e) {
      throw SQLError.createSQLException("Unable to register replication host management bean with JMX", null, e, null);
    }
  }
  
  public void addSlaveHost(String groupFilter, String host) throws SQLException
  {
    ReplicationConnectionGroupManager.addSlaveHost(groupFilter, host);
  }
  
  public void removeSlaveHost(String groupFilter, String host) throws SQLException {
    ReplicationConnectionGroupManager.removeSlaveHost(groupFilter, host);
  }
  
  public void promoteSlaveToMaster(String groupFilter, String host) throws SQLException {
    ReplicationConnectionGroupManager.promoteSlaveToMaster(groupFilter, host);
  }
  
  public void removeMasterHost(String groupFilter, String host) throws SQLException
  {
    ReplicationConnectionGroupManager.removeMasterHost(groupFilter, host);
  }
  
  public String getMasterHostsList(String group)
  {
    StringBuilder sb = new StringBuilder("");
    boolean found = false;
    for (String host : ReplicationConnectionGroupManager.getMasterHosts(group)) {
      if (found) {
        sb.append(",");
      }
      found = true;
      sb.append(host);
    }
    return sb.toString();
  }
  
  public String getSlaveHostsList(String group) {
    StringBuilder sb = new StringBuilder("");
    boolean found = false;
    for (String host : ReplicationConnectionGroupManager.getSlaveHosts(group)) {
      if (found) {
        sb.append(",");
      }
      found = true;
      sb.append(host);
    }
    return sb.toString();
  }
  
  public String getRegisteredConnectionGroups()
  {
    StringBuilder sb = new StringBuilder("");
    boolean found = false;
    for (ReplicationConnectionGroup group : ReplicationConnectionGroupManager.getGroupsMatching(null)) {
      if (found) {
        sb.append(",");
      }
      found = true;
      sb.append(group.getGroupName());
    }
    return sb.toString();
  }
  
  public int getActiveMasterHostCount(String group) {
    return ReplicationConnectionGroupManager.getMasterHosts(group).size();
  }
  
  public int getActiveSlaveHostCount(String group) {
    return ReplicationConnectionGroupManager.getSlaveHosts(group).size();
  }
  
  public int getSlavePromotionCount(String group) {
    return ReplicationConnectionGroupManager.getNumberOfMasterPromotion(group);
  }
  
  public long getTotalLogicalConnectionCount(String group) {
    return ReplicationConnectionGroupManager.getTotalConnectionCount(group);
  }
  
  public long getActiveLogicalConnectionCount(String group) {
    return ReplicationConnectionGroupManager.getActiveConnectionCount(group);
  }
}
