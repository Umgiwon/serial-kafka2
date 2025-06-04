# Modbus RTU와 Kafka 통합

이 애플리케이션은 Modbus RTU 장치에서 데이터를 읽고 Kafka로 전송합니다. KOS-500 프로토콜 참조 가이드에 명시된 KOS-500 프로토콜과 함께 작동하도록 설계되었습니다.

## 기능

- 시리얼 포트를 통해 Modbus RTU 장치에 연결
- 홀딩 레지스터, 입력 레지스터, 코일 및 이산 입력 읽기
- 데이터를 Kafka 토픽으로 전송
- 구성 가능한 폴링 간격
- 적절한 오류 처리 및 리소스 정리

## 요구 사항

- Java 8 이상
- Kafka 서버
- Modbus RTU 통신을 위한 시리얼 포트

## 의존성

- j2mod: Modbus RTU 라이브러리
- jssc: 시리얼 통신 라이브러리
- Kafka 클라이언트: Kafka로 데이터 전송
- SLF4J: 로깅

## 구성

애플리케이션은 `Main.java` 파일의 상수를 수정하여 구성할 수 있습니다:

### Modbus RTU 구성

```java
private static final String PORT_NAME = "/dev/ttyS0"; // 시리얼 포트로 업데이트
private static final int BAUD_RATE = 9600;
private static final int DATA_BITS = 8;
private static final int STOP_BITS = 1;
private static final int PARITY = 0; // 0=없음, 1=홀수, 2=짝수
private static final int SLAVE_ID = 1;
private static final int START_ADDRESS = 0;
private static final int QUANTITY = 10;
```

### Kafka 구성

```java
private static final String BOOTSTRAP_SERVERS = "localhost:9092"; // Kafka 서버로 업데이트
private static final String TOPIC = "modbus-data";
```

### 폴링 간격

```java
private static final int POLLING_INTERVAL = 5; // 초 단위
```

## 사용법

1. `Main.java`의 상수를 업데이트하여 애플리케이션 구성
2. Gradle을 사용하여 애플리케이션 빌드:
   ```
   ./gradlew build
   ```
3. 애플리케이션 실행:
   ```
   java -jar build/libs/serial-kafka2-1.0-SNAPSHOT.jar
   ```

## 데이터 형식

애플리케이션은 다음 데이터를 Kafka로 전송합니다:

- 홀딩 레지스터: 정수 값 배열
- 입력 레지스터: 정수 값 배열
- 코일: 불리언 값 배열
- 이산 입력: 불리언 값 배열

각 메시지에는 타임스탬프와 데이터 유형을 식별하는 키가 포함됩니다.

## 테스트 가이드

애플리케이션을 테스트하려면 Modbus RTU 시뮬레이터와 Kafka가 있는 테스트 환경을 설정해야 합니다.

### 테스트 환경 설정

#### 1. Modbus RTU 시뮬레이터

물리적 Modbus 장치 없이 테스트하려면 Modbus RTU 시뮬레이터를 사용할 수 있습니다:

- **Diagslave**: 무료 Modbus 슬레이브 시뮬레이터
    - 다운로드: https://www.modbusdriver.com/diagslave.html
    - RTU 모드로 실행: `diagslave -m rtu -a 1 -p /dev/ttyS1 -b 9600`

- **ModbusPal**: Java 기반 Modbus 시뮬레이터
    - 다운로드: https://sourceforge.net/projects/modbuspal/
    - 가상 시리얼 포트 사용하도록 구성

- **가상 시리얼 포트**: 테스트용 가상 시리얼 포트 쌍 생성
    - Linux: `socat`을 사용하여 가상 시리얼 포트 생성
      ```
      socat -d -d pty,raw,echo=0 pty,raw,echo=0
      ```
    - Windows: com0com 사용 (https://sourceforge.net/projects/com0com/)
    - macOS: socat 또는 Virtual Serial Port Driver와 같은 상용 솔루션 사용

#### 2. Kafka 설정

1. https://kafka.apache.org/downloads에서 Kafka 다운로드 및 압축 해제
2. ZooKeeper 시작:
   ```
   bin/zookeeper-server-start.sh config/zookeeper.properties
   ```
3. Kafka 서버 시작:
   ```
   bin/kafka-server-start.sh config/server.properties
   ```
4. 토픽 생성:
   ```
   bin/kafka-topics.sh --create --topic modbus-data --bootstrap-server localhost:9092
   ```

### 애플리케이션 빌드

Gradle을 사용하여 애플리케이션 빌드:
```
./gradlew build
```

### 테스트 매개변수로 애플리케이션 실행

1. `Main.java`의 상수를 테스트 환경에 맞게 수정:
    - `PORT_NAME`을 가상 시리얼 포트로 업데이트 (예: Linux에서 `/dev/pts/1`)
    - 다른 매개변수가 Modbus 시뮬레이터 구성과 일치하는지 확인

2. 애플리케이션 실행:
   ```
   java -jar build/libs/serial-kafka2-1.0-SNAPSHOT.jar
   ```

### 데이터 흐름 확인

1. Kafka 컨슈머를 시작하여 메시지 확인:
   ```
   bin/kafka-console-consumer.sh --topic modbus-data --from-beginning --bootstrap-server localhost:9092
   ```

2. 애플리케이션 로그에서 성공적인 연결 및 데이터 읽기 확인:
    - "Connected to Modbus slave" 메시지 확인
    - "Read holding registers", "Read input registers" 등 확인
    - "Sent holding registers to Kafka" 등 확인

3. Modbus 시뮬레이터를 사용하는 경우, 레지스터 값을 수정하고 Kafka 메시지에 반영된 변경 사항을 확인할 수 있습니다.

## 문제 해결

애플리케이션에 문제가 발생하면 로그에서 오류 메시지를 확인하세요. 애플리케이션은 SLF4J를 사용하여 로깅하며, 발생하는 모든 오류에 대한 자세한 정보를 제공합니다.

일반적인 문제:
- 시리얼 포트를 찾을 수 없음: 시리얼 포트가 올바르게 구성되어 있는지 확인
- Kafka 연결 문제: Kafka 서버가 실행 중이고 접근 가능한지 확인
- Modbus 통신 오류: 장치 연결 및 구성 확인
- 권한 문제: 시리얼 포트에 접근할 수 있는 권한이 있는지 확인 (Linux에서는 sudo가 필요할 수 있음)

## 라이선스

이 프로젝트는 MIT 라이선스에 따라 라이선스가 부여됩니다 - 자세한 내용은 LICENSE 파일을 참조하세요.
