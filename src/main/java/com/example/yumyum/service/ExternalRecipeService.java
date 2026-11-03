package com.example.yumyum.service;

import com.example.yumyum.client.RecipeApiClient;
import com.example.yumyum.config.DiscoverCombination;
import com.example.yumyum.config.DiscoverCombinationConfig;
import com.example.yumyum.dto.DiscoverRecipesResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExternalRecipeService {

    private final RecipeApiClient recipeApiClient;
    private final DiscoverCombinationConfig combinationConfig;
    private List<String> availableAreas;
    private final ObjectMapper objectMapper;

    public ExternalRecipeService(
            RecipeApiClient recipeApiClient,
            DiscoverCombinationConfig combinationConfig,
            ObjectMapper objectMapper) {
        this.recipeApiClient = recipeApiClient;
        this.combinationConfig = combinationConfig;
        this.objectMapper = objectMapper;
    }

    public List<String> getAvailableAreas() {

        if (availableAreas != null) {
            return availableAreas;
        }

        Path filePath = Path.of(
                "src/main/resources/data/available-areas.json"
        );

        try {
            // Load existing JSON file if it already contains data
            if (Files.exists(filePath) && Files.size(filePath) > 2) {

                JsonNode savedAreas = objectMapper.readTree(
                        Files.readString(filePath)
                );

                availableAreas = new ArrayList<>();

                for (JsonNode area : savedAreas) {
                    availableAreas.add(area.asText());
                }

                return availableAreas;
            }

            // Otherwise, validate all areas using TheMealDB
            availableAreas = new ArrayList<>();

            JsonNode response = recipeApiClient.getAllAreas();

            for (JsonNode areaNode : response.path("meals")) {

                String area = areaNode.path("strArea").asText();

                JsonNode recipes = recipeApiClient.getRecipesByArea(area);

                if (recipes.path("meals").isArray()
                        && !recipes.path("meals").isEmpty()) {

                    availableAreas.add(area);
                }
            }

            // Save validated areas into JSON
            Files.createDirectories(filePath.getParent());

            Files.writeString(
                    filePath,
                    objectMapper.writeValueAsString(availableAreas)
            );

            return availableAreas;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load or save available areas", e
            );
        }
    }

    public DiscoverRecipesResponse getDiscoverRecipes(
            String area,
            String ingredient) {

        // Return random recipes if neither filter is selected
        if ((area == null || area.isBlank())
                && (ingredient == null || ingredient.isBlank())) {

            ArrayNode randomRecipes = (ArrayNode) recipeApiClient.getRandomRecipes(4);

            return new DiscoverRecipesResponse(
                    new DiscoverCombination(null, null),
                    randomRecipes
            );
        }

        // Fetch recipes based on the selected filters
        JsonNode areaResponse = (area == null || area.isBlank())
                ? null
                : recipeApiClient.getRecipesByArea(area);

        JsonNode ingredientResponse = (ingredient == null || ingredient.isBlank())
                ? null
                : recipeApiClient.getRecipesByIngredient(ingredient);

        ArrayNode matchingRecipes = JsonNodeFactory.instance.arrayNode();

        // If both filters are selected, find their intersection
        if (areaResponse != null && ingredientResponse != null) {

            Set<String> ingredientIds = new HashSet<>();

            for (JsonNode meal : ingredientResponse.path("meals")) {
                ingredientIds.add(meal.path("idMeal").asText());
            }

            for (JsonNode meal : areaResponse.path("meals")) {
                String id = meal.path("idMeal").asText();

                if (ingredientIds.contains(id)) {
                    matchingRecipes.add(meal);
                }

                if (matchingRecipes.size() >= 4) {
                    break;
                }
            }

            // If only area is selected
        } else if (areaResponse != null) {

            for (JsonNode meal : areaResponse.path("meals")) {
                matchingRecipes.add(meal);

                if (matchingRecipes.size() >= 4) {
                    break;
                }
            }

            // If only ingredient is selected
        } else if (ingredientResponse != null) {

            for (JsonNode meal : ingredientResponse.path("meals")) {
                matchingRecipes.add(meal);

                if (matchingRecipes.size() >= 4) {
                    break;
                }
            }
        }

        DiscoverCombination selectedCombination =
                new DiscoverCombination(area, ingredient);

        System.out.println("Discover area: " + area);
        System.out.println("Discover ingredient: " + ingredient);

        System.out.println("Area API meals: " +
                (areaResponse == null ? "NOT REQUESTED" : areaResponse.path("meals")));

        System.out.println("Ingredient API meals: " +
                (ingredientResponse == null ? "NOT REQUESTED" : ingredientResponse.path("meals")));

        System.out.println("Final matching recipes: " + matchingRecipes.size());

        return new DiscoverRecipesResponse(selectedCombination, matchingRecipes);
    }

    public JsonNode getRecommendedRecipes() {
        return recipeApiClient.getRandomRecipes(8);
    }

    public JsonNode getRecipeById(String id) {
        return recipeApiClient.getRecipeById(id);
    }
}