package ua.happy.learning;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ua.happy.learning.annotation.Column;
import ua.happy.learning.annotation.Table;

@Table(name = "persons")
@Setter
@Getter
@ToString
public class Person {
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

}
