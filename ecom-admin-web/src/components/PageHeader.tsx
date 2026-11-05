import { Card, Flex, Space, Typography } from 'antd';
import type { ReactNode } from 'react';

const { Title, Text } = Typography;

/**
 * Page heading shared by every screen.
 *
 * Thirteen pages had each hand-rolled `<div className="page-head"><h1>…</h1>
 * <div className="sub">…</div></div>`. That is thirteen copies of the same
 * spacing decision — and it drifted: some titles were 22px, others 19px. A single
 * component is the only way to keep that consistent, and it lets the heading
 * level be semantic rather than a styled div.
 */
export function PageHeader({
  title,
  description,
  extra,
}: {
  title: ReactNode;
  description?: ReactNode;
  /** Actions, usually buttons. Rendered right-aligned, wrapping on narrow screens. */
  extra?: ReactNode;
}) {
  return (
    <Flex justify="space-between" align="flex-start" gap={16} wrap style={{ marginBottom: 16 }}>
      <div style={{ minWidth: 0 }}>
        <Title level={4} style={{ margin: 0, fontSize: 19, letterSpacing: '-0.012em' }}>
          {title}
        </Title>
        {description && (
          <Text type="secondary" style={{ fontSize: 12.5, lineHeight: 1.6 }}>
            {description}
          </Text>
        )}
      </div>
      {extra && <Space size={8}>{extra}</Space>}
    </Flex>
  );
}

/**
 * A filter row above a table: segmented status on the left, inputs on the right.
 *
 * Wraps by itself, which the previous `.toolbar` divs did not — on a narrow
 * window the search box used to push the table sideways.
 */
export function FilterBar({ left, right }: { left?: ReactNode; right?: ReactNode }) {
  return (
    <Flex
      justify="space-between"
      align="center"
      gap={12}
      wrap
      style={{ padding: '12px 16px', borderBottom: '1px solid var(--line)' }}
    >
      <div>{left}</div>
      {right && <Space size={8} wrap>{right}</Space>}
    </Flex>
  );
}

/**
 * Card that holds a table edge to edge.
 *
 * `styles.body.padding = 0` plus the flush table rule in global.css is what makes
 * rows sit against the card edge instead of inside a padded box, so the header
 * row and the card border share one line.
 */
export function TableCard({ children, extra }: { children: ReactNode; extra?: ReactNode }) {
  return (
    <Card
      className="soft-card"
      bordered={false}
      styles={{ body: { padding: 0 } }}
      {...(extra ? { title: extra } : {})}
    >
      {children}
    </Card>
  );
}
