package org.threadly.concurrent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * TaskDistributor is designed to take a multi threaded pool
 * and add tasks with a given key such that those tasks will
 * be run single threaded for any given key.  The thread which
 * runs those tasks may be different each time, but no two tasks
 * with the same key will ever be run in parallel.
 * 
 * Because of that, it is recommended that the executor provided 
 * has as many possible threads as possible keys that could be 
 * provided to be run in parallel.  If this class is starved for 
 * threads some keys may continue to process new tasks, while
 * other keys could be starved.
 * 
 * @author jent - Mike Jensen
 */
public class TaskDistributor {
  private final Executor executor;
  private final Object agentLock;
  private final Map<Object, TaskQueueWorker> taskWorkers;
  
  /**
   * Constructor which creates executor based off provided values
   * 
   * @param expectedParallism Expected number of keys that will be used in parallel
   * @param maxThreadCount Max thread count (and thus maximum number of keys which can be processed in parallel)
   */
  public TaskDistributor(int expectedParallism, int maxThreadCount) {
    this(new PriorityScheduledExecutor(expectedParallism, 
                                       maxThreadCount, 
                                       1000 * 10, 
                                       TaskPriority.High, 
                                       PriorityScheduledExecutor.DEFAULT_LOW_PRIORITY_MAX_WAIT));
  }
  
  /**
   * @param executor A multi-threaded executor to distribute tasks to.  
   *                 Ideally has as many possible threads as keys that 
   *                 will be used in parallel. 
   */
  public TaskDistributor(Executor executor) {
    this(executor, new Object());
  }
  
  /**
   * used for testing, so that agentLock can be held and prevent execution
   */
  protected TaskDistributor(Executor executor, Object agentLock) {
    if (executor == null) {
      throw new IllegalArgumentException("executor can not be null");
    }
    
    this.executor = executor;
    this.agentLock = agentLock;
    this.taskWorkers = new HashMap<Object, TaskQueueWorker>();
  }
  
  /**
   * @return executor tasks are being distributed to
   */
  public Executor getExecutor() {
    return executor;
  }
  
  /**
   * Provide a task to be run with a given thread key.
   * 
   * @param threadKey object key where hashCode will be used to determine execution thread
   * @param task Task to be executed.
   */
  public void addTask(Object threadKey, Runnable task) {
    synchronized (agentLock) {
      TaskQueueWorker worker = taskWorkers.get(threadKey);
      if (worker == null) {
        System.out.println("adding: " + threadKey.hashCode() + " - new woker");
        worker = new TaskQueueWorker(threadKey);
        worker.add(task);
        executor.execute(worker);
      } else {
        worker.add(task);
      }
    }
  }
  
  private class TaskQueueWorker implements Runnable {
    private final Object mapKey;
    private LinkedList<Runnable> queue;
    
    private TaskQueueWorker(Object mapKey) {
      this.mapKey = mapKey;
      this.queue = new LinkedList<Runnable>();
    }
    
    public void add(Runnable task) {
      synchronized (agentLock) {
        queue.addLast(task);
      }
    }
    
    private List<Runnable> next() {
      synchronized (agentLock) {
        List<Runnable> result = null;
        if (! queue.isEmpty()) {
          result = queue;
          queue = new LinkedList<Runnable>();
        }
        
        return result;
      }
    }
    
    @Override
    public void run() {
      while (true) {
        List<Runnable> nextList;
        synchronized (agentLock) {
          System.out.println("Starting");
          nextList = next();
          
          if (nextList == null) {
            taskWorkers.remove(mapKey);
            break;  // stop consuming tasks
          }
        }
        
        Iterator<Runnable> it = nextList.iterator();
        while (it.hasNext()) {
          it.next().run();
        }
      }
    }
  }
}
