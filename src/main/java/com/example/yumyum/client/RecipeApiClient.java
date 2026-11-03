package com.example.yumyum.client;

import com.example.yumyum.config.RecipeApiProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RecipeApiClient {

    private final RestClient restClient;
    private final RecipeApiProperties properties;

    public RecipeApiClient(RecipeApiProperties properties) {
        this.properties = properties;

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public JsonNode searchRecipes(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{apiKey}/search.php")
                        .queryParam("s", query)
                        .build(properties.getApiKey()))
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getRecipeById(String id) {
        return restClient.get()
                .uri("/{apiKey}/lookup.php?i={id}",
                        properties.getApiKey(), id)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getRandomRecipe() {
        return restClient.get()
                .uri("/{apiKey}/random.php", properties.getApiKey())
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getPopularRecipes() {
        return restClient.get()
                .uri("/{apiKey}/popular.php", properties.getApiKey())
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getRandomRecipes(int count) {
        ArrayNode recipes = JsonNodeFactory.instance.arrayNode();

        java.util.Set<String> recipeIds = new java.util.HashSet<>();

        int attempts = 0;
        int maxAttempts = count * 3;

        while (recipes.size() < count && attempts < maxAttempts) {
            attempts++;
            JsonNode response = getRandomRecipe();
            JsonNode meal = response.path("meals").path(0);

            if (!meal.isMissingNode()) {
                String id = meal.path("idMeal").asText();

                if (recipeIds.add(id)) {
                    recipes.add(meal);
                }
            }
        }

        return recipes;
    }

    public JsonNode getRecipesByArea(String area) {
        String apiArea = "Indian".equals(area) ? "India" : area;

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{apiKey}/filter.php")
                        .queryParam("a", apiArea)
                        .build(properties.getApiKey()))
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getRecipesByIngredient(String ingredient) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{apiKey}/filter.php")
                        .queryParam("i", ingredient)
                        .build(properties.getApiKey()))
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getAllAreas() {
        return restClient.get()
                .uri("/{apiKey}/list.php?a=list",
                        properties.getApiKey())
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getAllIngredients() {
        return restClient.get()
                .uri("/{apiKey}/list.php?i=list",
                        properties.getApiKey())
                .retrieve()
                .body(JsonNode.class);
    }
}
