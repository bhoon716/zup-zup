import { AdminGuard } from "@/features/admin/components/admin-guard";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AdminGuard>
      <div className="min-h-[calc(100dvh-4rem)] bg-[#f3f4f6] text-slate-900">
        {children}
      </div>
    </AdminGuard>
  );
}
