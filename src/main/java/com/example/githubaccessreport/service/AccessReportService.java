package com.example.githubaccessreport.service;

import com.example.githubaccessreport.client.GithubClient;
import com.example.githubaccessreport.model.RepositoryAccess;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collections;

@Service
public class AccessReportService {

    private final GithubClient githubClient;

    public AccessReportService(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

    public Mono<Map<String, List<RepositoryAccess>>> generateReport(String organization) {
        return githubClient.getRepositories(organization)
                // Limit concurrency to 10 to avoid hitting secondary rate limits abruptly
                .flatMap(repo -> githubClient.getCollaborators(organization, repo.getName())
                                .map(collaborator -> new UserAccess(collaborator.getLogin(), new RepositoryAccess(repo.getName(), collaborator.getRole()))),
                        10)
                .collectList()
                .map(accessList -> {
                    Map<String, List<RepositoryAccess>> report = new ConcurrentHashMap<>();
                    for (UserAccess access : accessList) {
                        report.computeIfAbsent(access.login(), k -> Collections.synchronizedList(new ArrayList<>()))
                              .add(access.access());
                    }
                    return report;
                });
    }

    private record UserAccess(String login, RepositoryAccess access) {}
}
