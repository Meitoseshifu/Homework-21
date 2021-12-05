package ua.happy.learning;

import org.postgresql.ds.PGSimpleDataSource;
import ua.happy.learning.orm.Session;
import ua.happy.learning.orm.SessionFactory;

public class Demo {
    public static void main(String[] args) {
        verifySamePerson();
        //verifyDifferentPerson();
        //verifyUpdatePerson();
    }

    private static void verifySamePerson() {
        Session session = initSession();
        Person person = session.find(Person.class, 1L);
        Person samePerson = session.find(Person.class, 1L);
        System.out.println(person == samePerson);
    }

    private static void verifyDifferentPerson() {
        Session session = initSession();
        Person person = session.find(Person.class, 1L);
        Person anotherPerson = session.find(Person.class, 2L);
        System.out.println(person == anotherPerson);
    }

    private static void verifyUpdatePerson() {
        Session session = initSession();
        Person person = session.find(Person.class, 1L);
        person.setLastName("AnotherLastName");
        session.close();
    }

    private static Session initSession() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");

        SessionFactory factory = new SessionFactory(dataSource);
        return factory.createSession();
    }
}
