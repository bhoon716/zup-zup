import { AdminGuard } from "@/features/admin/components/admin-guard";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AdminGuard>
      <div className="h-screen overflow-hidden bg-[#f3f4f6]">{children}</div>
    </AdminGuard>
  );
}
