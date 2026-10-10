package lab.analytics;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.HBaseConfiguration;

public final class AnalyticsJob {
    private static final String HEADER = "record_id,cohort,department,job_family";
    private static final String TABLE = "job_aggregates";
    private static final byte[] FAMILY = Bytes.toBytes("metrics");
    private static final byte[] COUNT = Bytes.toBytes("count");

    private AnalyticsJob() { }

    static String parseRow(String line) {
        String[] fields = line.split(",", -1);
        if (fields.length != 4 || !fields[0].matches("SYN-[0-9]{3}")
                || !fields[1].matches("20[0-9]{2}")
                || !fields[2].matches("[A-Za-z][A-Za-z0-9_-]*")
                || !fields[3].matches("[A-Za-z][A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException("invalid synthetic row: " + line);
        }
        return fields[1] + "|" + fields[2] + "|" + fields[3];
    }

    static Map<String, Integer> aggregate(Iterable<String> lines) {
        Map<String, Integer> counts = new TreeMap<>();
        Set<String> ids = new HashSet<>();
        for (String line : lines) {
            String[] fields = line.split(",", -1);
            String key = parseRow(line);
            if (!ids.add(fields[0])) throw new IllegalArgumentException("duplicate synthetic record id: " + fields[0]);
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    public static final class CountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private static final IntWritable ONE = new IntWritable(1);
        private final Text key = new Text();

        @Override
        protected void map(LongWritable offset, Text value, Context context) throws IOException, InterruptedException {
            if (offset.get() == 0 && HEADER.equals(value.toString())) return;
            try {
                key.set(parseRow(value.toString()));
            } catch (IllegalArgumentException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
            context.write(key, ONE);
        }
    }

    public static final class CountReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private final IntWritable total = new IntWritable();

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) sum = Math.addExact(sum, value.get());
            total.set(sum);
            context.write(key, total);
        }
    }

    public static void main(String[] args) throws Exception {
        Path input = Path.of("/lab/data/jobs.csv");
        Path expectedPath = Path.of("/lab/data/expected.tsv");
        Path output = Path.of("/lab/state/output");
        Map<String, Integer> expected = readExpected(expectedPath);
        validateInput(input, expected);

        Configuration conf = HBaseConfiguration.create();
        conf.set("fs.defaultFS", "file:///");
        conf.set("mapreduce.framework.name", "local");
        Job job = Job.getInstance(conf, "synthetic-job-counts");
        job.setJarByClass(AnalyticsJob.class);
        job.setMapperClass(CountMapper.class);
        job.setCombinerClass(CountReducer.class);
        job.setReducerClass(CountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        FileInputFormat.addInputPath(job, new org.apache.hadoop.fs.Path(input.toUri()));
        org.apache.hadoop.fs.Path outputPath = new org.apache.hadoop.fs.Path(output.toUri());
        FileSystem fs = outputPath.getFileSystem(conf);
        if (fs.exists(outputPath)) fs.delete(outputPath, true);
        FileOutputFormat.setOutputPath(job, outputPath);
        if (!job.waitForCompletion(true)) throw new IllegalStateException("Hadoop aggregation failed");

        Map<String, Integer> actual = readAggregates(output.resolve("part-r-00000"));
        if (!expected.equals(actual)) throw new IllegalStateException("Hadoop output mismatch: " + actual);
        writeAndVerifyHBase(conf, actual);
    }

    private static void validateInput(Path input, Map<String, Integer> expected) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            if (!HEADER.equals(reader.readLine())) throw new IOException("unexpected synthetic CSV header");
            Set<String> ids = new HashSet<>();
            java.util.List<String> rows = new java.util.ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);
                parseRow(line);
                if (!ids.add(fields[0])) throw new IOException("duplicate synthetic record id: " + fields[0]);
                rows.add(line);
            }
            if (!aggregate(rows).equals(expected)) {
                throw new IOException("synthetic input does not match its fixed expected output");
            }
        }
    }

    private static Map<String, Integer> readExpected(Path path) throws IOException {
        return readAggregates(path);
    }

    private static Map<String, Integer> readAggregates(Path path) throws IOException {
        Map<String, Integer> rows = new TreeMap<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String[] pair = line.split("\\t", -1);
            if (pair.length != 2 || !pair[0].matches("20[0-9]{2}\\|[A-Za-z][A-Za-z0-9_-]*\\|[A-Za-z][A-Za-z0-9_-]*"))
                throw new IOException("invalid aggregate TSV row: " + line);
            int count;
            try { count = Integer.parseInt(pair[1]); }
            catch (NumberFormatException ex) { throw new IOException("invalid aggregate count: " + line, ex); }
            if (count < 1 || rows.putIfAbsent(pair[0], count) != null) throw new IOException("duplicate or invalid aggregate: " + line);
        }
        return rows;
    }

    private static void writeAndVerifyHBase(Configuration conf, Map<String, Integer> aggregates) throws Exception {
        TableName tableName = TableName.valueOf(TABLE);
        try (Connection connection = ConnectionFactory.createConnection(conf);
             Admin admin = connection.getAdmin()) {
            boolean ready = false;
            for (int attempt = 0; attempt < 60 && !ready; attempt++) {
                try {
                    if (!admin.tableExists(tableName)) {
                        try { admin.createTable(TableDescriptorBuilder.newBuilder(tableName)
                                .setColumnFamily(ColumnFamilyDescriptorBuilder.of(FAMILY)).build()); }
                        catch (org.apache.hadoop.hbase.TableExistsException ignored) { }
                    }
                    ready = admin.tableExists(tableName) && admin.isTableAvailable(tableName);
                } catch (IOException ignored) {
                    // The master is still coming up; the bounded retry below is the readiness check.
                }
                if (!ready) Thread.sleep(2000);
            }
            if (!ready) throw new IllegalStateException("HBase did not become ready within 120 seconds");
            try (Table table = connection.getTable(tableName)) {
                for (Map.Entry<String, Integer> row : aggregates.entrySet()) {
                    byte[] key = Bytes.toBytes(row.getKey());
                    table.put(new Put(key).addColumn(FAMILY, COUNT, Bytes.toBytes(row.getValue())));
                    Result result = table.get(new Get(key));
                    int found = Bytes.toInt(result.getValue(FAMILY, COUNT));
                    if (found != row.getValue()) throw new IllegalStateException("HBase readback mismatch for " + row.getKey());
                    System.out.println("READBACK row=" + row.getKey() + " count=" + found);
                }
            }
        }
    }
}
