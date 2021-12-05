package ua.happy.learning.orm;

public record EntityKey<T>(Class<T> type, Object id) {
}
