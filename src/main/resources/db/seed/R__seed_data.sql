-- 로컬 개발용 시드데이터.
-- application-local.yaml 에서만 db/seed location 을 추가하므로 local 프로파일에서만 적용된다.
-- repeatable 마이그레이션이므로 파일 변경 시 다시 실행된다.
-- 로컬 실험 데이터를 알려진 초기 상태로 되돌리기 위해 신청 내역 전체를 삭제한다.
-- 시각은 UTC 저장 정책에 맞춰 UTC_TIMESTAMP(6) 기준 상대값으로 넣는다.

DELETE FROM applications;

INSERT INTO users (id, name, email, created_at, updated_at)
VALUES (1, '김선착', 'user1@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (2, '이순번', 'user2@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (3, '박대기', 'user3@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (4, '최정원', 'user4@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (5, '정신청', 'user5@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (6, '강적금', 'user6@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (7, '조특판', 'user7@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (8, '윤동시', 'user8@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (9, '장경합', 'user9@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (10, '임마감', 'user10@example.com', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)) AS new
ON DUPLICATE KEY UPDATE name       = new.name,
                        email      = new.email,
                        updated_at = UTC_TIMESTAMP(6);

-- id 1: 진행 중, 정원 넉넉        -> 신청이 바로 SUCCESS 로 처리된다
-- id 2: 진행 중, 정원 소량        -> 4번째 신청부터 WAITING 이 된다. 동시성 재현용
-- id 3: 신청 기간 전              -> 신청 데이터를 만들지 않고 실패 응답
-- id 4: 신청 기간 후              -> 신청 데이터를 만들지 않고 실패 응답
INSERT INTO savings_products (id, name, interest_rate, capacity, remaining_capacity,
                              start_at, end_at, created_at, updated_at)
VALUES (1, '선착순 특판 적금 100좌', 5.50, 100, 100,
        UTC_TIMESTAMP(6) - INTERVAL 1 DAY, UTC_TIMESTAMP(6) + INTERVAL 7 DAY,
        UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (2, '한정 특판 적금 3좌', 7.00, 3, 3,
        UTC_TIMESTAMP(6) - INTERVAL 1 DAY, UTC_TIMESTAMP(6) + INTERVAL 7 DAY,
        UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (3, '오픈 예정 특판 적금', 6.00, 50, 50,
        UTC_TIMESTAMP(6) + INTERVAL 3 DAY, UTC_TIMESTAMP(6) + INTERVAL 10 DAY,
        UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       (4, '종료된 특판 적금', 4.50, 20, 20,
        UTC_TIMESTAMP(6) - INTERVAL 30 DAY, UTC_TIMESTAMP(6) - INTERVAL 1 DAY,
        UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)) AS new
ON DUPLICATE KEY UPDATE name               = new.name,
                        interest_rate      = new.interest_rate,
                        capacity           = new.capacity,
                        remaining_capacity = new.remaining_capacity,
                        start_at           = new.start_at,
                        end_at             = new.end_at,
                        updated_at         = UTC_TIMESTAMP(6);
