const MOCK_DATA = {
  categories: [
    { id: 1, name: 'Áo sơ mi' },
    { id: 2, name: 'Áo thun' },
    { id: 3, name: 'Quần dài' },
    { id: 4, name: 'Áo khoác' },
    { id: 5, name: 'Quần short' },
    { id: 6, name: 'Áo polo' },
    { id: 7, name: 'Phụ kiện' },
    { id: 8, name: 'Đồ thể thao' }
  ],

  productsPage1: [
    {
      id: 1,
      name: 'Áo Sơ-mi Ngắn Tay Asymmetrical',
      categoryId: 1,
      gender: 'Nam',
      basePrice: 399000,
      description: 'Áo sơ mi form rộng, chất liệu cotton thoáng mát, dễ phối đồ.',
      images: [
        'mock/images/product1/image.png',
        'mock/images/product1/image2.png',
        'mock/images/product1/image3.png',
        'mock/images/product1/image4.png'
      ],
      soldCount: 1200,
      createdAt: '2026-01-15',
      variants: [
        { id: 101, color: 'Đen', size: 'S', stock: 30, price: null },
        { id: 102, color: 'Đen', size: 'M', stock: 50, price: null },
        { id: 103, color: 'Đen', size: 'L', stock: 40, price: null },
        { id: 104, color: 'Trắng', size: 'M', stock: 45, price: null }
      ]
    },
    {
      id: 2,
      name: 'Áo Sơ-mi Ngắn Tay Raglan Collection',
      categoryId: 1,
      gender: 'Nam',
      basePrice: 449000,
      description: 'Áo sơ mi raglan regular fit, chất liệu linen blend nhẹ.',
      images: [
        'mock/images/product2/image.png',
        'mock/images/product2/image2.png',
        'mock/images/product2/image3.png',
        'mock/images/product2/image4.png'
      ],
      soldCount: 890,
      createdAt: '2026-01-20',
      variants: [
        { id: 201, color: 'Trắng sọc', size: 'S', stock: 20, price: null },
        { id: 202, color: 'Trắng sọc', size: 'M', stock: 35, price: null },
        { id: 203, color: 'Xanh nhạt', size: 'L', stock: 15, price: null }
      ]
    },
    {
      id: 3,
      name: 'Áo Không Tay Thêu Hoa Collection',
      categoryId: 2,
      gender: 'Nữ',
      basePrice: 349000,
      description: 'Áo thun không tay thêu hoa, chất liệu cotton mềm mại.',
      images: [
        'mock/images/product3/image.png',
        'mock/images/product3/image2.png',
        'mock/images/product3/image3.png'
      ],
      soldCount: 1560,
      createdAt: '2026-01-10',
      variants: [
        { id: 301, color: 'Hồng', size: 'S', stock: 0, price: null },
        { id: 302, color: 'Hồng', size: 'M', stock: 0, price: null },
        { id: 303, color: 'Trắng', size: 'M', stock: 0, price: null }
      ]
    },
    {
      id: 4,
      name: 'Áo Sơ-mi Ngắn Tay Relax Fit Collection',
      categoryId: 1,
      gender: 'Unisex',
      basePrice: 399000,
      description: 'Áo sơ mi relax fit, form rộng thoải mái cho nam và nữ.',
      images: [
        'mock/images/product4/image.png',
        'mock/images/product4/image2.png',
        'mock/images/product4/image3.png'
      ],
      soldCount: 750,
      createdAt: '2026-02-01',
      variants: [
        { id: 401, color: 'Xanh navy', size: 'M', stock: 30, price: null },
        { id: 402, color: 'Xanh navy', size: 'L', stock: 25, price: null },
        { id: 403, color: 'Đen', size: 'L', stock: 15, price: null }
      ]
    },
    {
      id: 5,
      name: 'Quần Jeans Nam Regular Cán Túi',
      categoryId: 3,
      gender: 'Nam',
      basePrice: 449000,
      description: 'Quần jeans regular fit trẻ trung.',
      images: [
        'mock/images/product1/image4.png',
        'mock/images/product1/image2.png'
      ],
      soldCount: 680,
      createdAt: '2026-02-05',
      variants: [
        { id: 501, color: 'Xanh chàm', size: '28', stock: 25, price: null },
        { id: 502, color: 'Đen', size: '30', stock: 30, price: null }
      ]
    },
    {
      id: 6,
      name: 'Áo Khoác Gió Thể Thao Cao Cấp',
      categoryId: 4,
      gender: 'Nam',
      basePrice: 550000,
      description: 'Áo khoác gió chống nước nhẹ.',
      images: [
        'mock/images/product2/image6.png',
        'mock/images/product2/image5.png',
        'mock/images/product2/image4.png'
      ],
      soldCount: 420,
      createdAt: '2026-01-25',
      variants: [
        { id: 601, color: 'Trắng', size: 'M', stock: 15, price: null },
        { id: 602, color: 'Đen', size: 'L', stock: 12, price: null }
      ]
    },
    {
      id: 7,
      name: 'Quần Short Thể Thao Nam',
      categoryId: 5,
      gender: 'Nam',
      basePrice: 299000,
      description: 'Quần short nhanh khô, thoáng mát.',
      images: [
        'mock/images/product3/image2.png',
        'mock/images/product3/image3.png'
      ],
      soldCount: 950,
      createdAt: '2026-02-10',
      variants: [
        { id: 701, color: 'Xám', size: 'M', stock: 30, price: null },
        { id: 702, color: 'Đen', size: 'L', stock: 20, price: null }
      ]
    },
    {
      id: 8,
      name: 'Áo Polo Nam Thể Thao',
      categoryId: 6,
      gender: 'Nam',
      basePrice: 389000,
      description: 'Áo polo thể thao cổ bẻ, chất liệu thoáng khí.',
      images: [
        'mock/images/product4/image2.png',
        'mock/images/product4/image.png',
        'mock/images/product4/image3.png'
      ],
      soldCount: 1100,
      createdAt: '2026-02-15',
      variants: [
        { id: 801, color: 'Đen', size: 'M', stock: 40, price: null },
        { id: 802, color: 'Trắng', size: 'L', stock: 25, price: null }
      ]
    },
    {
      id: 9,
      name: 'Áo Thun Nam Cổ Tròn Basic',
      categoryId: 2,
      gender: 'Nam',
      basePrice: 199000,
      description: 'Áo thun basic dễ phối đồ.',
      images: [
        'mock/images/product1/image3.png',
        'mock/images/product1/image.png',
        'mock/images/product1/image2.png'
      ],
      soldCount: 2100,
      createdAt: '2026-01-05',
      variants: [
        { id: 901, color: 'Đen', size: 'M', stock: 60, price: null },
        { id: 902, color: 'Trắng', size: 'L', stock: 45, price: null }
      ]
    },
    {
      id: 10,
      name: 'Quần Tây Nam Slim Fit',
      categoryId: 3,
      gender: 'Nam',
      basePrice: 520000,
      description: 'Quần tây slim fit cho đi làm và sự kiện.',
      images: [
        'mock/images/product2/image4.png',
        'mock/images/product2/image6.png'
      ],
      soldCount: 560,
      createdAt: '2026-02-20',
      variants: [
        { id: 1001, color: 'Đen', size: '30', stock: 25, price: null },
        { id: 1002, color: 'Xanh navy', size: '31', stock: 15, price: null }
      ]
    },
    {
      id: 11,
      name: 'Áo Khoác Bomber Nữ',
      categoryId: 4,
      gender: 'Nữ',
      basePrice: 620000,
      description: 'Áo khoác bomber nữ dễ phối layer.',
      images: [
        'mock/images/product3/image.png',
        'mock/images/product3/image3.png',
        'mock/images/product3/image2.png'
      ],
      soldCount: 380,
      createdAt: '2026-02-25',
      variants: [
        { id: 1101, color: 'Đen', size: 'S', stock: 15, price: null },
        { id: 1102, color: 'Kem', size: 'M', stock: 15, price: null }
      ]
    },
    {
      id: 12,
      name: 'Áo Thun Nữ Oversize',
      categoryId: 2,
      gender: 'Nữ',
      basePrice: 249000,
      description: 'Áo thun oversize thoải mái trẻ trung.',
      images: [
        'mock/images/product4/image3.png',
        'mock/images/product4/image.png'
      ],
      soldCount: 1800,
      createdAt: '2026-01-18',
      variants: [
        { id: 1201, color: 'Trắng', size: 'M', stock: 40, price: null },
        { id: 1202, color: 'Đen', size: 'M', stock: 35, price: null }
      ]
    }
  ],

  productsPage2: [
    {
      id: 13,
      name: 'Áo Hoodie Unisex Minimal',
      categoryId: 4,
      gender: 'Unisex',
      basePrice: 589000,
      description: 'Hoodie unisex form boxy.',
      images: [
        'mock/images/product1/image2.png',
        'mock/images/product1/image4.png',
        'mock/images/product1/image.png'
      ],
      soldCount: 640,
      createdAt: '2026-02-27',
      variants: [
        { id: 1301, color: 'Xám', size: 'M', stock: 22, price: null },
        { id: 1302, color: 'Đen', size: 'L', stock: 18, price: null }
      ]
    },
    {
      id: 14,
      name: 'Áo Sơ-mi Linen Nữ Cổ V',
      categoryId: 1,
      gender: 'Nữ',
      basePrice: 459000,
      description: 'Áo linen nhẹ thoáng phù hợp mùa hè.',
      images: [
        'mock/images/product2/image3.png',
        'mock/images/product2/image2.png',
        'mock/images/product2/image5.png'
      ],
      soldCount: 510,
      createdAt: '2026-03-01',
      variants: [
        { id: 1401, color: 'Be', size: 'S', stock: 20, price: null },
        { id: 1402, color: 'Trắng', size: 'M', stock: 26, price: null }
      ]
    },
    {
      id: 15,
      name: 'Quần Jogger Nam Techwear',
      categoryId: 8,
      gender: 'Nam',
      basePrice: 499000,
      description: 'Quần jogger techwear có túi hộp.',
      images: [
        'mock/images/product4/image.png',
        'mock/images/product4/image2.png'
      ],
      soldCount: 470,
      createdAt: '2026-03-03',
      variants: [
        { id: 1501, color: 'Đen', size: 'M', stock: 24, price: null },
        { id: 1502, color: 'Xám đậm', size: 'L', stock: 16, price: null }
      ]
    }
  ],

  users: [
    { id: 1, email: 'nvbangg@gmail.com', password: '123456', displayName: 'nvbangg', role: 'admin', phone: '0987.654.321', address: 'PTIT Hà Nội' },
    { id: 2, email: 'tranb@gmail.com', password: '123456', displayName: 'tranb', role: 'user', phone: '0912.345.678', address: 'Cầu Giấy, Hà Nội' },
    { id: 3, email: 'levanc@gmail.com', password: '123456', displayName: 'levanc', role: 'user', phone: '0901.234.567', address: 'Hoàng Mai, Hà Nội' }
  ],

  orders: [
    {
      id: 'ORD-0304-001',
      userId: 1,
      userEmail: 'nvbangg@gmail.com',
      recipientName: 'Nguyễn Văn Bằng',
      phone: '0987.654.321',
      address: 'Học viện Công nghệ Bưu chính Viễn thông, Hà Nội',
      status: 'shipping',
      total: 1297000,
      createdAt: '2026-03-04',
      updatedAt: '2026-03-05',
      items: [
        { productId: 1, variantId: 102, name: 'Áo Sơ-mi Ngắn Tay Asymmetrical', color: 'Đen', size: 'M', quantity: 1, price: 399000, image: 'mock/images/product1/image.png' },
        { productId: 5, variantId: 501, name: 'Quần Jeans Nam Regular Cán Túi', color: 'Xanh chàm', size: '28', quantity: 2, price: 449000, image: 'mock/images/product1/image4.png' }
      ]
    },
    {
      id: 'ORD-0225-089',
      userId: 1,
      userEmail: 'nvbangg@gmail.com',
      recipientName: 'Nguyễn Văn Bằng',
      phone: '0987.654.321',
      address: 'Ký túc xá B2, Học viện Công nghệ Bưu chính Viễn thông',
      status: 'delivered',
      total: 550000,
      createdAt: '2026-02-25',
      updatedAt: '2026-02-28',
      items: [
        { productId: 6, variantId: 601, name: 'Áo Khoác Gió Thể Thao Cao Cấp', color: 'Trắng', size: 'L', quantity: 1, price: 550000, image: 'mock/images/product2/image6.png' }
      ]
    }
  ],

  statistics: {
    revenueThisMonth: 128500000,
    revenueThisYear: 1450000000,
    revenueAllTime: 4850000000,
    monthlyRevenue: {
      2026: [95000000, 78000000, 128500000, 0, 0, 0, 0, 0, 0, 0, 0, 0],
      2025: [120000000, 135000000, 98000000, 140000000, 155000000, 110000000, 125000000, 145000000, 130000000, 160000000, 175000000, 190000000]
    }
  },

  cart: {
    1: [
      { itemId: 1, productId: 1, variantId: 102, quantity: 1 },
      { itemId: 2, productId: 5, variantId: 501, quantity: 2 }
    ],
    2: [
      { itemId: 3, productId: 8, variantId: 801, quantity: 1 }
    ]
  }
};
