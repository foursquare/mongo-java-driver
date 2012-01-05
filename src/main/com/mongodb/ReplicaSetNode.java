package com.mongodb;

public interface ReplicaSetNode {
    boolean secondary();
    boolean checkTag(String key, String value);
    float getPingTime();
    int getQueueSize();
}
