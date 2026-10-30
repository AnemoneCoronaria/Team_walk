package kr.sanchaek.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import kr.sanchaek.common.ApiException;

@Service
public class GpxService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> GPX_NAMESPACES = Set.of(
            "http://www.topografix.com/GPX/1/0", "http://www.topografix.com/GPX/1/1");
    private static final int MIN_POINTS = 20;
    private static final double MIN_DISTANCE_METERS = 300;

    public record Result(double distanceKm, int durationMin, int pointCount, String area,
            List<List<Double>> path, List<Double> start, List<Double> end) {}

    public Result parse(MultipartFile file) {
        validateFile(file);
        try {
            DocumentBuilderFactory factory = secureDocumentFactory();
            Element root = factory.newDocumentBuilder().parse(file.getInputStream()).getDocumentElement();
            String namespace = root.getNamespaceURI();
            if (!"gpx".equals(root.getLocalName()) || (namespace != null && !GPX_NAMESPACES.contains(namespace))) {
                throw invalid();
            }
            NodeList points = root.getElementsByTagNameNS(namespace == null ? "" : namespace, "trkpt");
            return parsePoints(points);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw invalid();
        }
    }

    private static void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(400, "TOO_LARGE", "파일은 5MB 이하여야 해요.");
        }
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".gpx")) {
            throw invalid();
        }
    }

    private static DocumentBuilderFactory secureDocumentFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private static Result parsePoints(NodeList points) {
        List<List<Double>> path = new ArrayList<>();
        double distance = 0;
        Node previousSegment = null;
        int pointCount = points.getLength();
        for (int i = 0; i < pointCount; i++) {
            Element point = (Element) points.item(i);
            if (!(point.getParentNode() instanceof Element segment) || !"trkseg".equals(segment.getLocalName())) {
                throw invalid();
            }
            double lat = Double.parseDouble(point.getAttribute("lat"));
            double lon = Double.parseDouble(point.getAttribute("lon"));
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                throw invalid();
            }
            if (!path.isEmpty() && previousSegment == segment) {
                List<Double> prev = path.get(path.size() - 1);
                distance += Geo.meters(prev.get(0), prev.get(1), lat, lon);
            }
            path.add(List.of(lat, lon));
            previousSegment = segment;
        }
        if (path.isEmpty()) {
            throw invalid();
        }
        if (path.size() < MIN_POINTS || distance < MIN_DISTANCE_METERS) {
            throw new ApiException(400, "TOO_SHORT", "경로는 300m 이상이고 위치 점은 20개 이상이어야 해요.");
        }
        List<Double> start = path.get(0);
        return new Result(Math.round(distance / 10) / 100.0, (int) Math.round(distance / 1000 * 15), path.size(),
                Geo.area(start.get(0), start.get(1)), path, start, path.get(path.size() - 1));
    }

    private static ApiException invalid() {
        return new ApiException(400, "INVALID_FORMAT", "GPX 파일과 좌표를 확인해 주세요.");
    }
}
