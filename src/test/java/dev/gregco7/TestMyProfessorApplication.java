package dev.gregco7;

import org.springframework.boot.SpringApplication;

public class TestMyProfessorApplication {

    public static void main(String[] args) {
        SpringApplication.from(MyProfessorApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
