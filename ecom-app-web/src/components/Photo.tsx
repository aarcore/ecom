import { useState } from 'react';
import { useTheme } from '@/store/theme';

/**
 * Product and hero art is shot twice: once on a charcoal plate for the night
 * ride theme, once on a seamless white plate for the daylight theme.
 *
 * The database only stores a single path per asset, so the light variant is
 * addressed by convention — a `light/` folder that mirrors the dark one:
 *
 *   /images/products/helmet.jpg  →  /images/products/light/helmet.jpg
 *   /images/hero/hero-1.jpg      →  /images/hero/light/hero-1.jpg
 *
 * Any other path (avatars, brand marks, admin uploads) passes through
 * untouched. If a light variant is missing the browser gets a 404 and we fall
 * back to the dark source, so a half-populated folder degrades gracefully
 * instead of showing a broken image.
 */
const LIGHT_FOLDERS: [from: string, to: string][] = [
  ['/images/products', '/images/products/light'],
  ['/images/hero', '/images/hero/light'],
];

export function lightVariant(src: string): string {
  for (const [from, to] of LIGHT_FOLDERS) {
    if (src.startsWith(`${from}/`)) return `${to}${src.slice(from.length)}`;
  }
  return src;
}

export function photoSrc(src: string | undefined, theme: 'light' | 'dark'): string {
  if (!src) return '';
  return theme === 'dark' ? src : lightVariant(src);
}

type ImgProps = React.ImgHTMLAttributes<HTMLImageElement> & { src?: string };

/** An `<img>` that follows the active theme. Use this for every product/hero photo. */
export function Photo({ src, alt = '', ...rest }: ImgProps) {
  const theme = useTheme((s) => s.theme);
  const [brokenLight, setBrokenLight] = useState<string | null>(null);

  const base = src ?? '';
  const light = lightVariant(base);
  const wantsLight = theme === 'light' && light !== base && brokenLight !== light;

  return (
    <img
      {...rest}
      src={wantsLight ? light : base}
      alt={alt}
      onError={(event) => {
        if (wantsLight) setBrokenLight(light);
        rest.onError?.(event);
      }}
    />
  );
}
