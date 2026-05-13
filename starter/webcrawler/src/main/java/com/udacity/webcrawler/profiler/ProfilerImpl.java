package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.io.BufferedWriter;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

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
    Objects.requireNonNull(delegate);

    boolean hasProfiledMethod = false;

    for (java.lang.reflect.Method method : klass.getMethods()) {
      if (method.isAnnotationPresent(Profiled.class)) {
        hasProfiledMethod = true;
        break;
      }
    }

    if (!hasProfiledMethod) {
      throw new IllegalArgumentException(
              "Class does not contain @Profiled methods");
    }

    return klass.cast(
            Proxy.newProxyInstance(
                    klass.getClassLoader(),
                    new Class<?>[]{klass},
                    new ProfilingMethodInterceptor(clock, delegate, state)));
  }

  @Override
  public void writeData(Path path) {
    try (Writer writer = new BufferedWriter(
            Files.newBufferedWriter(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND))) {

      writeData(writer);

    } catch (IOException e) {
      throw new RuntimeException(e);
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
