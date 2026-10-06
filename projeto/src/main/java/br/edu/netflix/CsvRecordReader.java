package br.edu.netflix;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.LineReader;

import java.io.IOException;

public class CsvRecordReader extends RecordReader<LongWritable, Text> {

    private LineReader reader;
    private long start;
    private long end;
    private long position;
    private final LongWritable currentKey = new LongWritable();
    private final Text currentValue = new Text();

    @Override
    public void initialize(InputSplit split, TaskAttemptContext context) throws IOException {
        FileSplit fileSplit = (FileSplit) split;
        Path path = fileSplit.getPath();
        Configuration configuration = context.getConfiguration();
        FileSystem fileSystem = path.getFileSystem(configuration);
        FSDataInputStream input = fileSystem.open(path);

        reader = new LineReader(input, configuration);
        start = fileSplit.getStart();
        end = start + fileSplit.getLength();
        position = start;
    }

    @Override
    public boolean nextKeyValue() throws IOException {
        if (reader == null) {
            return false;
        }

        StringBuilder record = new StringBuilder();
        boolean inQuotes = false;
        boolean readAnyLine = false;
        boolean firstPhysicalLine = true;
        long recordStart = position;

        while (true) {
            Text physicalLine = new Text();
            int bytesRead = reader.readLine(physicalLine);

            if (bytesRead == 0) {
                break;
            }

            readAnyLine = true;
            position += bytesRead;

            if (!firstPhysicalLine) {
                record.append('\n');
            }
            record.append(physicalLine.toString());
            firstPhysicalLine = false;

            inQuotes = updateQuoteState(physicalLine.toString(), inQuotes);
            if (!inQuotes) {
                break;
            }
        }

        if (!readAnyLine) {
            return false;
        }

        currentKey.set(recordStart);
        currentValue.set(record.toString());
        return true;
    }

    private boolean updateQuoteState(String line, boolean inQuotes) {
        for (int index = 0; index < line.length(); index++) {
            if (line.charAt(index) != '"') {
                continue;
            }

            if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                index++;
            } else {
                inQuotes = !inQuotes;
            }
        }

        return inQuotes;
    }

    @Override
    public LongWritable getCurrentKey() {
        return currentKey;
    }

    @Override
    public Text getCurrentValue() {
        return currentValue;
    }

    @Override
    public float getProgress() {
        if (start == end) {
            return 1.0f;
        }

        return Math.min(1.0f, (position - start) / (float) (end - start));
    }

    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}
