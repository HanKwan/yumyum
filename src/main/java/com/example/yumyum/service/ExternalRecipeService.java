package com.example.yumyum.service;

import com.example.yumyum.client.RecipeApiClient;
import com.example.yumyum.config.DiscoverCombination;
import com.example.yumyum.config.DiscoverCombinationConfig;
import com.example.yumyum.dto.DiscoverRecipesResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ExternalRecipeService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalRecipeService.class);
    private static final long RANDOM_RECIPE_CACHE_TTL_NANOS =
            TimeUnit.MINUTES.toNanos(10);

    private final RecipeApiClient recipeApiClient;
    private final DiscoverCombinationConfig combinationConfig;
    private final Object availableAreasLock = new Object();
    private final Object availableIngredientsLock = new Object();
    private final Object discoverRecipeCacheLock = new Object();
    private final Object recommendedRecipeCacheLock = new Object();
    private List<String> availableAreas;
    private List<String> availableIngredients;
    private ArrayNode discoverRandomRecipeCache;
    private long discoverRandomRecipeCachedAt;
    private ArrayNode recommendedRecipeCache;
    private long recommendedRecipeCachedAt;
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
        synchronized (availableAreasLock) {
            if (availableAreas != null) {
                return availableAreas;
            }

            try {
                JsonNode savedAreas = objectMapper.readTree(
                        new ClassPathResource("data/available-areas.json").getInputStream()
                );

                if (!savedAreas.isArray()) {
                    throw new IllegalStateException(
                            "Supported area list must be a JSON array"
                    );
                }

                List<String> areas = new ArrayList<>();
                for (JsonNode area : savedAreas) {
                    if (area.isTextual() && !area.asText().isBlank()) {
                        areas.add(area.asText());
                    }
                }

                availableAreas = List.copyOf(areas);
                return availableAreas;
            } catch (IOException e) {
                logger.error("Failed to load supported recipe areas", e);
                throw new IllegalStateException(
                        "Failed to load supported recipe areas", e
                );
            }
        }
    }

    public List<String> getAvailableIngredients() {
        synchronized (availableIngredientsLock) {
            if (availableIngredients != null) {
                return availableIngredients;
            }

            try {
                JsonNode response = recipeApiClient.getAllIngredients();
                JsonNode meals = response == null ? null : response.path("meals");

                if (meals == null || !meals.isArray() || meals.isEmpty()) {
                    logger.warn("TheMealDB returned no ingredient list; it will be retried on the next request");
                    return List.of();
                }

                List<String> ingredients = new ArrayList<>();
                for (JsonNode ingredientNode : meals) {
                    String ingredient = ingredientNode.path("strIngredient").asText();
                    if (!ingredient.isBlank()) {
                        ingredients.add(ingredient);
                    }
                }

                if (ingredients.isEmpty()) {
                    logger.warn("TheMealDB returned an empty ingredient list; it will be retried on the next request");
                    return List.of();
                }

                availableIngredients = List.copyOf(ingredients);
                return availableIngredients;
            } catch (RestClientException e) {
                logger.warn("Failed to load ingredients from TheMealDB; the request will be retried", e);
                return List.of();
            }
        }
    }

    public DiscoverRecipesResponse getDiscoverRecipes(
            String area,
            String ingredient) {

        // Return random recipes if neither filter is selected
        if ((area == null || area.isBlank())
                && (ingredient == null || ingredient.isBlank())) {

            ArrayNode randomRecipes = getCachedDiscoverRandomRecipes();

            return new DiscoverRecipesResponse(
                    new DiscoverCombination(null, null),
                    randomRecipes
            );
        }

        // Fetch recipes based on the selected filters
        JsonNode areaResponse = (area == null || area.isBlank())
                ? null
                : getRecipesByAreaOrEmpty(area);

        JsonNode ingredientResponse = (ingredient == null || ingredient.isBlank())
                ? null
                : getRecipesByIngredientOrEmpty(ingredient);

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

        return new DiscoverRecipesResponse(selectedCombination, matchingRecipes);
    }

    public JsonNode getRecommendedRecipes() {
        return getCachedRecommendedRecipes();
    }

    private ArrayNode getCachedDiscoverRandomRecipes() {
        synchronized (discoverRecipeCacheLock) {
            long now = System.nanoTime();
            if (isCacheFresh(discoverRandomRecipeCache, discoverRandomRecipeCachedAt, now)) {
                return discoverRandomRecipeCache.deepCopy();
            }

            ArrayNode freshRecipes = loadRandomRecipes(4);
            if (freshRecipes != null) {
                discoverRandomRecipeCache = freshRecipes.deepCopy();
                discoverRandomRecipeCachedAt = System.nanoTime();
                return freshRecipes;
            }

            return discoverRandomRecipeCache == null
                    ? JsonNodeFactory.instance.arrayNode()
                    : discoverRandomRecipeCache.deepCopy();
        }
    }

    private ArrayNode getCachedRecommendedRecipes() {
        synchronized (recommendedRecipeCacheLock) {
            long now = System.nanoTime();
            if (isCacheFresh(recommendedRecipeCache, recommendedRecipeCachedAt, now)) {
                return recommendedRecipeCache.deepCopy();
            }

            ArrayNode freshRecipes = loadRandomRecipes(8);
            if (freshRecipes != null) {
                recommendedRecipeCache = freshRecipes.deepCopy();
                recommendedRecipeCachedAt = System.nanoTime();
                return freshRecipes;
            }

            return recommendedRecipeCache == null
                    ? JsonNodeFactory.instance.arrayNode()
                    : recommendedRecipeCache.deepCopy();
        }
    }

    private boolean isCacheFresh(ArrayNode cache, long cachedAt, long now) {
        return cache != null && now - cachedAt < RANDOM_RECIPE_CACHE_TTL_NANOS;
    }

    private ArrayNode loadRandomRecipes(int count) {
        try {
            JsonNode response = recipeApiClient.getRandomRecipes(count);
            if (!(response instanceof ArrayNode recipes) || recipes.size() < count) {
                logger.warn("TheMealDB returned fewer than {} random recipes; the list will be retried", count);
                return null;
            }
            return recipes;
        } catch (RestClientException e) {
            logger.warn("Failed to load random recipes from TheMealDB for a requested list of {}; it will be retried", count, e);
            return null;
        }
    }

    public JsonNode getRecipeById(String id) {
        return recipeApiClient.getRecipeById(id);
    }

    private JsonNode getRecipesByAreaOrEmpty(String area) {
        try {
            JsonNode response = recipeApiClient.getRecipesByArea(area);
            return response == null ? emptyMealsResponse() : response;
        } catch (RestClientException e) {
            logger.warn("Failed to load recipes for area {}; returning no matches", area, e);
            return emptyMealsResponse();
        }
    }

    private JsonNode getRecipesByIngredientOrEmpty(String ingredient) {
        try {
            JsonNode response = recipeApiClient.getRecipesByIngredient(ingredient);
            return response == null ? emptyMealsResponse() : response;
        } catch (RestClientException e) {
            logger.warn("Failed to load recipes for ingredient {}; returning no matches", ingredient, e);
            return emptyMealsResponse();
        }
    }

    private JsonNode emptyMealsResponse() {
        var response = JsonNodeFactory.instance.objectNode();
        response.set("meals", JsonNodeFactory.instance.arrayNode());
        return response;
    }
}
