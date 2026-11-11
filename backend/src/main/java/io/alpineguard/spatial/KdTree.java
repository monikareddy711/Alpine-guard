package io.alpineguard.spatial;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable two dimensional k-d tree over station coordinates. Points are projected
 * to a local metric plane first, because a degree of longitude is only about 0.68 of a
 * degree of latitude at 47 N and a tree built on raw degrees would return skewed
 * neighbours.
 */
public final class KdTree {

    private static final double METERS_PER_DEGREE_LAT = 111132.0;
    private static final double METERS_PER_DEGREE_LON = 111320.0;

    private final double[] xs;
    private final double[] ys;
    private final int[] order;
    private final double referenceLat;
    private final Node root;

    public KdTree(double[] lats, double[] lons) {
        int n = lats.length;
        this.xs = new double[n];
        this.ys = new double[n];
        double mean = 0;
        for (double lat : lats) {
            mean += lat;
        }
        this.referenceLat = n == 0 ? 46.7 : mean / n;
        double lonScale = METERS_PER_DEGREE_LON * Math.cos(Math.toRadians(referenceLat));
        for (int i = 0; i < n; i++) {
            xs[i] = lons[i] * lonScale;
            ys[i] = lats[i] * METERS_PER_DEGREE_LAT;
        }
        this.order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        this.root = n == 0 ? null : build(0, n - 1, 0);
    }

    public int size() {
        return xs.length;
    }

    private static final class Node {
        private final int item;
        private final int axis;
        private final Node left;
        private final Node right;

        private Node(int item, int axis, Node left, Node right) {
            this.item = item;
            this.axis = axis;
            this.left = left;
            this.right = right;
        }
    }

    private double coord(int item, int axis) {
        return axis == 0 ? xs[item] : ys[item];
    }

    private Node build(int lo, int hi, int axis) {
        if (lo > hi) {
            return null;
        }
        sortRange(lo, hi, axis);
        int mid = (lo + hi) >>> 1;
        int item = order[mid];
        return new Node(item, axis,
                build(lo, mid - 1, 1 - axis),
                build(mid + 1, hi, 1 - axis));
    }

    /** In place Hoare quicksort of the index slice on one axis. */
    private void sortRange(int lo, int hi, int axis) {
        int i = lo;
        int j = hi;
        double pivot = coord(order[(lo + hi) >>> 1], axis);
        while (i <= j) {
            while (coord(order[i], axis) < pivot) {
                i++;
            }
            while (coord(order[j], axis) > pivot) {
                j--;
            }
            if (i <= j) {
                int tmp = order[i];
                order[i] = order[j];
                order[j] = tmp;
                i++;
                j--;
            }
        }
        if (lo < j) {
            sortRange(lo, j, axis);
        }
        if (i < hi) {
            sortRange(i, hi, axis);
        }
    }

    public int nearest(double lat, double lon) {
        if (root == null) {
            return -1;
        }
        double px = projectX(lon);
        double py = projectY(lat);
        int[] best = {root.item};
        double[] bestDist = {squared(px, py, root.item)};
        nearest(root, px, py, best, bestDist);
        return best[0];
    }

    private void nearest(Node node, double px, double py, int[] best, double[] bestDist) {
        if (node == null) {
            return;
        }
        double d = squared(px, py, node.item);
        if (d < bestDist[0]) {
            bestDist[0] = d;
            best[0] = node.item;
        }
        double delta = (node.axis == 0 ? px : py) - coord(node.item, node.axis);
        Node nearSide = delta < 0 ? node.left : node.right;
        Node farSide = delta < 0 ? node.right : node.left;
        nearest(nearSide, px, py, best, bestDist);
        if (delta * delta < bestDist[0]) {
            nearest(farSide, px, py, best, bestDist);
        }
    }

    public List<Integer> within(double lat, double lon, double radiusM) {
        List<Integer> hits = new ArrayList<>();
        if (root == null) {
            return hits;
        }
        double px = projectX(lon);
        double py = projectY(lat);
        collect(root, px, py, radiusM * radiusM, hits);
        return hits;
    }

    private void collect(Node node, double px, double py, double radiusSq, List<Integer> hits) {
        if (node == null) {
            return;
        }
        if (squared(px, py, node.item) <= radiusSq) {
            hits.add(node.item);
        }
        double delta = (node.axis == 0 ? px : py) - coord(node.item, node.axis);
        Node nearSide = delta < 0 ? node.left : node.right;
        Node farSide = delta < 0 ? node.right : node.left;
        collect(nearSide, px, py, radiusSq, hits);
        if (delta * delta <= radiusSq) {
            collect(farSide, px, py, radiusSq, hits);
        }
    }

    private double projectX(double lon) {
        return lon * METERS_PER_DEGREE_LON * Math.cos(Math.toRadians(referenceLat));
    }

    private double projectY(double lat) {
        return lat * METERS_PER_DEGREE_LAT;
    }

    private double squared(double px, double py, int item) {
        double dx = px - xs[item];
        double dy = py - ys[item];
        return dx * dx + dy * dy;
    }
}
