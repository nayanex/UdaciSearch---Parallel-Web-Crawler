package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import java.lang.reflect.Proxy;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);
    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

    Objects.requireNonNull(delegate);

    // Check if the wrapped interface contains any methods annotated with @Profiled
    boolean hasProfiledMethod = Arrays.stream(klass.getMethods())
            .anyMatch(method -> method.isAnnotationPresent(Profiled.class));

    if (!hasProfiledMethod) {
      throw new IllegalArgumentException("The wrapped interface must contain a @Profiled method.");
    }

    return klass.cast(
            java.lang.reflect.Proxy.newProxyInstance(
                    klass.getClassLoader(),
                    new Class<?>[]{klass},
                    new ProfilingMethodInterceptor<>(delegate, clock, state)));
  }

  @Override
  public void writeData(Path path) throws IOException {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
    try (Writer writer = Files.newBufferedWriter(path)) {
      writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
      writer.write(System.lineSeparator());
      state.write(writer);
      writer.write(System.lineSeparator());
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
