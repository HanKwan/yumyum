package com.example.yumyum.dto;

import com.example.yumyum.config.DiscoverCombination;
import tools.jackson.databind.node.ArrayNode;

public record DiscoverRecipesResponse(
        DiscoverCombination combination,
        ArrayNode recipeName
) {
}