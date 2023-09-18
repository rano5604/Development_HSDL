package com.mysql.fabric.proto.xmlrpc;

import com.mysql.fabric.FabricCommunicationException;
import com.mysql.fabric.FabricStateResponse;
import com.mysql.fabric.Response;
import com.mysql.fabric.Server;
import com.mysql.fabric.ServerGroup;
import com.mysql.fabric.ServerMode;
import com.mysql.fabric.ServerRole;
import com.mysql.fabric.ShardIndex;
import com.mysql.fabric.ShardMapping;
import com.mysql.fabric.ShardMappingFactory;
import com.mysql.fabric.ShardTable;
import com.mysql.fabric.ShardingType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;





























public class XmlRpcClient
{
  private static final String THREAT_REPORTER_NAME = "MySQL Connector/J";
  private static final String METHOD_DUMP_FABRIC_NODES = "dump.fabric_nodes";
  private static final String METHOD_DUMP_SERVERS = "dump.servers";
  private static final String METHOD_DUMP_SHARD_TABLES = "dump.shard_tables";
  private static final String METHOD_DUMP_SHARD_INDEX = "dump.shard_index";
  private static final String METHOD_DUMP_SHARD_MAPS = "dump.shard_maps";
  private static final String METHOD_SHARDING_LOOKUP_SERVERS = "sharding.lookup_servers";
  private static final String METHOD_SHARDING_CREATE_DEFINITION = "sharding.create_definition";
  private static final String METHOD_SHARDING_ADD_TABLE = "sharding.add_table";
  private static final String METHOD_SHARDING_ADD_SHARD = "sharding.add_shard";
  private static final String METHOD_GROUP_LOOKUP_GROUPS = "group.lookup_groups";
  private static final String METHOD_GROUP_CREATE = "group.create";
  private static final String METHOD_GROUP_ADD = "group.add";
  private static final String METHOD_GROUP_REMOVE = "group.remove";
  private static final String METHOD_GROUP_PROMOTE = "group.promote";
  private static final String METHOD_GROUP_DESTROY = "group.destroy";
  private static final String METHOD_THREAT_REPORT_ERROR = "threat.report_error";
  private static final String METHOD_THREAT_REPORT_FAILURE = "threat.report_failure";
  private static final String FIELD_MODE = "mode";
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_HOST = "host";
  private static final String FIELD_PORT = "port";
  private static final String FIELD_ADDRESS = "address";
  private static final String FIELD_GROUP_ID = "group_id";
  private static final String FIELD_SERVER_UUID = "server_uuid";
  private static final String FIELD_WEIGHT = "weight";
  private static final String FIELD_SCHEMA_NAME = "schema_name";
  private static final String FIELD_TABLE_NAME = "table_name";
  private static final String FIELD_COLUMN_NAME = "column_name";
  private static final String FIELD_LOWER_BOUND = "lower_bound";
  private static final String FIELD_SHARD_ID = "shard_id";
  private static final String FIELD_MAPPING_ID = "mapping_id";
  private static final String FIELD_GLOBAL_GROUP_ID = "global_group_id";
  private static final String FIELD_TYPE_NAME = "type_name";
  private static final String FIELD_RESULT = "result";
  private XmlRpcMethodCaller methodCaller;
  
  public XmlRpcClient(String url, String username, String password)
    throws FabricCommunicationException
  {
    methodCaller = new InternalXmlRpcMethodCaller(url);
    if ((username != null) && (!"".equals(username)) && (password != null)) {
      methodCaller = new AuthenticatedXmlRpcMethodCaller(methodCaller, url, username, password);
    }
  }
  

  private static Server unmarshallServer(Map<String, ?> serverData)
    throws FabricCommunicationException
  {
    try
    {
      int port;
      
      ServerMode mode;
      ServerRole role;
      String host;
      int port;
      if (Integer.class.equals(serverData.get("mode").getClass())) {
        ServerMode mode = ServerMode.getFromConstant((Integer)serverData.get("mode"));
        ServerRole role = ServerRole.getFromConstant((Integer)serverData.get("status"));
        String host = (String)serverData.get("host");
        port = ((Integer)serverData.get("port")).intValue();
      }
      else {
        mode = ServerMode.valueOf((String)serverData.get("mode"));
        role = ServerRole.valueOf((String)serverData.get("status"));
        String[] hostnameAndPort = ((String)serverData.get("address")).split(":");
        host = hostnameAndPort[0];
        port = Integer.valueOf(hostnameAndPort[1]).intValue();
      }
      return new Server((String)serverData.get("group_id"), (String)serverData.get("server_uuid"), host, port, mode, role, ((Double)serverData.get("weight")).doubleValue());
    }
    catch (Exception ex)
    {
      throw new FabricCommunicationException("Unable to parse server definition", ex);
    }
  }
  

  private static Set<Server> toServerSet(List<Map<String, ?>> l)
    throws FabricCommunicationException
  {
    Set<Server> servers = new HashSet();
    for (Map<String, ?> serverData : l) {
      servers.add(unmarshallServer(serverData));
    }
    return servers;
  }
  




  private Response errorSafeCallMethod(String methodName, Object[] args)
    throws FabricCommunicationException
  {
    List<?> responseData = methodCaller.call(methodName, args);
    Response response = new Response(responseData);
    if (response.getErrorMessage() != null) {
      throw new FabricCommunicationException("Call failed to method `" + methodName + "':\n" + response.getErrorMessage());
    }
    return response;
  }
  

  public Set<String> getFabricNames()
    throws FabricCommunicationException
  {
    Response resp = errorSafeCallMethod("dump.fabric_nodes", new Object[0]);
    Set<String> names = new HashSet();
    for (Map<String, ?> node : resp.getResultSet()) {
      names.add(node.get("host") + ":" + node.get("port"));
    }
    return names;
  }
  

  public Set<String> getGroupNames()
    throws FabricCommunicationException
  {
    Set<String> groupNames = new HashSet();
    for (Map<String, ?> row : errorSafeCallMethod("group.lookup_groups", null).getResultSet()) {
      groupNames.add((String)row.get("group_id"));
    }
    return groupNames;
  }
  
  public ServerGroup getServerGroup(String groupName) throws FabricCommunicationException {
    Set<ServerGroup> groups = (Set)getServerGroups(groupName).getData();
    if (groups.size() == 1) {
      return (ServerGroup)groups.iterator().next();
    }
    return null;
  }
  
  public Set<Server> getServersForKey(String tableName, int key) throws FabricCommunicationException {
    Response r = errorSafeCallMethod("sharding.lookup_servers", new Object[] { tableName, Integer.valueOf(key) });
    return toServerSet(r.getResultSet());
  }
  

  public FabricStateResponse<Set<ServerGroup>> getServerGroups(String groupPattern)
    throws FabricCommunicationException
  {
    int version = 0;
    Response response = errorSafeCallMethod("dump.servers", new Object[] { Integer.valueOf(version), groupPattern });
    
    Map<String, Set<Server>> serversByGroupName = new HashMap();
    for (Map<String, ?> server : response.getResultSet()) {
      Server s = unmarshallServer(server);
      if (serversByGroupName.get(s.getGroupName()) == null) {
        serversByGroupName.put(s.getGroupName(), new HashSet());
      }
      ((Set)serversByGroupName.get(s.getGroupName())).add(s);
    }
    
    Set<ServerGroup> serverGroups = new HashSet();
    for (Map.Entry<String, Set<Server>> entry : serversByGroupName.entrySet()) {
      ServerGroup g = new ServerGroup((String)entry.getKey(), (Set)entry.getValue());
      serverGroups.add(g);
    }
    return new FabricStateResponse(serverGroups, response.getTtl());
  }
  
  public FabricStateResponse<Set<ServerGroup>> getServerGroups() throws FabricCommunicationException {
    return getServerGroups("");
  }
  
  private FabricStateResponse<Set<ShardTable>> getShardTables(int shardMappingId) throws FabricCommunicationException {
    int version = 0;
    Object[] args = { Integer.valueOf(version), String.valueOf(shardMappingId) };
    Response tablesResponse = errorSafeCallMethod("dump.shard_tables", args);
    Set<ShardTable> tables = new HashSet();
    
    for (Map<String, ?> rawTable : tablesResponse.getResultSet()) {
      String database = (String)rawTable.get("schema_name");
      String table = (String)rawTable.get("table_name");
      String column = (String)rawTable.get("column_name");
      ShardTable st = new ShardTable(database, table, column);
      tables.add(st);
    }
    return new FabricStateResponse(tables, tablesResponse.getTtl());
  }
  
  private FabricStateResponse<Set<ShardIndex>> getShardIndices(int shardMappingId) throws FabricCommunicationException {
    int version = 0;
    Object[] args = { Integer.valueOf(version), String.valueOf(shardMappingId) };
    Response indexResponse = errorSafeCallMethod("dump.shard_index", args);
    Set<ShardIndex> indices = new HashSet();
    

    for (Map<String, ?> rawIndexEntry : indexResponse.getResultSet()) {
      String bound = (String)rawIndexEntry.get("lower_bound");
      int shardId = ((Integer)rawIndexEntry.get("shard_id")).intValue();
      String groupName = (String)rawIndexEntry.get("group_id");
      ShardIndex si = new ShardIndex(bound, Integer.valueOf(shardId), groupName);
      indices.add(si);
    }
    return new FabricStateResponse(indices, indexResponse.getTtl());
  }
  





  public FabricStateResponse<Set<ShardMapping>> getShardMappings(String shardMappingIdPattern)
    throws FabricCommunicationException
  {
    int version = 0;
    Object[] args = { Integer.valueOf(version), shardMappingIdPattern };
    Response mapsResponse = errorSafeCallMethod("dump.shard_maps", args);
    
    long minExpireTimeMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(mapsResponse.getTtl());
    int baseTtl = mapsResponse.getTtl();
    

    Set<ShardMapping> mappings = new HashSet();
    for (Map<String, ?> rawMapping : mapsResponse.getResultSet()) {
      int mappingId = ((Integer)rawMapping.get("mapping_id")).intValue();
      ShardingType shardingType = ShardingType.valueOf((String)rawMapping.get("type_name"));
      String globalGroupName = (String)rawMapping.get("global_group_id");
      
      FabricStateResponse<Set<ShardTable>> tables = getShardTables(mappingId);
      FabricStateResponse<Set<ShardIndex>> indices = getShardIndices(mappingId);
      
      if (tables.getExpireTimeMillis() < minExpireTimeMillis) {
        minExpireTimeMillis = tables.getExpireTimeMillis();
      }
      if (indices.getExpireTimeMillis() < minExpireTimeMillis) {
        minExpireTimeMillis = indices.getExpireTimeMillis();
      }
      
      ShardMapping m = new ShardMappingFactory().createShardMapping(mappingId, shardingType, globalGroupName, (Set)tables.getData(), (Set)indices.getData());
      mappings.add(m);
    }
    
    return new FabricStateResponse(mappings, baseTtl, minExpireTimeMillis);
  }
  
  public FabricStateResponse<Set<ShardMapping>> getShardMappings() throws FabricCommunicationException {
    return getShardMappings("");
  }
  

  public void createGroup(String groupName)
    throws FabricCommunicationException
  {
    errorSafeCallMethod("group.create", new Object[] { groupName });
  }
  

  public void destroyGroup(String groupName)
    throws FabricCommunicationException
  {
    errorSafeCallMethod("group.destroy", new Object[] { groupName });
  }
  

  public void createServerInGroup(String groupName, String hostname, int port)
    throws FabricCommunicationException
  {
    errorSafeCallMethod("group.add", new Object[] { groupName, hostname + ":" + port });
  }
  







  public int createShardMapping(ShardingType type, String globalGroupName)
    throws FabricCommunicationException
  {
    Response r = errorSafeCallMethod("sharding.create_definition", new Object[] { type.toString(), globalGroupName });
    return ((Integer)((Map)r.getResultSet().get(0)).get("result")).intValue();
  }
  
  public void createShardTable(int shardMappingId, String database, String table, String column) throws FabricCommunicationException {
    errorSafeCallMethod("sharding.add_table", new Object[] { Integer.valueOf(shardMappingId), database + "." + table, column });
  }
  
  public void createShardIndex(int shardMappingId, String groupNameLowerBoundList) throws FabricCommunicationException {
    String status = "ENABLED";
    errorSafeCallMethod("sharding.add_shard", new Object[] { Integer.valueOf(shardMappingId), groupNameLowerBoundList, status });
  }
  
  public void addServerToGroup(String groupName, String hostname, int port) throws FabricCommunicationException {
    errorSafeCallMethod("group.add", new Object[] { groupName, hostname + ":" + port });
  }
  
  public void removeServerFromGroup(String groupName, String hostname, int port) throws FabricCommunicationException {
    errorSafeCallMethod("group.remove", new Object[] { groupName, hostname + ":" + port });
  }
  
  public void promoteServerInGroup(String groupName, String hostname, int port) throws FabricCommunicationException {
    ServerGroup serverGroup = getServerGroup(groupName);
    for (Server s : serverGroup.getServers()) {
      if ((s.getHostname().equals(hostname)) && (s.getPort() == port)) {
        errorSafeCallMethod("group.promote", new Object[] { groupName, s.getUuid() });
        break;
      }
    }
  }
  
  public void reportServerError(Server server, String errorDescription, boolean forceFaulty) throws FabricCommunicationException {
    String reporter = "MySQL Connector/J";
    String command = "threat.report_error";
    if (forceFaulty) {
      command = "threat.report_failure";
    }
    errorSafeCallMethod(command, new Object[] { server.getUuid(), reporter, errorDescription });
  }
}
