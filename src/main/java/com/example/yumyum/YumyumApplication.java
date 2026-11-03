package com.example.yumyum;

import com.example.yumyum.config.RecipeApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RecipeApiProperties.class)
public class YumyumApplication {

	public static void main(String[] args) {
		SpringApplication.run(YumyumApplication.class, args);
	}

}
