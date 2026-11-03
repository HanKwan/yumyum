package com.example.yumyum.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DiscoverCombinationConfig {

    private final List<DiscoverCombination> combinations = List.of(
            new DiscoverCombination("Japanese", "chicken"),
            new DiscoverCombination("Japanese", "pork"),
            new DiscoverCombination("Chinese", "chicken"),
            new DiscoverCombination("Chinese", "pork"),
            new DiscoverCombination("Mexican", "chicken"),
            new DiscoverCombination("Mexican", "beef"),
            new DiscoverCombination("Italian", "chicken"),
            new DiscoverCombination("India", "chicken")
    );

    public DiscoverCombination getRandomCombination() {
        int randomIndex = ThreadLocalRandom.current()
                .nextInt(combinations.size());

        return combinations.get(randomIndex);
    }

    public List<String> getAreas() {
        return combinations.stream()
                .map(DiscoverCombination::area)
                .distinct()
                .toList();
    }

    public List<String> getIngredients() {
        return combinations.stream()
                .map(DiscoverCombination::ingredient)
                .distinct()
                .toList();
    }
}