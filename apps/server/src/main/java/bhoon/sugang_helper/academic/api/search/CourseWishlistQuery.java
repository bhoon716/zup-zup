package bhoon.sugang_helper.academic.api.search;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;

public interface CourseWishlistQuery {

    BooleanExpression wishedBy(StringExpression courseKey, Long userId);

    NumberExpression<Long> popularity(StringExpression courseKey);
}
