"use client";

import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { UserCircle, Trash2, Save, AlertTriangle } from "lucide-react";
import { useUser, useUpdateProfile, useWithdraw } from "@/hooks/useUser";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";

export function AccountSettings() {
  const { data: user } = useUser();
  const { mutate: updateProfile, isPending: isUpdating } = useUpdateProfile();
  const { mutate: withdraw, isPending: isWithdrawing } = useWithdraw();

  const [name, setName] = useState(user?.name || "");
  const [prevUserId, setPrevUserId] = useState(user?.id);

  if (user?.id !== prevUserId) {
    setPrevUserId(user?.id);
    setName(user?.name || "");
  }


  const handleUpdateProfile = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    updateProfile({ name });
  };

  const handleWithdraw = () => {
    withdraw();
  };

  return (
    <div className="space-y-6 max-w-2xl mx-auto">
      {/* 프로필 수정 카드 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <UserCircle className="w-6 h-6 text-blue-600" />
            프로필 설정
          </CardTitle>
          <CardDescription>
            사용자 정보를 관리합니다.
          </CardDescription>
        </CardHeader>
        <form onSubmit={handleUpdateProfile}>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">이메일</Label>
              <Input id="email" value={user?.email || ""} disabled className="bg-gray-50" />
              <p className="text-xs text-muted-foreground">이메일은 변경할 수 없습니다.</p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="name">이름</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="이름을 입력하세요"
                required
              />
            </div>
          </CardContent>
          <CardFooter className="justify-end border-t pt-6">
            <Button type="submit" disabled={isUpdating || name === user?.name} className="gap-2">
              <Save className="w-4 h-4" />
              변경 사항 저장
            </Button>
          </CardFooter>
        </form>
      </Card>

      {/* 회원 탈퇴 카드 */}
      <Card className="border-red-100 overflow-hidden">
        <CardHeader className="bg-red-50/50">
          <CardTitle className="flex items-center gap-2 text-red-700">
            <Trash2 className="w-5 h-5" />
            계정 삭제
          </CardTitle>
          <CardDescription className="text-red-600/80">
            계정을 삭제하면 모든 구독 정보와 알림 내역이 영구적으로 제거됩니다.
          </CardDescription>
        </CardHeader>
        <CardContent className="pt-6">
          <div className="flex items-start gap-3 p-4 bg-red-50 rounded-lg text-red-800 text-sm">
            <AlertTriangle className="w-5 h-5 shrink-0 mt-0.5" />
            <p>
              회원 탈퇴 시 현재 구독 중인 모든 강의의 공석 알림 서비스가 중단됩니다. 
              이 작업은 되돌릴 수 없으니 주의하시기 바랍니다.
            </p>
          </div>
        </CardContent>
        <CardFooter className="justify-end bg-red-50/20 border-t pt-6">
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="destructive" className="gap-2">
                회원 탈퇴
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>정말 탈퇴하시겠습니까?</AlertDialogTitle>
                <AlertDialogDescription>
                  탈퇴 시 모든 데이터가 즉시 삭제되며 복구할 수 없습니다. 
                  그래도 진행하시겠습니까?
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>취소</AlertDialogCancel>
                <AlertDialogAction
                  onClick={handleWithdraw}
                  className="bg-red-600 hover:bg-red-700"
                  disabled={isWithdrawing}
                >
                  탈퇴하기
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </CardFooter>
      </Card>
    </div>
  );
}
