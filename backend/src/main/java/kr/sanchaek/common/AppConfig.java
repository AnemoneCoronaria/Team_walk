package kr.sanchaek.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 설정 파일 db.properties 읽기 (DB 접속 정보, 업로드 폴더).
 *
 * 파일 위치: backend/src/main/resources/db.properties
 * 깃허브에 올리지 않는 파일이라, 각자 db.properties.example 을 복사해서 만듭니다.
 */
public final class AppConfig {

    private static final String FILE = "db.properties";
    private static Properties props;

    private AppConfig() {
    }

    /** 값 읽기. 없으면 오류 (필수 값) */
    public static String get(String key) {
        String value = load().getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(FILE + "에 " + key + " 값이 없습니다. db.properties.example 을 참고하세요.");
        }
        return value.trim();
    }

    /** 값 읽기. 없으면 기본값 */
    public static String get(String key, String defaultValue) {
        String value = load().getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    private static synchronized Properties load() {
        if (props != null) {
            return props;
        }
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(FILE)) {
            if (in == null) {
                throw new IllegalStateException(FILE + " 파일이 없습니다. "
                        + "backend/src/main/resources/db.properties.example 을 같은 폴더에 db.properties 로 복사하고 비밀번호를 넣으세요.");
            }
            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            props = p;
            return props;
        } catch (IOException e) {
            throw new IllegalStateException(FILE + " 파일을 읽지 못했습니다.", e);
        }
    }
}
