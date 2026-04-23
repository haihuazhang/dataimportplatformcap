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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private static final Pattern REQUEST_LINE_PATTERN = Pattern.compile("^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\s+(.+)$");
    private static final int MAX_TEMPLATE_RESOLVE_ROUNDS = 10;

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
            SeedScript script = parseHttpFile(filePath);

            Map<String, String> rawVariables = new LinkedHashMap<>(script.variables());
            rawVariables.put("host", "http://localhost:" + serverPort);
            Map<String, String> resolvedVariables = resolveVariables(rawVariables);

            if (isSeedAlreadyApplied(resolvedVariables)) {
                LOG.info("Skip HTTP seed because config {} already exists", resolvedVariables.get("config_id"));
                return;
            }

            executeRequestsInOrder(filePath, script.requests(), resolvedVariables);
            LOG.info("HTTP seed completed from {}", filePath);
        } catch (Exception exception) {
            LOG.warn("HTTP seed failed: {}", exception.getMessage(), exception);
        }
    }

    private void executeRequestsInOrder(Path filePath, List<RequestBlock> requestBlocks, Map<String, String> variables)
            throws IOException, InterruptedException {
        Path baseDir = filePath.getParent() == null ? Paths.get(".").toAbsolutePath() : filePath.getParent();
        for (int index = 0; index < requestBlocks.size(); index++) {
            RequestBlock block = requestBlocks.get(index);
            ResolvedRequest request = resolveRequest(block, variables, baseDir);
            HttpResponse<String> response = sendRequest(request);
            int status = response.statusCode();
            if (!isSuccessStatus(status)) {
                throw new IllegalStateException("HTTP seed request failed at index " + index
                        + " with status " + status
                        + ": " + block.method() + " " + block.urlTemplate()
                        + " response=" + shorten(response.body(), 400));
            }

            LOG.info("HTTP seed request {} {} -> {}", request.method(), request.url(), status);
        }
    }

    private HttpResponse<String> sendRequest(ResolvedRequest request) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(request.url())
                .timeout(Duration.ofSeconds(timeoutSeconds));
        applyHeaders(builder, request.headers());

        HttpRequest.BodyPublisher bodyPublisher = request.body() == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(request.body());

        switch (request.method()) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(bodyPublisher);
                break;
            case "PUT":
                builder.PUT(bodyPublisher);
                break;
            case "PATCH":
                builder.method("PATCH", bodyPublisher);
                break;
            case "DELETE":
                if (request.body() == null) {
                    builder.DELETE();
                } else {
                    builder.method("DELETE", bodyPublisher);
                }
                break;
            case "HEAD":
            case "OPTIONS":
                builder.method(request.method(), HttpRequest.BodyPublishers.noBody());
                break;
            default:
                throw new IllegalStateException("Unsupported HTTP method: " + request.method());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private boolean isSeedAlreadyApplied(Map<String, String> variables) {
        String service = variables.get("service");
        String configId = variables.get("config_id");
        if (isBlank(service) || isBlank(configId)) {
            return false;
        }

        String getUrl = service + "/BatchImportConfig(ID=" + configId + ",IsActiveEntity=true)";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(getUrl))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .header("Accept", "application/json");

        String basicAuth = variables.get("basic_auth");
        if (!isBlank(basicAuth)) {
            builder.header("Authorization", basicAuth);
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return true;
            }
            if (response.statusCode() != 404) {
                LOG.warn("Unable to determine seed existence, status={} url={}", response.statusCode(), getUrl);
            }
        } catch (Exception exception) {
            LOG.warn("Failed to check seed existence: {}", exception.getMessage());
        }
        return false;
    }

    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
    }

    private ResolvedRequest resolveRequest(RequestBlock requestBlock, Map<String, String> variables, Path baseDir)
            throws IOException {
        String resolvedUrl = resolveTemplate(requestBlock.urlTemplate(), variables);
        Map<String, String> resolvedHeaders = resolveHeaders(requestBlock.headersTemplate(), variables);
        String resolvedBody = resolveTemplate(requestBlock.bodyTemplate(), variables).trim();

        byte[] bodyBytes = null;
        if (!resolvedBody.isEmpty()) {
            if (resolvedBody.startsWith("<")) {
                String filePathTemplate = resolvedBody.substring(1).trim();
                Path payloadPath = resolvePayloadPath(baseDir, filePathTemplate);
                bodyBytes = Files.readAllBytes(payloadPath);
            } else {
                bodyBytes = resolvedBody.getBytes(StandardCharsets.UTF_8);
            }
        }

        return new ResolvedRequest(requestBlock.method(), URI.create(resolvedUrl), resolvedHeaders, bodyBytes);
    }

    private Map<String, String> resolveHeaders(Map<String, String> headersTemplate, Map<String, String> variables) {
        Map<String, String> resolvedHeaders = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headersTemplate.entrySet()) {
            resolvedHeaders.put(entry.getKey(), resolveTemplate(entry.getValue(), variables));
        }
        return resolvedHeaders;
    }

    private SeedScript parseHttpFile(Path filePath) throws IOException {
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        Map<String, String> variables = parseVariables(lines);
        List<RequestBlock> requestBlocks = parseRequestBlocks(lines);
        if (requestBlocks.isEmpty()) {
            throw new IllegalStateException("No HTTP request block found in " + filePath);
        }
        return new SeedScript(variables, requestBlocks);
    }

    private List<RequestBlock> parseRequestBlocks(List<String> lines) {
        List<RequestBlock> requests = new ArrayList<>();
        List<String> section = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().startsWith("###")) {
                appendRequestSection(section, requests);
                section.clear();
                continue;
            }
            section.add(line);
        }
        appendRequestSection(section, requests);

        return requests;
    }

    private void appendRequestSection(List<String> section, List<RequestBlock> requests) {
        if (section.isEmpty()) {
            return;
        }

        int requestLineIndex = -1;
        Matcher requestMatcher = null;
        for (int index = 0; index < section.size(); index++) {
            String trimmed = section.get(index).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("@")) {
                continue;
            }

            Matcher matcher = REQUEST_LINE_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                requestLineIndex = index;
                requestMatcher = matcher;
                break;
            }
            return;
        }

        if (requestLineIndex < 0 || requestMatcher == null) {
            return;
        }

        String method = requestMatcher.group(1);
        String urlTemplate = requestMatcher.group(2).trim();

        Map<String, String> headersTemplate = new LinkedHashMap<>();
        int cursor = requestLineIndex + 1;
        while (cursor < section.size()) {
            String headerLine = section.get(cursor);
            if (headerLine.trim().isEmpty()) {
                cursor++;
                break;
            }

            int colonIndex = headerLine.indexOf(':');
            if (colonIndex > 0) {
                String key = headerLine.substring(0, colonIndex).trim();
                String value = headerLine.substring(colonIndex + 1).trim();
                headersTemplate.put(key, value);
            }
            cursor++;
        }

        List<String> bodyLines = new ArrayList<>();
        while (cursor < section.size()) {
            bodyLines.add(section.get(cursor));
            cursor++;
        }

        String bodyTemplate = String.join(System.lineSeparator(), bodyLines).trim();
        requests.add(new RequestBlock(method, urlTemplate, headersTemplate, bodyTemplate));
    }

    private Map<String, String> parseVariables(List<String> lines) {
        Map<String, String> rawVariables = new LinkedHashMap<>();
        for (String line : lines) {
            Matcher matcher = VARIABLE_PATTERN.matcher(line.trim());
            if (!matcher.matches()) {
                continue;
            }

            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            rawVariables.put(key, value);
        }
        return rawVariables;
    }

    private Map<String, String> resolveVariables(Map<String, String> rawVariables) {
        Map<String, String> resolved = new LinkedHashMap<>(rawVariables);
        for (int round = 0; round < MAX_TEMPLATE_RESOLVE_ROUNDS; round++) {
            boolean changed = false;
            for (Map.Entry<String, String> entry : rawVariables.entrySet()) {
                String key = entry.getKey();
                String next = resolveTemplate(entry.getValue(), resolved);
                if (!Objects.equals(resolved.get(key), next)) {
                    resolved.put(key, next);
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return resolved;
    }

    private String resolveTemplate(String text, Map<String, String> variables) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String resolved = text;
        for (int i = 0; i < MAX_TEMPLATE_RESOLVE_ROUNDS; i++) {
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

    private Path resolvePayloadPath(Path baseDir, String payloadPathTemplate) {
        Path payloadPath = Paths.get(payloadPathTemplate);
        if (!payloadPath.isAbsolute()) {
            payloadPath = baseDir.resolve(payloadPathTemplate);
        }

        Path normalized = payloadPath.normalize().toAbsolutePath();
        if (!Files.exists(normalized)) {
            throw new IllegalStateException("HTTP seed binary payload file not found: " + normalized);
        }
        return normalized;
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

    private boolean isSuccessStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private String shorten(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private record SeedScript(
            Map<String, String> variables,
            List<RequestBlock> requests) {
    }

    private record RequestBlock(
            String method,
            String urlTemplate,
            Map<String, String> headersTemplate,
            String bodyTemplate) {
    }

    private record ResolvedRequest(
            String method,
            URI url,
            Map<String, String> headers,
            byte[] body) {
    }
}
