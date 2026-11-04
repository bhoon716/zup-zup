# 운영 웹 글꼴 빌드 실패 복구

## 문제와 범위

2026-10-27 운영 Vercel 배포 `dpl_EortRnSQ3fRtKPx5BvVTJnTKXCuy`는 main `49b8a4fae7bcb011e124d8168361b4dbf3bc0ec5`에서 실패했다. 실제 Build Logs에는 `next/font/google queries have exactly one entry`와 Turbopack 내부 Google Font 모듈 해석 오류가 반복됐다. 같은 커밋의 로컬·GitHub CI·Vercel Preview 빌드는 성공했다. 기존 정상 웹은 계속 서비스됐으며 서버 CD는 별개로 성공했다. 취소된 PWA 설치 도우미는 이 릴리스에 포함되지 않았다.

사용자는 실패 원인 확인, 관련 범위의 수정·검증·정상 배포와 마지막 브라우저 확인을 요청했다. 기능이나 인증·마스킹 정책은 변경하지 않는다.

## 근거와 대안

[Next.js 공식 이슈 #99114](https://github.com/vercel/next.js/issues/99114)는 Google Fonts의 확장자 없는 URL과 여러 query parameter를 처리하는 실패를 설명한다. 현재 사용 버전 `16.3.0-preview.5`를 이용한 독립 fixture에서도 Google CSS mock의 `&`가 들어간 URL로 운영과 같은 오류를 재현했다. 운영에서 받은 CSS 원문 전체를 확보한 것은 아니므로 운영 응답의 세부 URL까지 동일하다고 단정하지 않는다. 문제는 실제 운영 로그와 독립 재현으로 확인한 Google Font 빌드 경로다.

- 같은 SHA 재시도: 코드 변화가 없지만 외부 응답에 따라 재발할 수 있다. UI 재배포는 이메일 재인증이 필요해 실행되지 않았고 인증 메일도 보내지 않았다.
- Webpack 전환 또는 Next 업그레이드: 번들러 전환만으로 보장할 수 없고, 라이브러리 업그레이드는 이번 복구보다 넓은 검증이 필요하다.
- 기존 글꼴을 로컬 asset으로 제공: 자산 크기와 라이선스 보관 비용을 수용하고 빌드 중 Google Fonts 응답 의존성을 제거한다. 새 라이브러리·서비스·자격 증명이 필요 없다.

## 선택

기존 Noto Sans KR와 Geist Mono를 `next/font/local`로 읽는다. Google Fonts 공식 저장소 고정 커밋 `ddf386ba1b9fbc6b2c818f1ddcd9148f54ced94b`의 원본 가변 TTF를 WOFF2로 변환했으며, 글리프 수·Unicode cmap·가변 굵기 축을 원본과 대조한다. subsetting하지 않는다. 원본 OFL 1.1 고지를 각 글꼴 옆에 보존한다. Noto Sans KR는 약 3.7MiB, Geist Mono는 약 69KiB로, 기존 Google CSS의 Unicode 분할 다운로드보다 첫 방문 전송량이 커질 수 있다. 이번에는 글리프 축소나 새 폰트 패키지를 추가하지 않고 신뢰성을 우선한다. 향후 glyph 범위별 정적 분할은 별도 성능 검토 사항이다.

기존 CSS 변수명, display swap, preload false, 정상체를 유지한다. 전체 100–900 가변 굵기는 기존 사용 굵기 400/500/700/900을 포함한다. 브라우저는 동일 출처의 정적 글꼴 파일을 읽으며 새로운 외부 폰트 요청을 하지 않는다. `body data-clarity-mask`, 사용자 입력 보호, API rewrite, 앱 기능은 유지한다. Next/React/lockfile 버전과 build 명령은 변경하지 않는다.

## 검증과 복구

검증 대상은 원본/변환 font cmap·glyph·weight 축 보존, 전체 웹 unit/type/lint/production build, 기존 브라우저 E2E 및 운영 글꼴 로딩·한국어 화면 표시다. Google Font CSS mock을 실패하도록 해도 로컬 글꼴 build가 성공해야 한다. GitHub 필수 CI와 Vercel Production 성공, 실제 도메인 표시를 각각 확인한 뒤 완료로 판단한다.

복구 시 이 변경만 되돌릴 수 있으나, Google Font 빌드 실패가 재발할 수 있다. 웹을 이전 정상 배포로 복원하는 것과 이미 성공한 서버 릴리스를 되돌리는 것은 별개다. 운영 DB migration·보안 설정·새 인증 토큰은 변경하지 않는다.

## 로컬 복구 검증 결과

- 웹 전체 321개 unit test, lint, TypeScript 검사 통과.
- Google Font CSS mock이 예외를 던지도록 한 production build 통과. sandbox 내부 포트 제한으로 실패한 최초 빌드는 보존하고, worker 포트 바인딩이 허용된 로컬 실행 결과로 확인했다.
- 기존 Playwright E2E 9개 통과, 재시도 0. 외부 요청과 service worker는 테스트 설정으로 차단하며 API mock만 사용했다. 실제 OAuth/알림 발송/사용자 데이터 쓰기는 없었다.
- 원본과 변환 파일의 Noto Sans KR 23,174개 Unicode codepoint·24,964 glyph, Geist Mono 889 codepoint·1,159 glyph 및 양쪽 100–900 weight 축이 일치한다. WOFF2 해시는 각각 `f0d364907533aacbf8c67ca22138afd277198466351a48c6b01c7501966564a2`, `623f6209747b8390112ca80d37a8177892ea7caf694f1d23169b8d43b0f95ab6`이다.
- 기존 dirty 문서 3개, Clarity body mask, 취소된 PWA 보관물을 보존했다. 운영 웹 성공과 최종 화면 검수는 배포 후 별도 확인한다.
