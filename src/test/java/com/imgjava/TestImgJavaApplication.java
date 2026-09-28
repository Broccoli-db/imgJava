package com.imgjava;

import org.springframework.boot.SpringApplication;

public class TestImgJavaApplication {

	public static void main(String[] args) {
		SpringApplication.from(ImgJavaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
