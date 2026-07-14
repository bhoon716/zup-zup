# [P2][server/web] 피드백 이미지 업로드에서 WebP 비허용

## 문제

현재 이미지 업로드는 피드백(건의/버그 리포트) 첨부파일에만 사용되지만, 웹 클라이언트가 선택한 이미지를 WebP로 변환하고 서버가 TwelveMonkeys WebP ImageIO plugin으로 이를 디코딩한다. 지원 포맷을 JPEG/PNG로 제한하면 업로드 계약과 이미지 decoder 의존성을 단순화할 수 있다.

## 완료 기준

- 피드백 multipart 업로드가 JPEG/PNG만 허용하고 WebP는 `400` 계열 오류로 거부한다.
- 웹 file input과 압축 결과가 JPEG/PNG 계약을 따른다.
- TwelveMonkeys WebP dependency, startup 검증, WebP 전용 분기와 테스트/문서를 제거한다.

## 근거

- `apps/web/src/shared/lib/image.ts`
- `apps/web/src/features/feedback/components/feedback-create-form.tsx`
- `apps/server/src/main/java/bhoon/sugang_helper/common/util/LocalFileUploadService.java`
- `apps/server/src/main/java/bhoon/sugang_helper/common/util/ImageUploadSanitizer.java`
