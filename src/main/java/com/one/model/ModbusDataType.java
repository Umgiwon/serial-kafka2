package com.one.model;

/**
 * Modbus 데이터 타입을 나타내는 열거형
 */
public enum ModbusDataType {
    HOLDING_REGISTER("holding_registers", "홀딩 레지스터"),
    INPUT_REGISTER("input_registers", "입력 레지스터"),
    COIL("coils", "코일"),
    DISCRETE_INPUT("discrete_inputs", "이산 입력");
    
    private final String key;
    private final String displayName;
    
    ModbusDataType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }
    
    public String getKey() {
        return key;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}