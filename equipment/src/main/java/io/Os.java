package io;


public interface Os {
    String getenv(String name);
    void setenv(String name, String value, boolean overwrite) throws Exception;
}