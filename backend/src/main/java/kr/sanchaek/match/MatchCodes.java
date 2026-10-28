package kr.sanchaek.match;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import kr.sanchaek.common.ApiException;

/** 동행에 쓰는 영어 코드 목록 (API 명세 "값 약속"). 순서는 화면에 보일 순서 */
final class MatchCodes {

    static final List<String> TARGETS = List.of("ANYONE", "EXCHANGE", "SAME_LANGUAGE", "MBTI");
    static final List<String> STYLES = List.of("SLOW", "FAST", "QUIET", "CHAT", "PHOTO", "SUNRISE_SUNSET", "DOG");
    static final List<String> LANGUAGES = List.of("KO", "ZH", "EN", "VI", "RU");
    static final List<String> AGE_GROUPS = List.of("20s", "30s", "40s", "50s+");
    static final List<String> AREAS = List.of("OKGU", "JEONGWANG", "BAEGOT", "SIHWA");
    static final List<String> TIME_SLOTS = List.of("MORNING", "AFTERNOON", "EVENING", "NIGHT");
    static final List<String> STATUSES = List.of("OPEN", "FULL", "CLOSED", "CANCELED", "DONE");

    private MatchCodes() {
    }

    /** 값 하나가 목록에 있는지. 없으면 400 */
    static String check(String field, String value, List<String> allowed) {
        if (value == null || !allowed.contains(value)) {
            throw ApiException.badRequest(field + ": " + String.join(", ", allowed) + " 중 하나여야 해요");
        }
        return value;
    }

    /** 여러 값 확인 + 중복 제거 + 화면 순서로 정렬. null 이면 빈 목록 */
    static List<String> checkAll(String field, List<String> values, List<String> allowed) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (String v : values) {
            if (v == null || v.isBlank()) {
                continue;
            }
            String value = v.trim();
            check(field, value, allowed);
            if (!result.contains(value)) {
                result.add(value);
            }
        }
        result.sort(order(allowed));
        return result;
    }

    /** "SLOW,CHAT" 같은 쉼표 목록 파라미터 → 확인된 목록 */
    static List<String> splitAndCheck(String field, String csv, List<String> allowed) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return checkAll(field, List.of(csv.split(",")), allowed);
    }

    static Comparator<String> order(List<String> allowed) {
        return Comparator.comparingInt(v -> {
            int i = allowed.indexOf(v);
            return i < 0 ? Integer.MAX_VALUE : i;
        });
    }

    static String upper(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase(Locale.ROOT);
    }
}
