# Concurrent Savings API 명세

## 1. 개요

이 문서는 Concurrent Savings의 MVP API 명세를 정의한다.

MVP에서는 선착순 특판 적금 신청 흐름을 검증하기 위해 상품 조회, 상품 신청, 신청 조회 API를 구현한다. 인증, 관리자 기능, 멱등성 키, 비동기 알림, 대기열 승격은 2차 구현 범위로 분리한다.

## 2. 공통 규칙

### Base URL

```text
/api
```

### 사용자 식별

MVP에서는 인증을 구현하지 않으므로 `X-User-Id` 헤더로 요청 유저를 식별한다.

```http
X-User-Id: 1
```

`X-User-Id`는 MVP용 임시 사용자 식별 방식이며, 실제 인증을 대체하지 않는다. 인증 체계가 없는 단계이므로 헤더 누락은 인증 실패(401)가 아니라 잘못된 요청(400)으로 처리한다.

이 헤더는 상품 신청 API에만 필수다. 상품 조회와 신청 조회 API는 요청 유저를 구분하지 않으므로 헤더 없이 호출할 수 있다.

| API | `X-User-Id` |
| --- | --- |
| `GET /api/products`, `GET /api/products/{productId}` | 불필요 |
| `POST /api/products/{productId}/applications` | 필수 |
| `GET /api/products/{productId}/applications` | 불필요 |
| `GET /api/applications/{applicationId}` | 불필요 |

### 응답 형식

성공 응답은 `data` 필드로 감싼 JSON 형식을 사용한다.

```json
{
  "data": {}
}
```

에러 응답은 `code`와 `message`를 포함한다.

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

요청 값 검증에 실패한 경우 `errors` 배열로 상세 사유를 함께 내려준다. `errors`는 선택 필드다.

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [
    {
      "field": "status",
      "reason": "지원하지 않는 신청 상태입니다."
    }
  ]
}
```

### 시간 형식

API 요청과 응답의 날짜, 시간은 오프셋을 포함한 ISO-8601 형식을 사용한다.

```text
2026-09-07T10:00:00+09:00
```

시각은 DB에 UTC로 저장하고, 응답 시점에 오프셋을 붙여 표현한다. 엔티티 필드 타입은 `Instant`로 통일한다.

`start_at`, `end_at`, `applied_at`은 모두 시간축 위의 한 시점이므로 오프셋을 값에 담을 이유가 없다. 오프셋은 저장 대상이 아니라 표현 형식이며, 응답 DTO 단계에서 붙인다.

`LocalDateTime`을 쓰면 로컬 개발 환경(KST)과 배포 환경(UTC)에서 신청 기간 판정 결과가 달라지므로 사용하지 않는다.

## 3. 상품 목록 조회

특판 적금 상품 목록을 조회한다.

```http
GET /api/products
```

### Response

```json
{
  "data": [
    {
      "id": 1,
      "name": "선착순 특판 적금",
      "interestRate": 5.5,
      "capacity": 100,
      "remainingCapacity": 42,
      "startAt": "2026-09-07T10:00:00+09:00",
      "endAt": "2026-09-30T23:59:59+09:00"
    }
  ]
}
```

MVP에서는 상품이 시드데이터 수준으로 적으므로 페이징을 두지 않는다.

### Status Code

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 상품 목록 조회 성공 |

## 4. 상품 상세 조회

특판 적금 상품 상세 정보를 조회한다.

```http
GET /api/products/{productId}
```

### Path Variable

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| productId | Long | 상품 ID |

### Response

```json
{
  "data": {
    "id": 1,
    "name": "선착순 특판 적금",
    "interestRate": 5.5,
    "capacity": 100,
    "remainingCapacity": 42,
    "startAt": "2026-09-07T10:00:00+09:00",
    "endAt": "2026-09-30T23:59:59+09:00"
  }
}
```

### 캐싱 관련 메모

`remainingCapacity`는 신청 1건마다 변하는 값이므로 상품 조회 응답 전체를 그대로 캐싱하기 어렵다. 로드맵 9단계에서 Redis 캐싱을 적용할 때 다음 중 하나를 선택한다.

- 잘 바뀌지 않는 상품 정보와 잔여 정원 응답을 분리한다.
- 잔여 정원에만 짧은 TTL을 적용한다.
- 정확한 잔여 수 대신 마감 여부만 노출한다.

### Status Code

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 상품 상세 조회 성공 |
| 404 Not Found | 상품이 존재하지 않음 |

## 5. 상품 신청

유저가 특판 적금 상품에 신청한다.

```http
POST /api/products/{productId}/applications
X-User-Id: 1
```

### Path Variable

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| productId | Long | 상품 ID |

### Request

MVP에서는 요청 본문을 사용하지 않는다. 신청자는 `X-User-Id` 헤더로만 식별한다.

### Response: 신청 성공

잔여 정원이 있는 경우 신청 상태는 `SUCCESS`가 된다.

```json
{
  "data": {
    "applicationId": 1,
    "productId": 1,
    "userId": 1,
    "status": "SUCCESS",
    "appliedAt": "2026-09-07T10:01:00+09:00"
  }
}
```

### Response: 대기 등록

잔여 정원이 없는 경우 신청 상태는 `WAITING`이 된다.

```json
{
  "data": {
    "applicationId": 2,
    "productId": 1,
    "userId": 101,
    "status": "WAITING",
    "appliedAt": "2026-09-07T10:01:03+09:00"
  }
}
```

MVP에서는 대기 등록 수에 상한을 두지 않는다. 정원이 소진된 이후 들어오는 신청은 모두 `WAITING`으로 저장한다. 대기열 상한 정책은 2차 구현에서 검토한다.

### Response: 중복 신청

이미 같은 상품에 신청한 유저의 요청은 409로 거절한다. 이때 클라이언트가 기존 신청을 다시 조회할 수 있도록 에러 응답에 기존 `applicationId`를 함께 내려준다.

```json
{
  "code": "DUPLICATE_APPLICATION",
  "message": "이미 신청한 상품입니다.",
  "applicationId": 1
}
```

### Status Code

| HTTP 상태 | 설명 |
| --- | --- |
| 201 Created | 신청 생성 성공. 상태는 `SUCCESS` 또는 `WAITING` |
| 400 Bad Request | 신청 기간이 아니거나 `X-User-Id`가 누락됨 |
| 404 Not Found | 상품 또는 유저가 존재하지 않음 |
| 409 Conflict | 동일 유저가 이미 같은 상품에 신청함 |

### 주요 검증

- `X-User-Id` 헤더가 있어야 한다.
- 상품이 존재해야 한다.
- 유저가 존재해야 한다.
- 현재 시간이 상품 신청 기간 안에 있어야 한다.
- 신청 기간이 아니면 신청 데이터를 생성하지 않고 실패 응답을 반환한다.
- 동일 유저는 같은 상품에 한 번만 신청할 수 있다.
- 잔여 정원이 있으면 `SUCCESS`, 없으면 `WAITING`으로 저장한다.
- 어떤 경우에도 `SUCCESS` 신청 수는 상품 정원을 초과할 수 없다.

## 6. 상품별 신청 목록 조회

특정 상품의 신청 목록을 조회한다.

이 API는 MVP에서 동시성 테스트 결과와 대기 순서를 검증하기 위한 확인용 엔드포인트다. 신청자 목록 전체를 노출하므로 2차 구현에서 인증을 도입하면 관리자 전용으로 제한하거나 제거한다.

```http
GET /api/products/{productId}/applications?status=WAITING&page=0&size=20
```

### Path Variable

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| productId | Long | 상품 ID |

### Query Parameter

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| status | String | false | 없음 | 신청 상태. `SUCCESS`, `WAITING` |
| page | int | false | 0 | 페이지 번호 |
| size | int | false | 20 | 페이지 크기 |

`WAITING` 신청은 부하 테스트 상황에서 수만 건까지 쌓일 수 있으므로 페이징 응답을 사용한다. `page`, `size`가 없으면 기본값을 사용한다.

정렬 기준은 `appliedAt` 오름차순이고, 이 순서가 곧 대기 순서다. 동시 신청 상황에서는 `appliedAt`이 같은 값으로 저장될 수 있으므로 `applicationId` 오름차순을 2차 정렬 기준으로 둔다.

```sql
ORDER BY applied_at ASC, id ASC
```

2차 정렬 기준이 없으면 같은 데이터에도 페이지마다 순서가 달라져 대기 순서를 검증할 수 없다.

### Response

```json
{
  "data": {
    "content": [
      {
        "applicationId": 2,
        "productId": 1,
        "userId": 101,
        "status": "WAITING",
        "appliedAt": "2026-09-07T10:01:03+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1
  }
}
```

### Status Code

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 신청 목록 조회 성공 |
| 400 Bad Request | 신청 상태 또는 페이징 값이 올바르지 않음. `errors`에 해당 필드를 담는다 |
| 404 Not Found | 상품이 존재하지 않음 |

## 7. 신청 단건 조회

신청 결과를 조회한다. 중복 신청 응답으로 받은 `applicationId`로 기존 신청 상태를 확인할 때도 사용한다.

```http
GET /api/applications/{applicationId}
```

### Path Variable

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| applicationId | Long | 신청 ID |

### Response

```json
{
  "data": {
    "applicationId": 1,
    "productId": 1,
    "userId": 1,
    "status": "SUCCESS",
    "appliedAt": "2026-09-07T10:01:00+09:00"
  }
}
```

### Status Code

| HTTP 상태 | 설명 |
| --- | --- |
| 200 OK | 신청 조회 성공 |
| 404 Not Found | 신청 내역이 존재하지 않음 |

## 8. 에러 코드

| 코드 | HTTP 상태 | 설명 |
| --- | --- | --- |
| PRODUCT_NOT_FOUND | 404 | 상품이 존재하지 않음 |
| USER_NOT_FOUND | 404 | 유저가 존재하지 않음 |
| APPLICATION_NOT_FOUND | 404 | 신청 내역이 존재하지 않음 |
| APPLICATION_PERIOD_CLOSED | 400 | 신청 기간이 아님 |
| DUPLICATE_APPLICATION | 409 | 동일 유저가 이미 신청함. 응답에 기존 `applicationId` 포함 |
| MISSING_USER_HEADER | 400 | `X-User-Id` 헤더가 누락됨 |
| INVALID_REQUEST | 400 | 요청 값이 올바르지 않음. 상세 사유는 `errors` 배열에 담는다 |

## 9. 2차 구현 후보

### 멱등성 키

2차 구현에서는 상품 신청 API에 멱등성 키를 추가한다.

```http
POST /api/products/{productId}/applications
X-User-Id: 1
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

동일한 멱등성 키로 재요청이 들어오면 새 신청을 생성하지 않고 기존 처리 결과를 반환한다.

멱등성 키와 중복 신청은 다음과 같이 구분한다.

- 같은 `Idempotency-Key`로 재요청이 오면 네트워크 재시도로 보고 기존 신청 결과를 그대로 반환한다.
- `Idempotency-Key`가 다르고 `(userId, productId)` 신청이 이미 존재하면 재시도가 아닌 중복 신청으로 보고 409 `DUPLICATE_APPLICATION`을 반환한다.

### 대기열 조회

2차 구현에서는 대기 상태인 신청자의 순번을 조회하는 API를 추가할 수 있다.

```http
GET /api/products/{productId}/applications/{applicationId}/queue-position
```

### 신청 취소

2차 구현에서는 신청 취소 API를 추가할 수 있다.

```http
PATCH /api/applications/{applicationId}/cancel
X-User-Id: 1
```

신청 취소가 발생하면 대기열의 다음 신청자를 승격하는 흐름을 구현한다.

### 비동기 알림

2차 구현에서는 신청 결과 알림을 비동기 이벤트로 분리한다.
