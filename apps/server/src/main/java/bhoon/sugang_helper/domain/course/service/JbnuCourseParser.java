package bhoon.sugang_helper.domain.course.service;

import bhoon.sugang_helper.domain.course.entity.Course;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class JbnuCourseParser {

    private static final String DATASET_ID = "GRD_COUR001";

    public List<Course> parseCourses(String xmlData) {
        List<Course> courseList = new ArrayList<>();
        Document doc = Jsoup.parse(xmlData, "", Parser.xmlParser());

        Elements rows = doc.select("Dataset[id=" + DATASET_ID + "] > Rows > Row");

        for (Element row : rows) {
            try {
                processRow(row).ifPresent(courseList::add);
            } catch (Exception e) {
                log.warn("Failed to parse row: {}", e.getMessage());
            }
        }
        return courseList;
    }

    private Optional<Course> processRow(Element row) {
        String sbjtCd = getColValue(row, "SBJTCD");
        String clss = getColValue(row, "CLSS");
        String sbjtNm = getColValue(row, "SBJTNM");
        String profNm = getColValue(row, "RPSTPROFNM");
        String targetGrade = getColValue(row, "UN_SHT_NM"); // 학년 정보 (예: 1학년)
        int lmtrCnt = safeParseInt(getColValue(row, "LMTRCNT"));
        int tlsnrCnt = safeParseInt(getColValue(row, "TLSNRCNT"));

        if (sbjtCd == null || clss == null) {
            return Optional.empty();
        }

        return Optional.of(Course.builder()
                .courseKey(sbjtCd + "-" + clss)
                .subjectCode(sbjtCd)
                .classNumber(clss)
                .name(sbjtNm)
                .professor(profNm)
                .capacity(lmtrCnt)
                .current(tlsnrCnt)
                .targetGrade(targetGrade)
                .build());
    }

    private String getColValue(Element row, String colId) {
        Element col = row.selectFirst("Col[id=" + colId + "]");
        return col != null ? col.text() : null;
    }

    private int safeParseInt(String value) {
        try {
            return value != null && !value.isEmpty() ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
