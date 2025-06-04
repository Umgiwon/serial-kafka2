package com.one.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * 애플리케이션 설정을 관리하는 클래스
 */
public class AppConfig {
    private static final Properties properties = new Properties();
    private static AppConfig instance;

    private AppConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("application.properties 파일을 찾을 수 없습니다.");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("설정 파일을 로드하는 중 오류가 발생했습니다.", e);
        }
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public int getIntProperty(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public long getLongProperty(String key) {
        return Long.parseLong(properties.getProperty(key));
    }

    public int[] getIntArrayProperty(String key) {
        String[] values = properties.getProperty(key).split(",");
        return Arrays.stream(values)
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}