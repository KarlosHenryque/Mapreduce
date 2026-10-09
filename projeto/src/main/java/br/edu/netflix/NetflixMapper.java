package br.edu.netflix;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NetflixMapper extends Mapper<LongWritable, Text, Text, Text> {

    private static final String DESCRIPTION_PREFIX = "DESCRIPTION|";
    private static final String WORD_PREFIX = "WORD|";
    private static final String TOTAL_KEY = "TOTAL";

    private static final int TITLE_INDEX = 2;
    private static final int DESCRIPTION_INDEX = 11;

    private final Set<String> stopWords = new HashSet<>();

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        loadStopWords();
    }

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        if (value == null) {
            return;
        }

        String line = value.toString();

        if (line.trim().isEmpty()) {
            return;
        }

        if (key.get() == 0 && line.startsWith("show_id,")) {
            return;
        }

        List<String> columns = parseCsvLine(line);

        if (columns.size() <= DESCRIPTION_INDEX) {
            return;
        }

        String title = columns.get(TITLE_INDEX);
        String description = columns.get(DESCRIPTION_INDEX);

        if (title == null || title.trim().isEmpty()) {
            return;
        }

        if (description == null || description.trim().isEmpty()) {
            return;
        }

        String normalizedDescription = normalizeText(description);

        if (normalizedDescription.isEmpty()) {
            return;
        }

        String[] words = normalizedDescription.split("\\s+");
        int descriptionWordCount = 0;

        for (String word : words) {

            String normalizedWord = normalizeWord(word);

            if (normalizedWord.isEmpty()) {
                continue;
            }

            if (stopWords.contains(normalizedWord)) {
                continue;
            }

            descriptionWordCount++;

            context.write(
                    new Text(WORD_PREFIX + normalizedWord),
                    new Text("1")
            );
        }

        if (descriptionWordCount > 0) {

            context.write(
                    new Text(DESCRIPTION_PREFIX + title.trim()),
                    new Text(String.valueOf(descriptionWordCount))
            );

            context.write(
                    new Text(TOTAL_KEY),
                    new Text(String.valueOf(descriptionWordCount))
            );
        }
    }

    private void loadStopWords() throws IOException {

        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("stopwords.txt");

        if (inputStream == null) {
            throw new IOException("Arquivo stopwords.txt não encontrado no classpath.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {

                String word = normalizeWord(line);

                if (!word.isEmpty()) {
                    stopWords.add(word);
                }
            }
        }
    }

    private String normalizeText(String value) {

        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);

        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.toLowerCase(Locale.ROOT);

        normalized = normalized
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u02BC", "'");

        normalized = normalized.replace("'", " ");
        normalized = normalized.replaceAll("[^\\p{L}\\p{Nd}\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    private String normalizeWord(String word) {

        if (word == null) {
            return "";
        }

        String normalized = normalizeText(word);
        normalized = normalized.replaceAll("\\s", "");

        return normalized;
    }

    private List<String> parseCsvLine(String line) {

        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char currentChar = line.charAt(i);

            if (currentChar == '"') {

                if (inQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    currentValue.append('"');
                    i++;

                } else {
                    inQuotes = !inQuotes;
                }

            } else if (currentChar == ',' && !inQuotes) {

                values.add(currentValue.toString());
                currentValue.setLength(0);

            } else {

                currentValue.append(currentChar);
            }
        }

        values.add(currentValue.toString());

        return values;
    }
}