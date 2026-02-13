package net.muttcode.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FileApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileApiApplication.class, args);
	}
}
