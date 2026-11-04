import { useEffect, useState } from 'react';
import { Card, Col, Empty, Row, Skeleton, Space, Tag, Typography } from 'antd';
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

const money = (n?: number) =>
  `¥${(Number(n ?? 0)).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const compact = (n?: number) => {
  const value = Number(n ?? 0);
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
  if (value >= 10_000) return `${(value / 10_000).toFixed(1)}w`;
  if (value >= 1000) return `${(value / 1000).toFixed(1)}k`;
  return String(value);
};

interface KpiProps {
  label: string;
  value: string;
  delta?: number;
  hint?: string;
  glyph: React.ReactNode;
  tint: string;
}

function Kpi({ label, value, delta, hint, glyph, tint }: KpiProps) {
  const positive = (delta ?? 0) >= 0;
  return (
    <div className="kpi-tile">
      <div className="label">{label}</div>
      <div className="value">{value}</div>
      <div className={`delta ${delta === undefined ? '' : positive ? 'up' : 'down'}`}>
        {delta !== undefined && (
          <>
            {positive ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
            {Math.abs(delta).toFixed(1)}%
          </>
        )}
        {hint && <span style={{ color: '#8b93a7' }}>{hint}</span>}
      </div>
      <div className="glyph" style={{ background: tint }}>
        {glyph}
      </div>
    </div>
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
            stroke="#eef0f5"
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
        <div
          style={{
            position: 'absolute',
            left: `${(points[hover].x / width) * 100}%`,
            top: 8,
            transform: 'translateX(-50%)',
            background: '#fff',
            border: '1px solid #eef0f5',
            borderRadius: 10,
            padding: '8px 12px',
            boxShadow: '0 8px 24px rgba(16,24,40,0.1)',
            fontSize: 12,
            pointerEvents: 'none',
            whiteSpace: 'nowrap',
          }}
        >
          <div style={{ fontWeight: 600, marginBottom: 4 }}>{points[hover].day}</div>
          <div>成交额 {money(Number(points[hover].gmv))}</div>
          <div style={{ color: '#8b93a7' }}>订单 {points[hover].orders} 笔</div>
        </div>
      )}

      <Space size={18} style={{ marginTop: 6, fontSize: 12, color: '#8b93a7' }}>
        <span>
          <span style={{ display: 'inline-block', width: 14, height: 3, background: '#2f54eb', borderRadius: 2, marginRight: 6 }} />
          成交额
        </span>
        <span>
          <span
            style={{
              display: 'inline-block',
              width: 14,
              height: 0,
              borderTop: '2px dashed #8a5cff',
              marginRight: 6,
              verticalAlign: 'middle',
            }}
          />
          新增会员
        </span>
      </Space>
    </div>
  );
}

/** Horizontal ranked bars, used for category share and top products. */
function RankBars({ data }: { data: { name: string; value: number }[] }) {
  if (!data.length) return <Empty description="暂无数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  const max = Math.max(...data.map((d) => Number(d.value)), 1);
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      {data.map((d, i) => (
        <div key={`${d.name}-${i}`}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 5 }}>
            <span style={{ color: '#3d4557' }}>{d.name}</span>
            <span className="muted">{compact(Number(d.value))}</span>
          </div>
          <div style={{ height: 7, background: '#f1f3f8', borderRadius: 6, overflow: 'hidden' }}>
            <div
              style={{
                width: `${Math.max((Number(d.value) / max) * 100, 2)}%`,
                height: '100%',
                borderRadius: 6,
                background: i === 0
                  ? 'linear-gradient(90deg, #2f54eb, #6f8dff)'
                  : 'linear-gradient(90deg, #a9bcf5, #cdd8fb)',
                transition: 'width .4s ease',
              }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

export default function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);

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

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>数据看板</h1>
          <div className="sub">近 30 天经营概览 · 数据实时汇总自 PostgreSQL</div>
        </div>
        <Space>
          <Tag color="green" bordered={false} style={{ borderRadius: 999, padding: '2px 10px' }}>
            待发货 {k.pendingShipment}
          </Tag>
          <Tag color="gold" bordered={false} style={{ borderRadius: 999, padding: '2px 10px' }}>
            待付款 {k.pendingPayment}
          </Tag>
          <Tag color="red" bordered={false} style={{ borderRadius: 999, padding: '2px 10px' }}>
            退款中 {k.refunding}
          </Tag>
        </Space>
      </div>

      <div className="kpi-grid" style={{ marginBottom: 16 }}>
        <Kpi
          label="今日成交额"
          value={money(k.gmvToday)}
          hint={`近 7 天 ${money(k.gmv7)}`}
          glyph={<WalletOutlined style={{ color: '#2f54eb' }} />}
          tint="#eef3ff"
        />
        <Kpi
          label="近 7 天成交额"
          value={money(k.gmv7)}
          delta={k.gmvGrowth}
          hint="较前 7 天"
          glyph={<ShoppingCartOutlined style={{ color: '#0f9d58' }} />}
          tint="#eafaf1"
        />
        <Kpi
          label="近 7 天订单"
          value={String(k.orders7)}
          delta={k.ordersGrowth}
          hint="较前 7 天"
          glyph={<TrophyOutlined style={{ color: '#e08a1e' }} />}
          tint="#fdf5e7"
        />
        <Kpi
          label="客单价"
          value={money(k.avgOrderValue)}
          hint="近 30 天均值"
          glyph={<WalletOutlined style={{ color: '#8a5cff' }} />}
          tint="#f3efff"
        />
        <Kpi
          label="会员总数"
          value={compact(k.memberTotal)}
          hint={`新增 ${k.memberNew7}`}
          glyph={<TeamOutlined style={{ color: '#0c8ce9' }} />}
          tint="#e9f6fe"
        />
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={16}>
          <Card
            className="soft-card"
            bordered={false}
            title="成交趋势"
            extra={<Typography.Text type="secondary" style={{ fontSize: 12 }}>近 14 天</Typography.Text>}
          >
            <TrendChart series={data.trend} secondary={data.memberTrend} />
          </Card>
        </Col>

        <Col xs={24} xl={8}>
          <Card className="soft-card" bordered={false} title="品类销量分布" style={{ height: '100%' }}>
            <RankBars data={data.categoryShare} />
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card className="soft-card" bordered={false} title="热销商品 TOP 6">
            <div style={{ display: 'grid', gap: 10 }}>
              {data.topProducts.map((p, i) => (
                <div key={p.name} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span
                    style={{
                      width: 22,
                      height: 22,
                      borderRadius: 7,
                      display: 'grid',
                      placeItems: 'center',
                      fontSize: 12,
                      fontWeight: 650,
                      background: i < 3 ? '#eef3ff' : '#f5f6fa',
                      color: i < 3 ? '#2f54eb' : '#8b93a7',
                    }}
                  >
                    {i + 1}
                  </span>
                  {p.cover && (
                    <img
                      src={p.cover}
                      alt=""
                      style={{ width: 36, height: 36, borderRadius: 8, objectFit: 'cover', background: '#f2f4f8' }}
                    />
                  )}
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div
                      style={{
                        fontSize: 13.5,
                        fontWeight: 500,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {p.name}
                    </div>
                    <div className="muted">销量 {p.sales} · {money(Number(p.price))}</div>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card className="soft-card" bordered={false} title="高价值会员">
            <div style={{ display: 'grid', gap: 12 }}>
              {data.topBuyers.map((b, i) => (
                <div key={`${b.nickname}-${i}`} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div
                    style={{
                      width: 34,
                      height: 34,
                      borderRadius: 999,
                      display: 'grid',
                      placeItems: 'center',
                      background: '#f3efff',
                      color: '#7a4ce0',
                      fontWeight: 600,
                      fontSize: 13,
                    }}
                  >
                    {(b.nickname ?? '客').slice(0, 1)}
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 13.5, fontWeight: 500 }}>{b.nickname}</div>
                    <div className="muted">{b.orders} 笔订单</div>
                  </div>
                  <span className="money">{money(Number(b.amount))}</span>
                </div>
              ))}
              {!data.topBuyers.length && <Empty description="暂无数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
