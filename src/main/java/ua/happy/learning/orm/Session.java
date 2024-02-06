package ua.happy.learning.orm;

import lombok.SneakyThrows;
import ua.happy.learning.action.Action;
import ua.happy.learning.annotation.Column;
import ua.happy.learning.annotation.Table;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class Session {

    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id=?";
    private static final String UPDATE_BY_ID = "UPDATE %s SET %s WHERE id=?";

    private final DataSource dataSource;

    private Map<EntityKey<?>, Object> cacheMap = new HashMap<>();
    private Map<EntityKey<?>, Object[]> snapshotMap = new HashMap<>();

    private Queue<Action> actionQueue = new PriorityQueue<>(Comparator.comparingInt(this::sortAction));

    public Session(DataSource dataSource) {
        //todo: open transaction
        this.dataSource = dataSource;
    }

    @SneakyThrows
    public <T> T find(Class<T> type, Object id) {
        EntityKey<T> key = new EntityKey<>(type, id);
        if(cacheMap.containsKey(key)) {
            return type.cast(cacheMap.get(key));
        }
        try(Connection connection = dataSource.getConnection()) {
            String selectQuery = String.format(SELECT_BY_ID, extractTableName(type), key.id());
            try(PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
                preparedStatement.setObject(1, key.id());
                ResultSet resultSet = preparedStatement.executeQuery();
                return type.cast(createEntityFromResultSet(key, resultSet));
            }
        }
    }

    @SneakyThrows
    private <T> T createEntityFromResultSet(EntityKey<T> entityKey, ResultSet resultSet) {
        Class<T> type = entityKey.type();
        Field[] declaredFields = sortFieldsByName(type.getDeclaredFields());
        Object[] snapshotCopies = new Object[declaredFields.length];

        if (resultSet.next()) {
            T entity = type.getConstructor().newInstance();
            for (int i = 0; i < declaredFields.length; i++) {
                Field field = declaredFields[i];
                String columnName = extractColumnName(field);
                field.setAccessible(true);

                Object fieldValue = resultSet.getObject(columnName);
                field.set(entity, fieldValue);
                snapshotCopies[i] = fieldValue;
            }
            cacheMap.put(entityKey, entity);
            snapshotMap.put(entityKey, snapshotCopies);
            return entity;
        }
        throw new RuntimeException("ResultSet is empty");
    }

    public void close() {
        cacheMap.entrySet()
                .stream()
                .filter(this::hasChanged)
                .forEach(this::performUpdate);
        //todo: close transaction
        //todo: refactor dirty checking so it creates UpdateAction and add it to the actionQueue
    }

    private boolean hasChanged(Map.Entry<EntityKey<?>, Object> entityEntry) {
        EntityKey<?> key = entityEntry.getKey();
        if (snapshotMap.containsKey(key)) {
            Object[] snapshotValues = snapshotMap.get(key);
            Object[] entityValues = extractValuesFromEntity(entityEntry.getValue(), new Object[snapshotValues.length]);

            for (int i = 0; i < entityValues.length; i++) {
                if (notEqual(entityValues[i], snapshotValues[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean notEqual(Object entityValue, Object snapshotValue) {
        return entityValue != snapshotValue && entityValue.hashCode() != snapshotValue.hashCode();
    }

    @SneakyThrows
    private Object[] extractValuesFromEntity(Object entity, Object[] entityValues) {
        Field[] declaredFields = sortFieldsByName(entity.getClass().getDeclaredFields());

        for (int i = 0; i < declaredFields.length; i++) {
            Field field = declaredFields[i];
            field.setAccessible(true);
            entityValues[i] = field.get(entity);
        }

        return entityValues;
    }

    private Field[] sortFieldsByName(Field[] declaredFields) {
        return Arrays.stream(declaredFields)
                .sorted(Comparator.comparing(Field::getName))
                .toArray(Field[]::new);
    }

    @SneakyThrows
    private void performUpdate(Map.Entry<EntityKey<?>, Object> entityKeyObjectEntry) {
        String updateQuery = buildUpdateQuery(entityKeyObjectEntry.getKey(), entityKeyObjectEntry.getValue());

        try (Connection connection = dataSource.getConnection()) {
            try(PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                preparedStatement.setObject(1, entityKeyObjectEntry.getKey().id());
                preparedStatement.executeUpdate();
            }
        }
    }

    @SneakyThrows
    private String buildUpdateQuery(EntityKey<?> key, Object entity) {
        Field[] declaredFields = entity.getClass().getDeclaredFields();
        StringBuilder nameValuePairBuilder = new StringBuilder();

        for (int i = 0; i < declaredFields.length; i++) {
            Field field = declaredFields[i];
            field.setAccessible(true);
            String name = extractColumnName(field);
            String value = field.get(entity).toString();

            nameValuePairBuilder.append(name);
            nameValuePairBuilder.append("='");
            nameValuePairBuilder.append(value);
            nameValuePairBuilder.append("'");

            if (notLastNameValuePair(i, declaredFields.length - 1)) {
                nameValuePairBuilder.append(", ");
            }
        }

        return String.format(UPDATE_BY_ID, extractTableName(key.type()), nameValuePairBuilder);
    }

    private boolean notLastNameValuePair(int counter, int arrayLength) {
        return counter != arrayLength;
    }

    private String extractTableName(Class<?> type) {
        return type.getAnnotation(Table.class).name() != null ?
                type.getAnnotation(Table.class).name() :
                type.getSimpleName().toLowerCase();
    }

    private String extractColumnName(Field field) {
        return field.getAnnotation(Column.class).name() != null ?
                field.getAnnotation(Column.class).name() :
                field.getName();
    }

    public void persist(Object entity) {
        String sql = "INSERT INTO Customers (CustomerName, ContactName, Address, City, PostalCode, Country)\n" +
                           "VALUES ('Cardinal', 'Tom B. Erichsen', 'Skagen 21', 'Stavanger', '4006', 'Norway');";
    }

    public void remove(Object entity) {
        String sql = "DELETE FROM Customers WHERE CustomerName='Alfreds Futterkiste';";
    }

    public void flush() {
        //todo go through actionQueue and perform sql requests: first insert, then delete
    }

    private int sortAction(Action action) {
        return switch (action.getClass().getSimpleName()) {
          case "InsertAction" -> 1;
          case "UpdateAction" -> 2;
          case "DeleteAction" -> 3;
            default -> 0;
        };
    }

}
