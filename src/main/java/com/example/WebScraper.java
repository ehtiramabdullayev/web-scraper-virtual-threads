package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class WebScraper {

    public static void main(String[] args) {
        final var queue = new LinkedBlockingQueue<String>(2000);
        Set<String> visited = ConcurrentHashMap.newKeySet(3000);

        queue.add("http://localhost:8080/v1/crawl/delay/330/57");

        long startTime = System.currentTimeMillis();

//        Runnable task =()-> new Scrape(queue, visited).run();

        //Normal threads
//        try (var executor = Executors.newFixedThreadPool(
//                Runtime.getRuntime().availableProcessors()
//        )) {
//
//            for (int i = 0; i < 100; i++) {
//                executor.submit(new Scrape(queue, visited));
//            }
//        }
//
//        measureTime(startTime, visited);

//         startTime = System.currentTimeMillis();

//      Virtual threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Runtime.getRuntime().availableProcessors();


            for (int i = 0; i < 100; i++) {
                executor.submit(new Scrape(queue, visited));
            }
        }


//        Code doesn't work
//        var executor = Executors.newFixedThreadPool(
//                Runtime.getRuntime().availableProcessors());
//
//            for (int i = 0; i < 100; i++) {
//                executor.submit(new Scrape(queue, visited));
//            }
//

        measureTime(startTime, visited);


    }

    private static void measureTime(long startTime, Set<String> visited) {
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        double totalTimeInSeconds = totalTime / 1000.0;

        System.out.printf("Crawled %s web page(s)", visited.size());
        System.out.println("Total execution time: " + totalTime + "ms");

        double throughput = visited.size() / totalTimeInSeconds;
        System.out.println("Throughput: " + throughput + " pages/sec");
    }

}

class Scrape implements Runnable {

    private final LinkedBlockingQueue<String> pageQueue;

    private final Set<String> visited;

    public Scrape(LinkedBlockingQueue<String> pageQueue, Set<String> visited) {
        this.pageQueue = pageQueue;
        this.visited = visited;
    }

    public void scrape() {

        try {
            String url = pageQueue.take();

            // Improved version
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).GET().build();
            String body = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();
                        Document document = Jsoup.parse(body);

//            //Default version
//            Document document = Jsoup.connect(url).get();



            Elements linksOnPage = document.select("a[href]");

            visited.add(url);
            for (Element link : linksOnPage) {
                String nextUrl = link.attr("abs:href");
                if (nextUrl.contains("http")) {
                    pageQueue.add(nextUrl);
                }
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void run() {
        scrape();
    }
}