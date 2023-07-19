package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final PageParserFactory parserFactory;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;

  @Inject
  ParallelWebCrawler(Clock clock, PageParserFactory parserFactory, @Timeout Duration timeout,
                     @MaxDepth int maxDepth, @PopularWordCount int popularWordCount,
                     @TargetParallelism int threadCount, @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(threadCount);
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    Set<String> visitedUrls = new ConcurrentSkipListSet<>();

    for (String url : startingUrls) {
      pool.invoke(new CrawlInternalAction(url, deadline, maxDepth, counts, visitedUrls));
    }

    Map<String, Integer> wordCounts = counts.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

    if (wordCounts.isEmpty()) {
      return new CrawlResult.Builder()
              .setWordCounts(Collections.emptyMap())
              .setUrlsVisited(visitedUrls.size())
              .build();
    }

    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(wordCounts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  private class CrawlInternalAction extends RecursiveAction {
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Map<String, AtomicInteger> counts;
    private final Set<String> visitedUrls;

    CrawlInternalAction(String url, Instant deadline, int maxDepth, Map<String, AtomicInteger> counts,
                        Set<String> visitedUrls) {
      this.url = url;
      this.deadline = deadline;
      this.maxDepth = maxDepth;
      this.counts = counts;
      this.visitedUrls = visitedUrls;
    }

    @Override
    protected void compute() {
      if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
        return;
      }

      if (visitedUrls.contains(url)) {
        return;
      }

      if (isIgnoredUrl(url)) {
        return;
      }

      visitedUrls.add(url);
      PageParser.Result result = parserFactory.get(url).parse();
      updateWordCounts(result.getWordCounts());
      startSubtasks(result.getLinks());
    }

    private boolean isIgnoredUrl(String url) {
      return ignoredUrls.stream().anyMatch(pattern -> pattern.matcher(url).matches());
    }

    private void updateWordCounts(Map<String, Integer> wordCounts) {
      for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
        counts.computeIfAbsent(entry.getKey(), key -> new AtomicInteger())
                .addAndGet(entry.getValue());
      }
    }

    private void startSubtasks(List<String> links) {
      List<CrawlInternalAction> subtasks = new ArrayList<>();
      for (String link : links) {
        if (!visitedUrls.contains(link) && !isIgnoredUrl(link)) {
          subtasks.add(new CrawlInternalAction(link, deadline, maxDepth - 1, counts, visitedUrls));
        }
      }
      invokeAll(subtasks);
    }
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
