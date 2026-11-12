import { ChevronRight } from 'lucide-react';
import { WEEK_DAYS, type RenderingBlock } from '@/features/timetable/lib/timetable';
import type { OpenTimetableBlock } from './timetable-block';

interface TimetableAgendaProps {
  dayBlocks: ReadonlyMap<string, RenderingBlock[]>;
  onOpen: OpenTimetableBlock;
}

export function TimetableAgenda({ dayBlocks, onOpen }: TimetableAgendaProps) {
  const scheduledDays = WEEK_DAYS.filter((day) => dayBlocks.get(day)?.length);

  if (scheduledDays.length === 0) {
    return <p className="p-6 text-center text-sm text-muted-foreground">등록된 일정이 없습니다.</p>;
  }

  return (
    <div className="space-y-4 p-3 sm:p-4">
      {scheduledDays.map((day) => (
        <section key={day}>
          <h2 className="mb-1 px-1 text-sm font-semibold text-foreground">{day}요일</h2>
          <ul className="divide-y divide-border rounded-lg border border-border">
            {dayBlocks.get(day)?.map((block) => (
              <li key={block.key}>
                <button
                  type="button"
                  aria-haspopup="dialog"
                  onClick={(event) => onOpen(block, event.currentTarget)}
                  className="flex w-full cursor-pointer items-center gap-2.5 rounded-lg px-3 py-2.5 text-left hover:bg-muted focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-ring sm:gap-3"
                >
                  <span
                    aria-hidden="true"
                    className="size-2 shrink-0 rounded-full"
                    style={{ backgroundColor: block.color }}
                  />
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm tabular-nums text-muted-foreground">
                      {block.startTime.slice(0, 5)}–{block.endTime.slice(0, 5)}
                    </span>
                    <span className="block text-sm leading-5 font-semibold wrap-anywhere text-foreground">
                      {block.title}
                    </span>
                    {(block.subTitle || block.classroom) && (
                      <span className="block text-sm wrap-anywhere text-muted-foreground">
                        {[block.subTitle, block.classroom].filter(Boolean).join(' · ')}
                      </span>
                    )}
                  </span>
                  <ChevronRight aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
                </button>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
