package kr.sanchaek.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * GPX 파일에서 경로 좌표를 꺼냅니다. (동행 GPX 첨부, 코스 등록, 랜드마크 인증에서 같이 씀)
 *
 * <pre>
 * List&lt;double[]&gt; points = GpxParser.parse(file);    // [위도, 경도] 목록
 * String pathJson = GpxParser.toJson(points);         // "[[37.3527,126.7099],...]" → path_json 칸에 저장
 * </pre>
 *
 * 트랙(trkpt)을 먼저 읽고, 없으면 루트(rtept)를 읽습니다.
 * .gpx 가 아니거나 점이 2개보다 적으면 400 INVALID_FORMAT.
 */
public final class GpxParser {

    private GpxParser() {
    }

    public static List<double[]> parse(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".gpx")) {
            throw new ApiException(400, "INVALID_FORMAT", "GPX 파일(.gpx)만 올릴 수 있어요.");
        }
        try (InputStream in = file.getInputStream()) {
            return parse(in);
        } catch (IOException e) {
            throw new ApiException(400, "INVALID_FORMAT", "GPX 파일을 읽지 못했어요.");
        }
    }

    public static List<double[]> parse(InputStream in) {
        Element root;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 외부 파일을 읽어 오는 XML 공격(XXE) 막기
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);   // 형식 오류를 콘솔에 [Fatal Error] 로 찍지 않게
            root = builder.parse(in).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ApiException(400, "INVALID_FORMAT", "GPX 형식이 아니에요.");
        }

        List<double[]> points = read(root.getElementsByTagName("trkpt"));
        if (points.isEmpty()) {
            points = read(root.getElementsByTagName("rtept"));
        }
        if (points.size() < 2) {
            throw new ApiException(400, "INVALID_FORMAT", "GPX에 경로 좌표가 없어요.");
        }
        return points;
    }

    /** [[위도, 경도], ...] JSON 문자열 (소수점 6자리) */
    public static String toJson(List<double[]> points) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            double[] p = points.get(i);
            sb.append('[').append(round(p[0])).append(',').append(round(p[1])).append(']');
        }
        return sb.append(']').toString();
    }

    private static List<double[]> read(NodeList nodes) {
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            try {
                double lat = Double.parseDouble(el.getAttribute("lat"));
                double lon = Double.parseDouble(el.getAttribute("lon"));
                if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                    points.add(new double[] { lat, lon });
                }
            } catch (NumberFormatException e) {
                // 좌표가 이상한 점은 건너뜀
            }
        }
        return points;
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1_000_000d) / 1_000_000d);
    }
}
