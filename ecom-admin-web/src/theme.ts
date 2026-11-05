import type { ThemeConfig } from 'antd';

/**
 * Visual language: a calm, dense operations console.
 *
 * Two deliberate choices worth keeping:
 *
 * 1. The sider uses the same light surface as the content. A dark rail next to a
 *    white canvas splits attention and makes the whole screen feel heavier than
 *    the data it shows; a single surface lets the table and the nav share one
 *    visual plane, which is the norm in modern admin products.
 *
 * 2. Metrics are tight. A console is scanned, not read: 52px header, 208px rail,
 *    32px controls. Every 8px saved vertically is one more table row visible
 *    without scrolling.
 */
export const themeConfig: ThemeConfig = {
  token: {
    colorPrimary: '#2f54eb',
    colorInfo: '#2f54eb',
    colorSuccess: '#0f9d58',
    colorWarning: '#e08a1e',
    colorError: '#d94848',
    colorTextBase: '#1d2129',
    colorBgLayout: '#f6f7f9',
    colorBorder: '#e5e7eb',
    colorBorderSecondary: '#eef0f3',
    borderRadius: 8,
    borderRadiusLG: 10,
    fontSize: 13.5,
    fontFamily:
      "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
    controlHeight: 32,
    controlHeightSM: 26,
    controlHeightLG: 38,
    wireframe: false,
  },
  components: {
    Card: {
      paddingLG: 18,
      boxShadowTertiary: '0 1px 2px rgba(16, 24, 40, 0.04)',
    },
    Layout: {
      headerBg: '#ffffff',
      // sider shares the content surface; the divider is what separates them
      siderBg: '#ffffff',
      bodyBg: '#f6f7f9',
      headerHeight: 52,
      headerPadding: '0 18px 0 8px',
    },
    Menu: {
      itemBg: 'transparent',
      subMenuItemBg: 'transparent',
      itemSelectedBg: '#eef2ff',
      itemSelectedColor: '#2f54eb',
      itemHoverBg: '#f4f6fa',
      itemColor: '#4b5565',
      itemHeight: 36,
      itemMarginInline: 0,
      itemMarginBlock: 2,
      itemBorderRadius: 7,
      iconSize: 15,
      collapsedIconSize: 16,
      groupTitleColor: '#98a2b3',
      groupTitleFontSize: 11,
    },
    Table: {
      headerBg: '#fafbfc',
      headerColor: '#667085',
      headerSplitColor: 'transparent',
      rowHoverBg: '#f7f9fc',
      cellPaddingBlock: 11,
      cellPaddingInline: 12,
      borderColor: '#eef0f3',
      headerBorderRadius: 0,
      fontSize: 13,
    },
    Button: {
      primaryShadow: '0 1px 2px rgba(47, 84, 235, 0.2)',
      fontWeight: 500,
      defaultShadow: 'none',
    },
    Input: {
      paddingBlock: 4,
    },
    Select: {
      optionSelectedBg: '#eef2ff',
    },
    Segmented: {
      itemSelectedBg: '#ffffff',
      trackBg: '#eef0f4',
      trackPadding: 2,
    },
    Tabs: {
      horizontalMargin: '0 0 14px 0',
    },
    Statistic: {
      contentFontSize: 26,
    },
    Descriptions: {
      labelBg: '#fafbfc',
      titleMarginBottom: 10,
    },
    Modal: {
      titleFontSize: 15,
      paddingContentHorizontalLG: 20,
    },
    Tag: {
      defaultBg: '#f4f6fa',
      defaultColor: '#4b5565',
    },
    Breadcrumb: {
      fontSize: 13,
      itemColor: '#8b94a6',
      lastItemColor: '#1d2129',
      separatorColor: '#c7ccd6',
    },
  },
};
