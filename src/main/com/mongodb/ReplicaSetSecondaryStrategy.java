package com.mongodb;

import java.util.List;


public interface ReplicaSetSecondaryStrategy {
    public <T extends ReplicaSetNode> T select(String key, String value, List<T> nodes);
}
