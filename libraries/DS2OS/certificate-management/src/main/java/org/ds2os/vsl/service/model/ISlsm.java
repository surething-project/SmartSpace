package org.ds2os.vsl.service.model;

import java.util.List;

public interface ISlsm {
    void addNode(INode paramINode);

    INode getNode(String paramString);

    List<INode> getNodes();

    String requestCertificate(String paramString1, String paramString2) throws Exception;
}
