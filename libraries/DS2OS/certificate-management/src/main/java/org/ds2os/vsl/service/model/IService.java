package org.ds2os.vsl.service.model;

public interface IService extends IEntity {
    String getServiceId();

    boolean isRunning();

    void start();

    void stop();
}
