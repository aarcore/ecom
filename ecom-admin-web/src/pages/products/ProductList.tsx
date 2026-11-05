import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Button,
  Image,
  Input,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  type TablePaginationConfig,
} from 'antd';
import { EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, withToast, type PageResult } from '@/api/client';
import { FilterBar, PageHeader, TableCard } from '@/components/PageHeader';
import type { Category, Product } from '@/types';

const money = (n: number) => `¥${Number(n).toFixed(2)}`;

export default function ProductListPage() {
  const navigate = useNavigate();
  const [rows, setRows] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<number | undefined>();
  const [categories, setCategories] = useState<Category[]>([]);
  const [categoryId, setCategoryId] = useState<number | undefined>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await api.get<PageResult<Product>>('/admin/products', {
        keyword: keyword || undefined,
        status,
        categoryId,
        page,
        limit,
      });
      setRows(result.records);
      setTotal(result.total);
    } catch (error) {
      await withToast(() => Promise.reject(error));
    } finally {
      setLoading(false);
    }
  }, [keyword, status, categoryId, page, limit]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    api.get<Category[]>('/admin/catalog/categories/flat').then(setCategories).catch(() => undefined);
  }, []);

  const toggleStatus = async (row: Product, next: boolean) => {
    const ok = await withToast(
      () => api.put<void>(`/admin/products/${row.id}/status`, null, { status: next ? 1 : 0 }),
      next ? '商品已上架' : '商品已下架',
    );
    if (ok !== null) load();
  };

  const remove = async (row: Product) => {
    const ok = await withToast(() => api.delete<void>(`/admin/products/${row.id}`), '商品已删除');
    if (ok !== null) load();
  };

  const onTableChange = (pagination: TablePaginationConfig) => {
    setPage(pagination.current ?? 1);
    setLimit(pagination.pageSize ?? 10);
  };

  return (
    <div>
      <PageHeader
        title="商品列表"
        description="管理 SPU、SKU、库存与上下架状态"
        extra={
          <>
            <Button icon={<ReloadOutlined />} onClick={load}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/products/new')}>
              新增商品
            </Button>
          </>
        }
      />

      <TableCard>
        <FilterBar
          right={
            <>
          <Input.Search
            allowClear
            placeholder="搜索商品名称"
            style={{ width: 260 }}
            onSearch={(value) => {
              setPage(1);
              setKeyword(value);
            }}
          />
          <Select
            allowClear
            placeholder="全部分类"
            style={{ width: 170 }}
            value={categoryId}
            onChange={(value) => {
              setPage(1);
              setCategoryId(value);
            }}
            options={categories.map((c) => ({
              value: c.id,
              label: c.level === 2 ? `　${c.name}` : c.name,
            }))}
          />
          <Select
            allowClear
            placeholder="全部状态"
            style={{ width: 140 }}
            value={status}
            onChange={(value) => {
              setPage(1);
              setStatus(value);
            }}
            options={[
              { value: 1, label: '已上架' },
              { value: 0, label: '已下架' },
            ]}
          />
            </>
          }
        />

        <Table<Product>
          rowKey="id"
          loading={loading}
          dataSource={rows}
          onChange={onTableChange}
          pagination={{
            current: page,
            pageSize: limit,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 件商品`,
          }}
          columns={[
            {
              title: '商品',
              dataIndex: 'name',
              width: 380,
              render: (_: unknown, row) => (
                <div className="product-cell">
                  <Image
                    src={row.cover}
                    width={46}
                    height={46}
                    style={{ borderRadius: 9, objectFit: 'cover' }}
                    fallback="data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg'/%3E"
                  />
                  <div className="meta">
                    <div className="name">{row.name}</div>
                    <div className="muted">
                      #{row.id} · {row.categoryName ?? '未分类'}
                      {row.brandName ? ` · ${row.brandName}` : ''}
                    </div>
                    <Space size={4} style={{ marginTop: 3 }}>
                      {row.isNew === 1 && <Tag color="blue" bordered={false}>新品</Tag>}
                      {row.isHot === 1 && <Tag color="volcano" bordered={false}>热销</Tag>}
                      {row.isRecommend === 1 && <Tag color="purple" bordered={false}>推荐</Tag>}
                    </Space>
                  </div>
                </div>
              ),
            },
            {
              title: '价格',
              dataIndex: 'price',
              width: 136,
              render: (value: number, row) => (
                <div>
                  <div className="money">{money(value)}</div>
                  {row.originalPrice ? (
                    <div className="muted" style={{ textDecoration: 'line-through' }}>
                      {money(row.originalPrice)}
                    </div>
                  ) : null}
                </div>
              ),
            },
            {
              title: '库存',
              dataIndex: 'stock',
              width: 100,
              render: (value: number) => (
                <Typography.Text type={value < 20 ? 'danger' : undefined}>{value}</Typography.Text>
              ),
            },
            { title: '销量', dataIndex: 'sales', width: 90 },
            {
              title: '浏览',
              dataIndex: 'views',
              width: 90,
              render: (value: number) => <span className="muted">{value ?? 0}</span>,
            },
            {
              title: '状态',
              dataIndex: 'status',
              width: 120,
              render: (value: number, row) => (
                <Tooltip title="切换上下架">
                  <Switch
                    checked={value === 1}
                    checkedChildren="上架"
                    unCheckedChildren="下架"
                    onChange={(checked) => toggleStatus(row, checked)}
                  />
                </Tooltip>
              ),
            },
            {
              title: '操作',
              width: 150,
              fixed: 'right',
              render: (_: unknown, row) => (
                <Space size={4}>
                  <Button type="link" size="small" icon={<EditOutlined />}>
                    <Link to={`/products/${row.id}/edit`}>编辑</Link>
                  </Button>
                  <Popconfirm
                    title="删除该商品？"
                    description="商品与 SKU 会一并移除"
                    okText="删除"
                    cancelText="取消"
                    okButtonProps={{ danger: true }}
                    onConfirm={() => remove(row)}
                  >
                    <Button type="link" size="small" danger>
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      </TableCard>
    </div>
  );
}
