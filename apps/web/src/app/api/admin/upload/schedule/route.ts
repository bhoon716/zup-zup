import { NextResponse } from "next/server";
import fs from "fs";
import path from "path";

/**
 * 수강신청 일정 이미지 업로드 API
 * @param request
 * @returns
 */
export async function POST(request: Request) {
  try {
    const formData = await request.formData();
    const file = formData.get("file") as File;

    if (!file) {
      return NextResponse.json({ error: "No file uploaded" }, { status: 400 });
    }

    // 파일 크기 검증 (5MB 제한)
    const MAX_SIZE = 5 * 1024 * 1024;
    if (file.size > MAX_SIZE) {
      return NextResponse.json({ error: "File size exceeds 5MB limit" }, { status: 400 });
    }

    // 파일 타입 검증 (이미지만 허용)
    const ALLOWED_TYPES = ["image/png", "image/jpeg", "image/jpg", "image/webp"];
    if (!ALLOWED_TYPES.includes(file.type)) {
      return NextResponse.json({ error: "Only image files (png, jpeg, webp) are allowed" }, { status: 400 });
    }

    const buffer = Buffer.from(await file.arrayBuffer());
    const uploadDir = path.join(process.cwd(), "public", "uploads");

    // 디렉토리 존재 확인 및 생성
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }

    const filePath = path.join(uploadDir, "schedule.png");
    fs.writeFileSync(filePath, buffer);

    return NextResponse.json({ success: true, url: "/uploads/schedule.png" });
  } catch (error) {
    console.error("Upload failed:", error);
    return NextResponse.json({ error: "Upload failed" }, { status: 500 });
  }
}
