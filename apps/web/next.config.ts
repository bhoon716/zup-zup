import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      {
        source: "/api/login/oauth2/code/:path*",
        destination: `${process.env.API_URL}/login/oauth2/code/:path*`,
      },
      {
        source: "/api/oauth2/:path*",
        destination: `${process.env.API_URL}/oauth2/:path*`,
      },
      {
        source: "/api/:path*",
        destination: `${process.env.API_URL}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
