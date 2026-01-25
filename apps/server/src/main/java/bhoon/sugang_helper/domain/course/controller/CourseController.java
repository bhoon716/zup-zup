package bhoon.sugang_helper.domain.course.controller;

import bhoon.sugang_helper.common.response.CommonResponse;
import bhoon.sugang_helper.domain.course.request.CourseSearchCondition;
import bhoon.sugang_helper.domain.course.response.CourseResponse;
import bhoon.sugang_helper.domain.course.response.CourseSeatHistoryResponse;
import bhoon.sugang_helper.domain.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course", description = "과목 관련 API")
public class CourseController {

  private final CourseService courseService;

  @Operation(summary = "과목 검색", description = "검색 조건에 맞는 과목 목록을 전체 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "과목 검색 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "과목 검색 결과입니다.",
            "data": {
              "content": [
                {
                  "courseKey": "CLTR.0031-01",
                  "subjectCode": "CLTR.0031",
                  "name": "기초프로그래밍",
                  "professorName": "홍길동",
                  "targetGrade": "1",
                  "totalSeats": 40,
                  "currentSeats": 35,
                  "status": "AVAILABLE"
                }
              ],
              "pageable": { ... },
              "totalElements": 1,
              "totalPages": 1
            }
          }
          """)))
  })
  @GetMapping
  public ResponseEntity<CommonResponse<List<CourseResponse>>> searchCourses(
      CourseSearchCondition condition) {
    List<CourseResponse> courses = courseService.searchCourses(condition);
    return CommonResponse.ok(courses, "과목 검색 결과입니다.");
  }

  @Operation(summary = "과목 공석 변동 이력 조회", description = "특정 과목의 공석 변동 이력을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "이력 조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "해당 과목의 인원 변동 이력입니다.",
            "data": [
              {
                "id": 1,
                "courseKey": "CLTR.0031-01",
                "currentSeats": 35,
                "changedSeats": -2,
                "createdAt": "2024-01-01T12:00:00"
              }
            ]
          }
          """)))
  })
  @GetMapping("/{courseKey}/history")
  public ResponseEntity<CommonResponse<List<CourseSeatHistoryResponse>>> getCourseHistory(
      @PathVariable String courseKey) {
    List<CourseSeatHistoryResponse> histories = courseService.getCourseHistory(courseKey);
    return CommonResponse.ok(histories, "해당 과목의 인원 변동 이력입니다.");
  }

  @Operation(summary = "과목 상세 조회", description = "특정 과목의 상세 정보를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "과목 상세 조회 성공", content = @Content(schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(value = """
          {
            "code": "SUCCESS",
            "message": "과목 상세 정보입니다.",
            "data": {
              "courseKey": "CLTR.0031-01",
              "subjectCode": "CLTR.0031",
              "name": "기초프로그래밍",
              "classNumber": "01",
              "professor": "홍길동",
              "capacity": 40,
              "current": 35,
              "available": 5
            }
          }
          """)))
  })
  @GetMapping("/{courseKey}")
  public ResponseEntity<CommonResponse<CourseResponse>> getCourse(
      @PathVariable String courseKey) {
    CourseResponse course = courseService.getCourse(courseKey);
    return CommonResponse.ok(course, "과목 상세 정보입니다.");
  }
}
