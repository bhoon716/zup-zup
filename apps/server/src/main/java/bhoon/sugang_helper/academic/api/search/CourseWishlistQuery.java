package bhoon.sugang_helper.academic.api.search;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;

/**
 * 검색이 필요로 하는 찜 조건의 확장 계약. planning이 자기 테이블의 쿼리를 구성한다.
 * 엔티티·Q 타입·테이블 이름은 노출하지 않고 기존 DB 정렬·페이지 처리를 유지한다.
 */
public interface CourseWishlistQuery {

    BooleanExpression wishedBy(StringExpression courseKey, Long userId);

    NumberExpression<Long> popularity(StringExpression courseKey);
}
