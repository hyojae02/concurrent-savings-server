package com.example.concurrent_savings.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.concurrent_savings.TestcontainersConfiguration;
import com.example.concurrent_savings.common.JpaAuditingConfig;
import com.example.concurrent_savings.product.SavingsProductRepository;
import com.example.concurrent_savings.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class SeedIsolationTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SavingsProductRepository savingsProductRepository;

	@Test
	@DisplayName("테스트 환경에는 시드데이터가 적용되지 않는다")
	void seedDataIsNotAppliedToTestEnvironment() {
		assertThat(userRepository.count()).isZero();
		assertThat(savingsProductRepository.count()).isZero();
	}
}
