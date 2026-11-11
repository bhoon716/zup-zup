# 프로젝트 코드 컨벤션

채택 전 항목은 후보로 표시한다.

- [CONV-001 FQCN 대신 import와 단순 타입명 사용](#conv-001-fqcn-대신-import와-단순-타입명-사용)
- [CONV-002 가드 절로 불필요한 중첩 줄이기](#conv-002-가드-절로-불필요한-중첩-줄이기)
- [CONV-003 긴 변환과 검증을 이름 있는 작은 메서드로 추출](#conv-003-긴-변환과-검증을-이름-있는-작은-메서드로-추출)
- [CONV-004 테스트 표시 이름으로 검증 동작 설명](#conv-004-테스트-표시-이름으로-검증-동작-설명)
- [CONV-005 테스트 준비 실행 검증 단계를 주석으로 구분](#conv-005-테스트-준비-실행-검증-단계를-주석으로-구분)
- [CONV-006 같은 의미의 반복 테스트 문자열을 상수로 추출](#conv-006-같은-의미의-반복-테스트-문자열을-상수로-추출)
- [CONV-007 import와 함수 주변의 과도한 연속 빈 줄 정리](#conv-007-import와-함수-주변의-과도한-연속-빈-줄-정리)
- [CONV-008 주석 정리에서 필드 의미와 도구 지시 보존](#conv-008-주석-정리에서-필드-의미와-도구-지시-보존)
- [CONV-009 사람이 읽는 메시지는 한국어로 작성](#conv-009-사람이-읽는-메시지는-한국어로-작성)
- [CONV-010 조회 이름으로 부재 처리 구분](#conv-010-조회-이름으로-부재-처리-구분)
- [CONV-011 예외 테스트에서 타입과 오류 코드 함께 검증](#conv-011-예외-테스트에서-타입과-오류-코드-함께-검증)
- [CONV-012 외부와 예외 값을 unknown에서 타입 가드로 좁히기](#conv-012-외부와-예외-값을-unknown에서-타입-가드로-좁히기)
- [CONV-013 조건 분기 표현 선택](#conv-013-조건-분기-표현-선택)
- [CONV-014 응답 DTO 생성은 정적 팩토리 메서드 사용](#conv-014-응답-dto-생성은-정적-팩토리-메서드-사용)
- [CONV-015 카드 상세 트리거와 별도 액션을 분리](#conv-015-카드-상세-트리거와-별도-액션을-분리)
- [CONV-016 강의 키 분해는 공용 파서를 사용](#conv-016-강의-키-분해는-공용-파서를-사용)
- [CONV-017 구조 로그 조합을 이름 있는 로그 메서드로 분리](#conv-017-구조-로그-조합을-이름-있는-로그-메서드로-분리)
- [CONV-018 정책 값과 검증 필드 키는 의미 있는 이름으로 상수화](#conv-018-정책-값과-검증-필드-키는-의미-있는-이름으로-상수화)
- [CONV-019 같은 클래스의 상태 비교에는 필드를 직접 사용](#conv-019-같은-클래스의-상태-비교에는-필드를-직접-사용)
- [CONV-020 조회 원본에서 필요한 요약을 바로 생성](#conv-020-조회-원본에서-필요한-요약을-바로-생성)
- [CONV-021 같은 동작 검증은 합치고 독립 경계는 유지](#conv-021-같은-동작-검증은-합치고-독립-경계는-유지)

## CONV-001 FQCN 대신 import와 단순 타입명 사용

타입은 import한 뒤 본문에서는 단순 클래스명으로 쓴다.

- 범위: 서버 Java 운영 코드와 테스트의 타입 참조
- 상태: 후보 · 논의 미확인 · 구현 반영

**설명용 예시**

변경 전

```java
java.net.URL resource = loader.getResource("config.xml");
```

변경 후

```java
import java.net.URL;

URL resource = loader.getResource("config.xml");
```

근거: 타입을 사용하는 식에서 패키지 경로의 반복을 줄이고 실제 연산을 읽기 쉽게 하려는 방식으로 해석했다. (추론)

- 한계: 동일한 단순 이름을 가진 타입 충돌의 해결 방식은 이 근거로 확정하지 않는다.; 명시적 규칙 채택은 하지 않았으며 리뷰 합의도 확인되지 않았다.

출처: 8ba68b62 (로컬) · [3d660634](https://github.com/bhoon716/zup-zup/commit/3d660634fd938c751f045b469e69dfea49897709) · 8ba68b62·2 (로컬)

## CONV-002 가드 절로 불필요한 중첩 줄이기

처리 제외 조건을 앞에서 return 또는 continue로 끝내고 정상 처리를 바깥 들여쓰기 수준에 둔다.

- 범위: 서버 Java 메서드와 반복문의 국소 제어 흐름
- 상태: 후보 · 논의 미확인 · 구현 반영

**설명용 예시**

변경 전

```java
void clean(List<String> files) {
    if (!files.isEmpty()) {
        deleteFiles(files);
    }
}
```

변경 후

```java
void clean(List<String> files) {
    if (files.isEmpty()) {
        return;
    }
    deleteFiles(files);
}
```

근거: AGENTS가 불필요한 중첩과 코드의 들여쓰기 깊이를 줄이라고 명시한다.

- 한계: 조건 평가·예외·부수효과 순서를 보존한다. 명시적 규칙 채택은 하지 않았으며 리뷰 합의도 확인되지 않았다.

분기 표현 선택 기준은 [CONV-013](#conv-013-조건-분기-표현-선택)을 따른다.

출처: 8ba68b62 (로컬) · [ac936f2b](https://github.com/bhoon716/zup-zup/commit/ac936f2b4c79fce5f9f1eabd2421e4ff91074990) · 8ba68b62·2 (로컬)

## CONV-003 긴 변환과 검증을 이름 있는 작은 메서드로 추출

같은 클래스 안에서 변환·검증 단계를 private 메서드로 추출하고 호출 이름으로 수행 작업을 드러낸다.

- 범위: 시간표 snapshot 변환과 검색 조건 검증처럼 같은 클래스 안의 국소 메서드
- 상태: 후보 · 논의 미확인 · 구현 반영

**설명용 예시**

변경 전

```java
List<CourseView> views = courses.stream()
    .map(course -> new CourseView(course.id(), course.name()))
    .toList();
```

변경 후

```java
List<CourseView> views = courses.stream().map(this::toView).toList();

private CourseView toView(Course course) {
    return new CourseView(course.id(), course.name());
}
```

근거: 처리 순서는 호출부에, 세부 필드 매핑과 조건은 작은 메서드에 두어 한 번에 읽는 내용을 줄이려는 정리로 해석했다. (추론)

- 한계: 호출·변환 순서와 필드 값은 유지한다. 클래스·모듈 책임 분리나 새 계층 도입의 근거로 확대하지 않는다.; 명시적 규칙 채택은 하지 않았으며 리뷰 합의도 확인되지 않았다.

출처: 8ba68b62 (로컬) · 8ba68b62·2 (로컬)

## CONV-004 테스트 표시 이름으로 검증 동작 설명

일반 테스트와 매개변수 테스트에 검증하는 동작이 드러나는 한국어 @DisplayName을 붙인다.

- 범위: 확인한 서버 JUnit 테스트 메서드의 표시 이름
- 상태: 후보 · 논의 미확인 · 구현 반영

**설명용 예시**

변경 전

```java
@Test
void rejectsUnknownSort() {
    assertThat(isValid("unknown")).isFalse();
}
```

변경 후

```java
@Test
@DisplayName("지원하지 않는 정렬 조건은 거부한다")
void rejectsUnknownSort() {
    assertThat(isValid("unknown")).isFalse();
}
```

근거: 테스트 보고서에서 메서드 구현을 열지 않고도 무엇을 검증하는지 읽게 하려는 방식으로 해석했다. (추론)

- 한계: 표시 이름만 덧붙이며 검증의 assertion·매개변수·실행 범위를 바꾸지 않는다.; 명시적 규칙 채택은 하지 않았으며 리뷰 합의도 확인되지 않았다.

출처: 8ba68b62 (로컬) · 8ba68b62·2 (로컬)

## CONV-005 테스트 준비 실행 검증 단계를 주석으로 구분

실제 흐름에 맞춰 given, when, then을 구분한다. 호출과 검증이 한 식이면 when &amp; then처럼 합친다.

- 범위: 서버 Java 테스트 본문의 단계 주석
- 상태: 후보 · 논의 미확인 · 구현 반영
- 예외: 준비 단계가 없는 한 식의 호출·검증은 when &amp; then으로 표시할 수 있다.

**설명용 예시**

변경 전

```java
@Test
void parsesValue() {
    String input = "3";
    int result = parse(input);
    assertThat(result).isEqualTo(3);
}
```

변경 후

```java
@Test
void parsesValue() {
    // given
    String input = "3";
    // when
    int result = parse(input);
    // then
    assertThat(result).isEqualTo(3);
}
```

근거: 준비 코드와 검증 대상 호출의 경계를 드러내어 테스트의 흐름을 읽기 쉽게 하려는 방식으로 해석했다. (추론)

- 한계: 단계 주석을 기계적으로 삽입해 준비 코드를 실행 단계로 오분류하지 않는다. 일반 주석 정리와 이 주석 패턴은 구분한다.; 명시적 규칙 채택은 하지 않았으며 리뷰 합의도 확인되지 않았다.

출처: 8ba68b62 (로컬) · 8ba68b62·2 (로컬) · 8ba68b62·3 (로컬)

## CONV-006 같은 의미의 반복 테스트 문자열을 상수로 추출

반복 문자열은 역할을 드러내는 private static final 상수로 추출하고 모든 관련 사용처를 함께 바꾼다.

- 범위: 반복되는 테스트 접두사·필드 이름·동일 의미의 테스트 시각 문자열
- 상태: 후보 · 논의 미확인 · 구현 반영
- 예외: 현재 PMD 규칙은 길이 4 미만의 리터럴과 annotation 문자열을 이 검사에서 제외한다.

**설명용 예시**

변경 전

```java
assertThat(first).startsWith("data=");
assertThat(second).startsWith("data=");
```

변경 후

```java
private static final String DATA_PREFIX = "data=";

assertThat(first).startsWith(DATA_PREFIX);
assertThat(second).startsWith(DATA_PREFIX);
```

근거: PR 11은 중복을 줄이고 향후 변경을 쉽게 하려는 상수 추출이라고 명시한다.

- 한계: 철자가 같아도 의미가 다른 값까지 하나의 상수로 합친다는 규칙은 아니다.; 명시적 규칙 채택은 하지 않았으며 리뷰 합의도 확인되지 않았다.

출처: [PR11](https://github.com/bhoon716/zup-zup/pull/11) · [7a100d17](https://github.com/bhoon716/zup-zup/commit/7a100d17e0a303180fe74001ef2d37c3fd8d3e8f) · 8ba68b62 (로컬)

## CONV-007 import와 함수 주변의 과도한 연속 빈 줄 정리

코드 요소의 구분에 필요한 빈 줄은 남기고, import와 함수 주변의 불필요한 연속 빈 줄을 줄인다.

- 범위: 확인한 TypeScript import·함수와 Java 멤버 주변의 빈 줄
- 상태: 후보 · 논의 미확인 · 구현 반영

**설명용 예시**

변경 전

```typescript
import { ready } from "./state";



export function isReady() {
  return ready;
}
```

변경 후

```typescript
import { ready } from "./state";

export function isReady() {
  return ready;
}
```

근거: 내용을 바꾸지 않고 코드 구획의 간격을 일정하게 읽도록 하려는 정리로 해석했다. (추론)

- 한계: 문자열·템플릿 리터럴·주석 안의 줄바꿈에는 적용하지 않는다. Java 필드별 구분에 필요한 빈 줄을 없애는 규칙도 아니다.; 명시적 규칙 채택은 하지 않았으며 리뷰 합의도 확인되지 않았다.

출처: [5a40aed5](https://github.com/bhoon716/zup-zup/commit/5a40aed56dbc2105b6d8a3131ab1422139ded07e) · 8ba68b62 (로컬)

## CONV-008 주석 정리에서 필드 의미와 도구 지시 보존

정리 대상인 일반 설명 주석을 줄이더라도 엔티티 필드 설명과 검사·실행 지시문은 보존한다. 테스트 단계 주석도 현재 패턴을 따른다.

- 범위: 일반 코드 주석 정리와 엔티티 필드·도구 지시·테스트 단계의 경계
- 상태: 후보 · 논의 미확인 · 구현 반영
- 예외: 엔티티 필드 설명과 검사·실행 지시문은 일반 주석 정리에서도 유지한다.; Java 테스트의 단계 주석은 이후 8ba68b62에서 다시 보완했다.

**설명용 예시**

변경 전

```java
private int capacity; // 정원

// 값을 반환한다
int capacity() {
    return capacity;
}
```

변경 후

```java
private int capacity; // 정원

int capacity() {
    return capacity;
}
```

근거: 불필요한 설명을 줄이면서 필드 해석과 도구 동작에 필요한 정보는 남기려는 방식으로 해석했다. (추론)

- 한계: 모든 주석을 금지하거나 중요한 설계 이유·주의 설명을 지우는 전역 규칙으로 확대하지 않는다.; 명시적 규칙 채택은 하지 않았으며 리뷰 합의도 확인되지 않았다.

출처: 66a66a05 (로컬) · 8ba68b62 (로컬) · 8ba68b62·2 (로컬)

## CONV-009 사람이 읽는 메시지는 한국어로 작성

예외·검증 메시지, 화면 안내, 로그 설명과 운영 스크립트 출력은 한국어로 작성한다. 오류 코드, 이벤트 키, 프로토콜 필드명과 제품명은 기존 표기를 유지한다. 외부 오류를 화면에 표시할 때는 한국어 안내 문구를 사용한다.

## CONV-010 조회 이름으로 부재 처리 구분

부재할 수 있는 내부 단건 조회는 find…와 Optional로 표현하고, 반드시 필요한 단건은 require…에서 부재 시 예외를 발생시킨다. 목록 조회는 결과가 없으면 빈 컬렉션을 반환한다.

- 범위: 서버 Java 내부 조회 helper의 이름과 반환 표현. find의 Optional 기준은 단건에만 적용한다.
- 상태: 채택 · 논의 미확인 · 구현 부분 반영
- 예외: 기존 공개 API의 null 응답 계약과 단순 필드 getter는 예외로 보존한다. 프레임워크·상위 계약이 고정한 이름도 임의로 바꾸지 않는다.; 목록 메서드에 find가 들어가도 Optional을 강제하지 않는다. findAccountStatuses처럼 목록은 빈 컬렉션으로 표현한다.

**설명용 예시**

변경 전

```java
Optional<User> loadUser(long id);
User loadRequiredUser(long id); // 없으면 예외
List<User> getUsers(); // 없으면 빈 목록
```

변경 후

```java
Optional<User> findUser(long id);
User requireUser(long id); // 없으면 예외
List<User> listUsers(); // 없으면 빈 목록
```

근거: findByEmail의 Optional과 목록의 빈 결과를 구분하는 현재 패턴을 바탕으로, 이름·반환 타입에서 호출자가 부재 처리 방식을 예측하기 쉽게 하려는 규칙으로 해석했다. (추론)

- 반례·반론: 현재 getTimetable은 부재 시 예외를 던지지만 require라는 이름을 쓰지 않는다. getPrimaryTimetable은 null을 반환한다. 전체 조회 명명이 이미 통일됐다는 뜻은 아니다.
- 한계: 기존 공개 API의 null 응답 계약과 단순 필드 getter는 예외로 보존한다. 프레임워크·상위 계약이 고정한 이름도 임의로 바꾸지 않는다.; 목록 메서드에 find가 들어가도 Optional을 강제하지 않는다. findAccountStatuses처럼 목록은 빈 컬렉션으로 표현한다.; 확인한 관행: UserIdentityQueryService.findByEmail/findById는 Optional, findAccountStatuses의 빈 입력은 List.of()다. PMD에는 ReturnEmptyCollectionRatherThanNull이 설정돼 있다. require 명명은 이번에 선택한 새 기준이다.; 이름 정리 과정에서 기존 부재 시 동작·HTTP 응답·예외 코드·컬렉션 가변성 계약을 바꾸지 않는다. 예시는 동작이 같은 선언의 이름만 정리한다.; 사용자가 2026-11-06 추천 1·2·3번을 선택해 이 항목을 채택했다. 팀 합의와 전수 구현 완료는 확인하지 않았다.

출처: 8ba68b62 (로컬) · 8ba68b62·2 (로컬) · 8ba68b62·3 (로컬)

## CONV-011 예외 테스트에서 타입과 오류 코드 함께 검증

CustomException의 타입과 기대 ErrorCode를 함께 검증한다. 메시지나 detail은 그 문구 자체가 검증 대상인 경우에만 추가로 검증한다.

- 범위: CustomException을 기대하는 서버 Java 단위 테스트의 assertion. 특정 assertion 라이브러리나 문법을 강제하지 않는다.
- 상태: 채택 · 논의 미확인 · 구현 부분 반영
- 예외: 오류 설명이 테스트 대상이면 메시지·detail 검증을 유지한다. 학점 검증 테스트처럼 detail이 구체적 검증 대상인 경우를 삭제하지 않는다.

**설명용 예시**

변경 전

```java
CustomException error = assertThrows(CustomException.class, () -> call());
assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
```

변경 후

```java
assertThatThrownBy(() -> call())
    .isInstanceOf(CustomException.class)
    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
```

근거: 같은 CustomException이라도 오류 코드가 다를 수 있으므로, 의도한 실패 분기를 검증하면서 무관한 문구 변경에 대한 결합을 줄이려는 규칙으로 해석했다. (추론)

- 한계: 오류 설명이 테스트 대상이면 메시지·detail 검증을 유지한다. 학점 검증 테스트처럼 detail이 구체적 검증 대상인 경우를 삭제하지 않는다.; 확인한 관행: CourseSearchConditionValidationTest는 예외 타입과 INVALID_INPUT을 함께 검증하며, 학점 조건 테스트는 detail도 확인한다. CustomException은 ErrorCode와 detail을 별도로 보유한다.; 다른 예외 타입의 테스트까지 ErrorCode를 요구하지 않는다. 아래 두 예시는 동일한 타입·오류 코드를 검증하는 동등한 표현이며 특정 문법으로의 일괄 교체를 뜻하지 않는다.; 사용자가 2026-11-06 추천 1·2·3번을 선택해 이 항목을 채택했다. 팀 합의와 전수 구현 완료는 확인하지 않았다.

출처: 8ba68b62 (로컬) · 8ba68b62·2 (로컬)

## CONV-012 외부와 예외 값을 unknown에서 타입 가드로 좁히기

외부·예외 값은 unknown으로 받고 typeof·instanceof·타입 가드로 사용할 값의 형태를 확인한 뒤 접근한다. SDK·DOM 연결에 필요한 타입 단언은 허용한다.

- 범위: 프런트의 신뢰하지 못하는 외부 입력·예외 처리 경계. 이미 검증된 내부 타입을 모두 unknown으로 바꾸는 규칙은 아니다.
- 상태: 채택 · 논의 미확인 · 구현 부분 반영
- 예외: SDK·DOM 연결에 필요한 단언은 허용한다. 예를 들어 실행 환경을 확인한 뒤 window를 분석 SDK 전역 타입으로 연결하는 단언은 일괄 제거하지 않는다.; object 여부를 먼저 검사하고 Record&lt;string, unknown&gt;으로 속성을 읽은 뒤 개별 값의 타입을 확인하는 국소 단언도 허용한다.

**설명용 예시**

변경 전

```typescript
function responseStatus(error: any) {
  return isAxiosError(error) ? error.response?.status : undefined;
}
```

변경 후

```typescript
function responseStatus(error: unknown) {
  return isAxiosError(error) ? error.response?.status : undefined;
}
```

근거: 현재 unknown 예외를 검사하는 코드처럼, 타입 단언만으로 미확인 외부 값의 구조를 보장했다고 가정하지 않고 필요한 속성을 확인해 사용하려는 규칙으로 해석했다. (추론)

- 반례·반론: auth-error.ts는 object 확인 후 response 구조를 단언해 접근하는 기존 코드가 남아 있다. 전면 타입 가드 적용 상태로 표시하지 않으며 이 문서 추가로 코드를 변경하지 않는다.
- 한계: SDK·DOM 연결에 필요한 단언은 허용한다. 예를 들어 실행 환경을 확인한 뒤 window를 분석 SDK 전역 타입으로 연결하는 단언은 일괄 제거하지 않는다.; object 여부를 먼저 검사하고 Record&lt;string, unknown&gt;으로 속성을 읽은 뒤 개별 값의 타입을 확인하는 국소 단언도 허용한다.; 확인한 관행: client-error.ts는 unknown을 typeof·instanceof와 속성 값 검사로 좁힌다. trackOnboardingFailed는 unknown에 isAxiosError를 사용한다. SDK 전역 연결 단언도 함께 존재한다.; 타입 가드가 보장하는 범위만 사용한다. isAxiosError 확인만으로 임의 response.data의 필드 구조까지 검증된 것으로 보지 않는다.; as 또는 이중 단언 자체의 전역 금지 규칙이 아니다. 예시는 기존 런타임 가드를 유지하고 매개변수의 any를 unknown으로 좁힌다.; 사용자가 2026-11-06 추천 1·2·3번을 선택해 이 항목을 채택했다. 팀 합의와 전수 구현 완료는 확인하지 않았다.

출처: 8ba68b62 (로컬) · 8ba68b62·2 (로컬) · 8ba68b62·3 (로컬)

## CONV-013 조건 분기 표현 선택

간단한 분기에는 삼항 연산자, `switch`, `if-else`를 사용할 수 있다. 분기를 작성하거나 수정할 때는 같은 동작을 더 명확하게 표현할 방법이 있는지 항상 검토한다.

- 삼항 연산자는 단순한 값 선택에 사용하고 중첩하지 않는다.
- `switch`는 하나의 상태나 열거값에 따른 분기에 사용한다. 값의 대응만 필요하면 맵도 고려한다.
- `if-else`는 두 흐름이 짧고 분명할 때 사용한다. 예외 처리나 복잡한 분기에는 가드 절과 조기 반환을 우선 검토한다.

표현을 바꿀 때는 조건 평가 순서, 부수 효과, 예외 처리를 보존한다.

## CONV-014 응답 DTO 생성은 정적 팩토리 메서드 사용

서버의 HTTP 응답 DTO는 DTO에 정의한 정적 팩토리 메서드로 생성한다.

- 범위: 서버의 `*Response` DTO 및 애플리케이션·테스트의 생성 호출부
- 상태: 채택 · 사용자 명시
- 도메인 객체나 조회 결과를 응답으로 변환할 때는 `from(...)`, 이미 준비된 응답 값을 조합할 때는 `of(...)`를 사용한다.
- 상황에 따라 문구, 상태 또는 기본값이 정해지면 `crawlerHealthy(...)`, `forGuest(...)`처럼 상황별 정적 팩토리를 두고 고정 값은 DTO 안에서 결정한다.
- 응답 DTO에 빌더를 선언하지 않는다. 생성자는 팩토리 내부에서만 호출하며, 일반 클래스의 생성자는 프레임워크에서 필요한 경우를 제외하고 `private`으로 둔다. 레코드의 기본 생성자도 애플리케이션과 테스트에서 직접 호출하지 않는다.
- 필드가 많을 때는 호출부에서 값을 하나씩 나열하지 않고, 도메인 객체나 조회 결과를 넘겨 DTO 내부에서 매핑한다.

**예시**

```java
return CourseSeatHistoryResponse.from(history);
return AdminRecentLogResponse.crawlerHealthy(lastCrawledAt);
```

- 요청 DTO, 엔티티, 응답으로 변환되기 전의 내부 결과 객체에는 적용하지 않는다.

출처: 사용자 명시 지시 — “DTO response의 정적 팩토리 메서드와 빌드 패턴을 하나로 통일”

## CONV-015 카드 상세 트리거와 별도 액션을 분리

상세는 제목의 네이티브 버튼으로 열고, 카드 클릭 영역은 그 버튼의 오버레이로 만든다. 별도 액션은 상세 트리거와 중첩되지 않는 독립 버튼으로 둔다.

- 범위: 웹 강의·구독 카드에서 모달 열기와 독립 액션
- 상태: 후보 · 논의 미확인 · 구현 반영
- 예외: 별도 페이지로 이동하면 버튼 대신 링크를 사용한다.

**설명용 예시**

변경 전

```tsx
<article onClick={openDetail}>
  <h2>강의 제목</h2>
  <button onClick={(event) => {
    event.stopPropagation();
    toggleWishlist();
  }}>찜</button>
</article>
```

변경 후

```tsx
<article className="relative">
  <h2><button type="button" onClick={openDetail}
    className="after:absolute after:inset-0 after:content-['']">강의 제목</button></h2>
  <button type="button" className="relative z-10"
    onClick={toggleWishlist}>찜</button>
</article>
```

근거: 카드 영역 클릭은 유지하면서 각각의 버튼을 키보드와 보조 기술이 구분할 수 있게 하기 위해서다.

- 한계: 모달을 닫을 때 원래 버튼으로 초점을 돌려주는 동작을 유지한다.

출처: [강의 카드](../../apps/web/src/features/course/components/course-table.tsx) · [구독 카드](../../apps/web/src/features/subscription/components/subscription-list.tsx)

## CONV-016 강의 키 분해는 공용 파서를 사용

화면에서 구분자와 배열 인덱스를 반복하지 않고 parseCourseKey의 이름 있는 결과를 사용한다.

- 범위: 웹의 강의 키 파싱 호출부
- 상태: 후보 · 논의 미확인 · 구현 반영

**설명용 예시**

변경 전

```typescript
const parts = subscription.courseKey.split(":");
const subjectCode = parts[2] || subscription.courseKey;
const classNumber = parts[3] || "01";
```

변경 후

```typescript
const fields = parseCourseKey(subscription.courseKey);
const subjectCode = fields.subjectCode || subscription.courseKey;
const classNumber = fields.classNumber || "01";
```

근거: 키의 형식이 바뀔 때 파싱 규칙을 한 곳에서 고치고 호출부에서 필드 의미를 드러내기 위해서다.

- 한계: 누락 값의 기본 표시와 실제 식별자는 각 호출부 계약을 유지한다.

출처: [강의 키 파서](../../apps/web/src/shared/lib/course.ts) · [구독 카드](../../apps/web/src/features/subscription/components/subscription-list.tsx)

## CONV-017 구조 로그 조합을 이름 있는 로그 메서드로 분리

호출부는 log… 메서드에 필요한 값만 전달하고, 로그 메서드에서 레벨·고정 이벤트와 구조화 속성을 조합한다.

- 범위: 서버 애플리케이션의 구조화 운영 로그
- 상태: 후보 · 논의 동의 · 구현 반영

**설명용 예시**

변경 전

```java
log.atInfo().addKeyValue("event", "crawl_started")
        .addKeyValue("year", year).log("강의 수집을 시작합니다.");
```

변경 후

```java
logCrawlStarted(year);

private void logCrawlStarted(String year) {
    log.atInfo().addKeyValue("event", "crawl_started")
            .addKeyValue("year", year).log("강의 수집을 시작합니다.");
}
```

근거: 업무 흐름과 로그 필드 조합을 나누고 검색용 값을 JSON 속성으로 남기기 위해서다. (추론)

- 한계: 레벨, 이벤트 키와 기존 마스킹을 보존한다.

출처: [52bfbebe](https://github.com/bhoon716/zup-zup/commit/52bfbebe478583eaf6878f5b0ccca866d554ab1d) · [구조화 로그 메서드](../../apps/server/src/main/java/bhoon/sugang_helper/academic/crawling/scheduler/CourseScheduler.java)

## CONV-018 정책 값과 검증 필드 키는 의미 있는 이름으로 상수화

역할이 있는 리터럴에는 의미 있는 상수 이름을 붙인다. 한 곳에서 의미가 드러나는 경로·일반 문구는 그 자리에서 읽을 수 있게 둔다.

- 범위: 서버 코드의 업무 한도·프로토콜 필드·단일 선언 경로
- 상태: 후보 · 논의 동의 · 구현 반영
- 예외: 한 번 쓰는 애너테이션 라우트 경로는 값 자체가 명확하면 리터럴로 둔다.

**설명용 예시**

변경 전

```java
if (terms.size() > 10) {
    reject("courseDirection");
}
```

변경 후

```java
private static final int MAX_DIRECTION_TERMS = 10;
private static final String COURSE_DIRECTION_FIELD = "courseDirection";

if (terms.size() > MAX_DIRECTION_TERMS) {
    reject(COURSE_DIRECTION_FIELD);
}
```

근거: 숫자나 키의 의도를 드러내면서 단일 사용 리터럴의 이름을 따라갈 필요를 줄이기 위해서다. (추론)

출처: [검색 필드 상수](../../apps/server/src/main/java/bhoon/sugang_helper/academic/course/dto/request/CourseSearchCondition.java)

## CONV-019 같은 클래스의 상태 비교에는 필드를 직접 사용

접근자가 같은 필드를 그대로 반환한다면 같은 클래스 안에서는 그 필드를 직접 읽는다.

- 범위: 같은 Java 클래스 안의 필드 비교·복사
- 상태: 후보 · 논의 미확인 · 구현 반영
- 예외: 계산·정규화·가상 호출 의미가 있는 접근자는 기존 의미를 보존한다.

**설명용 예시**

변경 전

```java
return !Objects.equals(this.name, other.getName());
```

변경 후

```java
return !Objects.equals(this.name, other.name);
```

근거: 동일한 저장 상태를 비교한다는 의도를 읽기 쉽게 만들기 위한 방식으로 해석했다. (추론)

출처: [강의 필드 비교](../../apps/server/src/main/java/bhoon/sugang_helper/academic/course/entity/Course.java)

## CONV-020 조회 원본에서 필요한 요약을 바로 생성

요약 호출은 원본에서 요약을 바로 만든다. 요약에 필요한 공통 계산은 원본이나 공통 변환에서 재사용한다.

- 범위: 서버 조회 결과에서 일정 요약처럼 별도 결과 타입을 조립하는 호출부
- 상태: 후보 · 논의 미확인 · 구현 반영
- 예외: 공유 변환에 필터·권한·날짜 보정이 포함돼 있으면 양쪽에서 그 동작을 보존한다.

**설명용 예시**

변경 전

```java
return getUpcomingSchedules().stream()
        .map(ScheduleResponse::toSummary)
        .toList();
```

변경 후

```java
return scheduleRepository.findUpcomingSchedules(today).stream()
        .map(schedule -> toSummary(schedule, today))
        .toList();
```

근거: 호출부가 사용하지 않는 상세 응답 생성과 해당 응답 타입에 대한 결합을 줄이기 위한 선택으로 해석했다. (추론)

출처: [일정 요약 조회](../../apps/server/src/main/java/bhoon/sugang_helper/campus/schedule/service/ScheduleService.java)

## CONV-021 같은 동작 검증은 합치고 독립 경계는 유지

한 동작 테스트로 통합하되 고유한 검증과 SQL 횟수·동시성·권한 같은 독립 경계 검사는 남긴다.

- 범위: 서버·웹에서 같은 호출 결과를 중복 검증하는 테스트
- 상태: 후보 · 논의 미확인 · 구현 반영
- 예외: 반환값이 같아도 서로 다른 실패 조건이나 실제 부수 효과를 검증하면 독립 테스트를 유지한다.

**설명용 예시**

변경 전

```java
@Test void showsTitle() {
    assertThat(readSchedule().title()).isEqualTo("신청");
}
@Test void showsPrimaryFlag() {
    assertThat(readSchedule().primary()).isTrue();
}
```

변경 후

```java
@Test void showsScheduleTitleAndPrimaryFlag() {
    var schedule = readSchedule();
    assertThat(schedule.title()).isEqualTo("신청");
    assertThat(schedule.primary()).isTrue();
}
```

근거: 실행과 유지의 중복을 줄이면서 테스트가 보호하는 결과와 회귀 경계를 보존하기 위해서다.

출처: [0ff996ed](https://github.com/bhoon716/zup-zup/commit/0ff996edf8655097d74afd583e4b3956251c84e7) · [f1a3c9bb](https://github.com/bhoon716/zup-zup/commit/f1a3c9bb87523a96b388c458d668c4336a2b406e) · [테스트 통합 작업 기록](../../docs/existing-code-simplification.md)
