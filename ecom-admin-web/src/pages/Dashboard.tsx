import { useEffect, useState } from 'react';
import {
  Avatar,
  Card,
  Col,
  Empty,
  Flex,
  List,
  Progress,
  Row,
  Segmented,
  Skeleton,
  Space,
  Statistic,
  Tag,
  Typography,
} from 'antd';
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  ShoppingCartOutlined,
  TeamOutlined,
  TrophyOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import { api, withToast } from '@/api/client';
import type { DashboardData } from '@/types';

const { Text } = Typography;

const money = (n?: number) =>
  `¥${Number(n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const compact = (n?: number) => {
  const value = Number(n ?? 0);
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 10_000) return `${(value / 10_000).toFixed(1)}w`;
  if (value >= 1000) return `${(value / 1000).toFixed(1)}k`;
  return String(value);
};

/**
 * KPI tile built on AntD's Statistic.
 *
 * Statistic already handles the label/value/typography relationship and the
 * count-up formatting hooks, so the bespoke `.kpi-tile` markup only existed to
 * re-implement it. What Statistic cannot do — the icon chip in the corner and
 * the coloured delta — is composed around it.
 */
function KpiTile({
  label,
  value,
  delta,
  hint,
  icon,
  tint,
}: {
  label: string;
  value: string;
  delta?: number;
  hint?: string;
  icon: React.ReactNode;
  tint: string;
}) {
  const positive = (delta ?? 0) >= 0;
  return (
    <Card className="soft-card kpi-card" bordered={false} styles={{ body: { padding: '15px 16px' } }}>
      <Flex justify="space-between" align="flex-start" gap={8}>
        <Statistic
          title={<span style={{ fontSize: 12.5, color: 'var(--ink-soft)' }}>{label}</span>}
          value={value}
          valueStyle={{ fontSize: 23, fontWeight: 650, letterSpacing: '-0.02em', lineHeight: 1.25 }}
        />
        <span className="kpi-glyph" style={{ background: tint }}>
          {icon}
        </span>
      </Flex>
      <Flex align="center" gap={6} style={{ marginTop: 6, fontSize: 11.5 }}>
        {delta !== undefined && (
          <Text type={positive ? 'success' : 'danger'} style={{ fontSize: 11.5 }}>
            {positive ? <ArrowUpOutlined /> : <ArrowDownOutlined />} {Math.abs(delta).toFixed(1)}%
          </Text>
        )}
        {hint && <Text type="secondary" style={{ fontSize: 11.5 }}>{hint}</Text>}
      </Flex>
    </Card>
  );
}

/** Area chart drawn with plain SVG — no chart library dependency. */
function TrendChart({
  series,
  secondary,
}: {
  series: { day: string; orders: number; gmv: number }[];
  secondary?: { day: string; members: number }[];
}) {
  const [hover, setHover] = useState<number | null>(null);
  if (!series.length) return <Empty description="暂无数据" />;

  const width = 1000;
  const height = 260;
  const padX = 34;
  const padY = 22;
  const maxGmv = Math.max(...series.map((s) => Number(s.gmv)), 1);
  const stepX = (width - padX * 2) / Math.max(series.length - 1, 1);
  const scaleY = (v: number) => height - padY - (v / maxGmv) * (height - padY * 2);

  const points = series.map((s, i) => ({
    x: padX + i * stepX,
    y: scaleY(Number(s.gmv)),
    ...s,
  }));

  const line = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ');
  const area = `${line} L${points[points.length - 1].x},${height - padY} L${points[0].x},${height - padY} Z`;

  const memberMax = Math.max(...(secondary ?? []).map((m) => Number(m.members)), 1);
  const memberLine = (secondary ?? [])
    .map((m, i) => {
      const x = padX + i * ((width - padX * 2) / Math.max((secondary?.length ?? 1) - 1, 1));
      const y = height - padY - (Number(m.members) / memberMax) * (height - padY * 2) * 0.55;
      return `${i === 0 ? 'M' : 'L'}${x},${y}`;
    })
    .join(' ');

  return (
    <div style={{ position: 'relative' }}>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        style={{ width: '100%', height: 260, display: 'block' }}
        onMouseLeave={() => setHover(null)}
      >
        <defs>
          <linearGradient id="gmvFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#2f54eb" stopOpacity="0.22" />
            <stop offset="100%" stopColor="#2f54eb" stopOpacity="0" />
          </linearGradient>
        </defs>

        {[0, 0.25, 0.5, 0.75, 1].map((t) => (
          <line
            key={t}
            x1={padX}
            x2={width - padX}
            y1={padY + t * (height - padY * 2)}
            y2={padY + t * (height - padY * 2)}
            stroke="#eef0f3"
            strokeWidth={1}
          />
        ))}

        <path d={area} fill="url(#gmvFill)" />
        <path d={line} fill="none" stroke="#2f54eb" strokeWidth={2.4} strokeLinejoin="round" />
        {memberLine && (
          <path
            d={memberLine}
            fill="none"
            stroke="#8a5cff"
            strokeWidth={1.8}
            strokeDasharray="5 4"
            opacity={0.85}
          />
        )}

        {points.map((p, i) => (
          <g key={p.day}>
            <rect
              x={p.x - stepX / 2}
              y={0}
              width={stepX}
              height={height}
              fill="transparent"
              onMouseEnter={() => setHover(i)}
            />
            {hover === i && (
              <>
                <line x1={p.x} x2={p.x} y1={padY} y2={height - padY} stroke="#c9d2ea" strokeDasharray="4 4" />
                <circle cx={p.x} cy={p.y} r={4.5} fill="#2f54eb" stroke="#fff" strokeWidth={2} />
              </>
            )}
          </g>
        ))}

        {points
          .filter((_, i) => i % Math.ceil(points.length / 7) === 0)
          .map((p) => (
            <text key={`x-${p.day}`} x={p.x} y={height - 4} textAnchor="middle" fontSize={11} fill="#98a1b3">
              {p.day}
            </text>
          ))}
      </svg>

      {hover !== null && (
        <div className="chart-tip" style={{ left: `${(points[hover].x / width) * 100}%` }}>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>{points[hover].day}</div>
          <div>成交额 {money(Number(points[hover].gmv))}</div>
          <Text type="secondary" style={{ fontSize: 12 }}>订单 {points[hover].orders} 笔</Text>
        </div>
      )}

      <Space size={18} style={{ marginTop: 6, fontSize: 12 }}>
        <Text type="secondary" style={{ fontSize: 12 }}>
          <span className="legend-line solid" />
          成交额
        </Text>
        <Text type="secondary" style={{ fontSize: 12 }}>
          <span className="legend-line dashed" />
          新增会员
        </Text>
      </Space>
    </div>
  );
}

/**
 * Ranked bars.
 *
 * Built on Progress rather than hand-drawn divs: Progress already owns the
 * track, the fill, the transition and the accessible role, and its
 * `percent` prop is exactly the ratio this chart computes by hand.
 */
function RankBars({ data }: { data: { name: string; value: number }[] }) {
  if (!data.length) return <Empty description="暂无数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  const max = Math.max(...data.map((d) => Number(d.value)), 1);
  return (
    <Flex vertical gap={12}>
      {data.map((d, i) => (
        <div key={`${d.name}-${i}`}>
          <Flex justify="space-between" style={{ fontSize: 13, marginBottom: 3 }}>
            <Text>{d.name}</Text>
            <Text type="secondary" style={{ fontSize: 12 }}>{compact(Number(d.value))}</Text>
          </Flex>
          <Progress
            percent={Math.max((Number(d.value) / max) * 100, 2)}
            showInfo={false}
            size={['100%', 7]}
            strokeColor={i === 0 ? '#2f54eb' : '#a9bcf5'}
            trailColor="#f1f3f8"
          />
        </div>
      ))}
    </Flex>
  );
}

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [trendRange, setTrendRange] = useState<'7' | '14'>('14');

  useEffect(() => {
    withToast(() => api.get<DashboardData>('/admin/dashboard/overview')).then(setData);
  }, []);

  if (!data) {
    return (
      <Card className="soft-card" bordered={false}>
        <Skeleton active paragraph={{ rows: 9 }} />
      </Card>
    );
  }

  const k = data.kpi;
  // the payload carries the full window; the toggle only slices the tail so the
  // chart can show a tighter period without another request
  const trendSlice = trendRange === '7' ? data.trend.slice(-7) : data.trend;
  const memberSlice = trendRange === '7' ? (data.memberTrend ?? []).slice(-7) : data.memberTrend;

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="flex-start" gap={16} wrap>
        <div>
          <Typography.Title level={4} style={{ margin: 0, fontSize: 19 }}>
            数据看板
          </Typography.Title>
          <Text type="secondary" style={{ fontSize: 12.5 }}>
            近 30 天经营概览 · 数据实时汇总自 PostgreSQL
          </Text>
        </div>
        <Space size={6}>
          <Tag color="success" bordered={false} style={{ margin: 0 }}>待发货 {k.pendingShipment}</Tag>
          <Tag color="warning" bordered={false} style={{ margin: 0 }}>待付款 {k.pendingPayment}</Tag>
          <Tag color="error" bordered={false} style={{ margin: 0 }}>退款中 {k.refunding}</Tag>
        </Space>
      </Flex>

      <Row gutter={[12, 12]}>
        <Col xs={24} sm={12} xl={{ flex: "1 1 0" }}>
          <KpiTile
            label="今日成交额"
            value={money(k.gmvToday)}
            hint={`近 7 天 ${money(k.gmv7)}`}
            icon={<WalletOutlined style={{ color: '#2f54eb' }} />}
            tint="#eef3ff"
          />
        </Col>
        <Col xs={24} sm={12} xl={{ flex: "1 1 0" }}>
          <KpiTile
            label="近 7 天成交额"
            value={money(k.gmv7)}
            delta={k.gmvGrowth}
            hint="较前 7 天"
            icon={<ShoppingCartOutlined style={{ color: '#0f9d58' }} />}
            tint="#eafaf1"
          />
        </Col>
        <Col xs={24} sm={12} xl={{ flex: "1 1 0" }}>
          <KpiTile
            label="近 7 天订单"
            value={String(k.orders7)}
            delta={k.ordersGrowth}
            hint="较前 7 天"
            icon={<TrophyOutlined style={{ color: '#e08a1e' }} />}
            tint="#fdf5e7"
          />
        </Col>
        <Col xs={24} sm={12} xl={{ flex: "1 1 0" }}>
          <KpiTile
            label="客单价"
            value={money(k.avgOrderValue)}
            hint="近 30 天均值"
            icon={<WalletOutlined style={{ color: '#8a5cff' }} />}
            tint="#f3efff"
          />
        </Col>
        <Col xs={24} sm={12} xl={{ flex: "1 1 0" }}>
          <KpiTile
            label="会员总数"
            value={compact(k.memberTotal)}
            hint={`新增 ${k.memberNew7}`}
            icon={<TeamOutlined style={{ color: '#0c8ce9' }} />}
            tint="#e9f6fe"
          />
        </Col>
      </Row>

      <Row gutter={[12, 12]}>
        <Col xs={24} xl={16}>
          <Card
            className="soft-card"
            bordered={false}
            title="成交趋势"
            extra={
              <Segmented
                size="small"
                value={trendRange}
                onChange={(value) => setTrendRange(value as '7' | '14')}
                options={[
                  { label: '近 7 天', value: '7' },
                  { label: '近 14 天', value: '14' },
                ]}
              />
            }
          >
            <TrendChart series={trendSlice} secondary={memberSlice} />
          </Card>
        </Col>

        <Col xs={24} xl={8}>
          <Card className="soft-card" bordered={false} title="品类销量分布" style={{ height: '100%' }}>
            <RankBars data={data.categoryShare} />
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card className="soft-card" bordered={false} title="热销商品 TOP 6" styles={{ body: { paddingTop: 6 } }}>
            <List
              dataSource={data.topProducts}
              split={false}
              renderItem={(p, i) => (
                <List.Item style={{ padding: '9px 0' }}>
                  <Flex align="center" gap={12} style={{ width: '100%', minWidth: 0 }}>
                    <span className={`rank-badge ${i < 3 ? 'top' : ''}`}>{i + 1}</span>
                    <Avatar
                      shape="square"
                      size={36}
                      src={p.cover}
                      style={{ borderRadius: 8, background: '#f2f4f8', flexShrink: 0 }}
                    />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div className="ellipsis" style={{ fontSize: 13.5, fontWeight: 500 }}>{p.name}</div>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        销量 {p.sales} · {money(Number(p.price))}
                      </Text>
                    </div>
                  </Flex>
                </List.Item>
              )}
            />
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card className="soft-card" bordered={false} title="高价值会员" styles={{ body: { paddingTop: 6 } }}>
            <List
              dataSource={data.topBuyers}
              split={false}
              locale={{ emptyText: <Empty description="暂无数据" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
              renderItem={(b, i) => (
                <List.Item style={{ padding: '9px 0' }}>
                  <Flex align="center" gap={12} style={{ width: '100%', minWidth: 0 }}>
                    <Avatar size={34} style={{ background: '#f3efff', color: '#7a4ce0', fontWeight: 600 }}>
                      {(b.nickname ?? '客').slice(0, 1)}
                    </Avatar>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontSize: 13.5, fontWeight: 500 }}>{b.nickname}</div>
                      <Text type="secondary" style={{ fontSize: 12 }}>{b.orders} 笔订单</Text>
                    </div>
                    <Text strong style={{ fontVariantNumeric: 'tabular-nums' }}>{money(Number(b.amount))}</Text>
                  </Flex>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </Flex>
  );
}
