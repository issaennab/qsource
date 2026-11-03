package com.coord.api.config;

import com.coord.api.domain.RevisionSource;

public record Actor(String name, RevisionSource source) {
    public static Actor anonymous() {
        return new Actor("anonymous", RevisionSource.API);
    }
}
