package dev.recruitops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecruitOpsApplication {
  public static void main(String[] args) { SpringApplication.run(RecruitOpsApplication.class, args); }
}
