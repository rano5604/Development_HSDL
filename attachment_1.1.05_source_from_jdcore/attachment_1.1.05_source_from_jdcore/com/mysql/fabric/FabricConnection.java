package com.mysql.fabric;

import com.mysql.fabric.proto.xmlrpc.XmlRpcClient;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

























public class FabricConnection
{
  private XmlRpcClient client;
  private Map<String, ShardMapping> shardMappingsByTableName = new HashMap();
  private Map<String, ServerGroup> serverGroupsByName = new HashMap();
  private long shardMappingsExpiration;
  private int shardMappingsTtl;
  private long serverGroupsExpiration;
  private int serverGroupsTtl;
  
  public FabricConnection(String url, String username, String password) throws FabricCommunicationException {
    client = new XmlRpcClient(url, username, password);
    refreshState();
  }
  




  public FabricConnection(Set<String> urls, String username, String password)
    throws FabricCommunicationException
  {
    throw new UnsupportedOperationException("Multiple connections not supported.");
  }
  
  public String getInstanceUuid() {
    return null;
  }
  
  public int getVersion() {
    return 0;
  }
  

  public int refreshState()
    throws FabricCommunicationException
  {
    FabricStateResponse<Set<ServerGroup>> serverGroups = client.getServerGroups();
    FabricStateResponse<Set<ShardMapping>> shardMappings = client.getShardMappings();
    
    serverGroupsExpiration = serverGroups.getExpireTimeMillis();
    serverGroupsTtl = serverGroups.getTtl();
    for (ServerGroup g : (Set)serverGroups.getData()) {
      serverGroupsByName.put(g.getName(), g);
    }
    
    shardMappingsExpiration = shardMappings.getExpireTimeMillis();
    shardMappingsTtl = shardMappings.getTtl();
    for (Iterator i$ = ((Set)shardMappings.getData()).iterator(); i$.hasNext();) { m = (ShardMapping)i$.next();
      
      for (ShardTable t : m.getShardTables()) {
        shardMappingsByTableName.put(t.getDatabase() + "." + t.getTable(), m);
      }
    }
    ShardMapping m;
    return 0;
  }
  
  public int refreshStatePassive() {
    try {
      return refreshState();
    }
    catch (FabricCommunicationException e) {
      serverGroupsExpiration = (System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(serverGroupsTtl));
      shardMappingsExpiration = (System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(shardMappingsTtl));
    }
    
    return 0;
  }
  
  public ServerGroup getServerGroup(String serverGroupName) {
    if (isStateExpired()) {
      refreshStatePassive();
    }
    return (ServerGroup)serverGroupsByName.get(serverGroupName);
  }
  
  public ShardMapping getShardMapping(String database, String table) {
    if (isStateExpired()) {
      refreshStatePassive();
    }
    return (ShardMapping)shardMappingsByTableName.get(database + "." + table);
  }
  
  public boolean isStateExpired() {
    return (System.currentTimeMillis() > shardMappingsExpiration) || (System.currentTimeMillis() > serverGroupsExpiration);
  }
  
  public Set<String> getFabricHosts() {
    return null;
  }
  
  public XmlRpcClient getClient() {
    return client;
  }
}
