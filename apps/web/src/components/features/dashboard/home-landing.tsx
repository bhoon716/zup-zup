"use client";

import { Button } from "@/components/ui/button";
import { motion } from "framer-motion";
import { Bell, Calendar, Search, TrendingUp, Sparkles, ArrowRight } from "lucide-react";
import Link from "next/link";

const features = [
  {
    title: "실시간 빈자리 알림",
    description: "원하는 과목의 여석이 생기는 즉시 FCM, 웹 푸시, 이메일로 알림을 보냅니다.",
    icon: Bell,
    color: "text-blue-500",
    bgColor: "bg-blue-500/10",
  },
  {
    title: "스마트 시간표 관리",
    description: "직관적인 인터페이스로 나만의 시간표를 짜고 대표 시간표를 관리하세요.",
    icon: Calendar,
    color: "text-purple-500",
    bgColor: "bg-purple-500/10",
  },
  {
    title: "정밀 강좌 검색",
    description: "학점, 시수, 요일, 교양 영역 등 모든 조건을 아우르는 강력한 필터링.",
    icon: Search,
    color: "text-green-500",
    bgColor: "bg-green-500/10",
  },
  {
    title: "여석 변동 이력",
    description: "과거 여석 데이터를 시각화하여 수강신청 확률을 예측할 수 있습니다.",
    icon: TrendingUp,
    color: "text-orange-500",
    bgColor: "bg-orange-500/10",
  },
];

export function HomeLanding() {
  return (
    <div className="flex flex-col overflow-hidden">
      {/* Hero Section */}
      <section className="relative bg-white pt-20 pb-32 overflow-hidden min-h-[calc(100vh-64px)] flex items-center">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col items-center text-center relative z-10">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="inline-flex items-center px-4 py-1.5 rounded-full bg-primary/5 text-primary border border-primary/10 mb-8 text-xs font-bold uppercase tracking-wider shadow-sm"
          >
            <Sparkles className="w-3.5 h-3.5 mr-1.5" />
            MORE INTELLIGENT SUGANG SYSTEM
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="text-4xl md:text-6xl font-black tracking-tight text-gray-900 mb-8 leading-[1.2]"
          >
            수강신청 빈자리,<br/>
            <span className="gradient-text">이제 알림으로 잡으세요.</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="mt-2 max-w-2xl text-lg text-gray-500 mb-14 leading-relaxed px-4"
          >
            전북대학교 오아시스 시스템을 실시간 모니터링하여 여석 발생 시<br className="hidden sm:inline"/>
            가장 빠르게 알려드립니다. 반복되는 수동 조회는 이제 그만.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="flex flex-col sm:flex-row gap-4 w-full justify-center items-center px-6"
          >
            <Link href="/login" className="w-full sm:w-auto">
              <Button className="w-full inline-flex justify-center items-center px-12 py-5 text-base font-bold rounded-full text-white bg-primary hover:bg-primary-dark transition-all shadow-lg hover:shadow-primary/30 group h-auto">
                  지금 시작하기<ArrowRight className="ml-1.5 w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </Button>
            </Link>
            <Link href="/search" className="w-full sm:w-auto">
              <Button variant="ghost" className="w-full inline-flex justify-center items-center px-12 py-5 text-base font-medium rounded-full text-gray-700 bg-white border border-transparent hover:bg-gray-50 transition-colors h-auto">
                  강의 먼저 둘러보기
              </Button>
            </Link>
          </motion.div>
        </div>

        {/* Floating Decorative Elements */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 -z-0 w-full h-[500px] opacity-10 pointer-events-none">
          <div className="absolute top-0 right-1/4 w-72 h-72 bg-primary blur-[120px] rounded-full animate-pulse" />
          <div className="absolute bottom-0 left-1/4 w-72 h-72 bg-indigo-500 blur-[120px] rounded-full animate-pulse" style={{ animationDelay: '2s' }} />
        </div>
      </section>

      {/* Features Bento Grid */}
      <section className="py-24 bg-surface-light border-t border-gray-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16 space-y-4">
            <h2 className="text-3xl font-extrabold text-gray-900 mb-4">핵심 기능</h2>
            <p className="text-gray-500 font-medium tracking-tight">수강신청에 꼭 필요한 4가지 기능을 제공합니다.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, idx) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: idx * 0.1 }}
              >
                <div className="bg-white rounded-3xl p-8 shadow-card hover:shadow-soft transition-all duration-300 transform hover:-translate-y-1 border border-gray-50 h-full group">
                  <div className={`w-12 h-12 ${feature.bgColor} rounded-2xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-300`}>
                    <feature.icon className={`w-6 h-6 ${feature.color}`} />
                  </div>
                  <h3 className="text-lg font-bold text-gray-900 mb-3">{feature.title}</h3>
                  <p className="text-sm text-gray-500 leading-relaxed font-medium">
                    {feature.description}
                  </p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

    </div>
  );
}
