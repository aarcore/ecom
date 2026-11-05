import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Image,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Space,
  Switch,
  Table,
  Tag,
  message,
} from 'antd';
import { EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { api, withToast } from '@/api/client';
import type { Brand } from '@/types';

export default function BrandPage() {
  const [rows, setRows] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Brand | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await api.get<Brand[]>('/admin/catalog/brands'));
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openModal = (record?: Brand) => {
    setEditing(record ?? null);
    form.resetFields();
    form.setFieldsValue(record ? { ...record, visible: record.visible === 1 } : { sort: 0, visible: true });
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    const ok = await withToast(
      () =>
        api.post<Brand>('/admin/catalog/brands', {
          ...values,
          id: editing?.id,
          visible: values.visible ? 1 : 0,
        }),
      editing ? '品牌已更新' : '品牌已创建',
    );
    if (ok) {
      setOpen(false);
      load();
    }
  };

  const remove = async (record: Brand) => {
    const ok = await withToast(() => api.delete<void>(`/admin/catalog/brands/${record.id}`), '品牌已删除');
    if (ok !== null) load();
  };

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>品牌管理</h1>
          <div className="sub">品牌用于商品归属与前台筛选</div>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新增品牌
          </Button>
        </Space>
      </div>

      <Card className="soft-card" bordered={false}>
        <Table<Brand>
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            {
              title: '品牌',
              dataIndex: 'name',
              render: (_: unknown, row) => (
                <Space>
                  {row.logo ? (
                    <Image src={row.logo} width={34} height={34} style={{ borderRadius: 8, objectFit: 'cover' }} />
                  ) : (
                    <span style={{ fontSize: 20 }}>🏷️</span>
                  )}
                  <span style={{ fontWeight: 550 }}>{row.name}</span>
                </Space>
              ),
            },
            {
              title: '简介',
              dataIndex: 'description',
              render: (value?: string) => <span className="muted">{value || '—'}</span>,
            },
            { title: '商品数', dataIndex: 'productCount', width: 100 },
            { title: '排序', dataIndex: 'sort', width: 90 },
            {
              title: '状态',
              dataIndex: 'visible',
              width: 100,
              render: (value: number) => (
                <Tag bordered={false} color={value === 1 ? 'green' : 'default'}>
                  {value === 1 ? '启用' : '停用'}
                </Tag>
              ),
            },
            {
              title: '操作',
              width: 160,
              render: (_: unknown, row) => (
                <Space size={0}>
                  <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(row)}>
                    编辑
                  </Button>
                  <Popconfirm title="删除该品牌？" onConfirm={() => remove(row)} okButtonProps={{ danger: true }}>
                    <Button type="link" size="small" danger>
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        open={open}
        title={editing ? '编辑品牌' : '新增品牌'}
        onCancel={() => setOpen(false)}
        onOk={save}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="name" label="品牌名称" rules={[{ required: true, message: '请输入品牌名称' }]}>
            <Input placeholder="如：Nova" />
          </Form.Item>
          <Form.Item name="logo" label="品牌 Logo 地址">
            <Input placeholder="/images/brands/nova.svg" />
          </Form.Item>
          <Form.Item name="description" label="品牌简介">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Space size={16}>
            <Form.Item name="sort" label="排序权重" style={{ marginBottom: 0 }}>
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item name="visible" label="启用" valuePropName="checked" style={{ marginBottom: 0 }}>
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
