package com.github.alura.groups;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Main {

    public static final String STRING_PATTERN = "(?:\s*(?:\"([^\"]*)\"|([^,]+))\s*,?)+?";
    public static final String OUTPUT_TXT = "output.txt";

    public static void main(String[] args) {
        long startTime = System.nanoTime();

        if (args.length == 0) {
            System.out.println("Please provide the input file as an argument when running the program.");
            System.out.println("Usage: java -jar groups-1.0-SNAPSHOT.jar <input_file>");
            return;
        }

        String inputFile = args[0];

        Set<String> uniqueLines;
        try {
            uniqueLines = getUniqueLines(inputFile);
        } catch (IOException e) {
            System.err.println("Error while reading the file: " + e.getMessage());
            return;
        }

        List<List<String>> groups = groupLines(uniqueLines);


        try {
            writeGroupsToFile(groups);
        } catch (IOException e) {
            System.err.println("Error while writing to the file " + e.getMessage());
            return;
        }

        long endTime = System.nanoTime();
        double timeElapsedSeconds = (endTime - startTime) / 1e9;
        System.out.println("Number of groups with more than one element: " + groups.size());
        System.out.printf("Total execution time: %.6f seconds%n", timeElapsedSeconds);
    }

    private static Set<String> getUniqueLines(String inputFile) throws IOException {
        Set<String> uniqueLines = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.matches(STRING_PATTERN)) {
                    String newLine = line.replace("\"", "");
                    uniqueLines.add(newLine);
                }
            }
        }
        return uniqueLines;
    }

    private static void writeGroupsToFile(List<List<String>> groups) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(OUTPUT_TXT))) {
            AtomicInteger groupCount = new AtomicInteger(0);
            groups.forEach(group -> {
                pw.println("Group " + groupCount.incrementAndGet());
                group.forEach(pw::println);
            });
        }
    }

    public static List<List<String>> groupLines(Set<String> lines) {
        Map<Integer, Map<String, Integer>> valuesCharacteristics = new HashMap<>();
        List<List<String>> groupsList = new ArrayList<>();
        Map<Integer, Integer> mergedGroups = new HashMap<>();

        for (String line : lines) {
            String[] values = line.split(";");
            Set<Integer> groupNumbers = new HashSet<>();
            Map<Integer, String> newValues = new HashMap<>();
            for (int i = 0; i < values.length; i++) {
                if (values[i].isEmpty())
                    continue;

                if (valuesCharacteristics.containsKey(i)
                        && valuesCharacteristics.get(i).get(values[i]) != null ) {
                    int group = getGroup(valuesCharacteristics, mergedGroups, values, i);
                    groupNumbers.add(group);
                } else {
                    newValues.put(i, values[i]);
                }
            }

            Integer currentLineGroup = getLineGroupNumber(groupsList, line, groupNumbers);

            if (groupNumbers.size() > 1) {
                groupNumbers.forEach(number -> mergedGroups.put(number, currentLineGroup));
            }
            updateValueCharacteristics(valuesCharacteristics, newValues, currentLineGroup);
        }

        return groupsList
                .stream()
                .filter(Objects::nonNull)
                .filter(x -> x.size() > 1)
                .sorted((g1, g2) -> g2.size() - g1.size())
                .collect(Collectors.toList());
    }

    private static int getGroup(Map<Integer, Map<String, Integer>> valuesCharacteristics, Map<Integer, Integer> mergedGroups, String[] words, int i) {
        int group = valuesCharacteristics.get(i).get(words[i]);
        while (mergedGroups.containsKey(group)) {
            group = mergedGroups.get(group);
        }
        return group;
    }

    private static Integer getLineGroupNumber(List<List<String>> groupsList, String line, Set<Integer> groupNumbers) {
        Integer currentLineGroup;
        if (!groupNumbers.isEmpty()) {
            if (groupNumbers.size() > 1) {
                currentLineGroup = mergeGroups(groupsList, line, groupNumbers);
            } else {
                currentLineGroup = addLineToGroup(groupsList, line, groupNumbers);
            }
        } else {
            currentLineGroup = createNewGroup(groupsList, line);
        }
        return currentLineGroup;
    }

    private static void updateValueCharacteristics(Map<Integer, Map<String, Integer>> valuesCharacteristics,
                                                   Map<Integer, String> newValues,
                                                   Integer currentGroupNumber) {
        newValues.keySet()
                .forEach(value -> {
                    valuesCharacteristics
                            .computeIfAbsent(value, k -> new HashMap<>())
                            .put(newValues.get(value), currentGroupNumber);
                });
    }

    private static Integer addLineToGroup(List<List<String>> groupsList,
                                          String line,
                                          Set<Integer> groupNumbers) {
        Integer group = groupNumbers.iterator().next();
        groupsList.get(group).add(line);
        return group;
    }

    private static Integer createNewGroup(List<List<String>> groupsList,
                                          String line) {
        List<String> newGroup = new ArrayList<>();
        newGroup.add(line);
        groupsList.add(newGroup);
        return groupsList.size() - 1;
    }

    private static Integer mergeGroups(List<List<String>> groupsList,
                                       String line,
                                       Set<Integer> groupNumbers) {
        List<String> mergedGroup = new ArrayList<>();
        for (Integer group : groupNumbers) {
            List<String> groupLines = groupsList.set(group, null);
            mergedGroup.addAll(groupLines);
        }
        mergedGroup.add(line);
        groupsList.add(mergedGroup);
        return groupsList.size() - 1;
    }
}