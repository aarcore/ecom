import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  Col,
  Divider,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import {
  ApiOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { api, withToast } from '@/api/client';

/**
 * A configured channel as the backend returns it.
 *
 * Note what is absent: there is no field carrying a secret. The API only ever
 * reports whether one is present, which is why the form never needs to render a
 * masked value and cannot accidentally resubmit one.
 */
interface PayChannelView {
  id: number;
  code: string;
  name: string;
  provider: string;
  enabled: boolean;
  mode: string;
  appId?: string;
  merchantId?: string;
  hasPrivateKey: boolean;
  hasPublicKey: boolean;
  hasApiKey: boolean;
  hasApiSecret: boolean;
  notifyUrl?: string;
  returnUrl?: string;
  settings: Record<string, string>;
  sort?: number;
  remark?: string;
  supported: boolean;
  healthMessage?: string | null;
}

/** An adapter compiled into this build. */
interface PayProvider {
  code: string;
  name: string;
  redirect: boolean;
  settings: { key: string; label: string; description: string }[];
}

/** Which credential fields a provider actually reads, per its adapter. */
const CREDENTIAL_FIELDS: Record<
  string,
  { appId?: string; merchantId?: string; privateKey?: string; publicKey?: string; apiKey?: string; apiSecret?: string }
> = {
  mock: {},
  stripe: { apiKey: 'Secret Key（sk_ 开头）' },
  wechat: {
    appId: 'AppID',
    merchantId: '商户号 mchid',
    privateKey: '商户 API 私钥（PEM）',
    apiKey: 'APIv3 密钥',
  },
  alipay: {
    appId: '应用 AppID',
    privateKey: '应用私钥（PEM）',
    publicKey: '支付宝公钥（PEM）',
  },
};

export default function PayChannelPage() {
  const [rows, setRows] = useState<PayChannelView[]>([]);
  const [providers, setProviders] = useState<PayProvider[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<PayChannelView | null>(null);
  const [testing, setTesting] = useState<string | null>(null);
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [channels, adapterList] = await Promise.all([
        api.get<PayChannelView[]>('/admin/pay/channels'),
        api.get<PayProvider[]>('/admin/pay/channels/providers'),
      ]);
      setRows(channels);
      setProviders(adapterList);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  /** Provider chosen in the form, which decides the credential fields shown. */
  const providerCode = Form.useWatch('provider', form) as string | undefined;
  const credentialFields = CREDENTIAL_FIELDS[providerCode ?? ''] ?? {};
  const providerMeta = useMemo(
    () => providers.find((item) => item.code === providerCode),
    [providers, providerCode],
  );

  const openModal = (record?: PayChannelView) => {
    setEditing(record ?? null);
    form.resetFields();
    form.setFieldsValue(
      record
        ? {
            code: record.code,
            name: record.name,
            provider: record.provider,
            mode: record.mode,
            enabled: record.enabled,
            appId: record.appId,
            merchantId: record.merchantId,
            notifyUrl: record.notifyUrl,
            returnUrl: record.returnUrl,
            sort: record.sort ?? 0,
            remark: record.remark,
            settings: record.settings ?? {},
            // secrets are deliberately left blank: the console never receives them
            privateKey: '',
            publicKey: '',
            apiKey: '',
            apiSecret: '',
          }
        : { mode: 'test', enabled: false, sort: 0, settings: {} },
    );
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    const payload = {
      ...values,
      settings: normaliseSettings(values.settings),
    };
    const ok = await withToast(
      () => api.post<PayChannelView>('/admin/pay/channels', payload),
      editing ? '渠道已更新' : '渠道已创建',
    );
    if (ok) {
      setOpen(false);
      load();
    }
  };

  const toggle = async (record: PayChannelView, enabled: boolean) => {
    const ok = await withToast(
      () =>
        api.put<PayChannelView>(`/admin/pay/channels/${record.code}/enabled`, { enabled }),
      enabled ? '渠道已启用' : '渠道已停用',
    );
    if (ok) load();
  };

  const test = async (record: PayChannelView) => {
    setTesting(record.code);
    const result = await withToast(() =>
      api.post<{ ok: boolean; message: string }>(`/admin/pay/channels/${record.code}/test`),
    );
    setTesting(null);
    if (!result) return;
    if (result.ok) {
      message.success(result.message);
    } else {
      message.warning(result.message);
    }
  };

  const remove = async (record: PayChannelView) => {
    const ok = await withToast(
      () => api.delete<void>(`/admin/pay/channels/${record.code}`),
      '渠道配置已删除',
    );
    if (ok !== null) load();
  };

  const columns = [
    {
      title: '渠道',
      dataIndex: 'name',
      render: (_: string, row: PayChannelView) => (
        <Space direction="vertical" size={0}>
          <Space size={6}>
            <Typography.Text strong>{row.name}</Typography.Text>
            <Tag bordered={false}>{row.code}</Tag>
            {!row.supported && (
              <Tag bordered={false} color="default">
                本版本不支持
              </Tag>
            )}
          </Space>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {row.remark || '—'}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: '模式',
      dataIndex: 'mode',
      width: 110,
      render: (mode: string) => (
        <Tag bordered={false} color={mode === 'live' ? 'red' : mode === 'test' ? 'blue' : 'default'}>
          {mode === 'live' ? '生产' : mode === 'test' ? '沙箱' : '演示'}
        </Tag>
      ),
    },
    {
      title: '配置状态',
      width: 220,
      render: (_: unknown, row: PayChannelView) => {
        if (!row.supported) {
          return <Tag bordered={false}>缺少适配器</Tag>;
        }
        if (row.healthMessage) {
          return (
            <Tooltip title={row.healthMessage}>
              <Tag bordered={false} color="warning" icon={<ExclamationCircleOutlined />}>
                配置不完整
              </Tag>
            </Tooltip>
          );
        }
        return (
          <Tag bordered={false} color="success" icon={<CheckCircleOutlined />}>
            就绪
          </Tag>
        );
      },
    },
    {
      title: '凭据',
      width: 180,
      render: (_: unknown, row: PayChannelView) => {
        const flags = [
          ['私钥', row.hasPrivateKey],
          ['公钥', row.hasPublicKey],
          ['Key', row.hasApiKey],
          ['Secret', row.hasApiSecret],
        ].filter(([, has]) => has);
        if (flags.length === 0) return <Typography.Text type="secondary">无</Typography.Text>;
        return (
          <Space size={4} wrap>
            {flags.map(([label]) => (
              <Tag key={String(label)} bordered={false} color="processing" icon={<SafetyCertificateOutlined />}>
                {String(label)}
              </Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      width: 90,
      render: (enabled: boolean, row: PayChannelView) => (
        <Switch
          checked={enabled}
          // a channel with no adapter or with missing credentials must not be
          // switchable: it would show up at checkout and then fail
          disabled={!row.supported || Boolean(row.healthMessage)}
          onChange={(next) => toggle(row, next)}
        />
      ),
    },
    {
      title: '操作',
      width: 210,
      render: (_: unknown, row: PayChannelView) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => openModal(row)}
          >
            配置
          </Button>
          <Button
            type="link"
            size="small"
            icon={<ApiOutlined />}
            loading={testing === row.code}
            onClick={() => test(row)}
          >
            检测
          </Button>
          {row.code !== 'mock' && (
            <Popconfirm
              title="删除该渠道配置？"
              description="删除后需要重新填写全部凭据"
              onConfirm={() => remove(row)}
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  /** Providers this build supports but that have no row yet. */
  const addable = providers.filter((provider) => !rows.some((row) => row.code === provider.code));

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>支付渠道</h1>
          <div className="sub">
            管理商城可用的支付方式。渠道由后端适配器提供，启停与凭据均在此配置
          </div>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>
            刷新
          </Button>
          <Button type="primary" onClick={() => openModal()}>
            新增渠道
          </Button>
        </Space>
      </div>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16, borderRadius: 10 }}
        message="凭据加密存储"
        description={
          <div style={{ fontSize: 12.5, lineHeight: 1.8 }}>
            渠道私钥与密钥以 AES-GCM 加密后写入数据库，页面只显示「是否已配置」，不回显明文。
            修改时留空表示保持原值。
            {addable.length > 0 && ` 当前可添加：${addable.map((p) => p.name).join('、')}。`}
          </div>
        }
      />

      <Card className="soft-card" bordered={false}>
        <Table
          rowKey="code"
          loading={loading}
          dataSource={rows}
          columns={columns}
          pagination={false}
          scroll={{ x: 900 }}
        />
      </Card>

      <Modal
        open={open}
        title={editing ? `配置 · ${editing.name}` : '新增支付渠道'}
        width={720}
        onCancel={() => setOpen(false)}
        onOk={save}
        okText="保存"
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 12 }}>
          <Row gutter={12}>
            <Col xs={24} md={8}>
              <Form.Item
                name="code"
                label="渠道标识"
                rules={[{ required: true, message: '请填写渠道标识' }]}
                tooltip="与后端适配器一致，保存后不可更改"
              >
                <Input placeholder="stripe" disabled={Boolean(editing)} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="name" label="显示名称" rules={[{ required: true, message: '请填写名称' }]}>
                <Input placeholder="Stripe" />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="provider" label="渠道类型" rules={[{ required: true }]}>
                <Select
                  placeholder="选择适配器"
                  // the provider decides which credential fields exist, so it is
                  // locked once the row has been created
                  disabled={Boolean(editing)}
                  options={providers.map((provider) => ({
                    value: provider.code,
                    label: `${provider.name}（${provider.redirect ? '跳转支付' : '页内支付'}）`,
                  }))}
                />
              </Form.Item>
            </Col>

            <Col xs={24} md={8}>
              <Form.Item name="mode" label="运行模式">
                <Select
                  options={[
                    { value: 'demo', label: '演示' },
                    { value: 'test', label: '沙箱' },
                    { value: 'live', label: '生产' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="sort" label="排序">
                <InputNumber min={0} max={999} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item name="enabled" label="启用" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>

          {Object.keys(credentialFields).length > 0 && (
            <>
              <Divider orientation="left" style={{ fontSize: 13 }}>
                凭据
              </Divider>
              <Row gutter={12}>
                {credentialFields.appId && (
                  <Col xs={24} md={12}>
                    <Form.Item name="appId" label={credentialFields.appId}>
                      <Input placeholder={editing?.appId || '未配置'} />
                    </Form.Item>
                  </Col>
                )}
                {credentialFields.merchantId && (
                  <Col xs={24} md={12}>
                    <Form.Item name="merchantId" label={credentialFields.merchantId}>
                      <Input placeholder={editing?.merchantId || '未配置'} />
                    </Form.Item>
                  </Col>
                )}
                {credentialFields.apiKey && (
                  <Col xs={24} md={12}>
                    <Form.Item
                      name="apiKey"
                      label={credentialFields.apiKey}
                      extra={editing?.hasApiKey ? '已配置，留空则保持不变' : undefined}
                    >
                      <Input.Password
                        placeholder={editing?.hasApiKey ? '••••••••' : '未配置'}
                        autoComplete="new-password"
                      />
                    </Form.Item>
                  </Col>
                )}
                {credentialFields.apiSecret && (
                  <Col xs={24} md={12}>
                    <Form.Item
                      name="apiSecret"
                      label={credentialFields.apiSecret}
                      extra={editing?.hasApiSecret ? '已配置，留空则保持不变' : undefined}
                    >
                      <Input.Password
                        placeholder={editing?.hasApiSecret ? '••••••••' : '未配置'}
                        autoComplete="new-password"
                      />
                    </Form.Item>
                  </Col>
                )}
                {credentialFields.privateKey && (
                  <Col xs={24}>
                    <Form.Item
                      name="privateKey"
                      label={credentialFields.privateKey}
                      extra={
                        editing?.hasPrivateKey
                          ? '已配置，留空则保持不变'
                          : '可直接粘贴 base64 内容，系统会自动补全 PEM 头尾'
                      }
                    >
                      <Input.TextArea
                        rows={3}
                        placeholder={editing?.hasPrivateKey ? '••••••••' : '-----BEGIN PRIVATE KEY-----'}
                      />
                    </Form.Item>
                  </Col>
                )}
                {credentialFields.publicKey && (
                  <Col xs={24}>
                    <Form.Item
                      name="publicKey"
                      label={credentialFields.publicKey}
                      extra={editing?.hasPublicKey ? '已配置，留空则保持不变' : undefined}
                    >
                      <Input.TextArea
                        rows={3}
                        placeholder={editing?.hasPublicKey ? '••••••••' : '-----BEGIN PUBLIC KEY-----'}
                      />
                    </Form.Item>
                  </Col>
                )}
              </Row>
            </>
          )}

          <Divider orientation="left" style={{ fontSize: 13 }}>
            回调
          </Divider>
          <Row gutter={12}>
            <Col xs={24} md={12}>
              <Form.Item
                name="notifyUrl"
                label="异步通知地址"
                extra={
                  editing?.notifyUrl ? `当前：${editing.notifyUrl}` : '由服务端地址拼接，通常无需手改'
                }
              >
                <Input placeholder="https://your-domain/api/app/pay/notify/stripe" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="returnUrl" label="支付完成返回地址">
                <Input placeholder="https://your-domain/orders/complete" />
              </Form.Item>
            </Col>
          </Row>

          {providerMeta && providerMeta.settings.length > 0 && (
            <>
              <Divider orientation="left" style={{ fontSize: 13 }}>
                渠道专属设置
              </Divider>
              {providerMeta.settings.map((hint) => (
                <Form.Item
                  key={hint.key}
                  name={['settings', hint.key]}
                  label={hint.label}
                  extra={hint.description}
                >
                  <Input
                    placeholder={
                      hint.key === 'webhookSecret' ? 'whsec_…' : '按渠道文档填写'
                    }
                  />
                </Form.Item>
              ))}
            </>
          )}

          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} placeholder="给同事看的说明，例如申请进度、负责同学" />
          </Form.Item>

          <Tabs
            items={[
              {
                key: 'help',
                label: '接入提示',
                children: <ProviderHelp code={providerCode} />,
              },
            ]}
          />
        </Form>
      </Modal>
    </div>
  );
}

/** Drops empty setting values so the stored JSON stays clean. */
function normaliseSettings(settings?: Record<string, string>) {
  if (!settings) return {};
  return Object.fromEntries(
    Object.entries(settings).filter(([, value]) => value != null && String(value).trim() !== ''),
  );
}

/** Provider-specific onboarding notes shown inside the dialog. */
function ProviderHelp({ code }: { code?: string }) {
  const text: Record<string, string> = {
    stripe:
      '在 Stripe Dashboard → Developers → API keys 获取 Secret Key；在 Webhooks 中添加端点 ' +
      'https://你的域名/api/app/pay/notify/stripe，订阅 checkout.session.completed 与 ' +
      'checkout.session.async_payment_succeeded，把 Signing secret 填到渠道专属设置里。',
    wechat:
      '需要：已开通 Native 支付的商户号、AppID（与商户号绑定）、商户 API 私钥、APIv3 密钥、' +
      '商户证书序列号。回调地址需公网可达且为 https。金额单位为分，系统已自动换算。',
    alipay:
      '需要：已签约「电脑网站支付」的应用 AppID、应用私钥，以及支付宝公钥（用于校验回调签名）。' +
      '沙箱环境请把模式切到「沙箱」或手动填写沙箱网关地址。',
    mock: '演示渠道：下单即完成支付，不需要任何凭据。仅在开发与演示环境启用。',
  };
  if (!code) return <Typography.Text type="secondary">请先选择渠道类型</Typography.Text>;
  return (
    <Typography.Paragraph style={{ fontSize: 12.5, lineHeight: 1.9, marginBottom: 0 }}>
      {text[code] ?? '该渠道暂无接入说明。'}
    </Typography.Paragraph>
  );
}
