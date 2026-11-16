import { describe, expect, it } from "vitest";
import { generateWebsiteJsonLd } from "@/shared/seo/json-ld";

describe("JSON-LD Schema Suite", () => {
  it("generates Website & SoftwareApplication JSON-LD schema", () => {
    const jsonLd = generateWebsiteJsonLd();
    expect(jsonLd["@context"]).toBe("https://schema.org");
    expect(jsonLd["@graph"]).toHaveLength(2);

    const website = jsonLd["@graph"][0];
    expect(website["@type"]).toBe("WebSite");
    expect(website.url).toBe("https://www.zup-zup.com");
    expect(website.image).toBe("https://www.zup-zup.com/icons/zup-zup-512.png");

    const app = jsonLd["@graph"][1];
    expect(app["@type"]).toBe("SoftwareApplication");
    expect(app.name).toBe("줍줍");
  });
});
