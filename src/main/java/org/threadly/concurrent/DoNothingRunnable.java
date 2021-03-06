package org.threadly.concurrent;

/**
 * {@link Runnable} implementation which does no action.
 * 
 * @since 4.1.0
 */
public class DoNothingRunnable implements Runnable {
  private static final DoNothingRunnable DEFAULT_INSTANCE = new DoNothingRunnable();
  
  /**
   * Constructs a new {@link DoNothingRunnable}.
   * 
   * @deprecated Please use {@link DoNothingRunnable#instance()} instead
   */
  @Deprecated
  public DoNothingRunnable() {
    // nothing here by default
  }

  /**
   * Call to get a default instance of the {@link DoNothingRunnable}.  Because there is no saved 
   * or shared state, the same instance can be reused as much as desired.
   * 
   * @return a static instance of DoNothingRunnable
   */
  public static DoNothingRunnable instance() {
    return DEFAULT_INSTANCE;
  }
  
  @Override
  public void run() {
    // as the name suggests, do nothing
  }
}
