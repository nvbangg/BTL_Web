package com.example.fashonshop.service;

import com.example.fashonshop.common.PageData;
import com.example.fashonshop.dto.order.CreateOrderRequest;
import com.example.fashonshop.dto.order.OrderSummaryResponse;
import com.example.fashonshop.entity.CartItem;
import com.example.fashonshop.entity.OrderEntity;
import com.example.fashonshop.entity.OrderItem;
import com.example.fashonshop.entity.ProductVariant;
import com.example.fashonshop.entity.User;
import com.example.fashonshop.exception.BadRequestException;
import com.example.fashonshop.repository.OrderRepository;
import com.example.fashonshop.repository.ProductVariantRepository;
import com.example.fashonshop.util.PageRequestUtil;
import com.example.fashonshop.util.PriceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CartService cartService;
    private final UserService userService;

    @Transactional
    public OrderSummaryResponse createOrder(Long userId, CreateOrderRequest request) {
        List<CartItem> cartItems = cartService.getAllItems(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("EMPTY_CART", "Cart is empty");
        }

        User user = userService.getUser(userId);

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setStatus("pending");
        order.setShippingName(request.shippingName().trim());
        order.setShippingPhone(request.shippingPhone().trim());
        order.setShippingAddress(request.shippingAddress().trim());

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getProductVariant();
            int quantity = cartItem.getQuantity();

            if (variant.getStock() < quantity) {
                throw new BadRequestException("OUT_OF_STOCK", "Some products exceed available stock");
            }

            BigDecimal priceAtPurchase = PriceUtil.effectiveVariantPrice(variant);
            totalPrice = totalPrice.add(priceAtPurchase.multiply(BigDecimal.valueOf(quantity)));

            variant.setStock(variant.getStock() - quantity);
            productVariantRepository.save(variant);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductVariant(variant);
            item.setQuantity(quantity);
            item.setPriceAtPurchase(priceAtPurchase);
            orderItems.add(item);
        }

        order.setTotalPrice(totalPrice);
        order.setItems(orderItems);

        OrderEntity savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);

        return new OrderSummaryResponse(
                savedOrder.getId(),
                savedOrder.getTotalPrice(),
                savedOrder.getStatus(),
                savedOrder.getCreatedAt()
        );
    }

    public PageData<OrderSummaryResponse> getOrders(Long userId, int page, int pageSize) {
        Pageable pageable = PageRequestUtil.create(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderEntity> orderPage = orderRepository.findByUserId(userId, pageable);

        List<OrderSummaryResponse> items = orderPage.getContent().stream()
                .map(order -> new OrderSummaryResponse(
                        order.getId(),
                        order.getTotalPrice(),
                        order.getStatus(),
                        order.getCreatedAt()
                ))
                .toList();

        return PageData.<OrderSummaryResponse>builder()
                .items(items)
                .page(Math.max(1, page))
                .pageSize(Math.max(1, Math.min(pageSize, 100)))
                .total(orderPage.getTotalElements())
                .build();
    }
}
