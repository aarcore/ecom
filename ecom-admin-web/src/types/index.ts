/** Shared entity types mirroring the backend VOs. */

export interface Product {
  id: number;
  name: string;
  subtitle?: string;
  cover?: string;
  price: number;
  originalPrice?: number;
  stock: number;
  sales: number;
  views: number;
  unit?: string;
  categoryId: number;
  categoryName?: string;
  brandId?: number;
  brandName?: string;
  status: number;
  isNew?: number;
  isHot?: number;
  isRecommend?: number;
  sort?: number;
  keywords?: string;
  detail?: string;
  imageList?: string[];
  specList?: Spec[];
  skuList?: Sku[];
  createdAt?: string;
}

export interface Spec {
  name: string;
  values: string[];
}

export interface Sku {
  id?: number;
  productId?: number;
  skuCode?: string;
  specValues: string;
  image?: string;
  price: number;
  stock: number;
  sales?: number;
}

export interface Category {
  id: number;
  parentId: number;
  name: string;
  icon?: string;
  banner?: string;
  level: number;
  sort: number;
  visible: number;
  productCount?: number;
  children?: Category[];
}

export interface Brand {
  id: number;
  name: string;
  logo?: string;
  description?: string;
  sort: number;
  visible: number;
  productCount?: number;
}

export interface Banner {
  id: number;
  title?: string;
  image: string;
  linkType: string;
  linkValue?: string;
  position: string;
  sort: number;
  enabled: number;
}

export interface Notice {
  id: number;
  title: string;
  content?: string;
  enabled: number;
  sort: number;
}

export interface Coupon {
  id: number;
  name: string;
  type: 'amount' | 'discount';
  amount: number;
  discount: number;
  minAmount: number;
  total: number;
  taken: number;
  validDays: number;
  enabled: number;
}

export interface OrderItem {
  id: number;
  productId: number;
  skuId: number;
  productName: string;
  productCover?: string;
  specValues?: string;
  price: number;
  quantity: number;
  totalAmount: number;
}

export interface Order {
  id: number;
  orderNo: string;
  memberId: number;
  memberNickname?: string;
  status: number;
  statusText: string;
  totalAmount: number;
  freightAmount: number;
  discountAmount: number;
  payAmount: number;
  payType?: string;
  paidAt?: string;
  receiver: string;
  receiverMobile: string;
  receiverAddress?: string;
  remark?: string;
  platform: string;
  expressCompany?: string;
  expressNo?: string;
  shippedAt?: string;
  finishedAt?: string;
  closedAt?: string;
  adminRemark?: string;
  createdAt: string;
  items: OrderItem[];
}

export interface Member {
  id: number;
  mobile?: string;
  mobileMasked?: string;
  nickname?: string;
  avatar?: string;
  gender?: number;
  genderText?: string;
  platform: string;
  level: number;
  levelName: string;
  points: number;
  balance: number;
  totalSpent: number;
  orderCount: number;
  status: number;
  lastLoginAt?: string;
  createdAt: string;
}

export interface AuditLog {
  id: number;
  operatorId?: number;
  operator?: string;
  module?: string;
  action?: string;
  method?: string;
  uri?: string;
  ip?: string;
  durationMs?: number;
  success: number;
  errorMsg?: string;
  createdAt: string;
}

export interface DashboardData {
  kpi: {
    gmvToday: number;
    gmv7: number;
    gmv30: number;
    gmvGrowth: number;
    ordersToday: number;
    orders7: number;
    orders30: number;
    ordersGrowth: number;
    avgOrderValue: number;
    pendingShipment: number;
    pendingPayment: number;
    shipped: number;
    refunding: number;
    memberTotal: number;
    memberNew7: number;
    productTotal: number;
  };
  trend: { day: string; orders: number; gmv: number }[];
  memberTrend: { day: string; members: number }[];
  categoryShare: { name: string; value: number }[];
  memberLevels: { name: string; value: number }[];
  topProducts: { name: string; sales: number; price: number; cover?: string }[];
  topBuyers: { nickname: string; orders: number; amount: number }[];
  orderStatus: Record<string, number>;
}

export const ORDER_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '待付款', color: 'gold' },
  1: { text: '待发货', color: 'blue' },
  2: { text: '待收货', color: 'cyan' },
  3: { text: '已完成', color: 'green' },
  4: { text: '已取消', color: 'default' },
  5: { text: '退款中', color: 'orange' },
  6: { text: '已退款', color: 'red' },
};
