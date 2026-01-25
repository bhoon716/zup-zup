import { AdminGuard } from "@/components/auth/admin-guard";
import { Header } from "@/components/layout/header";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AdminGuard>
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <Header />
        <div className="flex">
          {/* Admin Sidebar could go here */}
          <main className="flex-1 p-8">{children}</main>
        </div>
      </div>
    </AdminGuard>
  );
}
