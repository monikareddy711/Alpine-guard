package io.alpineguard.hazard;

public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String slug) {
        super("unknown ski route: " + slug);
    }
}
