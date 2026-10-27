# 지연 통계 패널 진단과 로컬 수정안

상태: 로컬 준비 완료 대상. 운영 적용·재시작·배포 승인은 아직 없으며 실제 반영하지 않았다.

사용자 요청은 모든 기존 latency 패널에 평균·min·max·p50·p95·p99를 일관되게 표시하는 것이다. 보류한 영구 알림함과 무관하며 그 자료와 기존 미커밋 문서는 보존한다.

## 확인한 사실

- 운영 앱 이미지/revision은 `787a441765008d8a2ab015441c97b2c6409c157a`, 로컬은 DTO 정리를 포함한 `bd9342840`이다.
- 운영 대시보드는 UID `zup-zup-operations`, 읽은 시점의 version은 19, 기본 시간 범위는 24시간, refresh는 30초였다. 저장소 provisioned 대시보드 UID `sugang-helper-metrics`와 다르다.
- 문제 패널은 운영 panel 33이며 `histogram_quantile(... rate(http_server_requests_seconds_bucket[5m]))` 세 쿼리를 사용했다. datasource UID `PBFA97CFB590B2093`는 실제 Prometheus datasource와 일치했다. 변수는 로그용 두 개뿐이며 이 패널의 메트릭 필터에는 사용되지 않는다.
- 실제 Prometheus target은 `up=1`, scrape 주기는 15초, lastError는 비어 있었다. job은 `sugang-helper-app`, instance는 `app:8081`이다.
- HTTP count/sum/max는 각각 53개 시계열이 있었고 uri/method/status 라벨도 존재했다. 반면 HTTP bucket 조회와 실제 panel 33의 p95 쿼리는 모두 빈 결과였다. 기존 Prometheus 컨테이너에서 앱 actuator를 직접 읽어도 HTTP bucket/min은 각각 0개였다. provider(4개), upstream(1개), Hikari acquire(1개) 역시 count/sum/max만 있고 bucket/min은 0개여서 수집 후 삭제가 아니라 앱의 미발행까지 확인했다.
- 검사 시점에는 모든 `*_seconds_bucket` 조회 및 `quantile` 라벨을 가진 seconds 요약 조회가 빈 결과였다. 관련 HTTP·Hikari·provider·upstream 타이머는 count/sum/max만 관측됐다. 따라서 현재 분위수 누락은 URI 라벨이나 datasource 장애가 아니라 histogram 미발행으로 확인했다.
- 같은 시점의 HTTP 최근 5분 증가량은 0, 24시간 증가량은 약 1,504였다. 이는 별개의 표시 조건이다. histogram을 켠 뒤에도 고정 5분 instant 표는 최근 트래픽이 없으면 비게 된다. 과거의 요청량으로 과거 bucket을 새로 만들 수는 없다.

위 수치는 한 시점 읽기 결과이지 지속 측정이나 부하 테스트가 아니다. 앱·제공자에 테스트 요청을 보내지 않았다.

## 준비한 변경

1. `application-prod.yml`에서 기존 타이머 네 개만 histogram을 발행한다: `http.server.requests`, `notification.provider.latency`, `crawler.upstream.latency`, `hikaricp.connections.acquire`. 모든 타이머에 일괄 활성화하지 않는다.
2. sub-ms 관측을 표현하도록 histogram 하한은 1µs로 둔다. 상한은 HTTP/커넥션 획득 1분, provider 10초, upstream 10분이다. 상한은 timeout 변경이나 샘플 폐기가 아니라 bucket 경계다. 범위 밖 관측은 +Inf에 포함되므로 극단 분위수의 해석에는 경계 한계를 고려해야 한다.
3. 저장소의 지연 패널 2/16/18/21/27에 여섯 통계 계약을 적용하고, 저장소에는 없던 운영 URI 표에 대응하는 panel 33을 추가한다. 나머지 27개 패널과 기존 대시보드 설정은 구조적으로 동일하게 보존한다.
4. 운영에서 편집한 대시보드를 통째로 덮어쓰지 않도록 `infra/grafana/panel-patches/latency-statistics.json`에는 운영 16/18/21/27/33의 변경 필드만 별도로 준비했다. 이 파일은 provisioning 대상 디렉터리가 아니고 자동 적용되지 않는다. version 19와 원본 panel SHA-256을 가드로 사용해야 한다. hash는 UTF-8 JSON, 키 정렬, compact separators `(',', ':')` 기준이다. 적용 시 최신 version/hash가 다르면 중단하고 다시 대조한다.
5. API URI 표/요약은 선택 기간의 `increase(sum)/increase(count)`와 `histogram_quantile`을 사용한다. 시계열 그래프는 `$__rate_interval`을 사용한다. 모든 평균은 관측 횟수 가중 평균이며 모든 분위수는 집계 라벨과 `le`를 보존한다. 초 단위 결과에 1000을 곱하고 Grafana unit을 ms로 맞춘다.

## 여섯 통계의 실제 지원 상태

| 패널 | 평균 | min | max | p50 | p95 | p99 |
| --- | --- | --- | --- | --- | --- | --- |
| HTTP 요약, 저장소 2 | 지원 | N/A·미계측 | 선택 기간에 수집한 time-window max의 최대 | histogram 적용 후 | 적용 후 | 적용 후 |
| 제공자 지연, 16 | provider/outcome별 가중 평균 | N/A·미계측 | provider/outcome별 time-window max | histogram 적용 후 | 적용 후 | 적용 후 |
| JBNU 외부 API, 18 | 지원 | N/A·미계측 | time-window max | histogram 적용 후 | 적용 후 | 적용 후 |
| HTTP 전체 그래프, 21 | 지원 | N/A·미계측 | time-window max | histogram 적용 후 | 적용 후 | 적용 후 |
| DB 커넥션 획득, 27 | 지원 | N/A·미계측 | time-window max | histogram 적용 후 | 적용 후 | 적용 후 |
| URI별 HTTP 표, 33 | URI별 선택 기간 가중 평균 | N/A·미계측 | 선택 기간에 수집한 URI별 time-window max의 최대 | histogram 적용 후 | 적용 후 | 적용 후 |

`GC pause seconds/second` 패널 24는 요청 한 건의 지연이 아니라 초당 GC 정지 시간 비율이다. 기존 의미를 다른 통계로 바꾸지 않는다. GC 한 번당 지연 분포 패널 신설은 별도 보완 제안이다. 호출 횟수/RPS 패널 14/15/19도 지연 통계로 바꾸지 않는다.

### min과 max를 오해하지 않기

- Micrometer Timer/Prometheus는 여기서 개별 요청의 min을 내보내지 않는다. min target은 관측이 있는 집계 차원에만 NaN을 반환하고 이를 `N/A (미계측)`로 표시한다. 숫자 0, histogram_quantile(0), 첫 bucket 하한, percentile 시계열의 Grafana reduce-min을 최소 요청 지연으로 쓰지 않는다.
- Micrometer 1.15.7 소스에서 Prometheus Timer의 max는 `TimeWindowMax`이며, 기본 registry expiry는 Prometheus step 1분, bufferLength는 3이다. 기본 조건에서 무관측으로 시간이 지나면 0으로 만료한다. 고유 요청의 전체 생애 최대값이 아니다. 로컬 mock-clock 테스트로 만료와 누적 count 보존을 확인한다.
- 그래프의 max는 해당 계측 창의 값이다. URI 표/요약의 max는 선택 기간에 scrape한 그 값들의 최대다. 계측 창이 선택 기간 시작 이전 관측을 포함하거나 scrape 전에 만료될 수 있으므로, 선택 기간에 완료된 모든 요청의 정확한 최대라고 표기하지 않는다. 무관측 0 값은 표시에서 제외한다.
- p50/p95/p99는 bucket 보간 추정이다. 분위수별 평균을 내지 않고 `sum by (그룹, le)` 뒤에 계산한다. p0/p100을 정확한 극값으로 바꾸지 않는다.

정확한 min이 꼭 필요하면 추가 계측 승인 후 해당 측정 경계에서 제한된 라벨/보존 창을 가진 최소값 계측을 설계해야 한다. 또는 전체 요청을 빠짐없이 보관한 로그/trace에서 계산할 수 있지만 저장 비용·민감정보·표본 누락을 함께 검토해야 한다. 샘플링된 trace의 min도 전체 요청의 정확한 min이 아니다. 이번에는 새 필터/관측 handler/로그 수집을 추가하지 않았다.

## 검증과 실제 운영 적용 전 확인

대표 확인 쿼리:

```promql
up{job="sugang-helper-app"}
count by (__name__, job, instance) ({__name__=~"http_server_requests_seconds_(count|sum|max|bucket)"})
sum by (uri, method) (increase(http_server_requests_seconds_count{job="sugang-helper-app"}[5m]))
histogram_quantile(0.95, sum by (uri, le) (rate(http_server_requests_seconds_bucket{job="sugang-helper-app",uri!=""}[5m]))) * 1000
```

- 로컬 단위 테스트는 실제 prod YAML을 MetricsProperties/PropertiesMeterFilter에 바인딩하고 Prometheus registry에서 네 타이머의 bucket/count/sum/max를 확인한다. minimum 메트릭은 없고 관련 없는 타이머에 histogram을 켜지 않는 것도 확인한다. 실제 HTTP/DB/Redis/외부 제공자 연결은 하지 않는다.
- 준비한 36개 PromQL은 Grafana 매크로를 5분/24시간으로 대체한 뒤 운영 Prometheus에 GET query로 문법과 실행을 검증한다. 현재 없는 bucket 쿼리가 빈 결과인 것은 데이터 검증 통과와 다르다.
- 운영 적용에는 별도 승인이 필요하다. 앱 설정 반영과 재시작/배포로 새 bucket을 수집하고 실제 관측 및 최소 두 번 scrape 후 확인해야 한다. histogram 활성화로 시계열·메모리·scrape 부하가 증가한다. HTTP URI/method/status 등 기존 라벨을 그대로 쓰며 사용자 ID나 원시 URL 새 라벨은 추가하지 않는다.
- 저장소 provisioned 대시보드만 배포하면 별개 UID인 현재 운영 대시보드에 자동 반영되지 않는다. 운영 최신 version을 재확인하고 해당 panel patch만 승인된 경로로 적용해야 한다. 이번에는 운영 dashboard API에 쓰지 않았다.
- URI 표는 기존처럼 빈 uri만 제외하며 method/status는 합쳐 집계한다. 정적/관리 URI·UNKNOWN 제외 여부는 별도 정책이다. 현재 없다고 확인하지 않은 정적 경로를 임의로 제외하거나 label 필터 오류로 단정하지 않는다.

## Notion 보완 목록으로 전달할 근거

| 공백 | 저장소/실측 근거 | 보완 후보 |
| --- | --- | --- |
| 분위수에 필요한 bucket 미수집 | 기존 prod management 설정에 histogram 없음, 실제 bucket 및 quantile 조회 빈 결과 | 이번 네 타이머 histogram 로컬 수정. 배포 후 bucket·부하 확인 필요 |
| min 미계측 | 현재 관련 Timer export는 count/sum/max이며 최소값 메트릭을 관측하지 못함 | 우선 N/A. 정확한 최소값 요구 시 별도 유한 시간창 계측 승인 |
| 운영 Grafana와 Git 기준 불일치 | repo UID `sugang-helper-metrics`, 운영 UID `zup-zup-operations` version 19 | 승인된 패널 패치 적용 후 운영 커스텀 변경을 안전하게 버전 관리할 기준 정하기 |
| 저트래픽 instant 표의 빈 결과 | 최근 5분 HTTP 증가 0, 24시간 약 1,504 | 선택 기간 기반 표와 no-data 설명; 0ms로 채우지 않기 |
| 이미 수집하지만 전용 지연 패널 없는 구간 | 실제 announcement search, crawler chunk write, outbox claim lag/claim-to-attempt count/sum/max 관측; 확인한 지연 패널 목록에는 없음 | 먼저 필요한 구간의 가중 평균·창 max 시각화 제안. 새 패널/계측은 이번 범위 밖 |
| 실행 알림 규칙 공백 | 해당 Prometheus rules groups=0, 인증한 Grafana 조직의 alert-rules 응답 0개. 저장소 provisioning은 datasource/dashboard만 존재 | SLO/지속 시간/저트래픽 no-data 처리·중복 억제를 결정한 뒤 경보 규칙 제안. 다른 조직·외부 알림 시스템 전체가 없다는 뜻은 아님 |

이번 수정은 위 보완 후보를 전부 구현하는 것이 아니다. 실제 코드 변경은 latency 설정·해당 패널·필수 계약 테스트에 한정한다.

## 이번 로컬 검증 결과

- 실제 prod YAML 바인딩 + Prometheus export/창 max 만료 단위 테스트 2개 통과.
- 전체 main/test 소스 컴파일과 bootJar 통과.
- 로컬 대시보드 구조/단위/6통계/기존 27개 패널 보존 검사 통과.
- Grafana 매크로를 구체 시간으로 치환한 36개 PromQL의 운영 읽기 실행이 모두 success. 기존 bucket 부재 상태의 빈 결과는 새 계측 검증을 대신하지 않는다.
- 기존 dirty 문서 3개의 바이트, HEAD, 로컬/원격 refs, 빈 index를 보존했다. 커밋하지 않았다.
- 운영 Grafana/API/config에 쓰지 않았고 restart/reload/deploy/migration/실발송을 하지 않았다.

### 측정 경계 근거

- `NotificationProviderResilience.java:86-122`: provider 타이머는 wrapper 실행부터 성공/실패/timeout까지의 호출자 대기 시간이다. 하위 HTTP가 timeout 뒤에도 남을 수 있어 실제 네트워크 종료 시각으로 해석하지 않는다.
- `JbnuCourseApiClient.java:82-94,275-282`: upstream 타이머는 한 시도별로 기록한다. 재시도 사이 sleep이나 수집 전체 DB 반영 총시간이 아니다.
- `AnnouncementService.java:52`, `CourseCatalogSynchronizationService.java:177`, `SeatNotificationOutboxProcessor.java:124,132`: 이미 있는 검색·chunk write·claim 지연 타이머의 관측 경계다. 해당 지연 패널 추가는 별도 제안으로만 남긴다.
- scrape/datasource 근거: `infra/prometheus/prometheus.yml:6-10`, `infra/grafana/provisioning/datasources/datasource.yml:4-8`.

## 후속 검토: 정확한 최소값의 최소 계측안

현재 선택은 기존 Timer를 유지하고 min을 N/A로 표시하는 것이다. 아래 내용은 추가 계측 제안이며 구현하지 않았다.

최소값을 수집하려면 기존 측정 지점에서 완료된 duration을 받아, 유한한 시간창의 원자적 min과 관측 count를 함께 관리해야 한다. 빈 창의 초기값은 0이 아니라 미관측이며 scrape에는 NaN/누락으로 표현한다. 실제로 측정한 0 duration과 빈 창을 구분해야 한다. 원문 URL·수신자·토큰을 라벨에 추가하지 않고 현재의 제한된 라우트/채널 차원을 재사용한다.

현재 max의 기본 구현은 expiry 1분과 buffer 3개의 회전 창이다. 기록 시점과 회전 위상에 따라 약 2~3분 동안 값이 남는 구조이며 정확한 `[now-3m, now]`의 모든 요청 극값과 같지는 않다. 새 min을 이와 비교하려면 같은 창 경계를 명시해야 한다. 분 단위 고정 창 min, 정확한 sliding window min, 기존 max와 맞춘 회전 창 min은 서로 다른 계약이다. 선택 기간 전체의 정확한 min을 원하면 scrape 이전 유실과 기간 경계까지 처리하는 다른 저장/조회 설계가 필요하다.

추가 계측의 최소 검증 항목은 다음과 같다.

- 10µs/20ms/90ms 관측의 최소값, 실제 0 관측, 빈 창의 NaN을 구분한다.
- 동시 기록과 창 회전이 겹쳐도 작은 관측을 잃거나 다음 창에 과거 값이 무기한 남지 않는다.
- HTTP, provider, upstream, Hikari의 실제 측정 경계와 성공/실패/timeout 포함 범위를 각각 확인한다. 모든 Timer 호출 경로를 한 번씩 계측한다는 증거가 필요하다.
- Clock을 제어해 경계 직전/직후·장기 무관측·만료를 검증하고 Prometheus scrape/단위/라벨 수 제한을 확인한다.
- 15초 scrape와 app 재시작/수집 공백이 존재하는 한 전체 요청의 임의 기간 정확한 극값을 보장한다고 표현하지 않는다.

이 작업은 wrapper/HTTP observation/Hikari 계측 변경 범위가 커지므로 별도 설계 확인 후 진행한다. histogram p0/p100이나 기존 percentile 시계열의 min/max를 대용하지 않는다. 이번 로컬 커밋에는 계측 제안·필수 테스트만 포함하고 구현은 포함하지 않는다.

## 로컬 최종 정적 검사

새 테스트를 포함한 `checkstyleTest`와 `pmdTest`가 통과했다. 운영 증설·재부팅과 충돌하지 않도록 이후 운영 SSH 조회와 지표 추가 조회는 중단하고 로컬 검증만 수행했다.
