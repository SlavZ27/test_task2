package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    /**
     * Имя файла конфигурации
     */
    private static final String configFile = "config.properties";
    /**
     * Путь к папке с файлом с исходными данными
     */
    private static String nameDirInput = "src/input";
    /**
     * Путь к папке, куда запишется файл с результатом
     */
    private static String nameDirOutput = "src/output";
    /**
     * Сет возрастов для группировки
     */
    private static TreeSet<Integer> age = new TreeSet<>(List.of(20, 40, 60, 80));

    /**
     * Компаратор для класса Person
     */


    public static void main(String[] args) throws IOException {
        loadProperties();
        List<Path> pathList = getPathsOfFiles(nameDirInput);
        for (Path path : pathList) {
            Map<Gender, Map<String, Long>> collect = getPersonsFromXml(path).stream()
                    .collect(Collectors.groupingBy(
                            person -> Objects.requireNonNullElse(person.getGender(), Gender.Absent),
                            Collectors.groupingBy(
                                    person -> getGroupAge(age, person.getAge()), Collectors.counting())));
            List<Result> results = getResult(collect);
            save(results, nameDirOutput, path.getFileName().toString());
        }
    }

    public static List<Result> getResult(Map<Gender, Map<String, Long>> collect) {
        List<Result> results = new ArrayList<>();
        collect.forEach((gender, integerListMap) ->
                integerListMap.forEach((groupAge, count) -> {
                    Result result = new Result();
                    result.setGender(gender);
                    result.setCount(count);
                    result.setAge(groupAge);
                    results.add(result);
                }));
        return results;
    }

    public static String getGroupAge(TreeSet<Integer> set, Integer value) {
        if (value == null) {
            return "Без возраста";
        } else {
            Iterator<Integer> iterator = set.descendingIterator();
            int groupAge = iterator.next();
            while (groupAge > value) {
                groupAge = iterator.next();
            }
            Optional<Integer> optional = Optional.ofNullable(age.higher(groupAge));
            if (optional.isEmpty()) {
                return groupAge + "...";
            } else {
                return groupAge + "-" + (optional.get() - 1);
            }
        }
    }

    /**
     * Метод сохраняет объект в JSON
     *
     * @param data     объект для сохранения
     * @param nameDir  папка для сохранения
     * @param nameFile имя файла для сохранения
     */
    public static void save(List<Result> data, String nameDir, String nameFile) throws IOException {
        File directory = new File(nameDir);
        if (!directory.exists()) {
            directory.mkdir();
        }
        String ext = ".json";
        if (nameFile.contains(".")) {
            nameFile = nameFile.substring(0, nameFile.lastIndexOf('.'));
        }
        int count = 1;
        String path = nameDir + "/" + nameFile + "_" + count++ + ext;
        while (Files.exists(Path.of(path))) {
            path = nameDir + "/" + nameFile + "_" + count++ + ext;
        }
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        Files.write(Path.of(path), objectWriter.writeValueAsBytes(data));
    }

    /**
     * Метод ищет файл настроек и переопределяет переменные
     */
    public static void loadProperties() throws IOException {
        Properties props = new Properties();
        InputStream in = new FileInputStream(configFile);
        props.load(in);
        nameDirInput = (String) props.getOrDefault("nameDirInput", nameDirInput);
        nameDirOutput = (String) props.getOrDefault("nameDirOutput", nameDirOutput);
        String ageStr = (String) props.get("age");
        if (ageStr != null) {
            age = Arrays.stream(ageStr.replace(" ", "").split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toCollection(TreeSet::new));
            age.add(-1);
            age.add(0);
        }
        in.close();
    }

    /**
     * Метод ищет файлы в указанной папке
     *
     * @param pathDir папка для сканирования файлов
     * @return список путей файлов из папки
     */
    public static List<Path> getPathsOfFiles(String pathDir) {
        File dir = new File(pathDir);
        File[] files = dir.listFiles();
        assert files != null;
        return Arrays.stream(files)
                .map(file -> Path.of(pathDir + "/" + file.getName()))
                .collect(Collectors.toList());
    }


    public static List<Person> getPersonsFromXml(Path path) throws IOException {
        File file = new File(String.valueOf(path));
        XmlMapper xmlMapper = new XmlMapper();
        return xmlMapper.readValue(file, new TypeReference<>() {
        });
    }
}