"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { Course } from "@/types/api";
import { motion, AnimatePresence } from "framer-motion";

interface CourseDetailDialogProps {
  course: Course | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const staggerContainer = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.05
    }
  }
};

const itemVariants = {
  hidden: { opacity: 0, y: 10 },
  show: { opacity: 1, y: 0 }
};

import { CourseDetailContent } from "./course-detail-content";

interface CourseDetailDialogProps {
  course: Course | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CourseDetailDialog({ course, open, onOpenChange }: CourseDetailDialogProps) {
  if (!course) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-6xl p-0 gap-0 bg-background/95 backdrop-blur-md border border-border shadow-2xl overflow-hidden sm:rounded-2xl h-[90vh]">
        <AnimatePresence mode="wait">
          {open && (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 10 }}
              className="flex flex-col h-full max-h-[90vh] overflow-y-auto"
            >
              <CourseDetailContent course={course} isDialog={true} />
            </motion.div>
          )}
        </AnimatePresence>
        <DialogDescription className="sr-only">
          강좌 상세 정보 - {course.name}
        </DialogDescription>
      </DialogContent>
    </Dialog>
  );
}
