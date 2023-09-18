package com.mysql.fabric;

import java.util.Set;
























public class ShardMappingFactory
{
  public ShardMappingFactory() {}
  
  public ShardMapping createShardMapping(int mappingId, ShardingType shardingType, String globalGroupName, Set<ShardTable> shardTables, Set<ShardIndex> shardIndices)
  {
    ShardMapping sm = null;
    switch (1.$SwitchMap$com$mysql$fabric$ShardingType[shardingType.ordinal()]) {
    case 1: 
      sm = new RangeShardMapping(mappingId, shardingType, globalGroupName, shardTables, shardIndices);
      break;
    case 2: 
      sm = new HashShardMapping(mappingId, shardingType, globalGroupName, shardTables, shardIndices);
      break;
    default: 
      throw new IllegalArgumentException("Invalid ShardingType");
    }
    return sm;
  }
}
