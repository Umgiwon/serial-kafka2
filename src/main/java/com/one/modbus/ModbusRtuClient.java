package com.one.modbus;

import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Modbus RTU 프로토콜 통신을 위한 클라이언트 (j2mod 라이브러리 사용)
 */
public class ModbusRtuClient {
    private static final Logger logger = LoggerFactory.getLogger(ModbusRtuClient.class);

    private final SerialConnection connection;
    private final int startAddress;
    private final int quantity;

    /**
     * ModbusRtuClient 생성자
     * 
     * @param portName 시리얼 포트 이름 (예: COM1, /dev/ttyS0)
     * @param baudRate 전송 속도 (예: 9600)
     * @param dataBits 데이터 비트 (예: 8)
     * @param stopBits 정지 비트 (예: 1)
     * @param parity 패리티 (예: 0=없음, 1=홀수, 2=짝수)
     * @param startAddress 레지스터 읽기 시작 주소
     * @param quantity 읽을 레지스터 수량
     * @throws Exception Modbus 클라이언트 초기화 중 오류 발생 시
     */
    public ModbusRtuClient(String portName, int baudRate, int dataBits, int stopBits, int parity,
                          int startAddress, int quantity) throws Exception {
        // 시리얼 포트 매개변수 구성
        SerialParameters params = new SerialParameters();
        params.setPortName(portName);
        params.setBaudRate(baudRate);
        params.setDatabits(dataBits);
        params.setStopbits(stopBits);
        params.setParity(parity);
        params.setEncoding("rtu");
        params.setEcho(false);

        // 시리얼 연결 생성
        this.connection = new SerialConnection(params);
        this.startAddress = startAddress;
        this.quantity = quantity;

        logger.info("ModbusRtuClient 초기화 완료 - 포트: {}, 전송속도: {}",
                   portName, baudRate);
    }

    /**
     * Modbus 슬레이브에 연결
     * 
     * @throws Exception 슬레이브 연결 중 오류 발생 시
     */
    public void connect() throws Exception {
        connection.open();
        logger.info("Modbus 슬레이브에 연결됨");
    }

    /**
     * Modbus 슬레이브 연결 해제
     */
    public void disconnect() {
        try {
            connection.close();
            logger.info("Modbus 슬레이브 연결 해제됨");
        } catch (Exception e) {
            logger.error("Modbus 슬레이브 연결 해제 중 오류 발생", e);
        }
    }

    /**
     * Modbus 슬레이브에서 홀딩 레지스터 읽기
     * 
     * @param slaveId 슬레이브 ID
     * @return 레지스터 값 배열
     * @throws Exception 레지스터 읽기 중 오류 발생 시
     */
    public int[] readHoldingRegisters(int slaveId) throws Exception {
        ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest(startAddress, quantity);
        request.setUnitID(slaveId);

        ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection);
        transaction.setRequest(request);
        transaction.execute();

        ReadMultipleRegistersResponse response = (ReadMultipleRegistersResponse) transaction.getResponse();

        int[] registers = new int[response.getWordCount()];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = response.getRegisterValue(i);
        }

        logger.info("슬레이브 {}: 홀딩 레지스터 읽기 완료: {}", slaveId, Arrays.toString(registers));
        return registers;
    }

    /**
     * Modbus 슬레이브에서 입력 레지스터 읽기
     * 
     * @param slaveId 슬레이브 ID
     * @return 레지스터 값 배열
     * @throws Exception 레지스터 읽기 중 오류 발생 시
     */
    public int[] readInputRegisters(int slaveId) throws Exception {
        ReadInputRegistersRequest request = new ReadInputRegistersRequest(startAddress, quantity);
        request.setUnitID(slaveId);

        ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection);
        transaction.setRequest(request);
        transaction.execute();

        ReadInputRegistersResponse response = (ReadInputRegistersResponse) transaction.getResponse();

        int[] registers = new int[response.getWordCount()];
        for (int i = 0; i < registers.length; i++) {
            registers[i] = response.getRegisterValue(i);
        }

        logger.info("슬레이브 {}: 입력 레지스터 읽기 완료: {}", slaveId, Arrays.toString(registers));
        return registers;
    }

    /**
     * Modbus 슬레이브에서 코일 읽기
     * 
     * @param slaveId 슬레이브 ID
     * @return 코일 값 배열
     * @throws Exception 코일 읽기 중 오류 발생 시
     */
    public boolean[] readCoils(int slaveId) throws Exception {
        ReadCoilsRequest request = new ReadCoilsRequest(startAddress, quantity);
        request.setUnitID(slaveId);

        ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection);
        transaction.setRequest(request);
        transaction.execute();

        ReadCoilsResponse response = (ReadCoilsResponse) transaction.getResponse();

        boolean[] coils = new boolean[response.getBitCount()];
        for (int i = 0; i < coils.length; i++) {
            coils[i] = response.getCoilStatus(i);
        }

        logger.info("슬레이브 {}: 코일 읽기 완료: {}", slaveId, Arrays.toString(coils));
        return coils;
    }

    /**
     * Modbus 슬레이브에서 이산 입력 읽기
     * 
     * @param slaveId 슬레이브 ID
     * @return 이산 입력 값 배열
     * @throws Exception 이산 입력 읽기 중 오류 발생 시
     */
    public boolean[] readDiscreteInputs(int slaveId) throws Exception {
        ReadInputDiscretesRequest request = new ReadInputDiscretesRequest(startAddress, quantity);
        request.setUnitID(slaveId);

        ModbusSerialTransaction transaction = new ModbusSerialTransaction(connection);
        transaction.setRequest(request);
        transaction.execute();

        ReadInputDiscretesResponse response = (ReadInputDiscretesResponse) transaction.getResponse();

        boolean[] discreteInputs = new boolean[response.getBitCount()];
        for (int i = 0; i < discreteInputs.length; i++) {
            discreteInputs[i] = response.getDiscreteStatus(i);
        }

        logger.info("슬레이브 {}: 이산 입력 읽기 완료: {}", slaveId, Arrays.toString(discreteInputs));
        return discreteInputs;
    }
}