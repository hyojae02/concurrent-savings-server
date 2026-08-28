# Concurrent Savings ERD

## 1. 개요

이 문서는 Concurrent Savings의 데이터 모델 초안을 정의한다.

MVP에서는 선착순 특판 적금 신청 흐름을 구현하기 위해 다음 3개 테이블을 우선 사용한다.

- users
- savings_products
- applications

대기열, 멱등성, 알림 관련 테이블은 2차 구현에서 추가한다.

## 2. MVP ERD

```mermaid
erDiagram
    USERS ||--o{ APPLICATIONS : applies
    SAVINGS_PRODUCTS ||--o{ APPLICATIONS : receives

    USERS {
        bigint id PK
        varchar name
        varchar email UK
        datetime created_at
        datetime updated_at
    }

    SAVINGS_PRODUCTS {
        bigint id PK
        varchar name
        decimal interest_rate
        int capacity
        int remaining_capacity
        datetime start_at
        datetime end_at
        datetime created_at
        datetime updated_at
    }

    APPLICATIONS {
        bigint id PK
        bigint user_id FK
        bigint product_id FK
        varchar status
        datetime applied_at
        datetime created_at
        datetime updated_at
    }