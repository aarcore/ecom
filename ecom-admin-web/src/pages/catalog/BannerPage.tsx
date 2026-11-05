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
  Select,
  Space,
  Switch,
  Table,
  Tag,
  message,
} from 'antd';
import { EditOutlined, PlusOutlined, ReloadOutlined, UploadOutlined } from '@ant-design/icons';
import { api, withToast } from '@/api/client';
import type { Banner, Category } from '@/types';

export default function BannerPage() {
  const [rows, setRows] = useState<Banner[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Banner | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await api.get<Banner[]>('/admin/catalog/banners'));
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    api.get<Category[]>('/admin/catalog/categories/flat').then(setCategories).catch(() => undefined);
  }, [load]);

  const openModal = (record?: Banner) => {
    setEditing(record ?? null);
    form.resetFields();
    form.setFieldsValue(
      record ? { ...record, enabled: record.enabled === 1 } : { position: 'home', sort: 0, enabled: true, linkType: 'none' },
    );
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    if (values.position === 'home' && values.sort === undefined) values.sort = 0;
    const ok = await withToast(
      () =>
        api.post<Banner>('/admin/catalog/banners', {
          ...values,
          id: editing?.id,
          enabled: values.enabled ? 1 : 0,
        }),
      editing ? '轮播已更新' : '轮播已创建',
    );
    if (ok) {
      setOpen(false);
      load();
    }
  };

  const remove = async (record: Banner) => {
    const ok = await withToast(() => api.delete<void>(`/admin/catalog/banners/${record.id}`), '轮播已删除');
    if (ok !== null) load();
  };

  const linkType = Form.useWatch('linkType', form);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>轮播图</h1>
          <div className="sub">首页轮播位，支持跳转商品、分类或外链</div>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新增轮播
          </Button>
        </Space>
      </div>

      <Card className="soft-card" bordered={false}>
        <Table<Banner>
          rowKey="id"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            {
              title: '预览',
              dataIndex: 'image',
              width: 200,
              render: (src: string) => (
                <Image
                  src={src}
                  width={168}
                  height={64}
                  style={{ borderRadius: 10, objectFit: 'cover', background: '#f2f4f8' }}
                />
              ),
            },
            { title: '标题', dataIndex: 'title', render: (v?: string) => v || '—' },
            {
              title: '跳转',
              dataIndex: 'linkType',
              width: 190,
              render: (_: unknown, row) => {
                const map: Record<string, string> = {
                  none: '无跳转',
                  product: '商品',
                  category: '分类',
                  url: '外链',
                };
                return (
                  <Space size={4}>
                    <Tag bordered={false}>{map[row.linkType] ?? row.linkType}</Tag>
                    {row.linkValue && <span className="muted">{row.linkValue}</span>}
                  </Space>
                );
              },
            },
            {
              title: '位置',
              dataIndex: 'position',
              width: 100,
              render: (v: string) => <Tag bordered={false} color="blue">{v}</Tag>,
            },
            { title: '排序', dataIndex: 'sort', width: 80 },
            {
              title: '状态',
              dataIndex: 'enabled',
              width: 90,
              render: (v: number) => (
                <Tag bordered={false} color={v === 1 ? 'green' : 'default'}>
                  {v === 1 ? '启用' : '停用'}
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
                  <Popconfirm title="删除该轮播？" onConfirm={() => remove(row)} okButtonProps={{ danger: true }}>
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
        title={editing ? '编辑轮播' : '新增轮播'}
        onCancel={() => setOpen(false)}
        onOk={save}
        okText="保存"
        cancelText="取消"
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item name="title" label="标题（运营内部备注）">
            <Input placeholder="如：新品首发 · Nova X1" />
          </Form.Item>
          <Form.Item name="image" label="图片地址" rules={[{ required: true, message: '请上传或填写图片地址' }]}>
            <Input placeholder="/images/banners/b1.svg" />
          </Form.Item>
          <Button
            icon={<UploadOutlined />}
            onClick={async () => {
              const input = document.createElement('input');
              input.type = 'file';
              input.accept = 'image/*';
              input.onchange = async () => {
                const file = input.files?.[0];
                if (!file) return;
                try {
                  const result = await api.upload<{ url: string }>('/admin/files/upload', file, 'banner');
                  form.setFieldValue('image', result.url);
                  message.success('上传成功');
                } catch (error) {
                  message.error(error instanceof Error ? error.message : '上传失败');
                }
              };
              input.click();
            }}
            style={{ marginBottom: 16 }}
          >
            上传图片
          </Button>

          <Space size={12} style={{ display: 'flex' }}>
            <Form.Item name="linkType" label="跳转类型" style={{ flex: 1 }}>
              <Select
                options={[
                  { value: 'none', label: '不跳转' },
                  { value: 'product', label: '商品详情' },
                  { value: 'category', label: '分类列表' },
                  { value: 'url', label: '外部链接' },
                ]}
              />
            </Form.Item>
            <Form.Item name="position" label="展示位" style={{ flex: 1 }}>
              <Select
                options={[
                  { value: 'home', label: '首页' },
                  { value: 'category', label: '分类页' },
                  { value: 'mine', label: '个人中心' },
                ]}
              />
            </Form.Item>
          </Space>

          {linkType === 'category' ? (
            <Form.Item name="linkValue" label="目标分类">
              <Select
                options={categories.map((c) => ({
                  value: String(c.id),
                  label: c.level === 2 ? `　${c.name}` : c.name,
                }))}
              />
            </Form.Item>
          ) : (
            <Form.Item name="linkValue" label="跳转值">
              <Input
                placeholder={
                  linkType === 'product' ? '商品 ID' : linkType === 'url' ? 'https://…' : '无跳转可留空'
                }
              />
            </Form.Item>
          )}

          <Space size={16}>
            <Form.Item name="sort" label="排序权重" style={{ marginBottom: 0 }}>
              <InputNumber min={0} />
            </Form.Item>
            <Form.Item name="enabled" label="启用" valuePropName="checked" style={{ marginBottom: 0 }}>
              <Switch />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
}
