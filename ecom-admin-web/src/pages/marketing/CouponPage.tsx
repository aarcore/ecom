import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Segmented,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  message,
} from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, withToast } from '@/api/client';
import type { Coupon } from '@/types';

const money = (n?: number) => `¥${Number(n ?? 0).toFixed(2)}`;

/** The face value rendered on the ticket stub. */
function faceValue(coupon: Coupon) {
  if (coupon.type === 'discount') {
    const rate = Number(coupon.discount ?? 10);
    return `${(rate / 10).toFixed(rate % 1 === 0 ? 0 : 1)} 折`;
  }
  return money(coupon.amount);
}

export default function CouponPage() {
  const [rows, setRows] = useState<Coupon[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Coupon | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await api.get<Coupon[]>('/admin/coupons'));
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openModal = (record?: Coupon) => {
    setEditing(record ?? null);
    form.resetFields();
    form.setFieldsValue(
      record
        ? { ...record, enabled: record.enabled === 1 }
        : { type: 'amount', amount: 20, discount: 9, minAmount: 99, total: 1000, validDays: 7, enabled: true },
    );
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    const ok = await withToast(
      () => api.post<Coupon>('/admin/coupons', { ...values, id: editing?.id, enabled: values.enabled ? 1 : 0 }),
      editing ? '优惠券已更新' : '优惠券已创建',
    );
    if (ok) {
      setOpen(false);
      load();
    }
  };

  const type = Form.useWatch('type', form);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>优惠券</h1>
          <div className="sub">满减与折扣模板，会员在个人中心或结算页领取</div>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新建优惠券
          </Button>
        </Space>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
          gap: 14,
          marginBottom: 16,
        }}
      >
        {rows.slice(0, 6).map((coupon) => {
          const ratio = coupon.total ? Math.min((coupon.taken / coupon.total) * 100, 100) : 0;
          return (
            <div
              key={coupon.id}
              className="soft-card"
              style={{
                padding: 0,
                overflow: 'hidden',
                display: 'flex',
                cursor: 'pointer',
                opacity: coupon.enabled ? 1 : 0.55,
              }}
              onClick={() => openModal(coupon)}
            >
              <div
                style={{
                  width: 108,
                  background: 'linear-gradient(135deg, #2f54eb, #6f8dff)',
                  color: '#fff',
                  display: 'grid',
                  placeItems: 'center',
                  padding: '16px 10px',
                  textAlign: 'center',
                }}
              >
                <div>
                  <div style={{ fontSize: 21, fontWeight: 700, letterSpacing: '-0.02em' }}>{faceValue(coupon)}</div>
                  <div style={{ fontSize: 11, opacity: 0.85, marginTop: 2 }}>
                    满 {Number(coupon.minAmount)} 可用
                  </div>
                </div>
              </div>
              <div style={{ flex: 1, padding: '14px 16px', minWidth: 0 }}>
                <div style={{ fontWeight: 600, fontSize: 13.5, marginBottom: 4 }}>{coupon.name}</div>
                <div className="muted" style={{ marginBottom: 10 }}>
                  领取后 {coupon.validDays} 天有效
                </div>
                <Progress
                  percent={Number(ratio.toFixed(0))}
                  size="small"
                  showInfo={false}
                  strokeColor="#2f54eb"
                  trailColor="#eef0f5"
                />
                <div className="muted" style={{ marginTop: 4 }}>
                  已领 {coupon.taken} / {coupon.total}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <Card className="soft-card" bordered={false} title="全部优惠券">
        <Table<Coupon>
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            { title: '名称', dataIndex: 'name', render: (v: string) => <span className="cell-title">{v}</span> },
            {
              title: '类型',
              dataIndex: 'type',
              width: 100,
              render: (v: string) => (
                <Tag bordered={false} color={v === 'discount' ? 'purple' : 'blue'}>
                  {v === 'discount' ? '折扣券' : '满减券'}
                </Tag>
              ),
            },
            {
              title: '面值',
              width: 110,
              render: (_: unknown, row) => <span className="money">{faceValue(row)}</span>,
            },
            {
              title: '门槛',
              dataIndex: 'minAmount',
              width: 110,
              render: (v: number) => <span className="muted">满 {money(v)}</span>,
            },
            {
              title: '领取情况',
              width: 170,
              render: (_: unknown, row) => (
                <div>
                  <div>
                    {row.taken} / {row.total}
                  </div>
                  <Progress
                    percent={row.total ? Math.round((row.taken / row.total) * 100) : 0}
                    size="small"
                    showInfo={false}
                    strokeColor="#2f54eb"
                    trailColor="#eef0f5"
                  />
                </div>
              ),
            },
            {
              title: '状态',
              dataIndex: 'enabled',
              width: 100,
              render: (v: number) => (
                <Tag bordered={false} color={v === 1 ? 'green' : 'default'}>
                  {v === 1 ? '进行中' : '已停用'}
                </Tag>
              ),
            },
            {
              title: '操作',
              width: 90,
              render: (_: unknown, row) => (
                <Button type="link" size="small" onClick={() => openModal(row)}>
                  编辑
                </Button>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        open={open}
        title={editing ? '编辑优惠券' : '新建优惠券'}
        onCancel={() => setOpen(false)}
        onOk={save}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="优惠券名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="如：满 300 减 50" />
          </Form.Item>

          <Segmented
            block
            options={[
              { label: '满减券', value: 'amount' },
              { label: '折扣券', value: 'discount' },
            ]}
            value={type}
            onChange={(value) => form.setFieldValue('type', value)}
            style={{ marginBottom: 16 }}
          />
          <Form.Item name="type" hidden>
            <Input />
          </Form.Item>

          <Space size={12} style={{ display: 'flex' }}>
            {type === 'discount' ? (
              <Form.Item name="discount" label="折扣（8.5 = 85%）" style={{ flex: 1 }}>
                <InputNumber min={0.1} max={10} step={0.5} style={{ width: '100%' }} />
              </Form.Item>
            ) : (
              <Form.Item name="amount" label="优惠金额" style={{ flex: 1 }}>
                <InputNumber min={0} precision={2} prefix="¥" style={{ width: '100%' }} />
              </Form.Item>
            )}
            <Form.Item name="minAmount" label="使用门槛" style={{ flex: 1 }}>
              <InputNumber min={0} precision={2} prefix="¥" style={{ width: '100%' }} />
            </Form.Item>
          </Space>

          <Space size={12} style={{ display: 'flex' }}>
            <Form.Item name="total" label="发行总量" style={{ flex: 1 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="validDays" label="有效天数" style={{ flex: 1 }}>
              <InputNumber min={1} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="enabled" label="启用" valuePropName="checked" style={{ flex: '0 0 80px' }}>
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
