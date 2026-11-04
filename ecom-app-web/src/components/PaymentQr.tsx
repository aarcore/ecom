import { useEffect, useState } from 'react';
import QRCode from 'qrcode';

/**
 * Renders a payment QR code.
 *
 * Uses the `qrcode` package rather than a chart library because the output has
 * to be a scannable matrix, and it renders to an inline data URL so the code is
 * crisp on retina displays and needs no image request.
 *
 * The QR is generated in an effect, not during render: encoding is cheap but not
 * free, and doing it during render would repeat it on every parent re-render.
 */
export function PaymentQr({ content, size = 220 }: { content: string; size?: number }) {
  const [src, setSrc] = useState<string | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setSrc(null);
    setError(false);

    QRCode.toDataURL(content, {
      width: size * 2, // 2x so the modules stay crisp on retina
      margin: 1,
      errorCorrectionLevel: 'M',
      // Dark modules on a white plate: a phone camera needs the quiet zone to be
      // light even when the surrounding page is in dark mode.
      color: { dark: '#101114', light: '#ffffff' },
    })
      .then((url) => {
        if (!cancelled) setSrc(url);
      })
      .catch(() => {
        if (!cancelled) setError(true);
      });

    return () => {
      cancelled = true;
    };
  }, [content, size]);

  if (error) {
    return <div className="pay-qr pay-qr-error">二维码生成失败，请重新发起支付</div>;
  }

  return (
    <div className="pay-qr" style={{ width: size, height: size }}>
      {src ? (
        <img src={src} alt="" width={size} height={size} />
      ) : (
        <div className="skel" style={{ width: '100%', height: '100%' }} />
      )}
    </div>
  );
}
