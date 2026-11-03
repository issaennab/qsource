package com.coord.api.config;

public final class ActorContext {
    private static final ThreadLocal<Actor> HOLDER = new ThreadLocal<>();

    private ActorContext() {}

    public static void set(Actor actor) {
        HOLDER.set(actor);
    }

    public static Actor get() {
        Actor actor = HOLDER.get();
        return actor != null ? actor : Actor.anonymous();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
