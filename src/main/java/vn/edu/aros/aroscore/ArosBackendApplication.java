package vn.edu.aros.aroscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArosBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArosBackendApplication.class, args);
    }

}
