import path from "node:path";
import type { NextConfig } from "next";

const apiUrl = process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const nextConfig: NextConfig = {
  agentRules: false,
  poweredByHeader: false,
  turbopack: {
    root: path.resolve(__dirname, "..", ".."),
  },
  async rewrites() {
    return [
      {
        source: "/api/login/oauth2/code/:path*",
        destination: `${apiUrl}/login/oauth2/code/:path*`,
      },
      {
        source: "/api/oauth2/:path*",
        destination: `${apiUrl}/oauth2/:path*`,
      },
      {
        source: "/api/:path*",
        destination: `${apiUrl}/api/:path*`,
      },
      {
        source: "/uploads/:path*",
        destination: `${apiUrl}/uploads/:path*`,
      },
    ];
  },

  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
        ],
      },
      {
        source: "/api/:path*",
        headers: [
          {
            key: "ngrok-skip-browser-warning",
            value: "69420",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
