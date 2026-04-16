package customer.batchimportcat.bootstrap;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("pg")
@ConditionalOnProperty(prefix = "app.seed-http", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HttpRequestSeedRunner {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestSeedRunner.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^@([A-Za-z0-9_]+)\\s*=\\s*(.+)$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    @Value("${app.seed-http.file:http/example-header-item-schedule-config.http}")
    private String seedHttpFile;

    @Value("${app.seed-http.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${local.server.port:${server.port:8080}}")
    private int serverPort;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfNeeded() {
        try {
            Path filePath = resolveFilePath(seedHttpFile);
            SeedRequest request = parseHttpFile(filePath);

            Map<String, String> variables = new HashMap<>(request.variables());
            variables.put("host", "http://localhost:" + serverPort);

            String postUrl = resolveTemplate(request.postUrlTemplate(), variables);
            String requestBody = resolveTemplate(request.bodyTemplate(), variables);
            Map<String, String> headers = resolveHeaders(request.headersTemplate(), variables);

            String configId = variables.get("config_id");
            if (configId == null || configId.isBlank()) {
                LOG.warn("Skip HTTP seed because @config_id is missing in {}", filePath);
                return;
            }

            if (configExists(postUrl, configId, headers)) {
                LOG.info("Skip HTTP seed because config {} already exists", configId);
                return;
            }

            int status = postSeed(postUrl, headers, requestBody);
            if (status == 200 || status == 201) {
                LOG.info("HTTP seed completed from {}", filePath);
            } else {
                LOG.warn("HTTP seed returned unexpected status: {}", status);
            }
        } catch (Exception exception) {
            LOG.warn("HTTP seed failed: {}", exception.getMessage());
        }
    }

    private int postSeed(String postUrl, Map<String, String> headers, String requestBody)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(postUrl))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));
        applyHeaders(builder, headers);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    private boolean configExists(String postUrl, String configId, Map<String, String> headers)
            throws IOException, InterruptedException {
        String getUrl = postUrl + "(ID=" + configId + ",IsActiveEntity=true)";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(getUrl))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET();
        applyHeaders(builder, headers);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }

    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, String> resolveHeaders(Map<String, String> headersTemplate, Map<String, String> variables) {
        Map<String, String> resolvedHeaders = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headersTemplate.entrySet()) {
            resolvedHeaders.put(entry.getKey(), resolveTemplate(entry.getValue(), variables));
        }
        return resolvedHeaders;
    }

    private SeedRequest parseHttpFile(Path filePath) throws IOException {
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        Map<String, String> variables = parseVariables(lines);

        String postUrlTemplate = null;
        Map<String, String> headersTemplate = new LinkedHashMap<>();
        List<String> bodyLines = new ArrayList<>();

        boolean inHeaders = false;
        boolean inBody = false;

        for (String line : lines) {
            if (line.startsWith("POST ")) {
                postUrlTemplate = line.substring(5).trim();
                inHeaders = true;
                inBody = false;
                continue;
            }

            if (postUrlTemplate == null) {
                continue;
            }

            if (inHeaders) {
                if (line.trim().isEmpty()) {
                    inHeaders = false;
                    inBody = true;
                    continue;
                }

                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    headersTemplate.put(key, value);
                }
                continue;
            }

            if (inBody) {
                if (line.startsWith("###")) {
                    break;
                }
                bodyLines.add(line);
            }
        }

        String bodyTemplate = String.join(System.lineSeparator(), bodyLines).trim();
        if (postUrlTemplate == null || bodyTemplate.isEmpty()) {
            throw new IllegalStateException("Unable to parse POST request block from " + filePath);
        }

        return new SeedRequest(variables, postUrlTemplate, headersTemplate, bodyTemplate);
    }

    private Map<String, String> parseVariables(List<String> lines) {
        Map<String, String> variables = new LinkedHashMap<>();
        for (String line : lines) {
            Matcher matcher = VARIABLE_PATTERN.matcher(line.trim());
            if (!matcher.matches()) {
                continue;
            }

            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            variables.put(key, value);
        }

        Map<String, String> resolvedVariables = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            resolvedVariables.put(entry.getKey(), resolveTemplate(entry.getValue(), variables));
        }
        return resolvedVariables;
    }

    private String resolveTemplate(String text, Map<String, String> variables) {
        String resolved = text;
        for (int i = 0; i < 10; i++) {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(resolved);
            if (!matcher.find()) {
                return resolved;
            }

            StringBuffer buffer = new StringBuffer();
            do {
                String key = matcher.group(1);
                String replacement = variables.getOrDefault(key, matcher.group(0));
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            } while (matcher.find());
            matcher.appendTail(buffer);
            resolved = buffer.toString();
        }
        return resolved;
    }

    private Path resolveFilePath(String configuredPath) {
        List<Path> candidates = List.of(
                Paths.get(configuredPath),
                Paths.get(".").resolve(configuredPath),
                Paths.get("..").resolve(configuredPath));

        for (Path candidate : candidates) {
            Path normalized = candidate.normalize().toAbsolutePath();
            if (Files.exists(normalized)) {
                return normalized;
            }
        }

        throw new IllegalStateException("HTTP seed file not found: " + configuredPath);
    }

    private record SeedRequest(
            Map<String, String> variables,
            String postUrlTemplate,
            Map<String, String> headersTemplate,
            String bodyTemplate) {
    }
}
