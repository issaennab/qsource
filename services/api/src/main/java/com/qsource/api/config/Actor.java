package com.qsource.api.config;

import com.qsource.api.domain.RevisionSource;
import java.util.UUID;

public record Actor(String name, RevisionSource source, UUID userId) {
    public static Actor anonymous() {
        return new Actor("anonymous", RevisionSource.API, null);
    }
}
