import { Header } from "@/components/layout/header";
import { NotificationSettings } from "@/components/features/settings/notification-settings";
import { AccountSettings } from "@/components/features/settings/account-settings";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Bell, User } from "lucide-react";

export default function SettingsPage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      <main className="container py-8">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-3xl font-bold mb-8">설정</h1>
          
          <Tabs defaultValue="notification" className="space-y-6">
            <TabsList className="grid w-full grid-cols-2 max-w-[400px]">
              <TabsTrigger value="notification" className="gap-2">
                <Bell className="w-4 h-4" />
                알림 설정
              </TabsTrigger>
              <TabsTrigger value="account" className="gap-2">
                <User className="w-4 h-4" />
                계정 관리
              </TabsTrigger>
            </TabsList>
            
            <TabsContent value="notification">
              <NotificationSettings />
            </TabsContent>
            
            <TabsContent value="account">
              <AccountSettings />
            </TabsContent>
          </Tabs>
        </div>
      </main>
    </div>
  );
}
