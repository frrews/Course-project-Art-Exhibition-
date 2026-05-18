package com.bsuir.exhibition;

import com.bsuir.exhibition.config.GigachatProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GigachatProperties.class)
public class ExhibitionApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExhibitionApplication.class, args);
	}

}
