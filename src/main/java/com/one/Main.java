package com.one;

import com.one.modbus.ModbusRtuClient;
import com.one.kafka.ModbusKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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

    // 슬레이브 ID 배열 (최대 10개 컴프레셔)
    private static final int[] SLAVE_IDS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    private static final int START_ADDRESS = 0;
    private static final int QUANTITY = 10;

    // Kafka 구성 FIXME: Kafka 서버에 맞게 수정
    private static final String BOOTSTRAP_SERVERS = "192.168.219.51:9092";
    private static final String TOPIC = "modbus-data";

    // 폴링 간격(초)
    private static final int POLLING_INTERVAL = 10;

    // 병렬 처리를 위한 스레드 풀 크기
    private static final int THREAD_POOL_SIZE = 5;

    public static void main(String[] args) {
        logger.info("Modbus RTU에서 Kafka로 애플리케이션 시작 중");

        try {
            // Modbus RTU 클라이언트 초기화 (slaveId 제외)
            ModbusRtuClient modbusClient = new ModbusRtuClient(
                PORT_NAME, BAUD_RATE, DATA_BITS, STOP_BITS, PARITY,
                START_ADDRESS, QUANTITY
            );

            // Kafka 프로듀서 초기화
            ModbusKafkaProducer kafkaProducer = new ModbusKafkaProducer(BOOTSTRAP_SERVERS, TOPIC);

            // Modbus 연결
            modbusClient.connect();

            // 병렬 처리를 위한 스레드 풀 생성
            ExecutorService dataCollectionExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            // 주기적으로 데이터를 폴링하기 위한 스케줄러 생성
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            // 폴링 작업 스케줄링
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // 현재 타임스탬프 가져오기
                    long timestamp = System.currentTimeMillis();
                    logger.info("Modbus 데이터 폴링 시작 (타임스탬프: {})", timestamp);

                    // 각 슬레이브에 대한 작업 목록 생성
                    List<CompletableFuture<Void>> futures = new ArrayList<>();

                    // 각 슬레이브 ID에 대해 병렬로 처리
                    for (int slaveId : SLAVE_IDS) {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            try {
                                logger.info("슬레이브 {}: Modbus 데이터 폴링 중...", slaveId);

                                // 홀딩 레지스터 읽기
                                int[] holdingRegisters = modbusClient.readHoldingRegisters(slaveId);

                                // 입력 레지스터 읽기
                                int[] inputRegisters = modbusClient.readInputRegisters(slaveId);

                                // 코일 읽기
                                boolean[] coils = modbusClient.readCoils(slaveId);

                                // 이산 입력 읽기
                                boolean[] discreteInputs = modbusClient.readDiscreteInputs(slaveId);

                                // 데이터를 Kafka로 전송
                                kafkaProducer.sendHoldingRegisters(slaveId, holdingRegisters, timestamp);
                                kafkaProducer.sendInputRegisters(slaveId, inputRegisters, timestamp);
                                kafkaProducer.sendCoils(slaveId, coils, timestamp);
                                kafkaProducer.sendDiscreteInputs(slaveId, discreteInputs, timestamp);

                                logger.info("슬레이브 {}: 데이터 수집 및 전송 완료", slaveId);
                            } catch (Exception e) {
                                logger.error("슬레이브 {}: Modbus 데이터 폴링 중 오류 발생", slaveId, e);
                                // 한 슬레이브에서 오류가 발생해도 다른 슬레이브는 계속 처리
                            }
                        }, dataCollectionExecutor);

                        futures.add(future);
                    }

                    // 모든 작업이 완료될 때까지 대기 (선택적)
                    try {
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(
                            POLLING_INTERVAL - 1, TimeUnit.SECONDS); // 다음 폴링 전에 완료되도록 타임아웃 설정
                        logger.info("모든 슬레이브 데이터 수집 완료");
                    } catch (TimeoutException e) {
                        logger.warn("일부 슬레이브 데이터 수집이 제한 시간 내에 완료되지 않았습니다");
                    } catch (Exception e) {
                        logger.error("슬레이브 데이터 수집 중 오류 발생", e);
                    }

                } catch (Exception e) {
                    logger.error("Modbus 데이터 폴링 중 오류 발생", e);
                }
            }, 0, POLLING_INTERVAL, TimeUnit.SECONDS);

            // 리소스 정리를 위한 종료 훅 추가
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logger.info("애플리케이션 종료 중...");
                    scheduler.shutdown();
                    dataCollectionExecutor.shutdown();

                    // 진행 중인 작업이 완료될 때까지 최대 10초 대기
                    if (!dataCollectionExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                        dataCollectionExecutor.shutdownNow();
                    }

                    modbusClient.disconnect();
                    kafkaProducer.close();
                    logger.info("모든 리소스가 정상적으로 정리되었습니다");
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