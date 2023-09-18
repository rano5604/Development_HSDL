package com.mysql.fabric;

import java.util.concurrent.TimeUnit;






















public class FabricStateResponse<T>
{
  private T data;
  private int secsTtl;
  private long expireTimeMillis;
  
  public FabricStateResponse(T data, int secsTtl)
  {
    this.data = data;
    this.secsTtl = secsTtl;
    expireTimeMillis = (System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(secsTtl));
  }
  
  public FabricStateResponse(T data, int secsTtl, long presetExpireTimeMillis) {
    this.data = data;
    this.secsTtl = secsTtl;
    expireTimeMillis = presetExpireTimeMillis;
  }
  
  public T getData() {
    return data;
  }
  
  public int getTtl() {
    return secsTtl;
  }
  


  public long getExpireTimeMillis()
  {
    return expireTimeMillis;
  }
}
