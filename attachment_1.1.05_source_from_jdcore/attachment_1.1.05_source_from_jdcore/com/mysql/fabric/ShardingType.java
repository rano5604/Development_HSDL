package com.mysql.fabric;

public enum ShardingType
{
  LIST,  RANGE,  HASH;
  
  private ShardingType() {}
}
