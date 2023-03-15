package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
    private static final Comparator<Person> comparatorPerson =
            Comparator.comparing(Person::getGender).thenComparing(Person::getAge).thenComparing(Person::getId);
    private static final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();


    public static void main(String[] args) throws XMLStreamException, JAXBException, IOException {
        loadProperties();
        List<Path> pathList = getPathsOfFiles(nameDirInput);
        for (Path path : pathList) {
            Map<Gender, Map<Integer, Long>> collect = getPersonsFromXml(path, comparatorPerson).stream()
                    .collect(Collectors.groupingBy(Person::getGender,
                            Collectors.groupingBy(
                                    person -> getNearestSmaller(age, person.getAge()), Collectors.counting())));
            List<Result> results = getResult(collect);
            save(results, nameDirOutput, path.getFileName().toString());
        }
    }

    public static List<Result> getResult(Map<Gender, Map<Integer, Long>> collect) {
        List<Result> results = new ArrayList<>();
        collect.forEach((gender, integerListMap) ->
                integerListMap.forEach((groupAge, count) -> {
                    Result result = new Result();
                    result.setGender(gender);
                    result.setCount(count);
                    String ageStr;
                    if (groupAge >= 0) {
                        try {
                            ageStr = groupAge + "-" + (age.higher(groupAge) - 1);
                        } catch (NullPointerException e) {
                            ageStr = groupAge + "...";
                        }
                    } else {
                        ageStr = "Без возраста";
                    }
                    result.setAge(ageStr);
                    results.add(result);
                }));
        return results;
    }

    public static Integer getNearestSmaller(TreeSet<Integer> set, int value) {
        Iterator<Integer> iterator = set.descendingIterator();
        int result = iterator.next();
        while (result > value) {
            result = iterator.next();
        }
        return result;
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

    /**
     * Метод читает XML файл и отдает сортированный список
     *
     * @param path       путь к файлу
     * @param comparator Компаратор для сортировки
     * @return отдает сортированный список Person
     */
    public static TreeSet<Person> getPersonsFromXml(Path path, Comparator<Person> comparator) throws JAXBException, XMLStreamException {
        JAXBContext jc = JAXBContext.newInstance(Person.class);
        XMLInputFactory xif = XMLInputFactory.newFactory();
        StreamSource xml = new StreamSource(path.toString());
        XMLStreamReader xsr = xif.createXMLStreamReader(xml);
        TreeSet<Person> persons = new TreeSet<>(comparator);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        while (xsr.getEventType() != XMLStreamReader.END_DOCUMENT) {
            if (xsr.isStartElement() && "person".equals(xsr.getLocalName())) {
                Person person = (Person) unmarshaller.unmarshal(xsr);
                if (person.getAge() == null) {
                    person.setAge(-1);
                }
                if (person.getGender() == null) {
                    person.setGender(Gender.Absent);
                }
                persons.add(person);
            }
            xsr.next();
        }
        return persons;
    }
}