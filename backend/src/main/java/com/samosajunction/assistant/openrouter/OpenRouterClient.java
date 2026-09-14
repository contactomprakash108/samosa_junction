package com.samosajunction.assistant.openrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.samosajunction.assistant.config.AssistantProperties;
import com.samosajunction.common.exception.InvalidRequestException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenRouterClient {

    private final AssistantProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenRouterClient(AssistantProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl() == null || properties.baseUrl().isBlank()
                        ? "https://openrouter.ai/api/v1"
                        : properties.baseUrl().replaceAll("/$", ""))
                .build();
    }

    public JsonNode chat(ArrayNode messages, ArrayNode tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.chatModel());
        body.set("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", tools);
            body.put("tool_choice", "auto");
        }
        body.put("temperature", 0.3);
        return post("/chat/completions", body);
    }

    public List<float[]> embed(List<String> inputs) {
        if (inputs.isEmpty()) {
            return List.of();
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.embeddingModel());
        ArrayNode input = body.putArray("input");
        inputs.forEach(input::add);
        JsonNode response = post("/embeddings", body);
        JsonNode data = response.path("data");
        List<float[]> vectors = new ArrayList<>(data.size());
        for (JsonNode row : data) {
            JsonNode embedding = row.path("embedding");
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }
            vectors.add(vector);
        }
        return vectors;
    }

    private JsonNode post(String path, ObjectNode body) {
        if (!properties.openRouterEnabled()) {
            throw new InvalidRequestException("OpenRouter is not configured");
        }
        try {
            String raw = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.openRouterApiKey())
                    .header("HTTP-Referer", "https://samosa-junction.local")
                    .header("X-Title", "Samosa Junction")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(raw);
        } catch (RestClientResponseException ex) {
            throw new InvalidRequestException("OpenRouter request failed: " + ex.getStatusCode().value());
        } catch (Exception ex) {
            throw new InvalidRequestException("OpenRouter request failed: " + ex.getMessage());
        }
    }
}
