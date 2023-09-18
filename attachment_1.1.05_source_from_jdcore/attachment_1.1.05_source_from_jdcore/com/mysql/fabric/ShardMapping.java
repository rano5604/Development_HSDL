package com.mysql.fabric;

import java.util.Collections;
import java.util.Set;

























public abstract class ShardMapping
{
  private int mappingId;
  private ShardingType shardingType;
  private String globalGroupName;
  protected Set<ShardTable> shardTables;
  protected Set<ShardIndex> shardIndices;
  
  public ShardMapping(int mappingId, ShardingType shardingType, String globalGroupName, Set<ShardTable> shardTables, Set<ShardIndex> shardIndices)
  {
    this.mappingId = mappingId;
    this.shardingType = shardingType;
    this.globalGroupName = globalGroupName;
    this.shardTables = shardTables;
    this.shardIndices = shardIndices;
  }
  


  public String getGroupNameForKey(String key)
  {
    return getShardIndexForKey(key).getGroupName();
  }
  



  protected abstract ShardIndex getShardIndexForKey(String paramString);
  


  public int getMappingId()
  {
    return mappingId;
  }
  


  public ShardingType getShardingType()
  {
    return shardingType;
  }
  


  public String getGlobalGroupName()
  {
    return globalGroupName;
  }
  


  public Set<ShardTable> getShardTables()
  {
    return Collections.unmodifiableSet(shardTables);
  }
  


  public Set<ShardIndex> getShardIndices()
  {
    return Collections.unmodifiableSet(shardIndices);
  }
}
