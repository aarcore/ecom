import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  DatePicker,
  Descriptions,
  Divider,
  Drawer,
  Input,
  Modal,
  Segmented,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
  type TablePaginationConfig,
} from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { api, withToast, type PageResult } from '@/api/client';
import { FilterBar, PageHeader, TableCard } from '@/components/PageHeader';
import { ORDER_STATUS, type Order } from '@/types';

const money = (n?: number) => `¥${Number(n ?? 0).toFixed(2)}`;

const STATUS_TABS = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: '0' },
  { label: '待发货', value: '1' },
  { label: '待收货', value: '2' },
  { label: '已完成', value: '3' },
  { label: '已取消', value: '4' },
  { label: '退款', value: '6' },
];

export default function OrderListPage() {
  const [rows, setRows] = useState<Order[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [statusTab, setStatusTab] = useState('all');
  const [platform, setPlatform] = useState<string | undefined>();
  const [range, setRange] = useState<[Dayjs, Dayjs] | null>(null);

  const [detail, setDetail] = useState<Order | null>(null);
  const [shipOpen, setShipOpen] = useState(false);
  const [shipForm, setShipForm] = useState({ expressCompany: '顺丰速运', expressNo: '' });
  const [shipTarget, setShipTarget] = useState<Order | null>(null);
  const [remark, setRemark] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await api.get<PageResult<Order>>('/admin/orders', {
        keyword: keyword || undefined,
        status: statusTab === 'all' ? undefined : Number(statusTab),
        platform,
        startDate: range?.[0]?.format('YYYY-MM-DD'),
        endDate: range?.[1]?.format('YYYY-MM-DD'),
        page,
        limit,
      });
      setRows(result.records);
      setTotal(result.total);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, statusTab, platform, range, page, limit]);

  useEffect(() => {
    load();
  }, [load]);

  const openDetail = async (orderNo: string) => {
    const data = await withToast(() => api.get<Order>(`/admin/orders/${orderNo}`));
    if (data) {
      setDetail(data);
      setRemark(data.adminRemark ?? '');
    }
  };

  const submitShip = async () => {
    if (!shipTarget) return;
    const ok = await withToast(
      () => api.post<Order>(`/admin/orders/${shipTarget.orderNo}/ship`, shipForm),
      '发货成功',
    );
    if (ok) {
      setShipOpen(false);
      load();
      if (detail?.orderNo === shipTarget.orderNo) openDetail(shipTarget.orderNo);
    }
  };

  const saveRemark = async () => {
    if (!detail) return;
    const ok = await withToast(
      () => api.post<Order>(`/admin/orders/${detail.orderNo}/remark`, { remark }),
      '备注已保存',
    );
    if (ok) openDetail(detail.orderNo);
  };

  const refund = async () => {
    if (!detail) return;
    const ok = await withToast(
      () => api.post<Order>(`/admin/orders/${detail.orderNo}/refund`, { remark: remark || '运营退款' }),
      '已退款',
    );
    if (ok) {
      load();
      openDetail(detail.orderNo);
    }
  };

  return (
    <div>
      <PageHeader
        title="订单管理"
        description="发货、备注与退款操作会记录到操作日志"
        extra={
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
        }
      />

      <TableCard>
        <FilterBar
          left={
          <Segmented
            options={STATUS_TABS}
            value={statusTab}
            onChange={(value) => {
              setPage(1);
              setStatusTab(String(value));
            }}
          />
          }
          right={
          <Space>
            <Input.Search
              allowClear
              placeholder="订单号 / 收货人 / 手机号"
              style={{ width: 240 }}
              onSearch={(value) => {
                setPage(1);
                setKeyword(value);
              }}
            />
            <Select
              allowClear
              placeholder="来源"
              style={{ width: 120 }}
              value={platform}
              onChange={(value) => {
                setPage(1);
                setPlatform(value);
              }}
              options={[
                { value: 'web', label: 'H5 / 网页' },
                { value: 'mp', label: '小程序' },
              ]}
            />
            <DatePicker.RangePicker
              value={range}
              onChange={(value) => {
                setPage(1);
                setRange(value as [Dayjs, Dayjs] | null);
              }}
            />
          </Space>
          }
        />

        <Table<Order>
          rowKey="id"
          loading={loading}
          dataSource={rows}
          onChange={(pagination: TablePaginationConfig) => {
            setPage(pagination.current ?? 1);
            setLimit(pagination.pageSize ?? 10);
          }}
          pagination={{
            current: page,
            pageSize: limit,
            total,
            showSizeChanger: true,
            showTotal: (t) => `共 ${t} 笔订单`,
          }}
          columns={[
            {
              title: '订单',
              dataIndex: 'orderNo',
              width: 210,
              render: (orderNo: string, row) => (
                <div>
                  <a onClick={() => openDetail(orderNo)} className="cell-title" style={{ fontFamily: 'monospace' }}>
                    {orderNo}
                  </a>
                  <div className="muted">{dayjs(row.createdAt).format('MM-DD HH:mm')}</div>
                </div>
              ),
            },
            {
              title: '商品',
              width: 260,
              render: (_: unknown, row) => {
                const first = row.items?.[0];
                if (!first) return <span className="muted">—</span>;
                return (
                  <div className="product-cell">
                    {first.productCover && (
                      <img src={first.productCover} alt="" style={{ width: 38, height: 38 }} />
                    )}
                    <div className="meta">
                      <div className="name" style={{ maxWidth: 180 }}>{first.productName}</div>
                      <div className="muted">
                        {first.specValues} × {first.quantity}
                        {row.items.length > 1 ? ` 等 ${row.items.length} 件` : ''}
                      </div>
                    </div>
                  </div>
                );
              },
            },
            {
              title: '买家',
              width: 150,
              render: (_: unknown, row) => (
                <div>
                  <div>{row.memberNickname || `#${row.memberId}`}</div>
                  <div className="muted">{row.platform === 'mp' ? '微信小程序' : 'H5 / 网页'}</div>
                </div>
              ),
            },
            {
              title: '金额',
              dataIndex: 'payAmount',
              width: 120,
              render: (value: number) => <span className="money">{money(value)}</span>,
            },
            {
              title: '状态',
              dataIndex: 'status',
              width: 100,
              render: (value: number) => {
                const meta = ORDER_STATUS[value];
                return (
                  <Tag bordered={false} color={meta?.color}>
                    {meta?.text ?? value}
                  </Tag>
                );
              },
            },
            {
              title: '操作',
              width: 170,
              fixed: 'right',
              render: (_: unknown, row) => (
                <Space size={0}>
                  <Button type="link" size="small" onClick={() => openDetail(row.orderNo)}>
                    详情
                  </Button>
                  {row.status === 1 && (
                    <Button
                      type="link"
                      size="small"
                      onClick={() => {
                        setShipTarget(row);
                        setShipForm({ expressCompany: '顺丰速运', expressNo: '' });
                        setShipOpen(true);
                      }}
                    >
                      发货
                    </Button>
                  )}
                </Space>
              ),
            },
          ]}
        />
      </TableCard>

      {/* ------------------------- detail drawer ------------------------- */}
      <Drawer
        open={Boolean(detail)}
        onClose={() => setDetail(null)}
        width={620}
        title={
          <Space>
            <span>订单详情</span>
            {detail && (
              <Tag bordered={false} color={ORDER_STATUS[detail.status]?.color}>
                {detail.statusText}
              </Tag>
            )}
          </Space>
        }
        extra={
          detail && (
            <Space>
              {detail.status === 1 && (
                <Button
                  type="primary"
                  onClick={() => {
                    setShipTarget(detail);
                    setShipForm({ expressCompany: '顺丰速运', expressNo: '' });
                    setShipOpen(true);
                  }}
                >
                  发货
                </Button>
              )}
              {[1, 2, 3].includes(detail.status) && (
                <Button danger onClick={refund}>
                  退款
                </Button>
              )}
            </Space>
          )
        }
      >
        {detail && (
          <>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="订单号">
                <span style={{ fontFamily: 'monospace' }}>{detail.orderNo}</span>
              </Descriptions.Item>
              <Descriptions.Item label="下单时间">{dayjs(detail.createdAt).format('YYYY-MM-DD HH:mm:ss')}</Descriptions.Item>
              <Descriptions.Item label="买家">
                {detail.memberNickname || `#${detail.memberId}`} · {detail.platform === 'mp' ? '小程序' : 'H5'}
              </Descriptions.Item>
              <Descriptions.Item label="收货信息">
                {detail.receiver} {detail.receiverMobile}
                <div className="muted">{detail.receiverAddress}</div>
              </Descriptions.Item>
              {detail.payType && <Descriptions.Item label="支付方式">{detail.payType}</Descriptions.Item>}
              {detail.expressNo && (
                <Descriptions.Item label="物流">
                  {detail.expressCompany} · {detail.expressNo}
                </Descriptions.Item>
              )}
              {detail.remark && <Descriptions.Item label="买家留言">{detail.remark}</Descriptions.Item>}
            </Descriptions>

            <Divider style={{ margin: '18px 0' }} />

            <Typography.Text strong>商品明细</Typography.Text>
            <div style={{ marginTop: 10, display: 'grid', gap: 10 }}>
              {detail.items.map((item) => (
                <div key={item.id} style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                  {item.productCover && (
                    <img
                      src={item.productCover}
                      alt=""
                      style={{ width: 44, height: 44, borderRadius: 9, objectFit: 'cover', background: '#f2f4f8' }}
                    />
                  )}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 500 }}>{item.productName}</div>
                    <div className="muted">
                      {item.specValues} · {money(item.price)} × {item.quantity}
                    </div>
                  </div>
                  <span className="money">{money(item.totalAmount)}</span>
                </div>
              ))}
            </div>

            <div
              style={{
                marginTop: 16,
                background: '#fafbfd',
                borderRadius: 12,
                padding: 14,
                display: 'grid',
                gap: 6,
                fontSize: 13,
              }}
            >
              <Row label="商品金额" value={money(detail.totalAmount)} />
              <Row label="运费" value={money(detail.freightAmount)} />
              <Row label="优惠" value={`- ${money(detail.discountAmount)}`} />
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  paddingTop: 8,
                  borderTop: '1px solid #eef0f5',
                  fontWeight: 650,
                  fontSize: 15,
                }}
              >
                <span>实付</span>
                <span style={{ color: '#d94848' }}>{money(detail.payAmount)}</span>
              </div>
            </div>

            <Divider style={{ margin: '18px 0' }} />
            <Typography.Text strong>商家备注</Typography.Text>
            <Input.TextArea
              rows={2}
              value={remark}
              onChange={(event) => setRemark(event.target.value)}
              placeholder="仅内部可见"
              style={{ margin: '10px 0' }}
            />
            <Button onClick={saveRemark}>保存备注</Button>
          </>
        )}
      </Drawer>

      {/* --------------------------- ship modal --------------------------- */}
      <Modal
        open={shipOpen}
        title={`订单发货 · ${shipTarget?.orderNo ?? ''}`}
        onCancel={() => setShipOpen(false)}
        onOk={submitShip}
        okText="确认发货"
        cancelText="取消"
      >
        <Space direction="vertical" size={12} style={{ width: '100%', marginTop: 12 }}>
          <Select
            style={{ width: '100%' }}
            value={shipForm.expressCompany}
            onChange={(value) => setShipForm({ ...shipForm, expressCompany: value })}
            options={[
              { value: '顺丰速运', label: '顺丰速运' },
              { value: '京东物流', label: '京东物流' },
              { value: '中通快递', label: '中通快递' },
              { value: '圆通速递', label: '圆通速递' },
              { value: '韵达快递', label: '韵达快递' },
            ]}
          />
          <Input
            placeholder="运单号"
            value={shipForm.expressNo}
            onChange={(event) => setShipForm({ ...shipForm, expressNo: event.target.value })}
          />
        </Space>
      </Modal>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
      <span style={{ color: '#8b93a7' }}>{label}</span>
      <span>{value}</span>
    </div>
  );
}
