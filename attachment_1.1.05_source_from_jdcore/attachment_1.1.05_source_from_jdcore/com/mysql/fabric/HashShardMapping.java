package com.mysql.fabric;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

















public class HashShardMapping
  extends ShardMapping
{
  private static final MessageDigest md5Hasher;
  
  private static class ReverseShardIndexSorter
    implements Comparator<ShardIndex>
  {
    private ReverseShardIndexSorter() {}
    
    public int compare(ShardIndex i1, ShardIndex i2)
    {
      return i2.getBound().compareTo(i1.getBound());
    }
    

    public static final ReverseShardIndexSorter instance = new ReverseShardIndexSorter();
  }
  
  static
  {
    try
    {
      md5Hasher = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }
  
  public HashShardMapping(int mappingId, ShardingType shardingType, String globalGroupName, Set<ShardTable> shardTables, Set<ShardIndex> shardIndices) {
    super(mappingId, shardingType, globalGroupName, shardTables, new TreeSet(ReverseShardIndexSorter.instance));
    this.shardIndices.addAll(shardIndices);
  }
  
  protected ShardIndex getShardIndexForKey(String stringKey)
  {
    String hashedKey;
    synchronized (md5Hasher) {
      hashedKey = new BigInteger(1, md5Hasher.digest(stringKey.getBytes())).toString(16).toUpperCase();
    }
    

    for (int i = 0; i < 32 - hashedKey.length(); i++) {
      hashedKey = "0" + hashedKey;
    }
    
    for (ShardIndex i : shardIndices) {
      if (i.getBound().compareTo(hashedKey) <= 0) {
        return i;
      }
    }
    


    return (ShardIndex)shardIndices.iterator().next();
  }
}
