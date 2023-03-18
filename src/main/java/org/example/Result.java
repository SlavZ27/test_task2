package org.example;

public class Result {
    private Gender gender;
    private String age;
    private Long count;

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public void countInc() {
        count++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("gender=").append(gender).append(", age='").append(age)
                .append(", count=").append(count).append(", \npersons={");

        sb.append("}");
        return sb.toString();
    }
}
