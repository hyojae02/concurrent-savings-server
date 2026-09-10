package com.example.concurrent_savings.product;

import com.example.concurrent_savings.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@Table(name = "savings_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavingsProduct extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal interestRate;

	@Column(name = "capacity", nullable = false)
	private int capacity;

	@Column(name = "remaining_capacity", nullable = false)
	private int remainingCapacity;

	@Column(name = "start_at", nullable = false)
	private Instant startAt;

	@Column(name = "end_at", nullable = false)
	private Instant endAt;

	@Builder
	private SavingsProduct(String name, BigDecimal interestRate, int capacity,
			int remainingCapacity, Instant startAt, Instant endAt) {
		this.name = name;
		this.interestRate = interestRate;
		this.capacity = capacity;
		this.remainingCapacity = remainingCapacity;
		this.startAt = startAt;
		this.endAt = endAt;
	}
}
