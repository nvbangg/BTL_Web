package com.nvbangg.fashonshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "product_images",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"product_id", "sort_order"})
        }
)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String image;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
