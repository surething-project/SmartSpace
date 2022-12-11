package org.ds2os.vsl.service.db;

public interface IAccessRight {
    String getDescription();

    String getName();

    boolean isRequired();
}
