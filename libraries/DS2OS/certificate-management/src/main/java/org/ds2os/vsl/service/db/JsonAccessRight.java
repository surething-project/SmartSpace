package org.ds2os.vsl.service.db;

import javax.validation.constraints.NotNull;

public class JsonAccessRight implements IAccessRight {
    private final String description;
    private final boolean isRequired;
    private final String name;

    public JsonAccessRight(@NotNull String name, @NotNull String description, boolean isRequired) {
        this.isRequired = isRequired;
        this.name = name;
        this.description = description;
    }

    public JsonAccessRight(@NotNull String name, @NotNull String description) {
        this(name, description, true);
    }

    public String getDescription() {
        return this.description;
    }

    public String getName() {
        return this.name;
    }

    public boolean isRequired() {
        return this.isRequired;
    }
}
