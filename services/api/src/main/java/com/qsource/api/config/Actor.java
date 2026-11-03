package com.qsource.api.config;

import com.qsource.api.domain.RevisionSource;

public record Actor(String name, RevisionSource source) {
    public static Actor anonymous() {
        return new Actor("anonymous", RevisionSource.API);
    }
}
