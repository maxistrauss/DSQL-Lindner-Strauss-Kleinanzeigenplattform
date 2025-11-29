package de.oth.kleinanzeigen.service;

import de.oth.kleinanzeigen.dto.ApiSearchResult;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class FederatedSearchService {

    private final RestTemplate restTemplate;
    private final ExecutorService executorService;

    public FederatedSearchService(RestTemplateBuilder builder) {
        this.restTemplate = builder.connectTimeout(Duration.ofMillis(1000)).readTimeout(Duration.ofMillis(3000))
                .build();
        this.executorService = Executors.newFixedThreadPool(10); // Parallelize requests
    }

    public List<ApiSearchResult> searchExternalPortals(String search, Double lon, Double lat, Integer distance) {
        List<String> ips = IntStream.rangeClosed(38, 47).mapToObj(i -> "10.127.0." + i).toList();

        List<CompletableFuture<ApiSearchResult>> futures = ips.stream()
                .map(ip -> CompletableFuture.supplyAsync(() -> queryPortal(ip, search, lon, lat, distance),
                        executorService)).toList();

        return futures.stream().map(CompletableFuture::join) // Wait for all
                .filter(result -> result != null && result.getResults() != null && !result.getResults().isEmpty())
                .collect(Collectors.toList());
    }

    private ApiSearchResult queryPortal(String ip, String search, Double lon, Double lat, Integer distance) {
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://" + ip + "/")
                    .queryParam("mode", "api_search");

            if (search != null && !search.isBlank()) {
                uriBuilder.queryParam("search", search);
            }

            if (lon != null)
                uriBuilder.queryParam("lon", lon);
            if (lat != null)
                uriBuilder.queryParam("lat", lat);
            if (distance != null)
                uriBuilder.queryParam("distance", distance);

            URI url = uriBuilder.build().toUri();
            // System.out.println("Querying external portal: " + url); // Too verbose if enabled for all

            ApiSearchResult result = restTemplate.getForObject(url, ApiSearchResult.class);

            if (result != null && result.getResults() != null) {
                if (result.getResults().isEmpty()) {
                    System.out.println("Portal " + ip + " returned 0 results.");
                } else {
                    System.out.println("Portal " + ip + " returned " + result.getResults().size() + " results.");
                }
            } else {
                System.out.println("Portal " + ip + " returned null response or null results list.");
            }

            return result;
        } catch (RestClientException e) {
            System.out.println("Failed to query external portal " + ip + ": " + e.getMessage());
            return null;
        }
    }
}
