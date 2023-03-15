package org.example;

import jakarta.xml.bind.annotation.*;

import java.io.Serializable;
import java.util.Objects;

@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.FIELD)
public class Person implements Serializable {
    @XmlElement
    private Long id;
    @XmlElement
    private String name;
    @XmlElement
    private Gender gender;
    @XmlElement
    private Integer age;
    @XmlElement
    private String address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        String ageStr = age.toString();
        if (age < 0) {
            ageStr = "Без возраста";
        }
        return "Person{id=" + id + " | name='" + name + " | gender=" + gender + " | age=" + ageStr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(id, person.id) && Objects.equals(name, person.name) && gender == person.gender && Objects.equals(age, person.age) && Objects.equals(address, person.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, gender, age, address);
    }
}
