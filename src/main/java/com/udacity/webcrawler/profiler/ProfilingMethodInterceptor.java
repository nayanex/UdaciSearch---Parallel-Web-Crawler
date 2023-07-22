package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor<T> implements InvocationHandler {

  private final T target;
  private final Clock clock;
  private final ProfilingState profilingState;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  public ProfilingMethodInterceptor(T target, Clock clock, ProfilingState profilingState) {
    this.target = Objects.requireNonNull(target);
    this.clock = Objects.requireNonNull(clock);
    this.profilingState = Objects.requireNonNull(profilingState);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.

    // Check if the method is annotated with @Profiled
    if (!method.isAnnotationPresent(Profiled.class)) {
      return method.invoke(target, args);
    }

    Instant beforeInvocation = clock.instant();
    try {
      Object result = method.invoke(target, args);
      Instant afterInvocation = clock.instant();
      Duration duration = Duration.between(beforeInvocation, afterInvocation);
      profilingState.record(target.getClass(), method, duration);
      return result;
    } catch (Throwable t) {
      Instant afterInvocation = clock.instant();
      Duration duration = Duration.between(beforeInvocation, afterInvocation);
      profilingState.record(target.getClass(), method, duration);
      throw t.getCause() != null ? t.getCause() : t;
    }
  }
}
