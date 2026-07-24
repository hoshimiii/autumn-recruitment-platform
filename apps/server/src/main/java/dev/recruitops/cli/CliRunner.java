package dev.recruitops.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.recruitops.application.JobExportService;
import dev.recruitops.application.JobService;
import dev.recruitops.infrastructure.JobRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CliRunner {
  private static final Set<String> FILTERS = Set.of("keyword", "companyCategory", "company", "jobType", "city",
      "techStack", "certificate", "deadlineFrom", "deadlineTo", "sourceTrust", "changedSince");
  private final JobService jobs;
  private final JobExportService exports;
  private final ObjectMapper json;

  public CliRunner(JobService jobs, JobExportService exports, ObjectMapper json) {
    this.jobs = jobs; this.exports = exports; this.json = json;
  }

  public int run(String[] args, OutputStream stdout, OutputStream stderr) {
    try {
      if (args.length == 0) throw new IllegalArgumentException("Missing CLI command");
      Map<String, String> options = options(Arrays.copyOfRange(args, 1, args.length));
      return switch (args[0]) {
        case "search" -> search(options, stdout);
        case "get" -> get(options, stdout);
        case "export" -> export(options, stdout);
        default -> throw new IllegalArgumentException("Unknown CLI command: " + args[0]);
      };
    } catch (IllegalArgumentException error) {
      write(stderr, error.getMessage()); return 2;
    } catch (IOException error) {
      write(stderr, "CLI failed: " + error.getMessage()); return 1;
    }
  }

  private int search(Map<String, String> options, OutputStream stdout) throws IOException {
    requireOnly(options, FILTERS);
    write(stdout, json.writeValueAsString(jobs.list(filter(options))));
    return 0;
  }

  private int get(Map<String, String> options, OutputStream stdout) throws IOException {
    requireOnly(options, Set.of("id"));
    String id = required(options, "id");
    try { write(stdout, json.writeValueAsString(jobs.detail(Long.parseLong(id)))); }
    catch (NumberFormatException error) { throw new IllegalArgumentException("--id must be a number"); }
    return 0;
  }

  private int export(Map<String, String> options, OutputStream stdout) throws IOException {
    requireOnly(options, union(FILTERS, Set.of("format", "output")));
    String format = required(options, "format");
    Path output = Path.of(required(options, "output"));
    if (Files.isDirectory(output)) throw new IllegalArgumentException("--output must be a file, not a directory");
    byte[] content = switch (format) {
      case "md" -> exports.markdown(filter(options)).getBytes(StandardCharsets.UTF_8);
      case "xlsx" -> exports.xlsx(filter(options));
      default -> throw new IllegalArgumentException("--format must be md or xlsx");
    };
    Files.write(output, content);
    write(stdout, json.writeValueAsString(Map.of("output", output.toAbsolutePath().toString(), "format", format)));
    return 0;
  }

  private Map<String, String> options(String[] args) {
    Map<String, String> output = new LinkedHashMap<>();
    for (int index = 0; index < args.length; index += 2) {
      if (!args[index].startsWith("--") || index + 1 == args.length || args[index + 1].startsWith("--"))
        throw new IllegalArgumentException("Expected a value after " + args[index]);
      String name = args[index].substring(2);
      if (name.isBlank() || output.putIfAbsent(name, args[index + 1]) != null) throw new IllegalArgumentException("Invalid option: " + args[index]);
    }
    return output;
  }

  private JobRepository.JobFilter filter(Map<String, String> values) {
    return new JobRepository.JobFilter(values.get("keyword"), values.get("companyCategory"), values.get("company"), values.get("jobType"),
        values.get("city"), values.get("techStack"), values.get("certificate"), values.get("deadlineFrom"), values.get("deadlineTo"),
        values.get("sourceTrust"), values.get("changedSince"), 1, 100);
  }

  private Set<String> union(Set<String> first, Set<String> second) { var result = new java.util.HashSet<>(first); result.addAll(second); return result; }
  private void requireOnly(Map<String, String> options, Set<String> allowed) { for (String key : options.keySet()) if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown option: --" + key); }
  private String required(Map<String, String> values, String name) { if (!values.containsKey(name)) throw new IllegalArgumentException("Missing --" + name); return values.get(name); }
  private void write(OutputStream output, String message) { try { output.write((message + System.lineSeparator()).getBytes(StandardCharsets.UTF_8)); } catch (IOException ignored) {} }
}
