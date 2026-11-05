import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  Card,
  Col,
  Divider,
  Form,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Switch,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import { ArrowLeftOutlined, DeleteOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons';
import { api, withToast } from '@/api/client';
import { PageHeader } from '@/components/PageHeader';
import type { Brand, Category, Product, Sku, Spec } from '@/types';

interface FormValues {
  name: string;
  subtitle?: string;
  categoryId: number;
  brandId?: number;
  cover?: string;
  images?: string[];
  price: number;
  originalPrice?: number;
  unit?: string;
  keywords?: string;
  detail?: string;
  status: boolean;
  isNew: boolean;
  isHot: boolean;
  isRecommend: boolean;
  sort: number;
}

export default function ProductEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form] = Form.useForm<FormValues>();
  const isEdit = Boolean(id);

  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [specs, setSpecs] = useState<Spec[]>([]);
  const [skus, setSkus] = useState<Sku[]>([]);
  const [images, setImages] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.get<Category[]>('/admin/catalog/categories/flat').then(setCategories).catch(() => undefined);
    api.get<Brand[]>('/admin/catalog/brands').then(setBrands).catch(() => undefined);
  }, []);

  useEffect(() => {
    if (!isEdit) {
      setSkus([]);
      setSpecs([]);
      setImages([]);
      return;
    }
    setLoading(true);
    api
      .get<Product>(`/admin/products/${id}`)
      .then((product) => {
        form.setFieldsValue({
          name: product.name,
          subtitle: product.subtitle,
          categoryId: product.categoryId,
          brandId: product.brandId,
          cover: product.cover,
          price: product.price,
          originalPrice: product.originalPrice,
          unit: product.unit,
          keywords: product.keywords,
          detail: product.detail,
          status: product.status === 1,
          isNew: product.isNew === 1,
          isHot: product.isHot === 1,
          isRecommend: product.isRecommend === 1,
          sort: product.sort ?? 0,
        });
        setImages(product.imageList ?? []);
        setSpecs(product.specList ?? []);
        setSkus(product.skuList ?? []);
      })
      .catch((error) => withToast(() => Promise.reject(error)))
      .finally(() => setLoading(false));
  }, [id, isEdit, form]);

  /**
   * Regenerates the SKU matrix as the cartesian product of up to two spec
   * dimensions, preserving prices/stock of SKUs that already existed.
   */
  const rebuildSkus = () => {
    const dimensions = specs.filter((s) => s.name?.trim() && s.values.length);
    if (!dimensions.length) {
      setSkus([]);
      return;
    }
    let combos: string[] = [];
    if (dimensions.length === 1) {
      combos = dimensions[0].values;
    } else {
      combos = dimensions[0].values.flatMap((a) => dimensions[1].values.map((b) => `${a},${b}`));
    }

    const existing = new Map(skus.map((s) => [s.specValues, s]));
    setSkus(
      combos.map((combo) => {
        const found = existing.get(combo);
        return (
          found ?? {
            specValues: combo,
            price: form.getFieldValue('price') ?? 0,
            stock: 0,
          }
        );
      }),
    );
  };

  const upload = async (file: File, folder: 'product') => {
    try {
      const result = await api.upload<{ url: string }>('/admin/files/upload', file, folder);
      message.success('上传成功');
      return result.url;
    } catch (error) {
      message.error(error instanceof Error ? error.message : '上传失败');
      return null;
    }
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSaving(true);
    const payload = {
      id: isEdit ? Number(id) : undefined,
      ...values,
      status: values.status ? 1 : 0,
      isNew: values.isNew ? 1 : 0,
      isHot: values.isHot ? 1 : 0,
      isRecommend: values.isRecommend ? 1 : 0,
      images,
      specs,
      skus: skus.map((s) => ({
        id: s.id,
        skuCode: s.skuCode,
        specValues: s.specValues,
        image: s.image,
        price: s.price,
        stock: s.stock,
      })),
    };

    const result = await withToast(
      () =>
        isEdit
          ? api.put<Product>(`/admin/products/${id}`, payload)
          : api.post<Product>('/admin/products', payload),
      isEdit ? '商品已更新' : '商品已创建',
    );
    setSaving(false);
    if (result) navigate('/products');
  };

  const categoryOptions = useMemo(
    () =>
      categories.map((c) => ({
        value: c.id,
        label: c.level === 2 ? `　${c.name}` : c.name,
      })),
    [categories],
  );

  return (
    <div>
      <PageHeader
        title={
          <Space align="center" size={10}>
            <Button
              icon={<ArrowLeftOutlined />}
              size="small"
              onClick={() => navigate('/products')}
              aria-label="返回商品列表"
            />
            <span>{isEdit ? '编辑商品' : '新增商品'}</span>
          </Space>
        }
        description="填写商品基础信息、规格矩阵与库存"
        extra={
          <>
            <Button onClick={() => navigate('/products')}>取消</Button>
            <Button type="primary" loading={saving} onClick={submit}>
              保存商品
            </Button>
          </>
        }
      />

      <Form form={form} layout="vertical" disabled={loading} initialValues={{ status: true, sort: 0, unit: '件' }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} xl={15}>
            <Card className="soft-card" bordered={false} title="基础信息">
              <Form.Item name="name" label="商品名称" rules={[{ required: true, message: '请输入商品名称' }]}>
                <Input placeholder="例如：Nova X1 5G 智能手机" maxLength={160} showCount />
              </Form.Item>

              <Form.Item name="subtitle" label="卖点副标题">
                <Input placeholder="一句话说明核心卖点" maxLength={255} showCount />
              </Form.Item>

              <Row gutter={12}>
                <Col span={12}>
                  <Form.Item name="categoryId" label="商品分类" rules={[{ required: true, message: '请选择分类' }]}>
                    <Select placeholder="选择分类" options={categoryOptions} showSearch optionFilterProp="label" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="brandId" label="品牌">
                    <Select
                      allowClear
                      placeholder="选择品牌"
                      options={brands.map((b) => ({ value: b.id, label: b.name }))}
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={12}>
                <Col span={8}>
                  <Form.Item name="price" label="售价" rules={[{ required: true, message: '请输入售价' }]}>
                    <InputNumber min={0} precision={2} prefix="¥" style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="originalPrice" label="划线价">
                    <InputNumber min={0} precision={2} prefix="¥" style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="unit" label="单位">
                    <Input placeholder="件 / 台 / 盒" />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item name="keywords" label="搜索关键词">
                <Input placeholder="用于搜索匹配，逗号分隔" />
              </Form.Item>

              <Form.Item name="detail" label="商品详情">
                <Input.TextArea rows={6} placeholder="支持富文本 HTML，或纯文本描述" />
              </Form.Item>
            </Card>

            <Card className="soft-card" bordered={false} title="规格与库存" style={{ marginTop: 16 }}>
              <Typography.Paragraph type="secondary" style={{ fontSize: 12.5, marginTop: -4 }}>
                最多支持两个规格维度（如 颜色 / 存储）。修改规格后点击「生成 SKU」重新计算组合。
              </Typography.Paragraph>

              {specs.map((spec, si) => (
                <Card
                  key={si}
                  size="small"
                  style={{ marginBottom: 12, background: '#fafbfd', borderColor: '#eef0f5' }}
                >
                  <Space align="start" style={{ width: '100%' }} direction="vertical" size={8}>
                    <Space.Compact style={{ width: '100%' }}>
                      <Input
                        placeholder="规格名，如 颜色"
                        value={spec.name}
                        onChange={(e) => {
                          const next = [...specs];
                          next[si] = { ...spec, name: e.target.value };
                          setSpecs(next);
                        }}
                      />
                      <Button
                        icon={<DeleteOutlined />}
                        onClick={() => {
                          const next = specs.filter((_, idx) => idx !== si);
                          setSpecs(next);
                          setSkus([]);
                        }}
                      />
                    </Space.Compact>
                    <Select
                      mode="tags"
                      style={{ width: '100%' }}
                      placeholder="输入规格值后回车，如 幻夜黑"
                      value={spec.values}
                      onChange={(values) => {
                        const next = [...specs];
                        next[si] = { ...spec, values };
                        setSpecs(next);
                      }}
                    />
                  </Space>
                </Card>
              ))}

              <Space>
                <Button
                  icon={<PlusOutlined />}
                  disabled={specs.length >= 2}
                  onClick={() => setSpecs([...specs, { name: '', values: [] }])}
                >
                  添加规格
                </Button>
                <Button onClick={rebuildSkus}>生成 SKU</Button>
              </Space>

              {skus.length > 0 && (
                <>
                  <Divider style={{ margin: '18px 0 12px' }} />
                  <div style={{ display: 'grid', gap: 8 }}>
                    {skus.map((sku, index) => (
                      <Row key={`${sku.specValues}-${index}`} gutter={8} align="middle">
                        <Col flex="1 1 200px">
                          <Tag color="blue" bordered={false} className="spec-tag">
                            {sku.specValues || '标准'}
                          </Tag>
                        </Col>
                        <Col flex="0 0 150px">
                          <InputNumber
                            min={0}
                            precision={2}
                            prefix="¥"
                            style={{ width: '100%' }}
                            value={sku.price}
                            onChange={(value) => {
                              const next = [...skus];
                              next[index] = { ...sku, price: Number(value ?? 0) };
                              setSkus(next);
                            }}
                          />
                        </Col>
                        <Col flex="0 0 150px">
                          <InputNumber
                            min={0}
                            style={{ width: '100%' }}
                            placeholder="库存"
                            value={sku.stock}
                            addonBefore="库存"
                            onChange={(value) => {
                              const next = [...skus];
                              next[index] = { ...sku, stock: Number(value ?? 0) };
                              setSkus(next);
                            }}
                          />
                        </Col>
                      </Row>
                    ))}
                  </div>
                </>
              )}
            </Card>
          </Col>

          <Col xs={24} xl={9}>
            <Card className="soft-card" bordered={false} title="商品图片">
              <Form.Item name="cover" label="封面图链接" rules={[{ required: true, message: '请上传封面图' }]}>
                <Input placeholder="上传后自动填充" />
              </Form.Item>

              <Upload
                accept="image/*"
                showUploadList={false}
                beforeUpload={async (file) => {
                  const url = await upload(file as File, 'product');
                  if (url) {
                    form.setFieldValue('cover', url);
                    if (!images.includes(url)) setImages([url, ...images]);
                  }
                  return false;
                }}
              >
                <Button icon={<UploadOutlined />} block>
                  上传封面图
                </Button>
              </Upload>

              {form.getFieldValue('cover') && (
                <img
                  src={form.getFieldValue('cover')}
                  alt="cover"
                  style={{
                    width: '100%',
                    aspectRatio: '1 / 1',
                    objectFit: 'cover',
                    borderRadius: 12,
                    marginTop: 12,
                    background: '#f2f4f8',
                  }}
                />
              )}

              <Divider style={{ margin: '18px 0' }} />

              <Typography.Text strong style={{ fontSize: 13 }}>
                详情图集（{images.length}）
              </Typography.Text>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 10 }}>
                {images.map((url) => (
                  <div key={url} style={{ position: 'relative' }}>
                    <img
                      src={url}
                      alt=""
                      style={{ width: 68, height: 68, objectFit: 'cover', borderRadius: 10, background: '#f2f4f8' }}
                    />
                    <Button
                      size="small"
                      type="text"
                      danger
                      icon={<DeleteOutlined />}
                      style={{ position: 'absolute', right: -6, top: -8, background: '#fff', borderRadius: 999 }}
                      onClick={() => setImages(images.filter((u) => u !== url))}
                    />
                  </div>
                ))}
                <Upload
                  accept="image/*"
                  showUploadList={false}
                  beforeUpload={async (file) => {
                    const url = await upload(file as File, 'product');
                    if (url) setImages([...images, url]);
                    return false;
                  }}
                >
                  <div
                    style={{
                      width: 68,
                      height: 68,
                      borderRadius: 10,
                      border: '1px dashed #d6dbe7',
                      display: 'grid',
                      placeItems: 'center',
                      cursor: 'pointer',
                      color: '#8b93a7',
                    }}
                  >
                    <PlusOutlined />
                  </div>
                </Upload>
              </div>
            </Card>

            <Card className="soft-card" bordered={false} title="销售设置" style={{ marginTop: 16 }}>
              <Form.Item name="status" label="上架状态" valuePropName="checked">
                <Switch checkedChildren="上架" unCheckedChildren="下架" />
              </Form.Item>
              <Form.Item name="isNew" label="新品标" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item name="isHot" label="热销标" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item name="isRecommend" label="首页推荐" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item name="sort" label="排序权重">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Card>
          </Col>
        </Row>
      </Form>
    </div>
  );
}
