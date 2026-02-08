"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { motion } from "framer-motion";
import { Bell, Calendar, Search, TrendingUp, Sparkles, ArrowRight, ShieldCheck, Mail, Smartphone } from "lucide-react";
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
    <div className="flex flex-col gap-24 pb-12 md:pb-20 overflow-hidden">
      {/* Hero Section */}
      <section className="relative text-center space-y-8 px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/5 border border-primary/20 text-primary text-xs font-bold uppercase tracking-widest shadow-sm"
        >
          <Sparkles className="w-3.5 h-3.5" />
          More Intelligent Sugang System
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
          className="text-4xl md:text-7xl font-black tracking-tight leading-[1.2] md:leading-[1.1]"
        >
          수강신청 빈자리, <br />
          <span className="bg-gradient-to-r from-primary via-purple-500 to-indigo-500 bg-clip-text text-transparent">
            이제 알림으로 잡으세요.
          </span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="max-w-2xl mx-auto text-muted-foreground text-base md:text-xl font-medium leading-relaxed px-2"
        >
          전북대학교 오아시스 시스템을 실시간 모니터링하여 여석 발생 시 <br className="hidden md:block" />
          가장 빠르게 알려드립니다. 반복되는 수동 조회는 이제 그만.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4 px-6"
        >
          <Link href="/login" className="w-full sm:w-auto">
            <Button size="lg" className="w-full rounded-2xl px-10 h-14 text-lg font-bold shadow-2xl shadow-primary/30 hover:shadow-primary/50 transition-all hover:-translate-y-1 bg-primary text-white group">
              지금 시작하기
              <ArrowRight className="ml-2 w-5 h-5 group-hover:translate-x-1 transition-transform" />
            </Button>
          </Link>
          <Link href="/search" className="w-full sm:w-auto">
            <Button variant="ghost" size="lg" className="w-full rounded-2xl px-10 h-14 text-lg font-bold hover:bg-white/50 backdrop-blur-sm transition-all">
              강의 먼저 둘러보기
            </Button>
          </Link>
        </motion.div>

        {/* Floating Decorative Elements */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 -z-10 w-full h-[500px] opacity-20 pointer-events-none">
          <div className="absolute top-0 right-1/4 w-72 h-72 bg-primary blur-[120px] rounded-full animate-pulse" />
          <div className="absolute bottom-0 left-1/4 w-72 h-72 bg-indigo-500 blur-[120px] rounded-full animate-pulse" style={{ animationDelay: '2s' }} />
        </div>
      </section>

      {/* Features Bento Grid */}
      <section className="container max-w-6xl px-4">
        <div className="text-center mb-16 space-y-4">
          <h2 className="text-3xl md:text-4xl font-black tracking-tight">당신의 수강신청을 쾌적하게</h2>
          <p className="text-muted-foreground font-medium">단순하지만 강력한 기능들로 수강신청의 고통을 덜어드립니다.</p>
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
              <Card className="h-full border-none bg-white/40 dark:bg-white/5 backdrop-blur-md shadow-xl hover:shadow-2xl transition-all duration-500 group overflow-hidden">
                <div className={`absolute inset-0 bg-gradient-to-br from-white/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500`} />
                <CardContent className="p-8 relative space-y-6">
                  <div className={`w-14 h-14 rounded-2xl ${feature.bgColor} flex items-center justify-center group-hover:scale-110 transition-transform duration-500 shadow-inner`}>
                    <feature.icon className={`w-7 h-7 ${feature.color}`} />
                  </div>
                  <div className="space-y-2">
                    <h3 className="text-xl font-bold tracking-tight">{feature.title}</h3>
                    <p className="text-sm text-muted-foreground leading-relaxed font-medium">
                      {feature.description}
                    </p>
                  </div>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Trust Section */}
      <section className="bg-white/30 dark:bg-white/5 backdrop-blur-lg border-y border-white/10 py-20 px-4">
        <div className="container max-w-5xl">
          <div className="grid md:grid-cols-3 gap-12 text-center">
            <div className="space-y-4">
              <div className="mx-auto w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center text-primary">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h4 className="text-lg font-bold">오아시스 연동</h4>
              <p className="text-sm text-muted-foreground font-medium leading-relaxed">학교 공식 데이터를 정밀 크롤링하여 <br /> 정확한 정보를 실시간 제공합니다.</p>
            </div>
            <div className="space-y-4">
              <div className="mx-auto w-12 h-12 bg-blue-500/10 rounded-full flex items-center justify-center text-blue-500">
                <Mail className="w-6 h-6" />
              </div>
              <h4 className="text-lg font-bold">다채로운 채널</h4>
              <p className="text-sm text-muted-foreground font-medium leading-relaxed">이메일부터 웹 푸시까지, <br /> 당신이 선호하는 채널로 알림을 보냅니다.</p>
            </div>
            <div className="space-y-4">
              <div className="mx-auto w-12 h-12 bg-green-500/10 rounded-full flex items-center justify-center text-green-500">
                <Smartphone className="w-6 h-6" />
              </div>
              <h4 className="text-lg font-bold">언제 어디서나</h4>
              <p className="text-sm text-muted-foreground font-medium leading-relaxed">PWA 지원으로 PC와 모바일 모두에서 <br /> 앱처럼 편리하게 이용하세요.</p>
            </div>
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="container max-w-4xl px-4 pb-20">
        <Card className="relative overflow-hidden border-none bg-gradient-to-br from-primary to-indigo-600 p-12 text-center text-white shadow-3xl rounded-[3rem]">
          <div className="absolute top-0 right-0 -mr-20 -mt-20 w-80 h-80 bg-white/10 blur-[100px] rounded-full" />
          <div className="absolute bottom-0 left-0 -ml-20 -mb-20 w-80 h-80 bg-black/10 blur-[100px] rounded-full" />
          
          <CardContent className="space-y-8 relative">
            <h2 className="text-3xl md:text-5xl font-black tracking-tight">완전히 새로운 <br className="md:hidden" /> 수강신청 경험</h2>
            <p className="text-white/80 text-lg font-medium max-w-xl mx-auto">
              지금 구글 계정으로 로그인하고 원하는 강의의 빈자리를 <br className="hidden md:block" /> 
              가장 먼저 알림으로 받아보세요.
            </p>
            <div className="flex justify-center pt-4">
               <Link href="/login">
                  <Button size="lg" variant="secondary" className="rounded-2xl px-12 h-14 text-lg font-black shadow-xl hover:scale-105 active:scale-95 transition-all bg-white text-primary hover:bg-white/90 group">
                    Google로 1초 만에 시작하기
                  </Button>
               </Link>
            </div>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
