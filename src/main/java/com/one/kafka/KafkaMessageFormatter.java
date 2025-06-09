package com.one.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.one.model.ModbusDataType;

import java.util.Arrays;

/**
 * Kafka 메시지를 JSON 형식으로 포맷팅하는 유틸리티 클래스
 */
public class KafkaMessageFormatter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Modbus 데이터를 JSON 형식으로 포맷팅
     *
     * @param slaveId   슬레이브 ID
     * @param data      데이터 (int[] 또는 boolean[])
     * @param dataType  데이터 타입
     * @param timestamp 타임스탬프
     * @return JSON 형식의 문자열
     */
    public static String formatMessage(int slaveId, Object data, ModbusDataType dataType, long timestamp) {
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("slaveId", slaveId);
        rootNode.put("timestamp", timestamp);
        rootNode.put("dataType", dataType.getKey());

        if (data instanceof int[]) {
            ArrayNode valuesNode = rootNode.putArray("values");
            for (int value : (int[]) data) {
                valuesNode.add(value);
            }
        } else if (data instanceof boolean[]) {
            ArrayNode valuesNode = rootNode.putArray("values");
            for (boolean value : (boolean[]) data) {
                valuesNode.add(value);
            }
        }

        try {
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            // JSON 변환 실패 시 기본 문자열 반환
            return String.format("{\"slaveId\":%d,\"dataType\":\"%s\",\"values\":%s,\"timestamp\":%d}",
                    slaveId, dataType.getKey(), Arrays.toString((Object[]) data), timestamp);
        }
    }
}