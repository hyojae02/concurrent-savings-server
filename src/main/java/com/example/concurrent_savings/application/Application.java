package com.example.concurrent_savings.application;

import com.example.concurrent_savings.common.BaseEntity;
import com.example.concurrent_savings.product.SavingsProduct;
import com.example.concurrent_savings.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@Table(
		name = "applications",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_user_product",
				columnNames = {"user_id", "product_id"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false,
			foreignKey = @jakarta.persistence.ForeignKey(name = "fk_applications_user"))
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false,
			foreignKey = @jakarta.persistence.ForeignKey(name = "fk_applications_product"))
	private SavingsProduct product;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ApplicationStatus status;

	@Column(name = "applied_at", nullable = false)
	private Instant appliedAt;

	@Builder
	private Application(User user, SavingsProduct product, ApplicationStatus status, Instant appliedAt) {
		this.user = user;
		this.product = product;
		this.status = status;
		this.appliedAt = appliedAt;
	}
}
