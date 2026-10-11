package dev.recruitops.application;

import dev.recruitops.domain.Job;
import dev.recruitops.infrastructure.JobRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class JobExportService {
  public static final List<String> COLUMNS = List.of("companyCategory", "companyName", "title", "jobFamily",
      "jobType", "city", "deadline", "techStack", "certificateRequirements", "jd", "requirements", "applyUrl",
      "sourceUrl", "sourceTrust", "capturedAt", "confidence", "reviewStatus", "updatedAt", "businessUnit", "recruitmentYear", "sourceRowId", "officialJobId");
  private final JobService jobs;

  public JobExportService(JobService jobs) { this.jobs = jobs; }

  public String markdown(JobRepository.JobFilter filter) {
    StringBuilder text = new StringBuilder("| ").append(String.join(" | ", COLUMNS)).append(" |\n| ");
    text.append("--- | ".repeat(COLUMNS.size())).append("\n");
    for (Job job : all(filter)) text.append("| ").append(String.join(" | ", values(job).stream().map(this::escape).toList())).append(" |\n");
    return text.toString();
  }

  public byte[] xlsx(JobRepository.JobFilter filter) {
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("jobs"));
      CellStyle wrapped = workbook.createCellStyle(); wrapped.setWrapText(true);
      write(sheet.createRow(0), COLUMNS, wrapped);
      int row = 1;
      for (Job job : all(filter)) write(sheet.createRow(row++), values(job), wrapped);
      for (int column = 0; column < COLUMNS.size(); column++) sheet.setColumnWidth(column, column < 7 ? 4_800 : 9_000);
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException error) {
      throw new IllegalStateException("Could not create XLSX export", error);
    }
  }

  public List<Job> all(JobRepository.JobFilter filter) {
    List<Job> output = new ArrayList<>();
    for (int page = 1;; page++) {
      JobRepository.Page result = jobs.list(withPage(filter, page));
      output.addAll(result.items());
      if (output.size() >= result.total() || result.items().isEmpty()) return output;
    }
  }

  public List<String> values(Job job) {
    return List.of(value(job.companyCategory()), value(job.companyName()), value(job.title()), value(job.jobFamily()),
        value(job.jobType()), value(job.city()), value(job.deadline()), joined(job.techStack()), joined(job.certificateRequirements()),
        value(job.jd()), value(job.requirements()), value(job.applyUrl()), value(job.sourceUrl()), value(job.sourceTrust()),
        value(job.capturedAt()), job.confidence() == null ? "" : job.confidence().toString(), value(job.reviewStatus()), value(job.updatedAt()), value(job.businessUnit()),
        job.recruitmentYear() == null ? "" : job.recruitmentYear().toString(), value(job.sourceRowId()), value(job.officialJobId()));
  }

  private JobRepository.JobFilter withPage(JobRepository.JobFilter filter, int page) {
    return new JobRepository.JobFilter(filter.keyword(), filter.companyCategory(), filter.company(), filter.jobType(), filter.city(),
        filter.techStack(), filter.certificate(), filter.deadlineFrom(), filter.deadlineTo(), filter.sourceTrust(), filter.changedSince(), page, 100,
        filter.businessUnit(), filter.title(), filter.recruitmentYear());
  }

  private void write(org.apache.poi.ss.usermodel.Row row, List<String> values, CellStyle style) {
    for (int index = 0; index < values.size(); index++) {
      Cell cell = row.createCell(index); cell.setCellValue(values.get(index)); cell.setCellStyle(style);
    }
  }

  private String joined(List<String> values) { return values == null ? "" : String.join(", ", values); }
  private String value(String text) { return text == null ? "" : text; }
  private String escape(String text) { return text.replace("\\", "\\\\").replace("|", "\\|").replace("\r", " ").replace("\n", "<br>"); }
}
