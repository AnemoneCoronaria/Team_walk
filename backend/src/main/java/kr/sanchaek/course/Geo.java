package kr.sanchaek.course;

/** 두 좌표 사이 거리 · 지역 계산. 코스 상세의 랜드마크와 랜드마크 인증이 같은 코드를 쓴다 */
public final class Geo {
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private Geo() {}

    public static double meters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return EARTH_RADIUS_METERS * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }

    // 명세에 지역 중심 좌표가 없어 시드 코스의 시작점을 대표점으로 사용한다.
    static String area(double lat, double lon) {
        String[] names = {"OKGU", "JEONGWANG", "BAEGOT", "SIHWA"};
        double[][] centers = {{37.352720,126.709915}, {37.351452,126.742790},
                {37.371410,126.716051}, {37.345543,126.687608}};
        int best = 0;
        for (int i = 1; i < centers.length; i++) {
            if (meters(lat, lon, centers[i][0], centers[i][1])
                    < meters(lat, lon, centers[best][0], centers[best][1])) {
                best = i;
            }
        }
        return names[best];
    }
}
