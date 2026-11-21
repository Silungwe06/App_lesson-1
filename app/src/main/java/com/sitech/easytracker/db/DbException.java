package com.sitech.easytracker.db;

/** A problem with a message that is safe to show to the user. */
public class DbException extends Exception {
    public DbException(String message) {
        super(message);
    }
}
