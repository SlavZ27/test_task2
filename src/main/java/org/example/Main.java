package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final Logger logger = Logger.getLogger(Main.class.toString());


    public static void main(String[] args) {
        loadProperties();
        List<Path> pathList = getPathsOfFiles(nameDirInput);
        for (Path path : pathList) {
            //the first way
            save(parseFile(path), nameDirOutput, path.getFileName().toString());
            //the second way. memory saving
            save(parseFile2(path), nameDirOutput, path.getFileName().toString());
        }
    }

    public static List<Result> parseFile2(Path filePath) {
        Map<String, Long> resultMap = new HashMap<>();
        for (Gender gender : Gender.values()) {
            resultMap.put(getKeyMap(gender, getGroupAge(null)), 0L);
            for (Integer age1 : age) {
                resultMap.put(getKeyMap(gender, getGroupAge(age1)), 0L);
            }
        }
        JAXBContext jc;
        try {
            jc = JAXBContext.newInstance(Person.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        XMLInputFactory xif = XMLInputFactory.newFactory();
        try {
            StreamSource xml = new StreamSource(filePath.toString());
            XMLStreamReader xsr = xif.createXMLStreamReader(xml);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            while (xsr.getEventType() != XMLStreamReader.END_DOCUMENT) {
                if (xsr.isStartElement() && "person".equals(xsr.getLocalName())) {
                    Person person = (Person) unmarshaller.unmarshal(xsr);
                    Gender gender;
                    if (person.getGender() == null) {
                        gender = Gender.Absent;
                    } else {
                        gender = person.getGender();
                    }
                    String groupAge = getGroupAge(person.getAge());
                    String keyMap = getKeyMap(gender, groupAge);
                    Long count = resultMap.get(keyMap) + 1;
                    resultMap.put(keyMap, count);
                }
                xsr.next();
            }
            return getResult2(resultMap);
        } catch (XMLStreamException | JAXBException e) {
            throw new RuntimeException("file reading error");
        }
    }

    /**
     * parsing the file by the jackson library. all objects instance
     * @return List of Result
     */
    public static List<Result> parseFile(Path filePath) {
        Map<Gender, Map<String, Long>> collect = getPersonsFromXml(filePath).stream()
                .collect(Collectors.groupingBy(
                        person -> Objects.requireNonNullElse(person.getGender(), Gender.Absent),
                        Collectors.groupingBy(
                                person -> getGroupAge(person.getAge()), Collectors.counting())));
        return getResult(collect);
    }

    /**
     * @return a string to define the desired hashmap
     */
    public static String getKeyMap(Gender gender, String groupAge) {
        return gender.name() + groupAge;
    }

    /**
     * Convert map to Result
     * @return list of Results
     */
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

    /**
     * Convert map to Result
     * @return list of Results
     */
    public static List<Result> getResult2(Map<String, Long> resultMap) {
        List<Result> results = new ArrayList<>();
        for (Gender gender : Gender.values()) {
            Result result = createResult(resultMap, gender, null);
            if (result != null) {
                results.add(result);
            }
            for (Integer age1 : age) {
                result = createResult(resultMap, gender, age1);
                if (result != null) {
                    results.add(result);
                }
            }
        }
        return results;
    }

    /**
     * Create Result with parameters
     * @return new Result
     */
    public static Result createResult(Map<String, Long> resultMap, Gender gender, Integer age) {
        String keyMap = getKeyMap(gender, getGroupAge(age));
        Long count = resultMap.get(keyMap);
        if (count != null && count > 0) {
            Result result = new Result();
            result.setGender(gender);
            result.setCount(count);
            result.setAge(getGroupAge(age));
            return result;
        }
        return null;
    }

    /**
     * @return string designation of the age group
     */
    public static String getGroupAge(Integer value) {
        if (value == null) {
            return "Absent";
        } else {
            Iterator<Integer> iterator = age.descendingIterator();
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
    public static void save(List<Result> data, String nameDir, String nameFile) {
        File directory = new File(nameDir);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                logger.log(Level.WARNING, "dir don't write = " + nameDir);
                return;
            }
        }
        String ext = ".json";
        if (nameFile.contains(".")) {
            nameFile = nameFile.substring(0, nameFile.lastIndexOf('.'));
        }
        int count = 1;
        Path of = Path.of(nameDir);
        Path path = of.resolve(nameFile + "_" + count++ + ext);
        while (Files.exists(path)) {
            path = of.resolve(nameFile + "_" + count++ + ext);
        }
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        try {
            Files.write(path, objectWriter.writeValueAsBytes(data));
            logger.log(Level.INFO, "result saved to = " + path);

        } catch (IOException e) {
            logger.log(Level.WARNING, "file don't write = " + path);
        }
    }

    /**
     * Метод ищет файл настроек и переопределяет переменные
     */
    public static void loadProperties() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);
            nameDirInput = (String) props.getOrDefault("nameDirInput", nameDirInput);
            nameDirOutput = (String) props.getOrDefault("nameDirOutput", nameDirOutput);
            String ageStr = (String) props.get("age");
            if (ageStr != null) {
                age = Arrays.stream(ageStr.replace(" ", "").split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toCollection(TreeSet::new));
                age.add(0);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "File config not found = " + configFile);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "File config reading error = " + configFile);
        }
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
        List<Path> pathList = Arrays.stream(files)
                .map(file -> Path.of(pathDir + "/" + file.getName()))
                .collect(Collectors.toList());
        logger.log(Level.INFO, "paths found = " + pathList.size());
        return pathList;
    }


    /**
     * getting all objects Person at once from XML
     * @return List of Persons
     */
    public static List<Person> getPersonsFromXml(Path path) {
        try {
            File file = new File(String.valueOf(path));
            XmlMapper xmlMapper = new XmlMapper();
            List<Person> personList = xmlMapper.readValue(file, new TypeReference<>() {
            });
            logger.log(Level.INFO, "persons found = " + personList.size());
            return personList;
        } catch (IOException e) {
            logger.log(Level.WARNING, "file don't read = " + path);
            throw new RuntimeException();
        }
    }
}