
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";


/**
 * CourseTableSkeleton Component
 * 
 * 역할: 데이터 로딩 시 사용자에게 보여지는 테이블 로딩 플레이스홀더(Loading Placeholder).
 * 특징: 깜빡임(Flicker) 현상을 방지하고 체감 속도를 높이기 위해 실제 테이블과 동일한 레이아웃을 가짐.
 * 차이점: TimeTableSelector는 '검색 조건 입력'을 위한 UI이고, 이 컴포넌트는 '로딩 상태'를 위한 UI임.
 */
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
