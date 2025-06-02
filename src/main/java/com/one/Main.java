package com.one;

import com.one.modbus.ModbusRtuClient;
import com.one.kafka.ModbusKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Modbus RTU 장치에서 데이터를 읽고 Kafka로 전송한다.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Modbus RTU 구성 FIXME: 환경에 맞게 수정
    private static final String PORT_NAME = "COM3";
    private static final int BAUD_RATE = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = 1;
    private static final int PARITY = 0; // 0=없음, 1=홀수, 2=짝수
    private static final int SLAVE_ID = 1;
    private static final int START_ADDRESS = 0;
    private static final int QUANTITY = 10;

    // Kafka 구성 FIXME: Kafka 서버에 맞게 수정
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "modbus-data";

    // 폴링 간격(초)
    private static final int POLLING_INTERVAL = 10;

    public static void main(String[] args) {
        logger.info("Modbus RTU에서 Kafka로 애플리케이션 시작 중");

        try {
            // Modbus RTU 클라이언트 초기화
            ModbusRtuClient modbusClient = new ModbusRtuClient(
                PORT_NAME, BAUD_RATE, DATA_BITS, STOP_BITS, PARITY,
                SLAVE_ID, START_ADDRESS, QUANTITY
            );

            // Kafka 프로듀서 초기화
            ModbusKafkaProducer kafkaProducer = new ModbusKafkaProducer(BOOTSTRAP_SERVERS, TOPIC);

            // Modbus 슬레이브에 연결
            modbusClient.connect();

            // 주기적으로 데이터를 폴링하기 위한 스케줄러 생성
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

            // 폴링 작업 스케줄링
            executor.scheduleAtFixedRate(() -> {
                try {
                    // Modbus에서 데이터 읽기
                    logger.info("Modbus 데이터 폴링 중...");

                    // 홀딩 레지스터 읽기
                    int[] holdingRegisters = modbusClient.readHoldingRegisters();

                    // 입력 레지스터 읽기
                    int[] inputRegisters = modbusClient.readInputRegisters();

                    // 코일 읽기
                    boolean[] coils = modbusClient.readCoils();

                    // 이산 입력 읽기
                    boolean[] discreteInputs = modbusClient.readDiscreteInputs();

                    // 현재 타임스탬프 가져오기
                    long timestamp = System.currentTimeMillis();

                    // 데이터를 Kafka로 전송
                    kafkaProducer.sendHoldingRegisters(holdingRegisters, timestamp);
                    kafkaProducer.sendInputRegisters(inputRegisters, timestamp);
                    kafkaProducer.sendCoils(coils, timestamp);
                    kafkaProducer.sendDiscreteInputs(discreteInputs, timestamp);

                } catch (Exception e) {
                    logger.error("Modbus 데이터 폴링 중 오류 발생", e);
                }
            }, 0, POLLING_INTERVAL, TimeUnit.SECONDS);

            // 리소스 정리를 위한 종료 훅 추가
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logger.info("애플리케이션 종료 중...");
                    executor.shutdown();
                    modbusClient.disconnect();
                    kafkaProducer.close();
                } catch (Exception e) {
                    logger.error("종료 중 오류 발생", e);
                }
            }));

            logger.info("애플리케이션이 시작되었습니다. 종료하려면 Ctrl+C를 누르세요.");

        } catch (Exception e) {
            logger.error("애플리케이션 초기화 중 오류 발생", e);
        }
    }
}