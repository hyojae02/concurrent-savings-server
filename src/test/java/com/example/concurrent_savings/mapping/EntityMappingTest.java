package com.example.concurrent_savings.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.concurrent_savings.TestcontainersConfiguration;
import com.example.concurrent_savings.application.Application;
import com.example.concurrent_savings.application.ApplicationRepository;
import com.example.concurrent_savings.application.ApplicationStatus;
import com.example.concurrent_savings.common.JpaAuditingConfig;
import com.example.concurrent_savings.product.SavingsProduct;
import com.example.concurrent_savings.user.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class EntityMappingTest {

	@Autowired
	private TestEntityManager testEntityManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ApplicationRepository applicationRepository;

	@Test
	@DisplayName("엔티티를 저장하면 감사 시각이 자동으로 채워진다")
	void auditingFieldsArePopulated() {
		User saved = testEntityManager.persistFlushFind(
				User.builder().name("문효재").email("hyojae@example.com").build());

		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("Instant 는 저장 후 같은 시점으로 복원된다")
	void instantRoundTripsUnchanged() {
		Instant startAt = Instant.parse("2026-09-07T01:00:00Z");
		Instant endAt = Instant.parse("2026-09-30T14:59:59Z");

		SavingsProduct saved = testEntityManager.persistFlushFind(product(startAt, endAt));
		testEntityManager.clear();

		SavingsProduct found = testEntityManager.find(SavingsProduct.class, saved.getId());
		assertThat(found.getStartAt()).isEqualTo(startAt);
		assertThat(found.getEndAt()).isEqualTo(endAt);
	}

	@Test
	@DisplayName("Instant 는 DB 에 UTC 벽시계 값으로 저장된다")
	void instantIsStoredAsUtcWallClock() {
		// KST 로 저장되면 10:00:00 이 된다. UTC 정책이 실제로 적용되는지 확인한다.
		Instant startAt = Instant.parse("2026-09-07T01:00:00Z");

		SavingsProduct saved = testEntityManager.persistFlushFind(
				product(startAt, Instant.parse("2026-09-30T14:59:59Z")));

		String stored = (String) entityManager
				.createNativeQuery("SELECT DATE_FORMAT(start_at, '%Y-%m-%d %H:%i:%s') "
						+ "FROM savings_products WHERE id = :id")
				.setParameter("id", saved.getId())
				.getSingleResult();

		assertThat(stored).isEqualTo("2026-09-07 01:00:00");
	}

	@Test
	@DisplayName("동일 유저가 같은 상품에 두 번 신청하면 유니크 제약에 걸린다")
	void duplicateApplicationViolatesUniqueConstraint() {
		User user = testEntityManager.persist(
				User.builder().name("문효재").email("hyojae@example.com").build());
		SavingsProduct product = testEntityManager.persist(
				product(Instant.parse("2026-09-07T01:00:00Z"), Instant.parse("2026-09-30T14:59:59Z")));

		// 운영 코드와 같은 경로인 Repository 로 저장해야 Spring 의 예외 변환을 거친다.
		applicationRepository.saveAndFlush(application(user, product));

		assertThatThrownBy(() -> applicationRepository.saveAndFlush(application(user, product)))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uk_user_product");
	}

	@Test
	@DisplayName("대기 순서 정렬용 인덱스가 생성되어 있다")
	void waitingOrderIndexExists() {
		Object count = entityManager
				.createNativeQuery("SELECT COUNT(*) FROM information_schema.STATISTICS "
						+ "WHERE table_schema = DATABASE() AND table_name = 'applications' "
						+ "AND index_name = 'idx_product_status_applied'")
				.getSingleResult();

		assertThat(((Number) count).intValue()).isEqualTo(3);
	}

	private SavingsProduct product(Instant startAt, Instant endAt) {
		return SavingsProduct.builder()
				.name("선착순 특판 적금")
				.interestRate(new BigDecimal("5.50"))
				.capacity(100)
				.remainingCapacity(100)
				.startAt(startAt)
				.endAt(endAt)
				.build();
	}

	private Application application(User user, SavingsProduct product) {
		return Application.builder()
				.user(user)
				.product(product)
				.status(ApplicationStatus.SUCCESS)
				.appliedAt(Instant.parse("2026-09-07T01:01:00Z"))
				.build();
	}
}
