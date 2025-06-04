package com.one.modbus;

import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Modbus RTU 프로토콜 통신을 위한 클라이언트 (j2mod 라이브러리 사용)
 */
public class ModbusRtuClient {
    private static final Logger logger = LoggerFactory.getLogger(ModbusRtuClient.class);

    // 재시도 관련 상수
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final SerialConnection connection;
    private final int startAddress;
    private final int quantity;
    private final SerialParameters params;

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
        this.params = new SerialParameters();
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
     * Modbus 슬레이브에 연결 (재시도 로직 포함)
     * 
     * @throws Exception 모든 재시도 후에도 연결 실패 시
     */
    public void connect() throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (!connection.isOpen()) {
                    connection.open();
                    logger.info("Modbus 슬레이브에 연결됨 (시도 {}/{})", attempt, MAX_RETRY_ATTEMPTS);
                    return; // 성공적으로 연결됨
                } else {
                    logger.info("Modbus 슬레이브에 이미 연결되어 있음");
                    return;
                }
            } catch (Exception e) {
                lastException = e;
                logger.warn("Modbus 슬레이브 연결 실패 (시도 {}/{}): {}",
                           attempt, MAX_RETRY_ATTEMPTS, e.getMessage());

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    logger.info("{}ms 후 재시도...", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }

        // 모든 재시도 실패 후 마지막 예외 던지기
        logger.error("최대 재시도 횟수({})를 초과했습니다. 연결 실패", MAX_RETRY_ATTEMPTS);
        throw lastException;
    }

    /**
     * 연결이 끊어진 경우 재연결 시도
     *
     * @return 재연결 성공 여부
     */
    private boolean reconnectIfNeeded() {
        try {
            if (!connection.isOpen()) {
                logger.warn("연결이 끊어졌습니다. 재연결 시도 중...");
                connect();
                return true;
            }
            return true; // 이미 연결되어 있음
        } catch (Exception e) {
            logger.error("재연결 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Modbus 슬레이브 연결 해제
     */
    public void disconnect() {
        try {
            if (connection.isOpen()) {
                connection.close();
                logger.info("Modbus 슬레이브 연결 해제됨");
            }
        } catch (Exception e) {
            logger.error("Modbus 슬레이브 연결 해제 중 오류 발생", e);
        }
    }

    /**
     * 재시도 로직을 포함한 제네릭 실행 메서드
     * 
     * @param <T> 반환 타입
     * @param slaveId 슬레이브 ID
     * @param operation 실행할 작업
     * @param operationName 작업 이름 (로깅용)
     * @return 작업 결과
     * @throws Exception 모든 재시도 후에도 실패 시
     */
    private <T> T executeWithRetry(int slaveId, Function<Integer, T> operation, String operationName) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                // 연결이 끊어진 경우 재연결 시도
                if (!reconnectIfNeeded()) {
                    continue; // 재연결 실패, 다음 시도로
                }

                T result = operation.apply(slaveId);
                logger.info("슬레이브 {}: {} 완료", slaveId, operationName);
                return result;
            } catch (Exception e) {
                lastException = e;
                logger.warn("슬레이브 {}: {} 실패 (시도 {}/{}): {}", 
                           slaveId, operationName, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    logger.info("{}ms 후 재시도...", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }

        // 모든 재시도 실패 후 마지막 예외 던지기
        logger.error("슬레이브 {}: 최대 재시도 횟수({})를 초과했습니다. {} 실패", 
                    slaveId, MAX_RETRY_ATTEMPTS, operationName);
        throw lastException;
    }

    /**
     * Modbus 슬레이브에서 홀딩 레지스터 읽기 (재시도 로직 포함)
     * 
     * @param slaveId 슬레이브 ID
     * @return 레지스터 값 배열
     * @throws Exception 모든 재시도 후에도 읽기 실패 시
     */
    public int[] readHoldingRegisters(int slaveId) throws Exception {
        return executeWithRetry(slaveId, id -> {
            try {
                return readHoldingRegistersInternal(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "홀딩 레지스터 읽기");
    }

    /**
     * 홀딩 레지스터 읽기 내부 구현
     */
    private int[] readHoldingRegistersInternal(int slaveId) throws Exception {
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

        logger.debug("슬레이브 {}: 홀딩 레지스터 읽기 완료: {}", slaveId, Arrays.toString(registers));
        return registers;
    }

    /**
     * Modbus 슬레이브에서 입력 레지스터 읽기 (재시도 로직 포함)
     * 
     * @param slaveId 슬레이브 ID
     * @return 레지스터 값 배열
     * @throws Exception 모든 재시도 후에도 읽기 실패 시
     */
    public int[] readInputRegisters(int slaveId) throws Exception {
        return executeWithRetry(slaveId, id -> {
            try {
                return readInputRegistersInternal(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "입력 레지스터 읽기");
    }

    /**
     * 입력 레지스터 읽기 내부 구현
     */
    private int[] readInputRegistersInternal(int slaveId) throws Exception {
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

        logger.debug("슬레이브 {}: 입력 레지스터 읽기 완료: {}", slaveId, Arrays.toString(registers));
        return registers;
    }

    /**
     * Modbus 슬레이브에서 코일 읽기 (재시도 로직 포함)
     * 
     * @param slaveId 슬레이브 ID
     * @return 코일 값 배열
     * @throws Exception 모든 재시도 후에도 읽기 실패 시
     */
    public boolean[] readCoils(int slaveId) throws Exception {
        return executeWithRetry(slaveId, id -> {
            try {
                return readCoilsInternal(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "코일 읽기");
    }

    /**
     * 코일 읽기 내부 구현
     */
    private boolean[] readCoilsInternal(int slaveId) throws Exception {
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

        logger.debug("슬레이브 {}: 코일 읽기 완료: {}", slaveId, Arrays.toString(coils));
        return coils;
    }

    /**
     * Modbus 슬레이브에서 이산 입력 읽기 (재시도 로직 포함)
     * 
     * @param slaveId 슬레이브 ID
     * @return 이산 입력 값 배열
     * @throws Exception 모든 재시도 후에도 읽기 실패 시
     */
    public boolean[] readDiscreteInputs(int slaveId) throws Exception {
        return executeWithRetry(slaveId, id -> {
            try {
                return readDiscreteInputsInternal(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "이산 입력 읽기");
    }

    /**
     * 이산 입력 읽기 내부 구현
     */
    private boolean[] readDiscreteInputsInternal(int slaveId) throws Exception {
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

        logger.debug("슬레이브 {}: 이산 입력 읽기 완료: {}", slaveId, Arrays.toString(discreteInputs));
        return discreteInputs;
    }
}
