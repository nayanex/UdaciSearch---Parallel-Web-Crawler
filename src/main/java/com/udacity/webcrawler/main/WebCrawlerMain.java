package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);
    // TODO: Write the crawl results to a JSON file (or System.out if the file name is empty)
    if (!config.getResultPath().isEmpty()) {
      // Create a Path using the result path from the configuration
      Path outputPath = Path.of(config.getResultPath());
      resultWriter.write(outputPath);
    } else {
      // Print the crawl results to System.out
      Writer writer = new BufferedWriter(new OutputStreamWriter(System.out));
      resultWriter.write(writer);
      writer.flush(); // Flush the writer to ensure all data is written
    }

    // TODO: Write the profile data to a text file (or System.out if the file name is empty)
    if (!config.getProfileOutputPath().isEmpty()) {
      // Create a Path using the profileOutputPath from the configuration
      Path profileOutputPath = Path.of(config.getProfileOutputPath());
      profiler.writeData(profileOutputPath);
    } else {
      // Print the profile data to System.out
      profiler.writeData(new BufferedWriter(new OutputStreamWriter(System.out)));
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
