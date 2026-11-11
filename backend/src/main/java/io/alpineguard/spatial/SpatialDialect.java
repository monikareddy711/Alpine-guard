package io.alpineguard.spatial;

/**
 * Database side proximity filter. The radius is expressed in the unit the engine
 * expects: PostGIS works on a geography column and takes metres, H2GIS works on a
 * planar geometry column and takes degrees.
 */
public interface SpatialDialect {

    String withinSql();

    double radiusParameter(double radiusM, double lat);

    String backfillGeometrySql();

    String name();
}
