"use client";

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from "chart.js";
import { Line } from "react-chartjs-2";
import type { CourseSeatHistory } from "@/types/api";
import { format } from "date-fns";
import { ko } from "date-fns/locale";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

interface CourseHistoryChartProps {
  histories: CourseSeatHistory[];
}

export function CourseHistoryChart({ histories }: CourseHistoryChartProps) {
  const sortedHistories = [...histories].sort(
    (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  );

  const data = {
    labels: sortedHistories.map((h) => format(new Date(h.createdAt), "MM/dd HH:mm", { locale: ko })),
    datasets: [
      {
        label: "신청 인원",
        data: sortedHistories.map((h) => h.currentSeats),
        borderColor: "rgb(59, 130, 246)",
        backgroundColor: "rgba(59, 130, 246, 0.1)",
        fill: true,
        tension: 0.4,
      },
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: {
        position: "top" as const,
      },
      tooltip: {
        mode: "index" as const,
        intersect: false,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          stepSize: 1,
        },
      },
    },
    interaction: {
      mode: "nearest" as const,
      axis: "x" as const,
      intersect: false,
    },
  };

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle className="text-lg">인원 변동 추이</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-[300px] w-full">
          <Line data={data} options={options} />
        </div>
      </CardContent>
    </Card>
  );
}
