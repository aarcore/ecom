/** Types mirroring the C-end API payloads. */

export interface Banner {
  id: number;
  title?: string;
  image: string;
  linkType: string;
  linkValue?: string;
  position: string;
}

export interface Notice {
  id: number;
  title: string;
  content?: string;
}

export interface Category {
  id: number;
  parentId: number;
  name: string;
  icon?: string;
  banner?: string;
  level: number;
  productCount?: number;
  children?: Category[];
}

export interface Spec {
  name: string;
  values: string[];
}

export interface Sku {
  id: number;
  productId: number;
  skuCode?: string;
  specValues: string;
  image?: string;
  price: number;
  stock: number;
  sales?: number;
}

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
  brandName?: string;
  categoryName?: string;
  isNew?: number;
  isHot?: number;
  detail?: string;
  imageList?: string[];
  specList?: Spec[];
  skuList?: Sku[];
}

export interface HomeData {
  banners: Banner[];
  categories: Category[];
  notices: Notice[];
  recommend: Product[];
  newArrivals: Product[];
  hot: Product[];
}

export interface ProductDetail {
  product: Product;
  related: Product[];
}

export interface CartItem {
  id: number;
  productId: number;
  skuId: number;
  productName: string;
  cover?: string;
  specValues?: string;
  price: number;
  originalPrice?: number;
  quantity: number;
  stock: number;
  checked: number;
  available: boolean;
  lineAmount?: number;
}

export interface CartData {
  items: CartItem[];
  total: number;
  checkedCount: number;
  checkedAmount: number;
  allChecked: boolean;
}

export interface Address {
  id: number;
  receiver: string;
  mobile: string;
  province?: string;
  city?: string;
  district?: string;
  detail: string;
  isDefault?: number;
  fullAddress?: string;
}

export interface MemberCoupon {
  id: number;
  couponId: number;
  name: string;
  type: string;
  amount: number;
  discount: number;
  minAmount: number;
  status: number;
  statusText: string;
  startAt?: string;
  endAt?: string;
  usable?: boolean;
  discountValue?: number;
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
  receiverAddress: string;
  remark?: string;
  expressCompany?: string;
  expressNo?: string;
  createdAt: string;
  payDeadlineSeconds?: number;
  items: OrderItem[];
}

export interface Member {
  id: number;
  mobile?: string;
  mobileMasked?: string;
  nickname?: string;
  avatar?: string;
  gender?: number;
  level: number;
  levelName: string;
  points: number;
  balance: number;
  totalSpent: number;
  orderCount: number;
}

export interface CheckoutPreview {
  items: CartItem[];
  totalAmount: number;
  freightAmount: number;
  discountAmount: number;
  payAmount: number;
  address: Address | null;
  availableCoupons: MemberCoupon[];
  selectedCouponId?: number | null;
}
