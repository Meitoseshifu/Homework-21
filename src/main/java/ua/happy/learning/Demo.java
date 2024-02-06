package ua.happy.learning;

import org.postgresql.ds.PGSimpleDataSource;
import ua.happy.learning.action.Action;
import ua.happy.learning.action.DeleteAction;
import ua.happy.learning.action.InsertAction;
import ua.happy.learning.action.UpdateAction;
import ua.happy.learning.orm.Session;
import ua.happy.learning.orm.SessionFactory;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class Demo {
    public static void main(String[] args) {
        Queue<Action> queue = new PriorityQueue<>(5, new Comparator<Action>() {
            @Override
            public int compare(Action o1, Action o2) {
                int result1 = switch (o1.getClass().getSimpleName()) {
                    case "InsertAction" -> 1;
                    case "UpdateAction" -> 2;
                    case "DeleteAction" -> 3;
                    default -> 0;
                };
                int result2 = switch (o2.getClass().getSimpleName()) {
                    case "InsertAction" -> 1;
                    case "UpdateAction" -> 2;
                    case "DeleteAction" -> 3;
                    default -> 0;
                };
                return result1 - result2;
            }
        });
        queue.add(new DeleteAction());
        queue.add(new UpdateAction());
        queue.add(new DeleteAction());
        queue.add(new UpdateAction());
        queue.add(new InsertAction());

        System.out.println("-------------------------------");

        while (!queue.isEmpty()) {
            System.out.println(queue.poll().getClass().getSimpleName());
        }






        //verifySamePerson();
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
