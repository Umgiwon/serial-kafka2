package com.one.model;

import java.util.Arrays;

/**
 * Modbus 데이터를 나타내는 제네릭 클래스
 * @param <T> 데이터 타입 (int[] 또는 boolean[])
 */
public class ModbusData<T> {
    private final int slaveId;
    private final ModbusDataType dataType;
    private final T data;
    private final long timestamp;
    
    /**
     * ModbusData 생성자
     * 
     * @param slaveId 슬레이브 ID
     * @param dataType 데이터 타입
     * @param data 데이터 값
     * @param timestamp 타임스탬프
     */
    public ModbusData(int slaveId, ModbusDataType dataType, T data, long timestamp) {
        this.slaveId = slaveId;
        this.dataType = dataType;
        this.data = data;
        this.timestamp = timestamp;
    }
    
    public int getSlaveId() {
        return slaveId;
    }
    
    public ModbusDataType getDataType() {
        return dataType;
    }
    
    public T getData() {
        return data;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        String dataString;
        if (data instanceof int[]) {
            dataString = Arrays.toString((int[]) data);
        } else if (data instanceof boolean[]) {
            dataString = Arrays.toString((boolean[]) data);
        } else {
            dataString = data.toString();
        }
        
        return String.format("ModbusData{slaveId=%d, dataType=%s, data=%s, timestamp=%d}",
                slaveId, dataType, dataString, timestamp);
    }
}