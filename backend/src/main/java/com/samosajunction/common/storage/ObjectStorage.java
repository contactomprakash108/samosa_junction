package com.samosajunction.common.storage;

import java.net.URI;
import java.time.Duration;

public interface ObjectStorage {

    void put(String key, byte[] content, String contentType);

    void delete(String key);

    URI presignGet(String key, Duration ttl);
}
