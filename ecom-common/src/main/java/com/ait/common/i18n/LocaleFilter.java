package com.ait.common.i18n;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * Negotiates the response language for the storefront.
 *
 * <p>Order of precedence:
 * <ol>
 *   <li>{@code ?lang=en} — explicit choice, used by the web client so a deep
 *       link keeps its language even when the browser profile disagrees</li>
 *   <li>{@code X-Lang: en} — set by <em>our own</em> frontends</li>
 *   <li>{@code Accept-Language} — honours browser ordering and q-values</li>
 * </ol>
 *
 * <p>Unknown languages are ignored rather than rejected: a Japanese visitor on
 * a locale we do not ship yet should still see the shop, just in Chinese.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LocaleFilter extends OncePerRequestFilter {

    public static final String LANG_PARAM = "lang";
    public static final String LANG_HEADER = "X-Lang";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            LocaleContext.set(resolve(request));
            // let caches and the CDN vary on the negotiated language
            response.setHeader("Vary", "Accept-Language, X-Lang");
            chain.doFilter(request, response);
        } finally {
            LocaleContext.clear();
        }
    }

    SupportedLocale resolve(HttpServletRequest request) {
        String param = request.getParameter(LANG_PARAM);
        Optional<SupportedLocale> explicit = SupportedLocale.byCode(param);
        if (explicit.isPresent()) {
            return explicit.get();
        }

        String header = request.getHeader(LANG_HEADER);
        Optional<SupportedLocale> preferred = SupportedLocale.byCode(header);
        if (preferred.isPresent()) {
            return preferred.get();
        }

        return negotiate(request.getHeader("Accept-Language"));
    }

    /** Picks the highest-weighted language we actually ship. */
    SupportedLocale negotiate(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.trim().isEmpty()) {
            return SupportedLocale.ZH_CN;
        }

        String[] parts = acceptLanguage.split(",");
        // simple weighted scan: parse "tag;q=0.8" and keep the best supported one
        String bestTag = null;
        double bestWeight = -1;
        for (String part : parts) {
            String[] pieces = part.trim().split(";");
            String tag = pieces[0].trim();
            double weight = 1.0;
            for (int i = 1; i < pieces.length; i++) {
                String piece = pieces[i].trim();
                if (piece.startsWith("q=")) {
                    try {
                        weight = Double.parseDouble(piece.substring(2));
                    } catch (NumberFormatException ignored) {
                        weight = 0;
                    }
                }
            }
            if (weight <= 0 || weight <= bestWeight) {
                continue;
            }
            Optional<SupportedLocale> match = SupportedLocale.byCode(tag);
            if (match.isEmpty()) {
                match = SupportedLocale.byLanguage(tag);
            }
            if (match.isPresent()) {
                bestTag = match.get().getCode();
                bestWeight = weight;
            }
        }
        if (bestTag == null) {
            return SupportedLocale.ZH_CN;
        }
        // normalise to the enum's own tag so "en-US" becomes "en"
        Optional<SupportedLocale> normalised = SupportedLocale.byCode(bestTag);
        if (normalised.isEmpty()) {
            normalised = SupportedLocale.byLanguage(bestTag);
        }
        return normalised.orElse(SupportedLocale.ZH_CN);
    }
}
