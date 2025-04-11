package com.noahalvandi.dbbserver;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DbbServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbbServerApplication.class, args);
    }

}
