"use client";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Chromium } from "lucide-react";

export function LoginCard() {
  const handleGoogleLogin = () => {
    // Redirect to Backend OAuth2 Endpoint
    const backendUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
    window.location.href = `${backendUrl}/oauth2/authorization/google`;
  };

  return (
    <Card className="w-full max-w-md mx-auto shadow-lg">
      <CardHeader className="text-center space-y-2">
        <CardTitle className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          환영합니다
        </CardTitle>
        <CardDescription>
          JBNU 수강신청 빈자리 알림 서비스입니다. <br />
          서비스 이용을 위해 로그인을 진행해 주세요.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <Button
          variant="outline"
          className="w-full flex items-center justify-center gap-2 py-6 text-base font-medium transition-all hover:bg-gray-50 dark:hover:bg-gray-800"
          onClick={handleGoogleLogin}
        >
          <Chromium className="w-5 h-5 text-red-500" />
          Google 계정으로 로그인 (학교 계정 권장)
        </Button>
        <p className="text-xs text-center text-gray-500">
          로그인 시 서비스 이용약관 및 개인정보 처리방침에 동의하게 됩니다.
        </p>
      </CardContent>
    </Card>
  );
}
