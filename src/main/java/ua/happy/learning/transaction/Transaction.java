package ua.happy.learning.transaction;

public interface Transaction {

    void begin();

    void commit();

    void rollback();

}
