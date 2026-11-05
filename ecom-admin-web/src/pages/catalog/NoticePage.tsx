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
import type { Notice } from '@/types';

export default function NoticePage() {
  const [rows, setRows] = useState<Notice[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Notice | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await api.get<Notice[]>('/admin/catalog/notices'));
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openModal = (record?: Notice) => {
    setEditing(record ?? null);
    form.resetFields();
    form.setFieldsValue(record ? { ...record, enabled: record.enabled === 1 } : { sort: 0, enabled: true });
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    const ok = await withToast(
      () =>
        api.post<Notice>('/admin/catalog/notices', {
          ...values,
          id: editing?.id,
          enabled: values.enabled ? 1 : 0,
        }),
      editing ? '公告已更新' : '公告已发布',
    );
    if (ok) {
      setOpen(false);
      load();
    }
  };

  const remove = async (record: Notice) => {
    const ok = await withToast(() => api.delete<void>(`/admin/catalog/notices/${record.id}`), '公告已删除');
    if (ok !== null) load();
  };

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>公告管理</h1>
          <div className="sub">首页滚动公告，用于活动说明与物流提示</div>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            发布公告
          </Button>
        </Space>
      </div>

      <Card className="soft-card" bordered={false}>
        <Table<Notice>
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            { title: '标题', dataIndex: 'title', render: (v: string) => <span className="cell-title">{v}</span> },
            {
              title: '内容',
              dataIndex: 'content',
              render: (v?: string) => (
                <span className="muted" style={{ display: 'inline-block', maxWidth: 520 }}>
                  {v || '—'}
                </span>
              ),
            },
            { title: '排序', dataIndex: 'sort', width: 90 },
            {
              title: '状态',
              dataIndex: 'enabled',
              width: 100,
              render: (v: number) => (
                <Tag bordered={false} color={v === 1 ? 'green' : 'default'}>
                  {v === 1 ? '展示中' : '已下线'}
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
                  <Popconfirm title="删除该公告？" onConfirm={() => remove(row)} okButtonProps={{ danger: true }}>
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
        title={editing ? '编辑公告' : '发布公告'}
        onCancel={() => setOpen(false)}
        onOk={save}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="title" label="公告标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="如：双十一预售开启" />
          </Form.Item>
          <Form.Item name="content" label="公告内容">
            <Input.TextArea rows={4} placeholder="补充说明，将在详情中展示" />
          </Form.Item>
          <Space size={16}>
            <Form.Item name="sort" label="排序权重" style={{ marginBottom: 0 }}>
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item name="enabled" label="前台展示" valuePropName="checked" style={{ marginBottom: 0 }}>
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
