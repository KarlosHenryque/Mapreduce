package br.edu.netflix;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetflixReducer extends Reducer<Text, Text, NullWritable, Text> {
    private static final String DESCRIPTION_PREFIX = "DESCRIPTION|";
    private static final String WORD_PREFIX = "WORD|";
    private static final String TOTAL_KEY = "TOTAL";

    private final Map<String, Long> wordFrequency = new HashMap<>();
    private long totalWords = 0L;
    private long highestWords = 0L;
    private long lowestWords = Long.MAX_VALUE;
    private String highestTitle = "N/A";
    private String lowestTitle = "N/A";

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        String keyString = key.toString();
        long totalForKey = 0L;

        for (Text value : values) {
            totalForKey += Long.parseLong(value.toString());
        }

        if (keyString.startsWith(DESCRIPTION_PREFIX)) {
            String title = keyString.substring(DESCRIPTION_PREFIX.length());
            if (totalForKey > highestWords) {
                highestWords = totalForKey;
                highestTitle = title;
            }
            if (totalForKey < lowestWords) {
                lowestWords = totalForKey;
                lowestTitle = title;
            }
        } else if (TOTAL_KEY.equals(keyString)) {
            totalWords += totalForKey;
        } else if (keyString.startsWith(WORD_PREFIX)) {
            String word = keyString.substring(WORD_PREFIX.length());
            wordFrequency.put(word, wordFrequency.getOrDefault(word, 0L) + totalForKey);
        }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        emitLine(context, "", "--- RESULTADO ---");
        emitLine(context, "", "");

        emitLine(context, "TITULO_MAIOR_DESCRICAO", "Título: " + highestTitle);
        emitLine(context, "", "");
        emitLine(context, "TITULO_MAIOR_DESCRICAO", "Palavras: " + highestWords);
        emitLine(context, "", "");

        emitLine(context, "TITULO_MENOR_DESCRICAO", "Título: " + lowestTitle);
        emitLine(context, "", "");
        emitLine(context, "TITULO_MENOR_DESCRICAO", "Palavras: " + lowestWords);
        emitLine(context, "", "");

        emitTopWords(context, "TOP_5_MAIS_FREQUENTES", getSortedWords(false));
        emitTopWords(context, "TOP_5_MENOS_FREQUENTES", getSortedWords(true));

        emitLine(context, "TOTAL_PALAVRAS", String.valueOf(totalWords));
    }

    private void emitLine(Context context, String key, String value) throws IOException, InterruptedException {
        String line = value.isEmpty() ? key : key.isEmpty() ? value : key + " " + value;
        context.write(NullWritable.get(), new Text(line));
    }

    private void emitTopWords(Context context, String label, List<Map.Entry<String, Long>> entries)
            throws IOException, InterruptedException {
        emitLine(context, label, "");

        int limit = Math.min(entries.size(), 5);
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Long> entry = entries.get(i);
            String value = (i + 1) + ". " + entry.getKey() + " = " + entry.getValue();
            emitLine(context, "", value);
        }
        emitLine(context, "", "");
    }

    private List<Map.Entry<String, Long>> getSortedWords(boolean ascending) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(wordFrequency.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<String, Long>>() {
            @Override
            public int compare(Map.Entry<String, Long> first, Map.Entry<String, Long> second) {
                int countComparison = Long.compare(first.getValue(), second.getValue());
                if (countComparison != 0) {
                    return ascending ? countComparison : -countComparison;
                }
                return first.getKey().compareTo(second.getKey());
            }
        });

        return entries;
    }
}
