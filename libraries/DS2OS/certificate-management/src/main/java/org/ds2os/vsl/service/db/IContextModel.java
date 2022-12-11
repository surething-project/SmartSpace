package org.ds2os.vsl.service.db;

public interface IContextModel {
    byte[] getContent() throws Exception;

    String getPath();
}
