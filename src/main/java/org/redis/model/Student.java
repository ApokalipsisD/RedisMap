package org.redis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    private String name;
    private int age;
    private List<String> skills;

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Student student = (Student) object;
        return age == student.age
                && Objects.equals(name, student.name)
                && Objects.equals(skills, student.skills);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, skills);
    }
}
