package org.ds2os.vsl.service.model;

public enum InstallStatus {
    OK(1),
    ERROR(0);

    private final int code;

    InstallStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
