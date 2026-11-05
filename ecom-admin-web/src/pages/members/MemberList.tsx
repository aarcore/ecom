import { useCallback, useEffect, useState } from 'react';
import {
  Avatar,
  Button,
  Card,
  Descriptions,
  Drawer,
  Input,
  Segmented,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  message,
  type TablePaginationConfig,
} from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { api, withToast, type PageResult } from '@/api/client';
import type { Member } from '@/types';

const money = (n?: number) => `¥${Number(n ?? 0).toFixed(2)}`;

const LEVEL_COLOR: Record<number, string> = {
  1: 'default',
  2: 'blue',
  3: 'gold',
  4: 'purple',
};

export default function MemberListPage() {
  const [rows, setRows] = useState<Member[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [platform, setPlatform] = useState<string | undefined>();
  const [statusTab, setStatusTab] = useState('all');
  const [detail, setDetail] = useState<Member | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await api.get<PageResult<Member>>('/admin/members', {
        keyword: keyword || undefined,
        platform,
        status: statusTab === 'all' ? undefined : Number(statusTab),
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
  }, [keyword, platform, statusTab, page, limit]);

  useEffect(() => {
    load();
  }, [load]);

  const toggleStatus = async (row: Member, enabled: boolean) => {
    const ok = await withToast(
      () => api.put<void>(`/admin/members/${row.id}/status`, null, { status: enabled ? 1 : 0 }),
      enabled ? '会员已启用' : '会员已禁用',
    );
    if (ok !== null) load();
  };

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>会员管理</h1>
          <div className="sub">会员等级由累计消费自动升级，支持按来源与状态筛选</div>
        </div>
        <Button icon={<ReloadOutlined />} onClick={load}>
          刷新
        </Button>
      </div>

      <Card className="soft-card" bordered={false}>
        <div className="toolbar" style={{ justifyContent: 'space-between' }}>
          <Segmented
            options={[
              { label: '全部', value: 'all' },
              { label: '正常', value: '1' },
              { label: '已禁用', value: '0' },
            ]}
            value={statusTab}
            onChange={(value) => {
              setPage(1);
              setStatusTab(String(value));
            }}
          />
          <Space>
            <Input.Search
              allowClear
              placeholder="昵称 / 手机号"
              style={{ width: 220 }}
              onSearch={(value) => {
                setPage(1);
                setKeyword(value);
              }}
            />
            <Select
              allowClear
              placeholder="注册来源"
              style={{ width: 130 }}
              value={platform}
              onChange={(value) => {
                setPage(1);
                setPlatform(value);
              }}
              options={[
                { value: 'web', label: 'H5 / 网页' },
                { value: 'mp', label: '微信小程序' },
              ]}
            />
          </Space>
        </div>

        <Table<Member>
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
            showTotal: (t) => `共 ${t} 位会员`,
          }}
          columns={[
            {
              title: '会员',
              width: 250,
              render: (_: unknown, row) => (
                <Space>
                  <Avatar src={row.avatar} size={36} style={{ background: '#eef3ff', color: '#2f54eb' }}>
                    {(row.nickname || 'U').slice(0, 1)}
                  </Avatar>
                  <div>
                    <div className="cell-title">{row.nickname || '匿名用户'}</div>
                    <div className="muted">{row.mobileMasked || row.mobile || '—'}</div>
                  </div>
                </Space>
              ),
            },
            {
              title: '等级',
              dataIndex: 'level',
              width: 110,
              render: (value: number, row) => (
                <Tag bordered={false} color={LEVEL_COLOR[value] ?? 'default'}>
                  {row.levelName || `L${value}`}
                </Tag>
              ),
            },
            {
              title: '累计消费',
              dataIndex: 'totalSpent',
              width: 130,
              render: (value: number) => <span className="money">{money(value)}</span>,
            },
            { title: '订单数', dataIndex: 'orderCount', width: 90 },
            {
              title: '积分 / 余额',
              width: 150,
              render: (_: unknown, row) => (
                <div>
                  <div>{row.points} 积分</div>
                  <div className="muted">{money(row.balance)}</div>
                </div>
              ),
            },
            {
              title: '来源',
              dataIndex: 'platform',
              width: 110,
              render: (value: string) => (
                <Tag bordered={false} color={value === 'mp' ? 'green' : 'blue'}>
                  {value === 'mp' ? '小程序' : 'H5'}
                </Tag>
              ),
            },
            {
              title: '最近登录',
              dataIndex: 'lastLoginAt',
              width: 140,
              render: (value?: string) => (
                <span className="muted">{value ? dayjs(value).format('MM-DD HH:mm') : '—'}</span>
              ),
            },
            {
              title: '启用',
              dataIndex: 'status',
              width: 90,
              render: (value: number, row) => (
                <Switch
                  checked={value === 1}
                  checkedChildren="启用"
                  unCheckedChildren="禁用"
                  onChange={(checked) => toggleStatus(row, checked)}
                />
              ),
            },
            {
              title: '操作',
              width: 90,
              render: (_: unknown, row) => (
                <Button type="link" size="small" onClick={() => setDetail(row)}>
                  查看
                </Button>
              ),
            },
          ]}
        />
      </Card>

      <Drawer
        open={Boolean(detail)}
        onClose={() => setDetail(null)}
        width={420}
        title="会员详情"
      >
        {detail && (
          <>
            <Space align="center" size={14} style={{ marginBottom: 20 }}>
              <Avatar src={detail.avatar} size={56} style={{ background: '#eef3ff', color: '#2f54eb' }}>
                {(detail.nickname || 'U').slice(0, 1)}
              </Avatar>
              <div>
                <div style={{ fontSize: 17, fontWeight: 600 }}>{detail.nickname}</div>
                <div className="muted">
                  {detail.levelName} · {detail.mobileMasked || detail.mobile}
                </div>
              </div>
            </Space>

            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="会员 ID">{detail.id}</Descriptions.Item>
              <Descriptions.Item label="性别">{detail.genderText}</Descriptions.Item>
              <Descriptions.Item label="注册来源">
                {detail.platform === 'mp' ? '微信小程序' : 'H5 / 网页'}
              </Descriptions.Item>
              <Descriptions.Item label="累计消费">{money(detail.totalSpent)}</Descriptions.Item>
              <Descriptions.Item label="订单数">{detail.orderCount}</Descriptions.Item>
              <Descriptions.Item label="积分">{detail.points}</Descriptions.Item>
              <Descriptions.Item label="账户余额">{money(detail.balance)}</Descriptions.Item>
              <Descriptions.Item label="注册时间">
                {dayjs(detail.createdAt).format('YYYY-MM-DD HH:mm')}
              </Descriptions.Item>
              <Descriptions.Item label="最近登录">
                {detail.lastLoginAt ? dayjs(detail.lastLoginAt).format('YYYY-MM-DD HH:mm') : '—'}
              </Descriptions.Item>
            </Descriptions>
          </>
        )}
      </Drawer>
    </div>
  );
}
