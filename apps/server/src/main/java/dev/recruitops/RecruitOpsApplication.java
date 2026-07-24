package dev.recruitops;

import dev.recruitops.cli.CliRunner;
import dev.recruitops.mcp.McpRuntime;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecruitOpsApplication {
  public static void main(String[] args) {
    if (args.length > 0 && args[0].equals("cli")) {
      int exitCode;
      try (ConfigurableApplicationContext context = nonWebContext()) {
        exitCode = context.getBean(CliRunner.class).run(Arrays.copyOfRange(args, 1, args.length), System.out, System.err);
      }
      System.exit(exitCode);
    }
    if (args.length > 0 && args[0].equals("mcp")) {
      nonWebContext().getBean(McpRuntime.class).run();
      return;
    }
    SpringApplication.run(RecruitOpsApplication.class, args);
  }

  private static ConfigurableApplicationContext nonWebContext() {
    return new SpringApplicationBuilder(RecruitOpsApplication.class).web(WebApplicationType.NONE).bannerMode(org.springframework.boot.Banner.Mode.OFF)
        .logStartupInfo(false).properties("logging.level.root=OFF").run();
  }
}
