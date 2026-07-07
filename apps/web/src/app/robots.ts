import type { MetadataRoute } from "next";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: "*",
        allow: "/",
        disallow: ["/admin", "/settings"],
      },
      {
        userAgent: ["GPTBot", "Google-Extended", "ClaudeBot"],
        disallow: ["/"],
      },
    ],
    sitemap: "https://zup-zup.com/sitemap.xml",
  };
}
