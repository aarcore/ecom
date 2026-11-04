import { useCallback, useEffect, useState } from 'react';
import { Button, Card, Input, Table, Tag, Tooltip, message, type TablePaginationConfig } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { api, withToast, type PageResult } from '@/api/client';
import type { AuditLog } from '@/types';

export default function LogPage() {
  const [rows, setRows] = useState<AuditLog[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(20);
  const [keyword, setKeyword] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = await api.get<PageResult<AuditLog>>('/admin/logs', {
        keyword: keyword || undefined,
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
  }, [keyword, page, limit]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>操作日志</h1>
          <div className="sub">由 AOP 切面自动记录每一次运营写操作</div>
        </div>
        <Button icon={<ReloadOutlined />} onClick={load}>
          刷新
        </Button>
      </div>

      <Card className="soft-card" bordered={false}>
        <div className="toolbar">
          <Input.Search
            allowClear
            placeholder="操作人 / 动作 / 接口"
            style={{ width: 280 }}
            onSearch={(value) => {
              setPage(1);
              setKeyword(value);
            }}
          />
        </div>

        <Table<AuditLog>
          rowKey="id"
          loading={loading}
          dataSource={rows}
          onChange={(pagination: TablePaginationConfig) => {
            setPage(pagination.current ?? 1);
            setLimit(pagination.pageSize ?? 20);
          }}
          pagination={{
            current: page,
            pageSize: limit,
            total,
            showTotal: (t) => `共 ${t} 条记录`,
          }}
          columns={[
            {
              title: '时间',
              dataIndex: 'createdAt',
              width: 170,
              render: (v: string) => <span className="muted">{dayjs(v).format('YYYY-MM-DD HH:mm:ss')}</span>,
            },
            { title: '操作人', dataIndex: 'operator', width: 120, render: (v?: string) => v || '—' },
            {
              title: '模块',
              dataIndex: 'module',
              width: 110,
              render: (v?: string) => (v ? <Tag bordered={false} color="blue">{v}</Tag> : '—'),
            },
            { title: '动作', dataIndex: 'action', width: 160 },
            {
              title: '接口',
              dataIndex: 'uri',
              render: (v?: string) => (
                <Tooltip title={v}>
                  <span className="muted" style={{ fontFamily: 'monospace' }}>{v}</span>
                </Tooltip>
              ),
            },
            {
              title: '耗时',
              dataIndex: 'durationMs',
              width: 100,
              render: (v?: number) => <span className="muted">{v ?? 0} ms</span>,
            },
            {
              title: '结果',
              dataIndex: 'success',
              width: 90,
              render: (v: number, row) => (
                <Tooltip title={row.errorMsg}>
                  <Tag bordered={false} color={v === 1 ? 'green' : 'red'}>
                    {v === 1 ? '成功' : '失败'}
                  </Tag>
                </Tooltip>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
