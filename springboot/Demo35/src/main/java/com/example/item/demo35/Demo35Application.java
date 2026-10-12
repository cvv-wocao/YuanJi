package com.example.item.demo35;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.bind.annotation.CrossOrigin;

@EnableKafka
@SpringBootApplication
public class Demo35Application {

    public static void main(String[] args) {
        SpringApplication.run(Demo35Application.class, args);
    }

}
