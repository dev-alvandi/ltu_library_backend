package com.noahalvandi.dbbserver;

import org.springframework.boot.SpringApplication;

public class TestDbbServerApplication {

    public static void main(String[] args) {
        SpringApplication.from(DbbServerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
