package com.imgjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ImgJavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImgJavaApplication.class, args);
	}

}
