package com.mysql.jdbc;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


























public class AbandonedConnectionCleanupThread
  implements Runnable
{
  private static final ExecutorService cleanupThreadExcecutorService;
  static Thread threadRef = null;
  
  static {
    cleanupThreadExcecutorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Abandoned connection cleanup thread");
        t.setDaemon(true);
        



        t.setContextClassLoader(AbandonedConnectionCleanupThread.class.getClassLoader());
        return AbandonedConnectionCleanupThread.threadRef = t;
      }
    });
    cleanupThreadExcecutorService.execute(new AbandonedConnectionCleanupThread());
  }
  
  public void run()
  {
    try
    {
      for (;;)
      {
        checkContextClassLoaders();
        Reference<? extends ConnectionImpl> ref = NonRegisteringDriver.refQueue.remove(5000L);
        if (ref != null) {
          try {
            ((NonRegisteringDriver.ConnectionPhantomReference)ref).cleanup();
          } finally {
            NonRegisteringDriver.connectionPhantomRefs.remove(ref);
          }
        }
      }
    } catch (InterruptedException e) {
      threadRef = null;
      return;
    }
    catch (Exception ex) {}
  }
  






  private void checkContextClassLoaders()
  {
    try
    {
      threadRef.getContextClassLoader().getResource("");
    }
    catch (Throwable e) {
      uncheckedShutdown();
    }
  }
  




  private static boolean consistentClassLoaders()
  {
    ClassLoader callerCtxClassLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader threadCtxClassLoader = threadRef.getContextClassLoader();
    return (callerCtxClassLoader != null) && (threadCtxClassLoader != null) && (callerCtxClassLoader == threadCtxClassLoader);
  }
  



  public static void checkedShutdown()
  {
    shutdown(true);
  }
  


  public static void uncheckedShutdown()
  {
    shutdown(false);
  }
  





  private static void shutdown(boolean checked)
  {
    if ((checked) && (!consistentClassLoaders()))
    {

      return;
    }
    cleanupThreadExcecutorService.shutdownNow();
  }
  
  private AbandonedConnectionCleanupThread() {}
  
  @Deprecated
  public static void shutdown() {}
}
