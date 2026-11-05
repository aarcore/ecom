import { useEffect, useState } from 'react';
import { Alert, Card, Col, Descriptions, Row, Space, Tag, Typography } from 'antd';
import { CloudServerOutlined, DatabaseOutlined, DeploymentUnitOutlined } from '@ant-design/icons';
import { api, withToast } from '@/api/client';

interface StorageConfig {
  type: string;
  backend: string;
  publicBaseUrl: string;
  maxSize: number;
  allowedExtensions: string[];
}

export default function SettingsPage() {
  const [config, setConfig] = useState<StorageConfig | null>(null);

  useEffect(() => {
    withToast(() => api.get<StorageConfig>('/admin/files/config')).then((data) => {
      if (data) setConfig(data);
    });
  }, []);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>存储与配置</h1>
          <div className="sub">文件存储后端由环境变量 ecom.storage.* 控制</div>
        </div>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={12}>
          <Card className="soft-card" bordered={false} title={<Space><CloudServerOutlined /> 文件存储</Space>}>
            {config ? (
              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="存储类型">
                  <Tag bordered={false} color={config.type === 's3' ? 'purple' : 'blue'}>
                    {config.type}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="后端">{config.backend}</Descriptions.Item>
                <Descriptions.Item label="访问前缀">{config.publicBaseUrl}</Descriptions.Item>
                <Descriptions.Item label="单文件上限">
                  {(config.maxSize / 1024 / 1024).toFixed(0)} MB
                </Descriptions.Item>
                <Descriptions.Item label="允许格式">
                  <Space size={4} wrap>
                    {config.allowedExtensions.map((ext) => (
                      <Tag key={ext} bordered={false}>
                        {ext}
                      </Tag>
                    ))}
                  </Space>
                </Descriptions.Item>
              </Descriptions>
            ) : (
              <Typography.Text type="secondary">加载中…</Typography.Text>
            )}

            <Alert
              style={{ marginTop: 16, borderRadius: 10 }}
              type="info"
              showIcon
              message="切换到对象存储"
              description={
                <div style={{ fontSize: 12.5, lineHeight: 1.8 }}>
                  设置 <code>STORAGE_TYPE=s3</code> 并补齐 <code>S3_ENDPOINT</code>、
                  <code>S3_BUCKET</code>、<code>S3_ACCESS_KEY</code>、<code>S3_SECRET_KEY</code>{' '}
                  即可切到 AWS S3 / MinIO / 任意兼容 S3 的对象存储，无需改动业务代码。
                </div>
              }
            />
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card className="soft-card" bordered={false} title={<Space><DatabaseOutlined /> 数据服务</Space>}>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="数据库">PostgreSQL 16（Flyway 托管迁移）</Descriptions.Item>
              <Descriptions.Item label="缓存">Redis 7（验证码、限流、Token 黑名单）</Descriptions.Item>
              <Descriptions.Item label="迁移脚本">db/migration/V1__init_schema.sql</Descriptions.Item>
              <Descriptions.Item label="演示数据">db/migration/V2__seed_data.sql</Descriptions.Item>
            </Descriptions>

            <Alert
              style={{ marginTop: 16, borderRadius: 10 }}
              type="warning"
              showIcon
              message="生产环境提示"
              description={
                <div style={{ fontSize: 12.5, lineHeight: 1.8 }}>
                  请通过环境变量覆盖 <code>JWT_SECRET</code> 与 <code>DB_PASSWORD</code>，
                  并在首次部署后删除演示账号或修改密码。
                </div>
              }
            />
          </Card>
        </Col>

        <Col xs={24}>
          <Card className="soft-card" bordered={false} title={<Space><DeploymentUnitOutlined /> 部署方式</Space>}>
            <Typography.Paragraph style={{ marginBottom: 8 }}>
              整个系统（后端 + 管理端 + C 端 + PostgreSQL + Redis）由单一{' '}
              <code>docker-compose.yml</code> 编排：
            </Typography.Paragraph>
            <pre
              style={{
                background: '#0f1526',
                color: '#d7e0ff',
                padding: 16,
                borderRadius: 12,
                fontSize: 12.5,
                lineHeight: 1.9,
                overflowX: 'auto',
              }}
            >
{`docker compose up -d --build

# 访问
# 管理端   http://localhost:8081
# C 端     http://localhost:8082
# 接口文档 http://localhost:8080/swagger-ui.html`}
            </pre>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
