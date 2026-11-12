import { z } from "zod";
import api from "@/shared/api/client";

const inboxPath = "/api/v1/notifications/inbox";
const localDateTime = z.iso.datetime({ local: true });

const inboxItemBaseSchema = z.object({
  id: z.number().int().positive(),
  occurredAt: localDateTime,
  readAt: localDateTime.nullable(),
});

const seatInboxItemSchema = inboxItemBaseSchema.extend({
  type: z.literal("SEAT_OPENED"),
  courseKey: z.string(),
  courseName: z.string(),
  availableSeats: z.number().int().nonnegative(),
});

const messageInboxItemSchema = inboxItemBaseSchema.extend({
  type: z.literal(["FEEDBACK_REPLY", "ANNOUNCEMENT", "DELIVERY_FAILED", "DEVICE_REREGISTRATION"]),
  courseName: z.string().nullish(),
  title: z.string().nullable(),
  message: z.string().nullable(),
  targetId: z.number().int().positive().nullable(),
  available: z.boolean(),
});

const inboxItemSchema = z.discriminatedUnion("type", [seatInboxItemSchema, messageInboxItemSchema]);

export const inboxTypeFilterSchema = z.union([
  z.literal("ALL"),
  seatInboxItemSchema.shape.type,
  messageInboxItemSchema.shape.type,
]);

export const inboxSortSchema = z.enum(["NEWEST", "OLDEST", "UNREAD_FIRST"]);

const inboxPageSchema = z.object({
  content: z.array(inboxItemSchema),
  number: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
  totalElements: z.number().int().nonnegative(),
});

export type InboxItem = z.infer<typeof inboxItemSchema>;
export type InboxPage = z.infer<typeof inboxPageSchema>;
export type InboxReadFilter = "ALL" | "UNREAD" | "READ";

export interface InboxSelection {
  page: number;
  read: InboxReadFilter;
  type: z.infer<typeof inboxTypeFilterSchema>;
  keyword: string;
  sort: z.infer<typeof inboxSortSchema>;
}

export type InboxCommand =
  | { kind: "read"; id: InboxItem["id"] }
  | { kind: "readAll" }
  | { kind: "delete"; id: InboxItem["id"] };

export async function getInbox({ page, read, type, keyword, sort }: InboxSelection, signal: AbortSignal): Promise<InboxPage> {
  const { data } = await api.get<unknown>(inboxPath, {
    params: {
      page,
      read: read === "ALL" ? undefined : read,
      type: type === "ALL" ? undefined : type,
      keyword: keyword || undefined,
      sort,
    },
    signal,
  });
  return inboxPageSchema.parse(data);
}

export async function updateInbox(command: InboxCommand): Promise<void> {
  switch (command.kind) {
    case "read":
      await api.patch(`${inboxPath}/${command.id}/read`);
      break;
    case "readAll":
      await api.patch(`${inboxPath}/read-all`);
      break;
    case "delete":
      await api.delete(`${inboxPath}/${command.id}`);
      break;
  }
}
