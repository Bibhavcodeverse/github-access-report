package com.example.githubaccessreport.client;

import com.example.githubaccessreport.model.Collaborator;
import com.example.githubaccessreport.model.Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GithubClient {

    private final WebClient webClient;
    private static final Pattern NEXT_LINK_PATTERN = Pattern.compile("<([^>]+)>; rel=\"next\"");

    public GithubClient(WebClient.Builder webClientBuilder,
                        @Value("${github.api.url:https://api.github.com}") String apiUrl,
                        @Value("${github.token:}") String token) {
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    public Flux<Repository> getRepositories(String organization) {
        String uri = "/orgs/" + organization + "/repos?per_page=100";
        return fetchAllPages(uri, Repository.class);
    }

    public Flux<Collaborator> getCollaborators(String owner, String repo) {
        String uri = "/repos/" + owner + "/" + repo + "/collaborators?per_page=100";
        return fetchAllPages(uri, Collaborator.class)
                .onErrorResume(e -> Flux.empty()); // If unauthorized or 404, just return empty
    }

    private <T> Flux<T> fetchAllPages(String initialUri, Class<T> responseType) {
        return fetchPage(initialUri, responseType)
                .expand(response -> {
                    String nextUri = extractNextUrl(response.getHeaders().get(HttpHeaders.LINK));
                    if (nextUri != null) {
                        return fetchPage(nextUri, responseType);
                    }
                    return Flux.empty();
                })
                .flatMap(response -> Flux.fromIterable(response.getBody() != null ? response.getBody() : List.of()));
    }

    private <T> reactor.core.publisher.Mono<org.springframework.http.ResponseEntity<List<T>>> fetchPage(String uri, Class<T> responseType) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .toEntityList(responseType);
    }

    private String extractNextUrl(List<String> linkHeaders) {
        if (linkHeaders == null || linkHeaders.isEmpty()) {
            return null;
        }
        for (String header : linkHeaders) {
            Matcher matcher = NEXT_LINK_PATTERN.matcher(header);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
