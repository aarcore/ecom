import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
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
import type { Category } from '@/types';

export default function CategoryPage() {
  const [rows, setRows] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const tree = await api.get<Category[]>('/admin/catalog/categories');
      setRows(tree);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openModal = (record?: Category, parentId?: number) => {
    setEditing(record ?? null);
    form.resetFields();
    form.setFieldsValue(
      record
        ? { ...record }
        : { parentId: parentId ?? 0, sort: 0, visible: true, level: parentId ? 2 : 1 },
    );
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    const payload = {
      ...values,
      id: editing?.id,
      visible: values.visible ? 1 : 0,
      parentId: values.parentId ?? 0,
    };
    const ok = await withToast(
      () => api.post<Category>('/admin/catalog/categories', payload),
      editing ? '分类已更新' : '分类已创建',
    );
    if (ok) {
      setOpen(false);
      load();
    }
  };

  const remove = async (record: Category) => {
    const ok = await withToast(
      () => api.delete<void>(`/admin/catalog/categories/${record.id}`),
      '分类已删除',
    );
    if (ok !== null) load();
  };

  const parents = rows.filter((c) => c.parentId === 0);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>商品分类</h1>
          <div className="sub">两级分类树，前台分类页与筛选面板共用</div>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新增一级分类
          </Button>
        </Space>
      </div>

      <Card className="soft-card" bordered={false}>
        <Table<Category>
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={false}
          expandable={{ defaultExpandAllRows: true }}
          columns={[
            {
              title: '分类',
              dataIndex: 'name',
              render: (_: unknown, row) => (
                <Space>
                  <span style={{ fontSize: 17 }}>{row.icon || '📦'}</span>
                  <span style={{ fontWeight: row.parentId === 0 ? 600 : 400 }}>{row.name}</span>
                  {row.parentId === 0 && (
                    <Tag bordered={false} color="blue">
                      一级
                    </Tag>
                  )}
                </Space>
              ),
            },
            { title: 'ID', dataIndex: 'id', width: 80 },
            {
              title: '商品数',
              dataIndex: 'productCount',
              width: 100,
              render: (value: number) => <span className="muted">{value ?? 0}</span>,
            },
            { title: '排序', dataIndex: 'sort', width: 90 },
            {
              title: '前台可见',
              dataIndex: 'visible',
              width: 110,
              render: (value: number) => (
                <Tag bordered={false} color={value === 1 ? 'green' : 'default'}>
                  {value === 1 ? '显示' : '隐藏'}
                </Tag>
              ),
            },
            {
              title: '操作',
              width: 220,
              render: (_: unknown, row) => (
                <Space size={0}>
                  <Button type="link" size="small" icon={<EditOutlined />} onClick={() => openModal(row)}>
                    编辑
                  </Button>
                  {row.parentId === 0 && (
                    <Button type="link" size="small" onClick={() => openModal(undefined, row.id)}>
                      添加子分类
                    </Button>
                  )}
                  <Popconfirm title="删除该分类？" onConfirm={() => remove(row)} okButtonProps={{ danger: true }}>
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
        title={editing ? '编辑分类' : '新增分类'}
        onCancel={() => setOpen(false)}
        onOk={save}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="parentId" label="上级分类">
            <select
              className="ant-input"
              style={{ width: '100%', height: 36, borderRadius: 10, padding: '0 11px' }}
            >
              <option value={0}>— 作为一级分类 —</option>
              {parents
                .filter((p) => p.id !== editing?.id)
                .map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
            </select>
          </Form.Item>
          <Form.Item name="name" label="分类名称" rules={[{ required: true, message: '请输入分类名称' }]}>
            <Input placeholder="如：手机数码" />
          </Form.Item>
          <Form.Item name="icon" label="图标（emoji 或图片地址）">
            <Input placeholder="📱" />
          </Form.Item>
          <Form.Item name="banner" label="分类横幅图">
            <Input placeholder="/images/banners/cat-digital.svg" />
          </Form.Item>
          <Space size={16}>
            <Form.Item name="sort" label="排序权重" style={{ marginBottom: 0 }}>
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item name="visible" label="前台可见" valuePropName="checked" style={{ marginBottom: 0 }}>
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
