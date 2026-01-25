
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

export function CourseTableSkeleton() {
  return (
    <div className="rounded-2xl border border-white/10 bg-card/50 backdrop-blur-xl shadow-2xl overflow-hidden animate-pulse">
      <Table>
        <TableHeader className="bg-muted/30">
          <TableRow className="border-white/5 hover:bg-transparent">
            {/* Headers Skeleton */}
            {[...Array(6)].map((_, i) => (
              <TableHead key={i}>
                <div className="h-4 bg-muted/50 rounded w-20"></div>
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {/* Rows Skeleton */}
          {[...Array(10)].map((_, i) => (
            <TableRow key={i} className="border-white/5">
              {[...Array(6)].map((_, j) => (
                <TableCell key={j}>
                  <div className="h-8 bg-muted/40 rounded-lg"></div>
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
