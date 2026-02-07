import { AdminGuard } from "@/components/auth/admin-guard";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AdminGuard>
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <div className="flex">
          <main className="container py-10">{children}</main>
        </div>
      </div>
    </AdminGuard>
  );
}
