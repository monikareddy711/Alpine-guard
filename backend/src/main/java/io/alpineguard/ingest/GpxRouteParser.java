package io.alpineguard.ingest;

import io.alpineguard.domain.GeoMath;
import io.alpineguard.domain.RoutePoint;
import io.alpineguard.domain.SkiRoute;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class GpxRouteParser {

    public record RawPoint(double lat, double lon, double elevationM) {
    }

    public List<RawPoint> readTrackPoints(InputStream in) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);
            NodeList trkpts = document.getElementsByTagName("trkpt");
            List<RawPoint> points = new ArrayList<>(trkpts.getLength());
            for (int i = 0; i < trkpts.getLength(); i++) {
                Element trkpt = (Element) trkpts.item(i);
                double lat = Double.parseDouble(trkpt.getAttribute("lat"));
                double lon = Double.parseDouble(trkpt.getAttribute("lon"));
                NodeList ele = trkpt.getElementsByTagName("ele");
                double elevation = ele.getLength() > 0 ? Double.parseDouble(ele.item(0).getTextContent().trim()) : 0.0;
                points.add(new RawPoint(lat, lon, elevation));
            }
            return points;
        } catch (IOException | ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new IllegalArgumentException("unreadable GPX track", e);
        }
    }

    /**
     * Turns raw track points into segments carrying distance from the start, slope angle and
     * aspect. The aspect of a point is taken from the next point in the track so that a
     * northbound ascent on a south facing slope records an aspect near 180 degrees.
     */
    public List<RoutePoint> buildRoutePoints(List<RawPoint> raw) {
        List<RoutePoint> points = new ArrayList<>(raw.size());
        double cumulative = 0.0;
        for (int i = 0; i < raw.size(); i++) {
            RawPoint current = raw.get(i);
            RawPoint next = raw.get(Math.min(i + 1, raw.size() - 1));
            RawPoint previous = raw.get(Math.max(i - 1, 0));
            if (i > 0) {
                cumulative += GeoMath.haversineM(previous.lat(), previous.lon(), current.lat(), current.lon());
            }
            double horizontal = GeoMath.haversineM(current.lat(), current.lon(), next.lat(), next.lon());
            double slope = Math.abs(GeoMath.slopeDegrees(next.elevationM() - current.elevationM(), horizontal));
            double aspect = GeoMath.normalizeDegrees(
                    GeoMath.bearingDeg(current.lat(), current.lon(), next.lat(), next.lon()) + 180.0);
            points.add(new RoutePoint(i, GeoMath.round(current.lat(), 6), GeoMath.round(current.lon(), 6),
                    GeoMath.round(current.elevationM(), 1), GeoMath.round(cumulative, 1),
                    GeoMath.round(slope, 2), GeoMath.round(aspect, 1)));
        }
        return points;
    }

    public SkiRoute toRoute(String slug, String name, String region, String difficulty,
                            String gpxResource, List<RoutePoint> points) {
        double distance = points.isEmpty() ? 0.0 : points.get(points.size() - 1).distanceM();
        double ascent = 0.0;
        double descent = 0.0;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < points.size(); i++) {
            double elevation = points.get(i).elevationM();
            max = Math.max(max, elevation);
            min = Math.min(min, elevation);
            if (i > 0) {
                double delta = elevation - points.get(i - 1).elevationM();
                if (delta > 0) {
                    ascent += delta;
                } else {
                    descent -= delta;
                }
            }
        }
        return new SkiRoute(null, slug, name, region, difficulty, GeoMath.round(distance, 1),
                GeoMath.round(ascent, 1), GeoMath.round(descent, 1),
                GeoMath.round(points.isEmpty() ? 0 : max, 1), GeoMath.round(points.isEmpty() ? 0 : min, 1),
                gpxResource, points);
    }
}
